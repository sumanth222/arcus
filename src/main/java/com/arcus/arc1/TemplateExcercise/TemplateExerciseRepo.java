package com.arcus.arc1.TemplateExcercise;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface TemplateExerciseRepo
        extends JpaRepository<TemplateExerciseEntity, Long> {

    List<TemplateExerciseEntity> findByTemplateIdOrderByOrderIndex(Long templateId);
}