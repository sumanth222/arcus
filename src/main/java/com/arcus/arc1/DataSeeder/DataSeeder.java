package com.arcus.arc1.DataSeeder;

import com.arcus.arc1.TemplateExcercise.TemplateExerciseEntity;
import com.arcus.arc1.TemplateExcercise.TemplateExerciseRepo;
import com.arcus.arc1.WorkoutTemplate.WorkoutTemplateEntity;
import com.arcus.arc1.WorkoutTemplate.WorkoutTemplateRepo;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedTemplates(
            WorkoutTemplateRepo templateRepo,
            TemplateExerciseRepo exerciseRepo
    ) {
        return args -> {

            if (templateRepo.count() > 0) {
                return; // Prevent duplicate seeding
            }

            WorkoutTemplateEntity chestDay = new WorkoutTemplateEntity();
            chestDay.setName("Bro Split");
            chestDay.setLevel("beginner");
            chestDay.setGoal("muscle_gain");
            chestDay.setDayNumber(1);

            chestDay = templateRepo.save(chestDay);

            TemplateExerciseEntity bench = new TemplateExerciseEntity();
            bench.setTemplateId(chestDay.getId());
            bench.setExerciseName("Bench Press");
            bench.setSets(3);
            bench.setRepMin(8);
            bench.setRepMax(12);
            bench.setTempo("3-1-1");
            bench.setOrderIndex(1);

            exerciseRepo.save(bench);

            // Add more exercises...
        };
    }
}
