package com.ppip.dallyeo.place;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PlaceBusinessHoursRepository extends JpaRepository<PlaceBusinessHours, String> {

    /** 목록 한 건당 조회 1회로 끝내기 위한 일괄 조회(N+1 방지). */
    List<PlaceBusinessHours> findByContentIdIn(Collection<String> contentIds);
}
