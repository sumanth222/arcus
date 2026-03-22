package com.arcus.arc1.ExerciseLibrary;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExerciseLibraryRepo extends JpaRepository<ExerciseLibraryEntity, Long> {

    Optional<ExerciseLibraryEntity> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    /** All exercises for a muscle group */
    List<ExerciseLibraryEntity> findByMuscleGroupIgnoreCase(String muscleGroup);

    /** All exercises for a muscle group and area */
    List<ExerciseLibraryEntity> findByMuscleGroupIgnoreCaseAndMuscleAreaIgnoreCase(String muscleGroup, String muscleArea);

    /** All exercises for a muscle group and area filtered by allowed levels */
    List<ExerciseLibraryEntity> findByMuscleGroupIgnoreCaseAndMuscleAreaIgnoreCaseAndLevelIn(String muscleGroup, String muscleArea, List<String> levels);

    /** All exercises for a muscle group where muscle_area is in a list, filtered by allowed levels */
    List<ExerciseLibraryEntity> findByMuscleGroupIgnoreCaseAndMuscleAreaInAndLevelIn(String muscleGroup, List<String> muscleAreas, List<String> levels);

    /** Substitution query: same muscle group, different equipment, different exercise */
    List<ExerciseLibraryEntity> findByMuscleGroupIgnoreCaseAndEquipmentNotInAndIdNot(
            String muscleGroup, List<String> unavailableEquipment, Long excludeId);

    /** Same muscle group AND same category (compound/isolation), different equipment */
    List<ExerciseLibraryEntity> findByMuscleGroupIgnoreCaseAndCategoryIgnoreCaseAndEquipmentNotInAndIdNot(
            String muscleGroup, String category, List<String> unavailableEquipment, Long excludeId);

    /** Same muscle group AND muscle area and same category */
    List<ExerciseLibraryEntity> findByMuscleGroupIgnoreCaseAndMuscleAreaIgnoreCaseAndCategoryIgnoreCaseAndEquipmentNotInAndIdNot(
            String muscleGroup, String muscleArea, String category, List<String> unavailableEquipment, Long excludeId);

    List<ExerciseLibraryEntity> findByMuscleGroupIgnoreCaseAndMuscleAreaInAndLevelIgnoreCase(
            String muscleGroup, List<String> muscleArea, String level
    );

    List<ExerciseLibraryEntity> findByMuscleGroupIgnoreCaseAndLevelIgnoreCase(
            String muscleGroup, String level
    );
}




