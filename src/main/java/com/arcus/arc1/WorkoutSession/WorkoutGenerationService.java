package com.arcus.arc1.WorkoutSession;

import com.arcus.arc1.ExerciseSession.ExerciseSessionEntity;
import com.arcus.arc1.ExerciseSession.ExerciseSessionRepo;
import com.arcus.arc1.TemplateExcercise.TemplateExerciseEntity;
import com.arcus.arc1.TemplateExcercise.TemplateExerciseRepo;
import com.arcus.arc1.WorkoutTemplate.WorkoutTemplateEntity;
import com.arcus.arc1.WorkoutTemplate.WorkoutTemplateRepo;
import com.arcus.arc1.dto.ExerciseDTO;
import com.arcus.arc1.dto.WorkoutResponseDTO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class WorkoutGenerationService {

    private final WorkoutTemplateRepo templateRepo;
    private final TemplateExerciseRepo exerciseRepo;
    private final WorkoutSessionRepo workoutSessionRepo;
    private final ExerciseSessionRepo exerciseSessionRepo;

    public WorkoutGenerationService(
            WorkoutTemplateRepo templateRepo,
            TemplateExerciseRepo exerciseRepo,
            WorkoutSessionRepo workoutSessionRepo,
            ExerciseSessionRepo exerciseSessionRepo
    ) {
        this.templateRepo = templateRepo;
        this.exerciseRepo = exerciseRepo;
        this.workoutSessionRepo = workoutSessionRepo;
        this.exerciseSessionRepo = exerciseSessionRepo;
    }

    public WorkoutResponseDTO generateWorkout(Long userId, String level) {

        List<ExerciseDTO> exerciseDTOList = new ArrayList<>();

        WorkoutTemplateEntity template =
                templateRepo.findByLevelAndGoalAndDayNumber(
                        level,
                        "muscle_gain",
                        1 // for now hardcoded day 1
                ).orElseThrow();

        WorkoutSessionEntity session = new WorkoutSessionEntity();
        session.setUserId(userId);
        session.setTemplateId(template.getId());
        session.setCreatedAt(LocalDateTime.now());
        session.setCompleted(false);

        session = workoutSessionRepo.save(session);

        List<TemplateExerciseEntity> exercises =
                exerciseRepo.findByTemplateIdOrderByOrderIndex(template.getId());

        for (TemplateExerciseEntity te : exercises) {

            ExerciseSessionEntity es = new ExerciseSessionEntity();
            es.setWorkoutSessionId(session.getId());
            es.setExerciseName(te.getExerciseName());
            es.setTargetWeight(50.0);
            es.setRepMin(te.getRepMin());
            es.setRepMax(te.getRepMax());
            es.setSets(te.getSets());
            es.setTempo(te.getTempo());

            es = exerciseSessionRepo.save(es);  // SAVE FIRST

            ExerciseDTO exerciseDTO = new ExerciseDTO();
            exerciseDTO.setExerciseSessionId(es.getId());  // use es.getId()
            exerciseDTO.setExerciseName(es.getExerciseName());
            exerciseDTO.setTargetWeight(es.getTargetWeight());
            exerciseDTO.setRepMin(es.getRepMin());
            exerciseDTO.setRepMax(es.getRepMax());
            exerciseDTO.setSets(es.getSets());
            exerciseDTO.setTempo(es.getTempo());

            exerciseDTOList.add(exerciseDTO);
        }

        return new WorkoutResponseDTO(session.getId(), level, exerciseDTOList);
    }
}

