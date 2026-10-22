# Set Evaluation System - Implementation Guide

## Overview

I've implemented a comprehensive **Set Evaluation System** for the Arcus Fitness App that evaluates user performance after each set is logged. The system detects fatigue based on rep drops and provides intelligent rest time recommendations.

## Architecture

The implementation follows a clean, layered architecture:

```
Controller Layer (SetLogController)
        ↓
Service Layer (SetLogService + SetEvaluationService)
        ↓
Repository Layer (SetLogRepo)
        ↓
Entity/DTO Layer
```

## Files Created/Modified

### 1. **SetEvaluationDTO.java** (NEW)
**Location:** `/src/main/java/com/arcus/arc1/dto/SetEvaluationDTO.java`

Response DTO that encapsulates set evaluation feedback.

**Fields:**
- `fatigueDetected: Boolean` - Whether fatigue was detected
- `suggestedRestSeconds: Integer` - Recommended rest time in seconds
- `message: String` - User-friendly feedback message

**Example Response:**
```json
{
  "fatigueDetected": false,
  "suggestedRestSeconds": 120,
  "message": "Good control. Maintain tempo."
}
```

### 2. **SetEvaluationService.java** (NEW)
**Location:** `/src/main/java/com/arcus/arc1/SetLog/SetEvaluationService.java`

Core business logic for evaluating set performance.

**Key Features:**

#### Fatigue Detection Algorithm
- **Absolute Threshold:** If reps drop by ≥2 reps compared to first set → FATIGUE
- **Percentage Threshold:** If reps drop by ≥15% compared to first set → FATIGUE
- Uses the more conservative (stricter) of the two thresholds

**Example:**
- First set: 10 reps
- Second set: 7 reps
- Rep drop: 3 reps (30%) → **Fatigue Detected** ✓

#### Rest Time Recommendations
Based on rep range (correlates to weight intensity):

| Rep Range | Rest Time | Use Case |
|-----------|-----------|----------|
| < 6 reps | 180 sec (3 min) | Heavy compound lifts (Squat, Deadlift, Bench) |
| 6-11 reps | 120 sec (2 min) | Moderate intensity strength work |
| ≥ 12 reps | 60 sec (1 min) | Light isolation exercises |

**Rationale:** Heavier weights (lower reps) require longer recovery for CNS recovery.

#### Message Generation
Dynamic feedback messages based on performance:
- ✓ **No Fatigue:** "Good control. Maintain tempo."
- ⚠️ **Fatigue Detected:** "Fatigue detected. Reps dropped {%}% from first set. Extend rest or reduce weight on next set."

**Key Methods:**

1. **evaluateSet(exerciseSessionId, setNumber)**
   - Main evaluation orchestrator
   - Fetches all sets for the exercise session
   - Compares current set to first set
   - Returns comprehensive feedback

2. **detectFatigue(firstSet, currentSet)**
   - Detects fatigue using dual-threshold approach
   - Absolute: ≥2 rep drop
   - Percentage: ≥15% rep drop

3. **determineRestTime(set)**
   - Maps rep count to rest duration
   - Considers intensity level implicitly via reps

4. **generateMessage(fatigueDetected, currentSet, firstSet)**
   - Creates user-friendly feedback
   - Shows percentage drop if fatigued

### 3. **SetLogService.java** (MODIFIED)
**Location:** `/src/main/java/com/arcus/arc1/SetLog/SetLogService.java`

Updated to coordinate between logging and evaluation.

**Changes:**
- Injected `SetEvaluationService` dependency
- Added new method: `saveLogAndEvaluate(SetLogDTO)` → returns `SetEvaluationDTO`
- Kept legacy `saveLog()` method for backwards compatibility

**Flow:**
```
1. Save SetLogEntity to database
2. Call SetEvaluationService.evaluateSet()
3. Return SetEvaluationDTO with feedback
```

### 4. **SetLogController.java** (MODIFIED)
**Location:** `/src/main/java/com/arcus/arc1/SetLog/SetLogController.java`

Updated endpoint to return evaluation feedback.

**Endpoint:**
```
POST /logs/log-set
Content-Type: application/json

Body:
{
  "exerciseSessionId": 14,
  "setNumber": 1,
  "weight": 50.0,
  "reps": 10
}

Response:
{
  "fatigueDetected": false,
  "suggestedRestSeconds": 120,
  "message": "Good control. Maintain tempo."
}
```

## System Design Decisions

### 1. Fatigue Detection: Two-Threshold Approach
- **Why?** Single threshold might miss edge cases
- **Absolute (2 reps):** Catches significant drops in any rep range
- **Percentage (15%):** Catches relative drops (e.g., 12→10 reps = 17%)
- **Implementation:** Uses OR logic (either threshold triggers fatigue)

### 2. Rest Time Based on Reps
- **Why?** Reps correlate to weight intensity
- **Biological basis:** Heavier loads (3-5 reps) require longer ATP-PC recovery (~3 min)
- **Lighter loads (12+ reps):** Metabolic fatigue, less CNS recovery needed
- **Flexibility:** Can be adjusted later per user level/goal

