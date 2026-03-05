package com.arcus.arc1.SetLog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SetLogRepo
        extends JpaRepository<SetLogEntity, Long> {

    List<SetLogEntity> findByExerciseSessionIdOrderBySetNumberAsc(Long exerciseSessionId);
}