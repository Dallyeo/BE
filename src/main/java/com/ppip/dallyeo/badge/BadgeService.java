package com.ppip.dallyeo.badge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 장소에 배지 부여 (US-BADGE-1, BR-U3-8). 정규화 업소명 AND 주소 둘 다 일치 시에만 매칭.
 * 미매칭/미적재 → 빈 배열.
 */
@Service
@Transactional(readOnly = true)
public class BadgeService {

    private static final Logger log = LoggerFactory.getLogger(BadgeService.class);

    private final BadgeRepository badgeRepository;
    private final AddressNormalizer normalizer;

    public BadgeService(BadgeRepository badgeRepository, AddressNormalizer normalizer) {
        this.badgeRepository = badgeRepository;
        this.normalizer = normalizer;
    }

    /** 장소(업소명+주소)에 해당하는 배지 타입 목록. 없으면 빈 리스트. */
    public List<String> badgesFor(String placeName, String address) {
        if (placeName == null || address == null) {
            return List.of();
        }
        String normName = normalizer.normalizeName(placeName);
        String normAddr = normalizer.normalizeAddress(address);
        if (normName.isEmpty() || normAddr.isEmpty()) {
            return List.of();
        }
        List<String> badges = badgeRepository
                .findByNormalizedNameAndNormalizedAddress(normName, normAddr).stream()
                .map(b -> b.getType().name())
                .distinct()
                .toList();
        if (badges.isEmpty()) {
            // BR-U3-8: 미매칭 관측(향후 하이브리드 매칭 전환 판단 근거)
            log.debug("No badge matched for place='{}' (normName={}, normAddr={})", placeName, normName, normAddr);
        }
        return badges;
    }
}
