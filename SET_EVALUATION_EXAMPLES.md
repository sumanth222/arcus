# Set Evaluation System - Quick Start & Examples

## Quick Start

### 1. POST a Set with Frontend
```bash
curl -X POST http://localhost:8080/logs/log-set \
  -H "Content-Type: application/json" \
  -d '{
    "exerciseSessionId": 14,
    "setNumber": 1,
    "weight": 50.0,
    "reps": 10
  }'
```

### 2. Response Example
```json
{
  "fatigueDetected": false,
  "suggestedRestSeconds": 120,
  "message": "Good control. Maintain tempo."
}
```

---

## Real-World Scenarios

### Scenario 1: No Fatigue (Strength Set)
**Situation:** User doing heavy squats
- Set 1: 5 reps at 185 lbs
- Set 2: 5 reps at 185 lbs

**Response:**
```json
{
  "fatigueDetected": false,
  "suggestedRestSeconds": 180,
  "message": "Good control. Maintain tempo."
}
```

**Why:** 
- No rep drop (5 → 5)
- Rep range < 6 → 3 min rest recommended

---

### Scenario 2: Moderate Fatigue (Hypertrophy Set)
**Situation:** User doing barbell rows
- Set 1: 10 reps at 185 lbs
- Set 2: 8 reps at 185 lbs

**Response:**
```json
{
  "fatigueDetected": true,
  "suggestedRestSeconds": 120,
  "message": "Fatigue detected. Reps dropped 20% from first set. Extend rest or reduce weight on next set."
}
```

**Why:**
- Rep drop: 10 → 8 = 2 reps (meets absolute threshold)
- Percentage drop: 20% (exceeds 15% threshold)
- Rep range 6-11 → 2 min rest

---

### Scenario 3: Severe Fatigue (Isolation Exercise)
**Situation:** User doing leg press
- Set 1: 15 reps at 315 lbs
- Set 2: 11 reps at 315 lbs
- Set 3: 8 reps at 315 lbs

**For Set 2:**
```json
{
  "fatigueDetected": true,
  "suggestedRestSeconds": 60,
  "message": "Fatigue detected. Reps dropped 27% from first set. Extend rest or reduce weight on next set."
}
```

**For Set 3:**
```json
{
  "fatigueDetected": true,
  "suggestedRestSeconds": 60,
  "message": "Fatigue detected. Reps dropped 47% from first set. Extend rest or reduce weight on next set."
}
```

**Why:**
- Set 2: 27% drop (exceeds 15%) → Fatigue
- Set 3: 47% drop → Strong fatigue signal
- Rep range ≥12 → 1 min rest (isolation work)

---

### Scenario 4: First Set (No Comparison)
**Situation:** User logs first set of the day
- Set 1: 10 reps at 185 lbs

**Response:**
```json
{
  "fatigueDetected": false,
  "suggestedRestSeconds": 120,
  "message": "Set logged successfully."
}
```

**Why:** No previous sets to compare to, neutral message

---

## Frontend Integration Example

### React Component
```jsx
import { useState } from 'react';

function SetLogger() {
  const [feedback, setFeedback] = useState(null);
  const [isLoading, setIsLoading] = useState(false);

  const logSet = async (exerciseSessionId, setNumber, weight, reps) => {
    setIsLoading(true);
    
    try {
      const response = await fetch('/logs/log-set', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          exerciseSessionId,
          setNumber,
          weight,
          reps
        })
      });

      const evaluation = await response.json();
      setFeedback(evaluation);

      // Display feedback to user
      if (evaluation.fatigueDetected) {
        showWarning(`⚠️ ${evaluation.message}`);
        showTimer(evaluation.suggestedRestSeconds);
      } else {
        showSuccess(`✅ ${evaluation.message}`);
        showTimer(evaluation.suggestedRestSeconds);
      }
    } catch (error) {
      console.error('Error logging set:', error);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div>
      <button onClick={() => logSet(14, 1, 50.0, 10)}>
        Log Set
      </button>
      {feedback && (
        <div className={feedback.fatigueDetected ? 'warning' : 'success'}>
          <p>{feedback.message}</p>
          <p>Rest for: {feedback.suggestedRestSeconds} seconds</p>
        </div>
      )}
    </div>
  );
}
```

---

## REST Timer Implementation

### JavaScript Timer Component
```jsx
function RestTimer({ durationSeconds, onComplete }) {
  const [timeLeft, setTimeLeft] = useState(durationSeconds);

  useEffect(() => {
    if (timeLeft <= 0) {
      onComplete?.();
      return;
    }

    const interval = setInterval(() => {
      setTimeLeft(prev => prev - 1);
    }, 1000);

    return () => clearInterval(interval);
  }, [timeLeft, onComplete]);

  const minutes = Math.floor(timeLeft / 60);
  const seconds = timeLeft % 60;

  return (
    <div className="timer">
      <h2>{minutes}:{seconds.toString().padStart(2, '0')}</h2>
      <p>Rest time remaining</p>
    </div>
  );
}
```

