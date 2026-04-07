package com.arcus.arc1.DataSeeder;

import com.arcus.arc1.ExerciseLibrary.ExerciseLibraryEntity;
import com.arcus.arc1.ExerciseLibrary.ExerciseLibraryRepo;
import com.arcus.arc1.Nutrition.FoodItemEntity;
import com.arcus.arc1.Nutrition.FoodItemRepo;
import com.arcus.arc1.TemplateExcercise.TemplateExerciseEntity;
import com.arcus.arc1.TemplateExcercise.TemplateExerciseRepo;
import com.arcus.arc1.WorkoutTemplate.WorkoutTemplateEntity;
import com.arcus.arc1.WorkoutTemplate.WorkoutTemplateRepo;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedTemplates(
            WorkoutTemplateRepo templateRepo,
            TemplateExerciseRepo exerciseRepo,
            ExerciseLibraryRepo libraryRepo
    ) {
        return args -> {

            // ─── 1. Seed Exercise Library ──────────────────────────────────────────────
            if (!libraryRepo.existsByNameIgnoreCase("Bench Press")) {
                seedExerciseLibrary(libraryRepo);
            }

            // ─── 2. Seed Workout Templates ────────────────────────────────────────────
            if (templateRepo.count() > 0) {
                return;
            }

            // Helper: look up library ID by name
            Map<String, Long> lib = buildLibraryMap(libraryRepo);

            // ── Day 1: Chest ──────────────────────────────────────────────────────────
            WorkoutTemplateEntity chestDay = save(templateRepo, "Bro Split - Chest", "beginner", "muscle_gain", 1);
            addExercise(exerciseRepo, chestDay.getId(), lib, "Bench Press",        3, 8, 12, "3-1-1", 1);
            addExercise(exerciseRepo, chestDay.getId(), lib, "Incline Dumbbell Press", 3, 10, 12, "3-1-1", 2);
            addExercise(exerciseRepo, chestDay.getId(), lib, "Cable Fly",           3, 12, 15, "2-1-2", 3);
            addExercise(exerciseRepo, chestDay.getId(), lib, "Dumbbell Fly",        3, 12, 15, "2-1-2", 4);
            addExercise(exerciseRepo, chestDay.getId(), lib, "Chest Dip",           3, 8,  12, "3-0-1", 5);

            // ── Day 2: Back ───────────────────────────────────────────────────────────
            WorkoutTemplateEntity backDay = save(templateRepo, "Bro Split - Back", "beginner", "muscle_gain", 2);
            addExercise(exerciseRepo, backDay.getId(), lib, "Deadlift",             4, 5,  8,  "3-1-1", 1);
            addExercise(exerciseRepo, backDay.getId(), lib, "Barbell Row",          3, 8,  10, "3-1-1", 2);
            addExercise(exerciseRepo, backDay.getId(), lib, "Lat Pulldown",         3, 10, 12, "2-1-2", 3);
            addExercise(exerciseRepo, backDay.getId(), lib, "Seated Cable Row",     3, 10, 12, "2-1-2", 4);
            addExercise(exerciseRepo, backDay.getId(), lib, "Face Pull",            3, 15, 20, "2-1-2", 5);

            // ── Day 3: Shoulders ──────────────────────────────────────────────────────
            WorkoutTemplateEntity shoulderDay = save(templateRepo, "Bro Split - Shoulders", "beginner", "muscle_gain", 3);
            addExercise(exerciseRepo, shoulderDay.getId(), lib, "Barbell Overhead Press", 4, 6, 10, "3-1-1", 1);
            addExercise(exerciseRepo, shoulderDay.getId(), lib, "Dumbbell Shoulder Press", 3, 8, 12, "3-1-1", 2);
            addExercise(exerciseRepo, shoulderDay.getId(), lib, "Lateral Raise",    3, 12, 15, "2-1-2", 3);
            addExercise(exerciseRepo, shoulderDay.getId(), lib, "Front Raise",      3, 12, 15, "2-1-2", 4);
            addExercise(exerciseRepo, shoulderDay.getId(), lib, "Rear Delt Fly",    3, 15, 20, "2-1-2", 5);

            // ── Day 4: Legs ───────────────────────────────────────────────────────────
            WorkoutTemplateEntity legDay = save(templateRepo, "Bro Split - Legs", "beginner", "muscle_gain", 4);
            addExercise(exerciseRepo, legDay.getId(), lib, "Barbell Squat",         4, 6,  10, "3-1-1", 1);
            addExercise(exerciseRepo, legDay.getId(), lib, "Romanian Deadlift",     3, 8,  12, "3-1-1", 2);
            addExercise(exerciseRepo, legDay.getId(), lib, "Leg Press",             3, 10, 15, "2-1-2", 3);
            addExercise(exerciseRepo, legDay.getId(), lib, "Leg Extension",         3, 12, 15, "2-1-2", 4);
            addExercise(exerciseRepo, legDay.getId(), lib, "Leg Curl",              3, 12, 15, "2-1-2", 5);

            // ── Day 5: Arms ───────────────────────────────────────────────────────────
            WorkoutTemplateEntity armDay = save(templateRepo, "Bro Split - Arms", "beginner", "muscle_gain", 5);
            addExercise(exerciseRepo, armDay.getId(), lib, "Barbell Curl",          3, 8,  12, "3-1-1", 1);
            addExercise(exerciseRepo, armDay.getId(), lib, "Hammer Curl",           3, 10, 12, "3-1-1", 2);
            addExercise(exerciseRepo, armDay.getId(), lib, "Tricep Pushdown",       3, 10, 15, "2-1-2", 3);
            addExercise(exerciseRepo, armDay.getId(), lib, "Skull Crusher",         3, 8,  12, "3-1-1", 4);
            addExercise(exerciseRepo, armDay.getId(), lib, "Cable Curl",            3, 12, 15, "2-1-2", 5);
        };
    }

    // ─── Food Item Seeder ─────────────────────────────────────────────────────

    @Bean
    CommandLineRunner seedFoodItems(FoodItemRepo foodItemRepo) {
        return args -> {
            if (foodItemRepo.count() > 0) return;

            // ── veg ──────────────────────────────────────────────────────────
            foodItemRepo.save(new FoodItemEntity("Paneer",              18.0, 265, "veg",     "100g paneer"));
            foodItemRepo.save(new FoodItemEntity("Eggs",                13.0, 154, "veg",     "2 large eggs"));
            foodItemRepo.save(new FoodItemEntity("Greek Yoghurt",       17.0, 100, "veg",     "170g serving"));
            foodItemRepo.save(new FoodItemEntity("Whole Milk",           8.0, 149, "veg",     "250 ml"));
            foodItemRepo.save(new FoodItemEntity("Tofu",                17.0, 144, "veg",     "200g firm tofu"));
            foodItemRepo.save(new FoodItemEntity("Chickpeas",           15.0, 269, "veg",     "200g cooked"));
            foodItemRepo.save(new FoodItemEntity("Lentils",             18.0, 230, "veg",     "200g cooked"));
            foodItemRepo.save(new FoodItemEntity("Cottage Cheese",      14.0,  98, "veg",     "100g"));
            foodItemRepo.save(new FoodItemEntity("Peanut Butter",        8.0, 190, "veg",     "2 tbsp"));
            // everyday Indian veg
            foodItemRepo.save(new FoodItemEntity("Moong Dal",           14.0, 212, "veg",     "1 katori (150g) cooked"));
            foodItemRepo.save(new FoodItemEntity("Toor Dal",            12.0, 198, "veg",     "1 katori (150g) cooked"));
            foodItemRepo.save(new FoodItemEntity("Masoor Dal",          15.0, 195, "veg",     "1 katori (150g) cooked"));
            foodItemRepo.save(new FoodItemEntity("Rajma",               15.0, 230, "veg",     "1 katori (150g) cooked"));
            foodItemRepo.save(new FoodItemEntity("Chana Dal",           13.0, 218, "veg",     "1 katori (150g) cooked"));
            foodItemRepo.save(new FoodItemEntity("Soya Chunks",         25.0, 173, "veg",     "50g dry / 100g soaked"));
            foodItemRepo.save(new FoodItemEntity("Dahi (Curd)",          6.0,  61, "veg",     "1 katori (150g)"));
            foodItemRepo.save(new FoodItemEntity("Roasted Chana",       12.0, 180, "veg",     "50g handful"));
            foodItemRepo.save(new FoodItemEntity("Peanuts",             13.0, 280, "veg",     "50g (small fistful)"));
            foodItemRepo.save(new FoodItemEntity("Moong Sprouts",        9.0,  85, "veg",     "1 cup (100g) sprouted"));
            foodItemRepo.save(new FoodItemEntity("Sattu",               20.0, 220, "veg",     "50g powder in water"));
            foodItemRepo.save(new FoodItemEntity("Besan Chilla",        12.0, 180, "veg",     "2 medium chillas"));
            foodItemRepo.save(new FoodItemEntity("Poha with Peanuts",    6.0, 195, "veg",     "1 medium plate (150g)"));
            foodItemRepo.save(new FoodItemEntity("Almonds",              6.0, 173, "veg",     "25g (about 20 almonds)"));
            foodItemRepo.save(new FoodItemEntity("Roti with Dal",       14.0, 310, "veg",     "2 rotis + 1 katori dal"));

            // ── non_veg ──────────────────────────────────────────────────────
            foodItemRepo.save(new FoodItemEntity("Chicken Breast",      31.0, 165, "non_veg", "100g grilled"));
            foodItemRepo.save(new FoodItemEntity("Tuna (canned)",       25.0, 116, "non_veg", "100g drained"));
            foodItemRepo.save(new FoodItemEntity("Boiled Eggs",         13.0, 155, "non_veg", "2 eggs"));
            foodItemRepo.save(new FoodItemEntity("Salmon",              25.0, 208, "non_veg", "100g baked"));
            foodItemRepo.save(new FoodItemEntity("Egg Whites",          11.0,  52, "non_veg", "3 egg whites"));
            // everyday Indian non-veg
            foodItemRepo.save(new FoodItemEntity("Egg Bhurji",          14.0, 180, "non_veg", "2-egg scramble with onion/tomato"));
            foodItemRepo.save(new FoodItemEntity("Boiled Chicken",      27.0, 150, "non_veg", "100g boneless"));
            foodItemRepo.save(new FoodItemEntity("Chicken Curry",       22.0, 240, "non_veg", "1 medium serving (150g)"));
            foodItemRepo.save(new FoodItemEntity("Omelette",            12.0, 154, "non_veg", "2-egg omelette"));
            foodItemRepo.save(new FoodItemEntity("Rohu Fish",           20.0, 140, "non_veg", "100g cooked"));
            foodItemRepo.save(new FoodItemEntity("Catla Fish",          19.0, 130, "non_veg", "100g cooked"));

            // ── shake ─────────────────────────────────────────────────────────
            foodItemRepo.save(new FoodItemEntity("Whey Protein Shake",  25.0, 130, "shake",   "1 scoop (30g) in water"));
            foodItemRepo.save(new FoodItemEntity("Casein Shake",        24.0, 120, "shake",   "1 scoop (33g) in milk"));
            foodItemRepo.save(new FoodItemEntity("Plant Protein Shake", 20.0, 140, "shake",   "1 scoop (35g) in water"));
            // everyday Indian shakes/drinks
            foodItemRepo.save(new FoodItemEntity("Sattu Drink",         18.0, 180, "shake",   "4 tbsp sattu in 300ml water with lemon"));
            foodItemRepo.save(new FoodItemEntity("Banana Milk Shake",   10.0, 265, "shake",   "1 banana + 250ml full-fat milk"));
            foodItemRepo.save(new FoodItemEntity("Dahi Lassi",           8.0, 140, "shake",   "250ml curd blended with water"));
            foodItemRepo.save(new FoodItemEntity("Peanut Butter Shake", 18.0, 310, "shake",   "2 tbsp PB + 250ml milk + banana"));
        };
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────────

    private WorkoutTemplateEntity save(WorkoutTemplateRepo repo, String name, String level, String goal, int day) {
        WorkoutTemplateEntity t = new WorkoutTemplateEntity();
        t.setName(name); t.setLevel(level); t.setGoal(goal); t.setDayNumber(day);
        return repo.save(t);
    }

    private void addExercise(TemplateExerciseRepo repo, Long templateId,
                              Map<String, Long> lib, String name,
                              int sets, int repMin, int repMax, String tempo, int order) {
        TemplateExerciseEntity e = new TemplateExerciseEntity();
        e.setTemplateId(templateId);
        e.setExerciseName(name);
        e.setExerciseLibraryId(lib.get(name));   // link to library
        e.setSets(sets);
        e.setRepMin(repMin);
        e.setRepMax(repMax);
        e.setTempo(tempo);
        e.setOrderIndex(order);
        repo.save(e);
    }

    private Map<String, Long> buildLibraryMap(ExerciseLibraryRepo repo) {
        Map<String, Long> map = new java.util.HashMap<>();
        repo.findAll().forEach(e -> map.put(e.getName(), e.getId()));
        return map;
    }

    private void seedExerciseLibrary(ExerciseLibraryRepo repo) {
        // ── CHEST ─────────────────────────────────────────────────────────────────
        lib(repo, "Bench Press",              "chest",      "triceps,shoulders", "middle",      "barbell",     "compound",  "Keep shoulder blades retracted and drive feet into the floor.");
        lib(repo, "Dumbbell Bench Press",     "chest",      "triceps,shoulders", "middle",      "dumbbell",    "compound",  "Let dumbbells stretch deeper at the bottom for full chest activation.");
        lib(repo, "Incline Bench Press",      "chest",      "triceps,shoulders", "upper",       "barbell",     "compound",  "Set bench to 30-45° — too steep shifts load to shoulders.");
        lib(repo, "Incline Dumbbell Press",   "chest",      "triceps,shoulders", "upper",       "dumbbell",    "compound",  "Control the descent to maximise upper chest stretch.");
        lib(repo, "Cable Fly",                "chest",      "",                   "middle",      "cable",       "isolation", "Keep a slight bend in the elbows throughout; squeeze at the top.");
        lib(repo, "Dumbbell Fly",             "chest",      "",                   "middle",      "dumbbell",    "isolation", "Think of hugging a barrel — never lock the elbows.");
        lib(repo, "Push-Up",                  "chest",      "triceps,shoulders",  "middle",      "bodyweight",  "compound",  "Maintain a rigid plank; flare elbows to 45° not 90°.");
        lib(repo, "Chest Dip",                "chest",      "triceps",            "lower",       "bodyweight",  "compound",  "Lean forward to bias the chest; stay upright for triceps.");
        lib(repo, "Pec Deck",                 "chest",      "",                   "middle",      "machine",     "isolation", "Adjust seat so handles are at mid-chest height.");
        lib(repo, "Smith Machine Bench Press","chest",      "triceps,shoulders",  "middle",      "smith_machine","compound", "Bar path is fixed — focus on a strong leg drive.");

        // ── BACK ──────────────────────────────────────────────────────────────────
        lib(repo, "Deadlift",                 "back",       "glutes,hamstrings",  "posterior_chain","barbell",     "compound",  "Neutral spine from start to finish; push the floor away.");
        lib(repo, "Barbell Row",              "back",       "biceps",             "mid_back",        "barbell",     "compound",  "Hinge to ~45°; pull the bar to the lower belly.");
        lib(repo, "Dumbbell Row",             "back",       "biceps",             "mid_back",        "dumbbell",    "compound",  "Brace the non-working arm; pull elbow past your hip.");
        lib(repo, "Pull-Up",                  "back",       "biceps",             "upper_back",      "bodyweight",  "compound",  "Full dead hang at the bottom; chin over the bar at the top.");
        lib(repo, "Lat Pulldown",             "back",       "biceps",             "upper_back",      "machine",     "compound",  "Lean back slightly; pull the bar to your upper chest.");
        lib(repo, "Seated Cable Row",         "back",       "biceps",             "mid_back",        "cable",       "compound",  "Keep chest tall; squeeze shoulder blades together at the end.");
        lib(repo, "T-Bar Row",                "back",       "biceps",             "mid_back",        "barbell",     "compound",  "Use a neutral grip and a slight knee bend throughout.");
        lib(repo, "Face Pull",                "back",       "shoulders",          "rear_delts",      "cable",       "isolation", "Pull to forehead level and externally rotate at the end.");
        lib(repo, "Machine Row",              "back",       "biceps",             "mid_back",        "machine",     "compound",  "Adjust chest pad so arms are parallel to the floor.");

        // ── SHOULDERS ─────────────────────────────────────────────────────────────
        lib(repo, "Barbell Overhead Press",   "shoulders",  "triceps",            "overall",     "barbell",     "compound",  "Press straight up; don't overarch the lower back.");
        lib(repo, "Dumbbell Shoulder Press",  "shoulders",  "triceps",            "overall",     "dumbbell",    "compound",  "Lower dumbbells to ear height for a full range of motion.");
        lib(repo, "Lateral Raise",            "shoulders",  "",                   "lateral",     "dumbbell",    "isolation", "Lead with your elbows, not your hands; stop at shoulder height.");
        lib(repo, "Cable Lateral Raise",      "shoulders",  "",                   "lateral",     "cable",       "isolation", "Cable provides constant tension — great for the mid-delt.");
        lib(repo, "Machine Shoulder Press",   "shoulders",  "triceps",            "overall",     "machine",     "compound",  "Adjust seat so handles align with your shoulders.");
        lib(repo, "Front Raise",              "shoulders",  "",                   "anterior",    "dumbbell",    "isolation", "Keep a slight bend in the elbows; don't swing.");
        lib(repo, "Rear Delt Fly",            "shoulders",  "back",               "posterior",   "dumbbell",    "isolation", "Hinge forward; lead with your elbows wide.");
        lib(repo, "Cable Face Pull",          "shoulders",  "back",               "posterior",   "cable",       "isolation", "Use a rope attachment; pull to eye level and rotate out.");

        // ── QUADS ─────────────────────────────────────────────────────────────────
        lib(repo, "Barbell Squat",            "quads",      "glutes,hamstrings",  "overall",     "barbell",     "compound",  "Break at hips and knees simultaneously; keep chest tall.");
        lib(repo, "Leg Press",                "quads",      "glutes,hamstrings",  "quads",        "machine",     "compound",  "Place feet shoulder-width; don't lock out knees at the top.");
        lib(repo, "Hack Squat",               "quads",      "glutes",             "quads",        "machine",     "compound",  "Keep heels on the platform; go as deep as mobility allows.");
        lib(repo, "Leg Extension",            "quads",      "",                   "quads",        "machine",     "isolation", "Pause briefly at the top; don't use momentum.");
        lib(repo, "Bulgarian Split Squat",    "quads",      "glutes,hamstrings",  "quads",        "dumbbell",    "compound",  "Rear foot elevated; most weight through the front heel.");
        lib(repo, "Smith Machine Squat",      "quads",      "glutes,hamstrings",  "quads",        "smith_machine","compound", "Position feet slightly forward for a more vertical shin angle.");

        // ── HAMSTRINGS / GLUTES ───────────────────────────────────────────────────
        lib(repo, "Romanian Deadlift",        "hamstrings", "glutes",             "hamstrings",  "barbell",     "compound",  "Soft knees; push hips back until you feel a hamstring stretch.");
        lib(repo, "Leg Curl",                 "hamstrings", "",                   "hamstrings",  "machine",     "isolation", "Flex at the top; lower slowly for a 3-second negative.");
        lib(repo, "Stiff-Leg Deadlift",       "hamstrings", "glutes",             "hamstrings",  "dumbbell",    "compound",  "Keep the dumbbells close to your legs the whole way down.");
        lib(repo, "Hip Thrust",               "glutes",     "hamstrings",         "glutes",      "barbell",     "compound",  "Drive through your heels; fully extend the hip at the top.");
        lib(repo, "Glute Kickback",           "glutes",     "hamstrings",         "glutes",      "cable",       "isolation", "Keep the core braced; isolate the glute — don't hyperextend the back.");

        // ── BICEPS ────────────────────────────────────────────────────────────────
        lib(repo, "Barbell Curl",             "biceps",     "",                   "short_head",  "barbell",     "isolation", "Keep elbows pinned to your sides throughout the lift.");
        lib(repo, "Dumbbell Curl",            "biceps",     "",                   "short_head",  "dumbbell",    "isolation", "Supinate at the top for maximum bicep peak contraction.");
        lib(repo, "Cable Curl",               "biceps",     "",                   "long_head",   "cable",       "isolation", "Cable keeps tension at the bottom — great for the long head.");
        lib(repo, "Hammer Curl",              "biceps",     "forearms",           "brachialis",  "dumbbell",    "isolation", "Neutral grip targets the brachialis for arm thickness.");
        lib(repo, "Preacher Curl",            "biceps",     "",                   "short_head",  "machine",     "isolation", "Full extension at the bottom; squeeze hard at the top.");
        lib(repo, "EZ Bar Curl",              "biceps",     "",                   "short_head",  "ez_bar",      "isolation", "Angled grip reduces wrist strain; keep elbows tucked.");

        // ── TRICEPS ───────────────────────────────────────────────────────────────
        lib(repo, "Tricep Pushdown",          "triceps",    "",                   "overall",     "cable",       "isolation", "Keep elbows fixed at your sides; fully extend and squeeze.");
        lib(repo, "Overhead Tricep Extension","triceps",    "",                   "long_head",   "cable",       "isolation", "Elbows pointing at the ceiling; maximises long-head stretch.");
        lib(repo, "Skull Crusher",            "triceps",    "",                   "long_head",   "barbell",     "isolation", "Lower bar to forehead; elbows stay vertical — don't flare.");
        lib(repo, "Dumbbell Tricep Extension","triceps",    "",                   "long_head",   "dumbbell",    "isolation", "Use both hands on one dumbbell; lower slowly behind the head.");
        lib(repo, "Close-Grip Bench Press",   "triceps",    "chest",              "overall",     "barbell",     "compound",  "Hands just inside shoulder-width; tuck elbows at 45°.");

        // ── CORE ──────────────────────────────────────────────────────────────────
        lib(repo, "Plank",                    "core",       "",                   "overall",     "bodyweight",  "isolation", "Brace abs as if bracing for a punch; breathe steadily.");
        lib(repo, "Cable Crunch",             "core",       "",                   "upper_abs",   "cable",       "isolation", "Round the spine; pull down with your abs, not your hip flexors.");
        lib(repo, "Hanging Leg Raise",        "core",       "",                   "lower_abs",   "bodyweight",  "isolation", "Control the swing; exhale as you raise the legs.");
        lib(repo, "Ab Wheel Rollout",         "core",       "",                   "overall",     "bodyweight",  "compound",  "Brace hard before rolling out; don't let your lower back sag.");
    }

    private void lib(ExerciseLibraryRepo repo,
                     String name, String muscleGroup, String secondaryMuscles,
                     String muscleArea, String equipment, String category, String tip) {
        if (!repo.existsByNameIgnoreCase(name)) {
//            repo.save(new ExerciseLibraryEntity(name, muscleGroup, secondaryMuscles,
//                    muscleArea, equipment, category, tip, 0, 0, 0));
        }
    }
}
