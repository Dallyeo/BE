package com.ppip.dallyeo.badge;

import com.ppip.dallyeo.domain.region.Region;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 배지 CSV 적재 (US-BADGE-1, BR-U3-10, P6). 기동 시 1회. 군산·요식업만.
 * <b>파일별 인코딩 명시</b>: 모범음식점 UTF-8 / 착한가격 EUC-KR (오독 시 정규화 매칭 전멸).
 * (type, normName, normAddr) 기준 멱등.
 */
@Component
public class BadgeCsvLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BadgeCsvLoader.class);

    private static final String MODEL_CSV = "data/model-restaurants-gunsan.csv";   // UTF-8
    private static final String GOOD_PRICE_CSV = "data/good-price-gunsan.csv";      // EUC-KR
    private static final Charset EUC_KR = Charset.forName("EUC-KR");

    /** 착한가격업소 중 비요식업(제외 대상). */
    private static final Set<String> NON_FOOD = Set.of("미용업", "목욕업", "이용업");

    private final BadgeRepository badgeRepository;
    private final AddressNormalizer normalizer;

    public BadgeCsvLoader(BadgeRepository badgeRepository, AddressNormalizer normalizer) {
        this.badgeRepository = badgeRepository;
        this.normalizer = normalizer;
    }

    @Override
    public void run(ApplicationArguments args) {
        int model = loadModelRestaurants();
        int goodPrice = loadGoodPrice();
        log.info("Badge CSV load complete: MODEL_RESTAURANT={}, GOOD_PRICE={} inserted", model, goodPrice);
    }

    /** 모범음식점(UTF-8): 업소명(1)/소재지(2), 전건 MODEL_RESTAURANT. */
    private int loadModelRestaurants() {
        int inserted = 0;
        for (String[] cols : readCsv(MODEL_CSV, StandardCharsets.UTF_8)) {
            if (cols.length < 3) {
                continue;
            }
            if (save(BadgeType.MODEL_RESTAURANT, cols[1], cols[2])) {
                inserted++;
            }
        }
        return inserted;
    }

    /** 착한가격(EUC-KR): 업종(1)/업소명(2)/주소(3), 요식업만 GOOD_PRICE. */
    private int loadGoodPrice() {
        int inserted = 0;
        int excluded = 0;
        for (String[] cols : readCsv(GOOD_PRICE_CSV, EUC_KR)) {
            if (cols.length < 4) {
                continue;
            }
            String industry = cols[1].trim();
            if (NON_FOOD.contains(industry)) {
                excluded++;          // BR-U3-10: 미용/목욕/이용 제외
                continue;
            }
            if (save(BadgeType.GOOD_PRICE, cols[2], cols[3])) {
                inserted++;
            }
        }
        if (excluded > 0) {
            log.info("Badge GOOD_PRICE: excluded {} non-food row(s)", excluded);
        }
        return inserted;
    }

    /** 멱등 저장. 이미 있으면 false. */
    private boolean save(BadgeType type, String rawName, String rawAddress) {
        String normName = normalizer.normalizeName(rawName);
        String normAddr = normalizer.normalizeAddress(rawAddress);
        if (normName.isEmpty() || normAddr.isEmpty()) {
            return false;
        }
        if (badgeRepository.existsByTypeAndNormalizedNameAndNormalizedAddress(type, normName, normAddr)) {
            return false;   // 멱등 스킵
        }
        badgeRepository.save(Badge.builder()
                .type(type).region(Region.GUNSAN)
                .placeName(rawName == null ? null : rawName.trim())
                .normalizedName(normName).normalizedAddress(normAddr)
                .build());
        return true;
    }

    /** classpath CSV 읽기(헤더 스킵, BOM 제거, quote-aware). 없으면 빈 리스트. */
    private List<String[]> readCsv(String resourcePath, Charset charset) {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        if (!resource.exists()) {
            log.warn("Badge CSV not found: {} — skipping", resourcePath);
            return List.of();
        }
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(resource.getInputStream(), charset))) {
            String line;
            boolean header = true;
            while ((line = br.readLine()) != null) {
                if (header) {
                    header = false;   // 첫 줄 헤더 스킵
                    continue;
                }
                if (line.isBlank()) {
                    continue;
                }
                rows.add(parseCsvLine(stripBom(line)));
            }
        } catch (Exception e) {
            log.error("Failed to read badge CSV {}: {}", resourcePath, e.toString());
        }
        return rows;
    }

    private String stripBom(String s) {
        return (!s.isEmpty() && s.charAt(0) == '﻿') ? s.substring(1) : s;
    }

    /** 최소 quote-aware CSV 파서 (따옴표 내 콤마 보존). */
    private String[] parseCsvLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cur.append('"');   // 이스케이프된 따옴표
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString());
        return out.toArray(new String[0]);
    }
}
