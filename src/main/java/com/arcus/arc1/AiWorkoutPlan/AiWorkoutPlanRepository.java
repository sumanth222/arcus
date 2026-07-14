package com.arcus.arc1.AiWorkoutPlan;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiWorkoutPlanRepository extends JpaRepository<AiWorkoutPlanEntity, Long> {

    /** All exercises for a user on a specific day, in insertion order. */
    List<AiWorkoutPlanEntity> findByUserIdAndDayNumberOrderByIdAsc(Long userId, Integer dayNumber);

    /** Full plan for a user, ordered by day then insertion order. */
    List<AiWorkoutPlanEntity> findByUserIdOrderByDayNumberAscIdAsc(Long userId);

    /** Used before regenerating a plan to wipe old entries. */
    void deleteByUserId(Long userId);

    /** Wipes only a single day's entries — used on per-cycle refresh. */
    void deleteByUserIdAndDayNumber(Long userId, Integer dayNumber);

    /** Quick check — if false we trigger generation. */
    boolean existsByUserId(Long userId);
}
