package com.arcus.arc1.WorkoutTemplate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkoutTemplateRepo
        extends JpaRepository<WorkoutTemplateEntity, Long> {

    // ── Level-free lookups (preferred) ────────────────────────────────────────
    /** Primary lookup: split + goal + day — level is irrelevant for template selection. */
    Optional<WorkoutTemplateEntity> findByGoalAndSplitAndDayNumber(
            String goal, String split, Integer dayNumber);

    /** Fallback when split is not specified. */
    Optional<WorkoutTemplateEntity> findByGoalAndDayNumber(
            String goal, Integer dayNumber);

    @Query("SELECT MAX(wt.dayNumber) FROM WorkoutTemplateEntity wt " +
           "WHERE wt.goal = :goal AND wt.split = :split")
    Optional<Integer> findMaxDayNumberByGoalAndSplit(
            @Param("goal") String goal, @Param("split") String split);

    @Query("SELECT MAX(wt.dayNumber) FROM WorkoutTemplateEntity wt " +
           "WHERE wt.goal = :goal")
    Optional<Integer> findMaxDayNumberByGoal(@Param("goal") String goal);

    // ── Legacy level-based lookups (kept for backward compatibility) ───────────
    Optional<WorkoutTemplateEntity> findByLevelAndGoalAndDayNumber(
            String level, String goal, Integer dayNumber);

    Optional<WorkoutTemplateEntity> findByLevelAndGoalAndSplitAndDayNumber(
            String level, String goal, String split, Integer dayNumber);

    @Query("SELECT MAX(wt.dayNumber) FROM WorkoutTemplateEntity wt " +
           "WHERE wt.level = :level AND wt.goal = :goal")
    Optional<Integer> findMaxDayNumberByLevelAndGoal(
            @Param("level") String level, @Param("goal") String goal);

    @Query("SELECT MAX(wt.dayNumber) FROM WorkoutTemplateEntity wt " +
           "WHERE wt.level = :level AND wt.goal = :goal AND wt.split = :split")
    Optional<Integer> findMaxDayNumberByLevelAndGoalAndSplit(
            @Param("level") String level, @Param("goal") String goal, @Param("split") String split);
}