### 3. Service Layer Separation
- **SetLogService:** Orchestrates the workflow (save + evaluate)
- **SetEvaluationService:** Isolated logic for testability
- **Benefits:** Easy to test, easy to extend (e.g., add progressive overload logic)

### 4. Message-Based Feedback
- User-friendly descriptions instead of raw data
- Actionable advice ("reduce weight" vs. "fatigue=true")
- Foundation for future ML-based coaching messages

## Integration Points

### Current Flow
```
Frontend POST /logs/log-set
    ↓
SetLogController.logSet()
    ↓
SetLogService.saveLogAndEvaluate()
    ├─ Save SetLogEntity
    └─ Evaluate via SetEvaluationService
        ├─ Fetch all sets for exercise session
        ├─ Detect fatigue
        ├─ Determine rest time
        └─ Generate message
    ↓
Return SetEvaluationDTO
    ↓
Frontend renders feedback to user
```

## Future Extension Points

### 1. Progressive Overload Logic
**Next Phase:** Implement rolling average progression (last 3 sessions)
```java
// In SetEvaluationService
private Double calculateProgressionWeight(ExerciseSessionEntity exercise) {
    List<WorkoutSession> last3Sessions = getLastThreeSessions(exercise);
    Double avgWeight = calculateRollingAverage(last3Sessions);
    return suggestNextWeight(avgWeight);
}
```

### 2. User Level Customization
Make thresholds user-specific:
```java
private void loadThresholdsForUser(Long userId) {
    UserProfile profile = userRepo.findById(userId);
    REP_DROP_THRESHOLD = profile.getFatigueThreshold();
    REST_HEAVY_COMPOUND = profile.getRestPreference();
}
```

### 3. Exercise-Specific Rules
```java
private Integer determineRestTime(SetLogEntity set, String exerciseName) {
    if (isCompound(exerciseName)) return REST_HEAVY_COMPOUND;
    if (isIsolation(exerciseName)) return REST_LIGHT;
    // ...
}
```

### 4. Velocity-Based Fatigue Detection
Add bar velocity tracking for more accurate fatigue detection:
```java
private boolean detectFatigueByVelocity(SetLogEntity first, SetLogEntity current) {
    if (firstVelocity - currentVelocity > 0.3) {  // 30% velocity drop
        return true;
    }
}
```

## Testing Recommendations

### Unit Tests for SetEvaluationService
```java
@Test
public void testFatigueDetection_AbsoluteThreshold() {
    // First set: 10 reps, Second set: 8 reps → Fatigue (2 rep drop)
    assertTrue(evaluationService.detectFatigue(...));
}

@Test
public void testFatigueDetection_PercentageThreshold() {
    // First set: 12 reps, Second set: 10 reps (17% drop) → Fatigue
    assertTrue(evaluationService.detectFatigue(...));
}

@Test
public void testRestTime_HeavyCompound() {
    // 5 reps → 180 sec rest
    assertEquals(180, evaluationService.determineRestTime(...));
}
```

### Integration Tests
```java
@Test
public void testLogSetAndEvaluate() {
    // Full flow: log set and verify evaluation response
    SetEvaluationDTO result = controller.logSet(new SetLogDTO(...));
    assertFalse(result.getFatigueDetected());
    assertEquals(120, result.getSuggestedRestSeconds());
}
```

## Configuration Constants

**Current values (easily adjustable):**
```java
REP_DROP_THRESHOLD = 2              // Absolute rep drop
REP_DROP_PERCENTAGE = 0.15          // 15% relative drop
REST_HEAVY_COMPOUND = 180           // 3 minutes
REST_MODERATE = 120                 // 2 minutes
REST_LIGHT = 60                     // 1 minute
```

These can be moved to `application.properties` for runtime configuration:
```properties
arcus.fatigue.rep-drop-threshold=2
arcus.fatigue.rep-drop-percentage=0.15
arcus.rest.heavy-compound=180
arcus.rest.moderate=120
arcus.rest.light=60
```

## Validation & Error Handling

Current implementation handles edge cases:
- ✓ Empty set logs → Default feedback
- ✓ Null checks on set data
- ✓ Single set per exercise → No comparison, default feedback

**Future improvements:**
- Add logging for debugging
- Implement error metrics tracking
- Add circuit breaker for database failures

## Compile Status

✅ **Project builds successfully** - No errors, all dependencies resolved

```
BUILD SUCCESSFUL in 5s
6 actionable tasks: 6 executed
```

## Summary

The Set Evaluation System is now fully integrated into your Arcus backend. It provides intelligent, science-backed feedback to users after each set, laying the foundation for future progressive overload recommendations and advanced coaching features.

The implementation is:
- ✅ Clean and maintainable
- ✅ Testable (isolated service logic)
- ✅ Extensible (easy to add new evaluation criteria)
- ✅ Production-ready (proper exception handling, logging-ready)

