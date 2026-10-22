# Arcus Backend — AI Workout Plan: Complete Flow

> **Last updated:** June 2026  
> **AI model:** `gpt-4o-mini` (set via `openai.api.model` in `application.properties`)

---

## Table of Contents

1. [High-Level Summary](#1-high-level-summary)
2. [Database Tables Involved](#2-database-tables-involved)
3. [File Map](#3-file-map)
4. [Flow 1 — Onboarding: Full AI Plan Generation](#4-flow-1--onboarding-full-ai-plan-generation)
5. [Flow 2 — Daily Workout: generateCustomWorkout](#5-flow-2--daily-workout-generatecustomworkout)
6. [Flow 3 — New Cycle: Per-Day ChatGPT Refresh](#6-flow-3--new-cycle-per-day-chatgpt-refresh)
7. [Weight Assignment Logic](#7-weight-assignment-logic)
8. [Exercise Matching Logic (findBestMatch)](#8-exercise-matching-logic-findbestmatch)
9. [Legacy Path — generateWorkout](#9-legacy-path--generateworkout)
10. [All REST Endpoints](#10-all-rest-endpoints)
11. [ChatGPT Call Comparison](#11-chatgpt-call-comparison)
12. [Week-by-Week Example (PPL User)](#12-week-by-week-example-ppl-user)

---

## 1. High-Level Summary

There are **two ChatGPT calls** in the entire system:

| When | What | Tokens | Timeout | Async? |
|------|------|--------|---------|--------|
| Profile created | Full multi-day plan for all days of the split | ~6000 | 90 s | ✅ Yes — doesn't block HTTP |
| User starts a day they've already completed | Compact refresh for that single day only | ~1500 | 30 s | ❌ No — runs inline (2-4 s) |

The rest of the time — **no ChatGPT is called at all.** Exercises are read directly from the `ai_workout_plan` table.

```
ONBOARDING                 EVERY WEEK (new cycle)         EVERY OTHER WORKOUT
──────────────             ─────────────────────          ───────────────────
Profile saved              User opens Push day            User opens Push day
     │                     again (week 2+)                (same week, re-opens)
     ▼                           │                               │
ChatGPT (6000 tok)         ChatGPT (1500 tok)             Read ai_workout_plan
async background           compact "avoid last             table directly
     │                     week's exercises"                      │
     ▼                           │                               ▼
ai_workout_plan            ai_workout_plan day 1           Return exercises
populated for all days     replaced with new exercises     (no API call)
```

---

## 2. Database Tables Involved

### `user_profile`
Stores the user's level, goal, split, workout location, injuries, weak muscles etc.  
Read at the start of every workout generation to get current split and location.

### `workout_template`
Pre-seeded rows — one per day per split. E.g., PPL has 3 rows: Push (day 1), Pull (day 2), Legs (day 3).  
Used to resolve which template applies to the requested day and to create a `workout_session` record.  
Fields: `id`, `name`, `goal`, `split`, `day_number`, `muscle_group`

### `exercise_library`
Master catalog of all exercises.  
Fields: `id`, `name`, `muscle_group`, `muscle_area`, `equipment`, `category`, `level` (CSV e.g. `beginner,intermediate`), `beginner_weight`, `intermediate_weight`, `advanced_weight`, `sets`, `rep_min`, `rep_max`, `tip`, `video_url`, `workout_location`

### `ai_workout_plan` ← The core AI table
One row per exercise, per day, per user. Populated by ChatGPT at onboarding; a single day's rows are replaced each time that day starts a new cycle.  
Fields: `id`, `user_id`, `day_number`, `day_name`, `exercise_library_id`, `muscle_group`, `sets`, `rep_min`, `rep_max`, `created_at`

### `workout_session`
One row per workout the user starts.  
`completed = true` is the signal that triggers a new-cycle ChatGPT refresh for that day.  
Fields: `id`, `user_id`, `template_id`, `completed` (boolean), `total_weight`, `created_at`

### `workout_exercise_template`
Tracks which exercises were assigned to a user for a specific session. Linked to `set_log` for weight history.  
Fields: `id`, `user_id`, `day_number`, `exercise_library_id`, `created_at`

### `set_log`
Every individual set the user logs during a workout.  
Fields: `id`, `workout_exercise_template_id`, `set_number`, `weight`, `reps`

---

## 3. File Map

| File | Responsibility |
|------|----------------|
| `AiWorkoutPlan/AiWorkoutPlanEntity.java` | JPA entity for `ai_workout_plan` table |
| `AiWorkoutPlan/AiWorkoutPlanRepository.java` | Spring Data repo — find by user+day, delete by user, delete by user+day |
| `AiWorkoutPlan/AiWorkoutPlanService.java` | **Core AI logic** — full plan generation, per-cycle refresh, exercise matching, ChatGPT calls |
| `AiWorkoutPlan/AiWorkoutPlanController.java` | REST: generate, view, delete AI plan |
| `WorkoutSession/WorkoutGenerationService.java` | **Core workout logic** — generateCustomWorkout (primary), generateWorkout (legacy) |
| `WorkoutSession/WorkoutController.java` | REST: generate, complete, replace exercises |
| `UserProfile/UserProfileService.java` | After profile save → triggers `aiWorkoutPlanService.generatePlanAsync()` |
| `levelDeterminer/WeightAssignmentService.java` | Weight math — base weights, dynamic progression, RPE adjustment, gym-valid rounding |

---

## 4. Flow 1 — Onboarding: Full AI Plan Generation

### Trigger
`POST /user/profile/create` → profile saved → `aiWorkoutPlanService.generatePlanAsync(userId)` is called on a background thread (`@Async`). The HTTP response returns to the client **immediately**.

### Steps inside `AiWorkoutPlanService.generateAndStorePlan(userId)`

**Step 1 — Load full user profile from DB**  
Reads: level, goal, split, daysPerWeek, workoutLocation, injuries, weakMuscleGroups, additionalNotes, age, gender, weightKg.

**Step 2 — Determine plan size**
- PPL → 3 days | Upper/Lower → 2 days | Bro split → 5 days | Full body → 3 days
- Exercises per day: **5** (beginner) / **6** (intermediate or advanced)

**Step 3 — Build the full prompt**

The prompt includes every user profile field plus:
- A closed vocabulary for `muscleGroup`: `chest | back | shoulders | triceps | biceps | quads | hamstrings | glutes | core | calves`
- Valid `muscleArea` values per group (e.g. chest → upper/middle/lower)
- Valid `preferredEquipment` values
- A concrete day-by-day structure guide (e.g. "Day 1 Push: chest upper/middle/lower, shoulders front/side, triceps long_head/lateral_head")
- Rules: respect injuries, prioritise weak muscles, equipment must match location

**Step 4 — Call ChatGPT (90 s timeout, max_tokens 6000)**

ChatGPT returns **attributes** — not exercise names — for each slot:
```json
{
  "planName": "Alex's PPL Plan",
  "days": [
    {
      "dayNumber": 1,
      "dayName": "Push – Chest, Shoulders & Triceps",
      "exercises": [
        { "muscleGroup": "chest", "muscleArea": "upper",
          "preferredEquipment": "barbell", "category": "compound",
          "sets": 4, "repMin": 6, "repMax": 10 }
      ]
    }
  ]
}
```

**Step 5 — Match each slot to `exercise_library`** — see §8 for the cascade logic.

**Step 6 — Persist to `ai_workout_plan`**  
Old entries deleted first (`deleteByUserId`). One row saved per matched exercise.

**On any failure** — timeout, bad JSON, no matches — the error is logged and the plan is simply not stored. `generateCustomWorkout` falls back to random selection until the plan exists.

---

## 5. Flow 2 — Daily Workout: generateCustomWorkout

### Trigger
`POST /workout/generateCustom` from the mobile app with:
```json
{ "userId": 5, "level": "intermediate", "goal": "muscle_gain",
  "split": "ppl", "lastWorkoutDay": 1, "requestedMuscles": ["chest", "shoulders", "triceps"] }
```

### Full Step-by-Step

```
1.  Read user profile from DB
    → use split from DB, not from request (handles split changes)
    → derive workoutLocation → build allowed equipment list

2.  Resolve WorkoutTemplate
    → findFirstByGoalAndSplitAndDayNumber(goal, split, dayNumber)
    → gives us template_id, used to detect cycle completion

3.  Delete incomplete workout_session rows for this user (always start fresh)

4.  Delete stale workout_exercise_template rows for this user+day (recreated below)

5.  CYCLE DETECTION
    → findByUserIdAndTemplateIdAndCompletedTrue(userId, templateId)
    → result NOT empty → user has finished this day before → hasDoneThisDayBefore = true
    → result empty     → first time on this day → hasDoneThisDayBefore = false

6.  If hasDoneThisDayBefore = true AND ai_workout_plan has entries for this day
    → [Go to Flow 3 — ChatGPT Refresh]
    → ai_workout_plan rows for this day are replaced
    → re-read ai_workout_plan after refresh

7.  Read ai_workout_plan WHERE user_id=? AND day_number=?

    ┌─ AI PLAN PATH (rows found) ─────────────────────────────────────────────┐
    │  For each ai_workout_plan row:                                           │
    │  → fetch exercise_library by exercise_library_id                        │
    │  → use AI-prescribed sets/repMin/repMax (fallback to library defaults)   │
    │  → include tip, videoUrl, muscleArea, secondaryMuscles from library      │
    └─────────────────────────────────────────────────────────────────────────┘

    ┌─ FALLBACK PATH (no rows — plan not generated yet or failed) ────────────┐
    │  Use requestedMuscles from the request body                             │
    │  → distributeExercises() splits count across groups (major gets ~60%)  │
    │  → query exercise_library by muscleGroup + level + equipment filter     │
    │  → pick randomly from results                                           │
    └─────────────────────────────────────────────────────────────────────────┘

8.  Save workout_exercise_template rows (one per exercise, fresh IDs each session)

9.  Assign weights for each exercise
    → check set_log WHERE workout_exercise_template_id = <new id>
    → if logs found → average of logged weights → round to valid gym weight
    → if no logs   → read beginner/intermediate/advanced_weight from exercise_library
                   → round to valid gym weight

10. Create new workout_session (completed=false)

11. Return WorkoutResponseDTO
    → workoutSessionId, level, dayNumber, List<ExerciseDTO>
    → ExerciseDTO: exerciseName, targetWeight, sets, repMin, repMax,
                   tempo, tip, videoUrl, muscleArea, exerciseTemplateSessionID
```

---

## 6. Flow 3 — New Cycle: Per-Day ChatGPT Refresh

Runs inline inside `generateCustomWorkout` when cycle detection fires (step 5-6 above).

### Steps inside `AiWorkoutPlanService.refreshDayPlan()`

**Step 1 — Collect last cycle's exercise names**  
Reads current `ai_workout_plan` rows for this user+day, resolves names via `exercise_library`.

**Step 2 — Build compact prompt (~200 tokens)**
```
Day: Push – Chest, Shoulders & Triceps
Level: intermediate | Goal: muscle_gain | Equipment: commercial gym

Exercises done LAST CYCLE — DO NOT repeat any of these:
Incline Barbell Bench Press, Flat Dumbbell Press, Cable Fly, ...

Generate exactly 6 NEW exercises.
[closed vocabulary for muscleGroup and equipment]
Return ONLY JSON: {"exercises":[{...}]}
```

**Step 3 — Call ChatGPT (temperature 0.9, max_tokens 1500, 30 s block timeout)**  
Higher temperature → more variety in exercise selection.

**Step 4 — Match to `exercise_library`** via same 5-level cascade (§8).

**Step 5 — Safety net**: if a matched exercise is still in the previous list, skip it.

**Step 6 — Replace only this day's rows**
```
deleteByUserIdAndDayNumber(userId, dayNumber)
saveAll(newEntries)
```

**On failure** — old plan kept, workout still loads with last cycle's exercises. Silent fallback.

---

## 7. Weight Assignment Logic

Weights are assigned **after** exercise selection, for each `workout_exercise_template` row just created.

### Rule 1 — Check set_log for current template ID
```
setLogRepo.findByWorkoutExerciseTemplateIdOrderBySetNumberAsc(template.getId())
```
Since template IDs are fresh each session, this only finds logs from the **current session** (i.e., the user already did some sets and re-opened the workout). It does not carry over history from previous weeks.

### Rule 2 — Base weight from exercise_library (no history)
Reads the column matching the user's level:
- `beginner_weight` / `intermediate_weight` / `advanced_weight`
- Falls back to 10 kg if the column is null

### Rule 3 — Round to a valid gym weight
`weightAssignmentService.roundToGymWeight(raw, exerciseName)` enforces gym-realistic increments:
- Exercise name includes `dumbbell / db / barbell / bb / bench` → split-weight aware rounding
- Valid increments: 2.5, 5, 7.5, 10, 12.5, 15, 17.5, 20, 22.5, 25 ... kg

---

## 8. Exercise Matching Logic (findBestMatch)

ChatGPT returns **attributes**, not names. `findBestMatch()` translates them to a real `exercise_library` row using a 5-level cascade:

```
Input: { muscleGroup, muscleArea, preferredEquipment, category, userLevel, workoutLocation }

Step 1: muscleGroup + muscleArea + equipment + level + location filter  ← most specific
        ↓ no match
Step 2: muscleGroup + muscleArea + level + location filter (any equipment)
        ↓ no match
Step 3: muscleGroup + level + location filter (any area)
        ↓ no match
Step 4: muscleGroup + level only (any area, any equipment)
        ↓ no match
Step 5: muscleGroup only — ignore level entirely              ← guarantees a result if exercise exists
```

Each step picks **one exercise at random** from the candidates.

### Muscle Group Normalisation

ChatGPT sometimes drifts from the prompt vocabulary. `normaliseMuscleGroup()` catches this:

| ChatGPT says | Normalised to |
|--------------|---------------|
| `legs`, `lower body` | `quads` |
| `arms` | `biceps` |
| `lats`, `upper back` | `back` |
| `traps`, `trapezius` | `back` |
| `rear delt` | `shoulders` |
| `abs`, `abdominals` | `core` |
| `pecs`, `pectorals` | `chest` |
| `delts`, `deltoids` | `shoulders` |
| `calf` | `calves` |

### Equipment Filter

| `workoutLocation` | Allowed equipment passed to query |
|-------------------|----------------------------------|
| `gym` | barbell, dumbbell, cable, machine, smith_machine, bodyweight, ez_bar |
| `home` | dumbbell, bodyweight, resistance_band |
| `both` | no restriction |

---

## 9. Legacy Path — generateWorkout

**Endpoint:** `GET /workout/generateWorkout?userId=X&level=Y&goal=Z`

Kept for backward compatibility. Does **not** use the AI plan.

**Differences from generateCustomWorkout:**
- Reads exercises from `WorkoutTemplate` + `TemplateExercise` (pre-seeded DB rows)
- Creates `ExerciseSession` records, not `WorkoutExerciseTemplate`
- Weight logic: searches `ExerciseSession` history by user + exercise name (`findRecentExercisesByUserAndName`)
- Calls `WeightAssignmentService.assignDynamicAdjustment()` for progressive overload based on ExerciseSession history

---

## 10. All REST Endpoints

### AI Plan (`/ai-plan`)

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/ai-plan/generate?userId={id}` | Trigger/re-trigger full plan generation (synchronous) |
| `GET` | `/ai-plan/{userId}` | View stored plan grouped by day with exercise names |
| `DELETE` | `/ai-plan/{userId}` | Delete plan — next workout falls back to random selection |

Re-trigger after: split change, goal change, injury/location update.

### Workout (`/workout`)

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/workout/generateCustom` | **Primary path** — AI plan + cycle refresh |
| `GET` | `/workout/generateWorkout` | Legacy path using TemplateExercise rows |
| `POST` | `/workout/{sessionId}/assignWeights` | Retroactively assign weights to a session |
| `GET` | `/workout/nextWorkoutName` | Next workout name + last session weight delta |
| `POST` | `/workout/completeWorkout?userId=X&totalWeight=Y` | Mark session complete, save total weight |
| `POST` | `/workout/replaceExercise` | Swap one exercise with a different one from the same muscle group |

### Profile (`/user/profile`)

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/user/profile/create` | Create profile → triggers async AI plan |
| `GET` | `/user/profile/{userId}` | Get full profile |
| `PUT` | `/user/profile/{userId}` | Update profile (re-trigger AI plan manually if needed) |

---

## 11. ChatGPT Call Comparison

| | Full Plan (Onboarding) | Compact Refresh (Per-Cycle) |
|--|------------------------|------------------------------|
| Method | `callChatGpt()` | `callChatGptCompact()` |
| Trigger | Profile creation | User re-opens a completed day |
| Response covers | All days of the split | Single day only |
| max_tokens | 6000 | 1500 |
| Temperature | 0.7 (consistent) | 0.9 (more variety) |
| Timeout | 90 s | 30 s block timeout |
| Execution | `@Async` background thread | Synchronous inline (2–4 s) |
| On failure | Plan not stored, fallback to random | Old plan kept, workout still loads |
| Estimated cost (gpt-4o-mini) | ~$0.001 | ~$0.0003 |
| Frequency | Once per user (or on manual re-trigger) | ~Once per day per week |

---

## 12. Week-by-Week Example (PPL User)

**Alex — intermediate, muscle_gain, PPL, gym**

### Day 0 — Profile Created
```
POST /user/profile/create
→ Profile saved → HTTP 200 returned immediately
→ Background thread: ChatGPT generates 3-day plan (18 exercises total)
→ ai_workout_plan populated:
    day 1: Incline Barbell Bench Press, Flat DB Press, Cable Fly,
            Overhead Press, Lateral Raises, Tricep Pushdown
    day 2: Pull-ups, Seated Cable Row, DB Pullover,
            Face Pull, EZ Bar Curl, Hammer Curl
    day 3: Barbell Squat, Leg Press, Romanian Deadlift,
            Leg Curl, Glute Bridge, Plank
```

### Week 1 — Push / Pull / Legs (first time each)
```
For each day:
→ findByUserIdAndTemplateIdAndCompletedTrue → empty → hasDoneThisDayBefore = false
→ NO ChatGPT call
→ Read ai_workout_plan → 6 rows
→ Assign base weights from exercise_library columns
→ User completes workout → completeWorkout called → workout_session.completed = true
```

### Week 2 — Push Day (new cycle detected)
```
POST /workout/generateCustom { dayNumber: 1 }

→ findByUserIdAndTemplateIdAndCompletedTrue → [session from week 1] → NOT empty
→ hasDoneThisDayBefore = true

→ Previous exercises collected: ["Incline Barbell Bench Press", "Flat DB Press", "Cable Fly", ...]
→ refreshDayPlan() called with avoid list

→ ChatGPT compact call:
    "Avoid: Incline Barbell Bench Press, Flat DB Press, Cable Fly, ..."
    Returns: Smith Machine Bench, Pec Deck, DB Incline Press,
             Arnold Press, Cable Lateral Raise, Overhead Tricep Extension

→ ai_workout_plan day 1 replaced with fresh exercises
→ User sees a completely different Push day ✓
```

### Week 3 — Push Day
Same pattern — week 2's exercises are in the avoid list, ChatGPT picks new ones again.

---

## Quick Reference — When to Re-Trigger AI Plan

| Scenario | Action |
|----------|--------|
| User just created profile | Automatic (async) |
| User changed split (e.g. PPL → bro split) | `POST /ai-plan/generate?userId={id}` |
| User changed fitness goal | `POST /ai-plan/generate?userId={id}` |
| User updated injuries or weak muscles | `POST /ai-plan/generate?userId={id}` |
| User changed location (gym → home) | `POST /ai-plan/generate?userId={id}` |
| New cycle for a specific day | Automatic (inline during generateCustom) |
| View current plan | `GET /ai-plan/{userId}` |
| Force full reset | `DELETE /ai-plan/{userId}` then `POST /ai-plan/generate?userId={id}` |

---

## Database DDL Required

Run `AI_WORKOUT_PLAN_DDL.sql` in Supabase SQL editor before first deploy:

```sql
CREATE TABLE IF NOT EXISTS public.ai_workout_plan (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT        NOT NULL,
    day_number          INTEGER       NOT NULL,
    day_name            VARCHAR(255),
    exercise_library_id BIGINT REFERENCES public.exercise_library(id) ON DELETE SET NULL,
    muscle_group        VARCHAR(100),
    sets                INTEGER,
    rep_min             INTEGER,
    rep_max             INTEGER,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ai_workout_plan_user_day
    ON public.ai_workout_plan (user_id, day_number);
```
