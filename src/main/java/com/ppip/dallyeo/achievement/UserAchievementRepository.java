package com.ppip.dallyeo.achievement;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 사용자 업적 달성 기록 영속성. 본인 달성 목록 조회 + 중복 달성 방지 확인.
 */
public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {

    List<UserAchievement> findByUserId(Long userId);

    boolean existsByUserIdAndAchievement(Long userId, AchievementType achievement);
}
