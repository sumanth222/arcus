package com.arcus.arc1.WorkoutTemplate;

import org.springframework.data.jpa.repository.JpaRepository;
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
}

