-- MINIMAL VERSION - JUST COPY & PASTE THIS INTO SUPABASE SQL EDITOR

CREATE TABLE user_profile (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    current_level VARCHAR(50) NOT NULL,
    fitness_goal VARCHAR(50) NOT NULL,
    bio TEXT,
    total_workouts INTEGER DEFAULT 0,
    total_exercise_sessions INTEGER DEFAULT 0,
    total_sets_sessions INTEGER DEFAULT 0,
    total_weight_lifted DOUBLE PRECISION DEFAULT 0.0,
    consecutive_workout_days INTEGER DEFAULT 0,
    last_workout_date TIMESTAMP,
    last_updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_profile_user_id ON user_profile(user_id);
CREATE INDEX idx_user_profile_current_level ON user_profile(current_level);
CREATE INDEX idx_user_profile_fitness_goal ON user_profile(fitness_goal);

CREATE OR REPLACE FUNCTION update_user_profile_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER user_profile_updated_at_trigger
BEFORE UPDATE ON user_profile
FOR EACH ROW
EXECUTE FUNCTION update_user_profile_timestamp();

