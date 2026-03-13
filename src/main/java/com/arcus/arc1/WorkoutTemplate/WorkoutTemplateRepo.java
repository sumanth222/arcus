package com.arcus.arc1.WorkoutTemplate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface WorkoutTemplateRepo
        extends JpaRepository<WorkoutTemplateEntity, Long> {

    Optional<WorkoutTemplateEntity> findByLevelAndGoalAndDayNumber(
            String level,
            String goal,
            Integer dayNumber
    );

    /**
     * Find the maximum day number for a given level and goal.
     * Used to wrap around to day 1 when the user has completed all days.
     */
    @Query("SELECT MAX(wt.dayNumber) FROM WorkoutTemplateEntity wt " +
           "WHERE wt.level = :level AND wt.goal = :goal")
    Optional<Integer> findMaxDayNumberByLevelAndGoal(String level, String goal);
}

