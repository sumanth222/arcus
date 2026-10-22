-- User Profile Table DDL for Supabase PostgreSQL
-- This script creates the user_profile table with all necessary columns and constraints

CREATE TABLE IF NOT EXISTS user_profile (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    current_level VARCHAR(50) NOT NULL,
    fitness_goal VARCHAR(50) NOT NULL,
    bio TEXT,
    total_workouts INTEGER NOT NULL DEFAULT 0,
    total_exercise_sessions INTEGER NOT NULL DEFAULT 0,
    total_sets_sessions INTEGER NOT NULL DEFAULT 0,
    total_weight_lifted DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    consecutive_workout_days INTEGER NOT NULL DEFAULT 0,
    last_workout_date TIMESTAMP,
    last_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for commonly queried columns
CREATE INDEX IF NOT EXISTS idx_user_profile_user_id ON user_profile(user_id);
CREATE INDEX IF NOT EXISTS idx_user_profile_current_level ON user_profile(current_level);
CREATE INDEX IF NOT EXISTS idx_user_profile_fitness_goal ON user_profile(fitness_goal);
CREATE INDEX IF NOT EXISTS idx_user_profile_last_workout_date ON user_profile(last_workout_date);

-- Create a comment on the table for documentation
COMMENT ON TABLE user_profile IS 'User fitness profiles with complete workout statistics and engagement metrics';
COMMENT ON COLUMN user_profile.user_id IS 'Unique user identifier - foreign key to users table';
COMMENT ON COLUMN user_profile.total_weight_lifted IS 'Total weight lifted calculated as sum of (weight × reps) for all sets';
COMMENT ON COLUMN user_profile.consecutive_workout_days IS 'Current streak of consecutive days with workouts';
COMMENT ON COLUMN user_profile.last_updated_at IS 'Timestamp of last profile update';

-- Optional: Create a trigger to automatically update the updated_at column
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

-- Insert sample data (optional - remove in production)
-- INSERT INTO user_profile (
--     user_id,
--     name,
--     email,
--     current_level,
--     fitness_goal,
--     bio
-- ) VALUES (
--     1,
--     'John Doe',
--     'john@example.com',
--     'beginner',
--     'muscle_gain',
--     'Fitness enthusiast'
-- );