---

## Database Query Examples

### Check Set History for Exercise
```sql
SELECT * FROM set_log_entity 
WHERE exercise_session_id = 14 
ORDER BY set_number ASC;
```

**Output Example:**
```
id  | exercise_session_id | set_number | weight | reps
----|---------------------|-----------|--------|-----
1   | 14                  | 1         | 50.0   | 10
2   | 14                  | 2         | 50.0   | 9
3   | 14                  | 3         | 50.0   | 8
```

### Track Fatigue Over Multiple Sessions
```sql
SELECT 
    ws.id as workout_session_id,
    es.id as exercise_session_id,
    es.exercise_name,
    sl.set_number,
    sl.reps,
    (
        SELECT reps FROM set_log_entity 
        WHERE exercise_session_id = es.id 
        ORDER BY set_number ASC 
        LIMIT 1
    ) as first_set_reps,
    ROUND(100.0 * (
        (SELECT reps FROM set_log_entity WHERE exercise_session_id = es.id ORDER BY set_number ASC LIMIT 1) - sl.reps
    ) / (SELECT reps FROM set_log_entity WHERE exercise_session_id = es.id ORDER BY set_number ASC LIMIT 1), 1) as rep_drop_percent
FROM workout_session_entity ws
JOIN exercise_session_entity es ON ws.id = es.workout_session_id
JOIN set_log_entity sl ON es.id = sl.exercise_session_id
WHERE ws.user_id = 1
ORDER BY ws.created_at DESC, es.id, sl.set_number;
```

---

## Testing the System

### Manual Test Scenario
1. Create a workout session
2. Log Set 1: 185 lbs × 10 reps
3. Verify response: `fatigueDetected=false, restSeconds=120`
4. Wait 2 minutes
5. Log Set 2: 185 lbs × 8 reps
6. Verify response: `fatigueDetected=true, message contains "20%"`
7. Log Set 3: 185 lbs × 7 reps
8. Verify response: `fatigueDetected=true, message contains "30%"`

---

## Performance Considerations

### Query Optimization
The current implementation uses:
```java
List<SetLogEntity> allSets = 
    setLogRepo.findByExerciseSessionIdOrderBySetNumberAsc(exerciseSessionId);
```

**Current:** Fetches all sets for the exercise session
- **Pros:** Simple, maintainable
- **Cons:** Scales linearly with number of sets

**Future optimization for high volume:**
```java
// Only fetch first and current set
SetLogEntity firstSet = setLogRepo.findFirstByExerciseSessionIdOrderBySetNumberAsc(exerciseSessionId);
SetLogEntity currentSet = setLogRepo.findByExerciseSessionIdAndSetNumber(exerciseSessionId, setNumber);
```

---

## Troubleshooting

### Issue: "Unexpected Rest Time"
**Check:**
- Is the rep count correctly mapped to rest duration?
- Heavy compound (< 6 reps) → 180 seconds
- Moderate (6-11 reps) → 120 seconds
- Light isolation (≥ 12 reps) → 60 seconds

### Issue: "Fatigue Not Detected When Expected"
**Check:**
- Rep drop must be ≥ 2 reps OR ≥ 15%
- Example: 10 reps → 9 reps = 10% drop (not detected)
- Example: 10 reps → 8 reps = 20% drop (detected ✓)

### Issue: "Wrong Exercise Session ID"
**Verify:**
- Exercise session exists in database
- Set is being logged to correct exercise session
- Use `/workout/generate` response to get correct exerciseSessionId

---

## Future Enhancement Ideas

1. **Cumulative Fatigue Tracking**
   - Track fatigue across entire workout
   - Flag if user is tired before starting next exercise

2. **Rep Range Flexibility**
   - Let users set custom rep ranges per exercise
   - Personalize fatigue thresholds

3. **Weight Maintenance Detection**
   - Alert if user is maintaining same weight/reps
   - Suggest progressive overload

4. **Peak Performance Tracking**
   - Remember user's best sets
   - Show "Personal Record" alerts

5. **Coaching AI Integration**
   - ML model to predict fatigue before it happens
   - Recommend weight adjustments mid-workout

---

## API Reference

### POST /logs/log-set

**Request:**
```json
{
  "exerciseSessionId": 14,
  "setNumber": 1,
  "weight": 50.0,
  "reps": 10
}
```

**Response:**
```json
{
  "fatigueDetected": boolean,
  "suggestedRestSeconds": number,
  "message": string
}
```

**HTTP Status:**
- `200 OK` - Set logged successfully
- `400 Bad Request` - Invalid input (missing fields)
- `404 Not Found` - Exercise session not found
- `500 Internal Server Error` - Database error

**Curl Example:**
```bash
curl -X POST http://localhost:8080/logs/log-set \
  -H "Content-Type: application/json" \
  -d '{"exerciseSessionId":14,"setNumber":1,"weight":50.0,"reps":10}'
```

