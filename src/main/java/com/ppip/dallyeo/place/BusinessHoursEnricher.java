package com.ppip.dallyeo.place;

import com.ppip.dallyeo.common.util.BusinessHoursText;
import com.ppip.dallyeo.external.tourapi.TourApiClient;
import com.ppip.dallyeo.external.tourapi.TourApiProperties;
import com.ppip.dallyeo.external.tourapi.dto.TourIntro;
import com.ppip.dallyeo.place.dto.PlaceSummary;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 목록 응답 항목에 영업시간을 채운다.
 *
 * <p><b>보관소를 먼저 본다</b>({@link BusinessHoursStore}). 한 번 받은 영업시간은 DB에 남기므로
 * 두 번째부터는 TourAPI를 부르지 않는다 — 캐시 TTL로만 버티면 만료될 때마다 전 장소를 다시 받아
 * 일일 호출 한도를 매일 소진한다. 보관분이 없거나 오래된 장소만 아래 병렬 조회로 채운다.
 *
 * <p>TourAPI 목록 오퍼레이션(areaBasedList2/searchKeyword2/locationBasedList2)은 영업시간을 주지 않는다.
 * 영업시간은 detailIntro2에만 있고 <b>항목당 1회 호출</b>이므로, 목록 크기만큼 외부 호출이 늘어난다.
 * 그래서 두 가지 상한을 둔다:
 * <ul>
 *   <li>동시 호출 수({@code tourapi.intro-concurrency}) — TourAPI 쪽에 몰아치지 않도록</li>
 *   <li>총 소요 예산({@code tourapi.intro-budget}) — 목록 응답이 통째로 느려지지 않도록.
 *       예산 안에 못 채운 항목은 businessHours=null로 그냥 내보낸다(목록 자체는 항상 응답).</li>
 * </ul>
 *
 * <p>detailIntro2 응답은 Redis에 캐시되므로 같은 지역·검색을 두 번째로 부를 때는
 * 외부 호출 없이 캐시에서 채워진다. 개별 항목 실패는 삼키고 null로 둔다 —
 * 영업시간 하나 때문에 목록 전체가 실패하면 안 된다.
 *
 * <p>호출은 {@link TourApiClient#detailIntroBulk}(전용 서킷 + 레이트리미터)를 쓴다.
 * 상세 화면과 같은 서킷을 쓰면 목록 한 번의 실패가 {@code /places/{id}}까지 막는다.
 */
@Component
public class BusinessHoursEnricher {

    private static final Logger log = LoggerFactory.getLogger(BusinessHoursEnricher.class);

    private final TourApiClient tourApiClient;
    private final BusinessHoursStore store;
    private final ExecutorService pool;
    private final long budgetNanos;

    public BusinessHoursEnricher(TourApiClient tourApiClient, BusinessHoursStore store,
                                 TourApiProperties props) {
        this.tourApiClient = tourApiClient;
        this.store = store;
        this.budgetNanos = props.introBudget().toNanos();
        AtomicInteger seq = new AtomicInteger();
        int threads = props.introConcurrency();
        // 대기열은 유한하게 — 요청이 몰려도 대기 작업이 무한정 쌓이지 않도록. 넘치면 그 항목만 포기(null).
        this.pool = new ThreadPoolExecutor(threads, threads, 0L, TimeUnit.MILLISECONDS,
                new NewestFirstQueue(threads * 64), r -> {
            Thread t = new Thread(r, "tourapi-intro-" + seq.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * 각 항목의 businessHours/openHours를 채운 새 목록을 돌려준다.
     *
     * @param places            목록(카테고리 필터·배지 부착까지 끝난 상태)
     * @param contentTypeById   contentId → TourAPI contentTypeId (detailIntro2 필수 파라미터)
     */
    public List<PlaceSummary> enrich(List<PlaceSummary> places, Map<String, Integer> contentTypeById) {
        if (places.isEmpty()) {
            return places;
        }
        // 1) 보관소 우선 — 여기서 나오면 외부 호출이 아예 없다.
        Map<String, Optional<String>> stored = store.findFresh(
                places.stream().map(PlaceSummary::id).filter(Objects::nonNull).distinct().toList());

        // 2) 보관분이 없는 장소만 TourAPI로.
        Map<String, CompletableFuture<TourIntro>> futures =
                submitAll(places, contentTypeById, stored.keySet());
        Fetched fetchedResult = collect(futures);
        Map<String, String> fetched = fetchedResult.hours();
        persist(fetchedResult, contentTypeById);

        return places.stream()
                .map(p -> {
                    String hours = stored.containsKey(p.id())
                            ? stored.get(p.id()).orElse(null)
                            : fetched.get(p.id());
                    return hours == null ? p : p.withBusinessHours(hours, BusinessHoursText.first(hours));
                })
                .toList();
    }

    /**
     * 이번에 받아온 값을 보관한다. <b>영업시간이 없는 장소도 남긴다</b> —
     * 그래야 "정보 없음"을 기억해 매번 다시 묻지 않는다. 예산을 넘겨 아직 안 끝난 항목은
     * 결과를 모르니 남기지 않는다(다음 호출에서 다시 시도).
     */
    private void persist(Fetched fetched, Map<String, Integer> contentTypeById) {
        List<PlaceBusinessHours> rows = new ArrayList<>();
        fetched.hours().forEach((id, hours) ->
                rows.add(BusinessHoursStore.row(id, contentTypeById.getOrDefault(id, 0), hours)));
        fetched.noHours().forEach(id ->
                rows.add(BusinessHoursStore.row(id, contentTypeById.getOrDefault(id, 0), null)));
        store.saveAll(rows);
    }

    /**
     * 이번 요청에서 실제로 조회가 끝난 것들.
     *
     * @param hours   영업시간이 있던 장소
     * @param noHours 조회는 성공했지만 원천에 정보가 없던 장소 — 이것도 보관해야 매번 다시 묻지 않는다
     */
    private record Fetched(Map<String, String> hours, Set<String> noHours) {
    }

    /** 항목별 detailIntro2 호출 제출(중복 contentId는 1회). 실패·대기열 초과는 null로 흡수. */
    private Map<String, CompletableFuture<TourIntro>> submitAll(List<PlaceSummary> places,
                                                               Map<String, Integer> contentTypeById,
                                                               Set<String> alreadyStored) {
        Map<String, CompletableFuture<TourIntro>> futures = new LinkedHashMap<>();
        for (PlaceSummary p : places) {
            String id = p.id();
            Integer typeId = contentTypeById.get(id);
            if (id == null || typeId == null || typeId == 0
                    || futures.containsKey(id) || alreadyStored.contains(id)) {
                continue;   // 보관분이 있거나 contentTypeId가 없으면 부르지 않는다
            }
            try {
                futures.put(id, CompletableFuture
                        .supplyAsync(() -> tourApiClient.detailIntroBulk(id, typeId), pool)
                        .exceptionally(t -> {
                            log.debug("detailIntro2 skipped for contentId={}: {}", id, t.toString());
                            return null;
                        }));
            } catch (RejectedExecutionException e) {
                log.info("영업시간 조회 대기열 초과 — contentId={} 건너뜀", id);
            }
        }
        return futures;
    }

    /**
     * 예산 안에서 결과 수확. 예산을 넘긴 항목은 기다리지 않고 비운 채 응답한다.
     * 취소하지는 않는다 — 백그라운드로 끝내 두면 캐시가 채워져 다음 호출에서 값이 나온다.
     */
    private Fetched collect(Map<String, CompletableFuture<TourIntro>> futures) {
        long deadline = System.nanoTime() + budgetNanos;
        Map<String, String> hoursById = new HashMap<>();
        Set<String> noHours = new HashSet<>();
        int unfinished = 0;
        for (Map.Entry<String, CompletableFuture<TourIntro>> e : futures.entrySet()) {
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0 && !e.getValue().isDone()) {
                unfinished++;   // 예산 소진 후 남은 항목 — 기다리지 않고 비운다
                continue;
            }
            try {
                TourIntro intro = remaining <= 0
                        ? e.getValue().getNow(null)
                        : e.getValue().get(remaining, TimeUnit.NANOSECONDS);
                if (intro != null) {
                    if (intro.businessHours() != null) {
                        hoursById.put(e.getKey(), intro.businessHours());
                    } else {
                        noHours.add(e.getKey());   // 조회는 성공, 원천에 정보가 없음
                    }
                }
            } catch (TimeoutException te) {
                unfinished++;
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception ex) {
                log.debug("detailIntro2 skipped for contentId={}: {}", e.getKey(), ex.toString());
            }
        }
        if (unfinished > 0) {
            log.info("영업시간 채우기 예산 초과: {}/{}건 미완료(businessHours=null, 다음 호출은 캐시로 채워짐)",
                    unfinished, futures.size());
        }
        return new Fetched(hoursById, noHours);
    }

    /**
     * 최근에 제출된 작업부터 꺼내는 대기열(LIFO).
     *
     * <p>기본 FIFO면 앞선 요청이 남긴 백로그가 전부 끝날 때까지 <b>새 요청이 한 건도 처리되지 않는다</b>.
     * 실제로 전주 목록(318건)을 한 번 조회하면 그 백로그가 30초 넘게 대기열을 점유해,
     * 그동안 들어온 모든 목록이 영업시간 0건으로 나갔다.
     *
     * <p>LIFO면 새 요청이 먼저 처리되고 옛 백로그는 남는 여유로 천천히 끝나 캐시를 채운다 —
     * 어차피 백로그는 "있으면 좋은" 캐시 워밍이라 늦어져도 된다.
     */
    private static final class NewestFirstQueue extends LinkedBlockingDeque<Runnable> {
        NewestFirstQueue(int capacity) {
            super(capacity);
        }

        @Override
        public boolean offer(Runnable r) {
            return offerFirst(r);   // 워커는 head에서 take → 최신 작업이 먼저 나간다
        }
    }

    @PreDestroy
    void shutdown() {
        pool.shutdownNow();
    }
}
