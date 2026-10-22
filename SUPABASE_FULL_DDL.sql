-- =============================================================
-- ARCUS - Full Schema DDL
-- Run this once in Supabase SQL Editor to create all tables.
-- After this, the app uses ddl-auto=none and never touches DDL.
-- =============================================================

-- 1. workout_template
CREATE TABLE IF NOT EXISTS workout_template (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255),
    level       VARCHAR(50),
    goal        VARCHAR(50),
    day_number  INTEGER
);

-- 2. template_exercise_entity (Hibernate default naming for TemplateExerciseEntity)
CREATE TABLE IF NOT EXISTS template_exercise_entity (
    id             BIGSERIAL PRIMARY KEY,
    template_id    BIGINT,
    exercise_name  VARCHAR(255),
    sets           INTEGER,
    rep_min        INTEGER,
    rep_max        INTEGER,
    tempo          VARCHAR(50),
    order_index    INTEGER
);

-- 3. workout_session_entity
CREATE TABLE IF NOT EXISTS workout_session_entity (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT,
    template_id BIGINT,
    created_at  TIMESTAMP,
    completed   BOOLEAN DEFAULT FALSE
);

-- 4. exercise_session_entity
CREATE TABLE IF NOT EXISTS exercise_session_entity (
    id                   BIGSERIAL PRIMARY KEY,
    workout_session_id   BIGINT,
    exercise_name        VARCHAR(255),
    target_weight        DOUBLE PRECISION,
    rep_min              INTEGER,
    rep_max              INTEGER,
    sets                 INTEGER,
    tempo                VARCHAR(50),
    completed_sets       INTEGER,
    is_completed         BOOLEAN DEFAULT FALSE
);

-- 5. set_log_entity
CREATE TABLE IF NOT EXISTS set_log_entity (
    id                    BIGSERIAL PRIMARY KEY,
    exercise_session_id   BIGINT,
    set_number            INTEGER,
    weight                DOUBLE PRECISION,
    reps                  INTEGER
);

-- 6. user_profile
CREATE TABLE IF NOT EXISTS user_profile (
    id                          BIGSERIAL PRIMARY KEY,
    user_id                     BIGINT UNIQUE NOT NULL,
    name                        VARCHAR(255) NOT NULL,
    email                       VARCHAR(255) NOT NULL,
    joined_at                   TIMESTAMP NOT NULL,
    current_level               VARCHAR(50) NOT NULL,
    fitness_goal                VARCHAR(50) NOT NULL,
    bio                         TEXT,
    total_workouts              INTEGER NOT NULL DEFAULT 0,
    total_exercise_sessions     INTEGER NOT NULL DEFAULT 0,
    total_sets_sessions         INTEGER NOT NULL DEFAULT 0,
    total_weight_lifted         DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    consecutive_workout_days    INTEGER NOT NULL DEFAULT 0,
    last_workout_date           TIMESTAMP,
    last_updated_at             TIMESTAMP NOT NULL,
    last_workout_day            INTEGER DEFAULT 0,
    last_exercise_session_id    INTEGER DEFAULT 0
);

