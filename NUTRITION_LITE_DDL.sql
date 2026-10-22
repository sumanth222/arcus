-- ============================================================
-- Arcus — Nutrition Lite  |  Database DDL
-- Run in your Supabase SQL editor (or any PostgreSQL client)
-- ============================================================


-- ─────────────────────────────────────────────────────────────
-- 1.  ALTER existing user_profile table
--     Add body weight + height columns needed for calculations
-- ─────────────────────────────────────────────────────────────
ALTER TABLE public.user_profile
    ADD COLUMN IF NOT EXISTS weight_kg  DOUBLE PRECISION NULL,
    ADD COLUMN IF NOT EXISTS height_cm  DOUBLE PRECISION NULL;


-- ─────────────────────────────────────────────────────────────
-- 2.  nutrition_profile
--     One row per user — stores computed daily targets.
--     Re-written on every call to GET /nutrition/target
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.nutrition_profile (
    id                    BIGSERIAL PRIMARY KEY,
    user_id               BIGINT           NOT NULL UNIQUE,
    protein_target_grams  DOUBLE PRECISION NOT NULL,
    calorie_target_min    INTEGER          NOT NULL,
    calorie_target_max    INTEGER          NOT NULL,
    weight_kg_snapshot    DOUBLE PRECISION NOT NULL,
    last_updated          TIMESTAMP        NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_nutrition_profile_user_id
    ON public.nutrition_profile (user_id);


-- ─────────────────────────────────────────────────────────────
-- 3.  food_item
--     Static catalog — seeded once by the DataSeeder bean.
--     17 items (veg / non_veg / shake) are inserted on startup.
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.food_item (
    id             BIGSERIAL PRIMARY KEY,
    name           TEXT             NOT NULL UNIQUE,
    protein_grams  DOUBLE PRECISION NOT NULL,
    calories_kcal  INTEGER          NOT NULL,
    category       TEXT             NOT NULL,   -- veg | non_veg | shake
    serving_size   TEXT             NOT NULL
);


-- ─────────────────────────────────────────────────────────────
-- 4.  nutrition_insight
--     Heuristic tips generated per user.
--     type: INFO | WARNING | SUCCESS
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.nutrition_insight (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT    NOT NULL,
    message     TEXT      NOT NULL,
    type        TEXT      NOT NULL,   -- INFO | WARNING | SUCCESS
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_nutrition_insight_user_id_created_at
    ON public.nutrition_insight (user_id, created_at DESC);


-- ─────────────────────────────────────────────────────────────
-- 5.  Seed food_item
--     The DataSeeder bean does this automatically on boot.
--     Run this manually in Supabase if the app hasn't booted yet
--     or if the seeder skipped due to an existing count.
-- ─────────────────────────────────────────────────────────────
INSERT INTO public.food_item (name, protein_grams, calories_kcal, category, serving_size) VALUES
-- ── veg ──────────────────────────────────────────────────────
('Paneer',              18.0, 265, 'veg',     '100g paneer'),
('Eggs',                13.0, 154, 'veg',     '2 large eggs'),
('Greek Yoghurt',       17.0, 100, 'veg',     '170g serving'),
('Whole Milk',           8.0, 149, 'veg',     '250 ml'),
('Tofu',                17.0, 144, 'veg',     '200g firm tofu'),
('Chickpeas',           15.0, 269, 'veg',     '200g cooked'),
('Lentils',             18.0, 230, 'veg',     '200g cooked'),
('Cottage Cheese',      14.0,  98, 'veg',     '100g'),
('Peanut Butter',        8.0, 190, 'veg',     '2 tbsp'),
-- everyday Indian veg
('Moong Dal',           14.0, 212, 'veg',     '1 katori (150g) cooked'),
('Toor Dal',            12.0, 198, 'veg',     '1 katori (150g) cooked'),
('Masoor Dal',          15.0, 195, 'veg',     '1 katori (150g) cooked'),
('Rajma',               15.0, 230, 'veg',     '1 katori (150g) cooked'),
('Chana Dal',           13.0, 218, 'veg',     '1 katori (150g) cooked'),
('Soya Chunks',         25.0, 173, 'veg',     '50g dry / 100g soaked'),
('Dahi (Curd)',          6.0,  61, 'veg',     '1 katori (150g)'),
('Roasted Chana',       12.0, 180, 'veg',     '50g handful'),
('Peanuts',             13.0, 280, 'veg',     '50g (small fistful)'),
('Moong Sprouts',        9.0,  85, 'veg',     '1 cup (100g) sprouted'),
('Sattu',               20.0, 220, 'veg',     '50g powder in water'),
('Besan Chilla',        12.0, 180, 'veg',     '2 medium chillas'),
('Poha with Peanuts',    6.0, 195, 'veg',     '1 medium plate (150g)'),
('Almonds',              6.0, 173, 'veg',     '25g (about 20 almonds)'),
('Roti with Dal',       14.0, 310, 'veg',     '2 rotis + 1 katori dal'),
-- ── non_veg ──────────────────────────────────────────────────
('Chicken Breast',      31.0, 165, 'non_veg', '100g grilled'),
('Tuna (canned)',       25.0, 116, 'non_veg', '100g drained'),
('Boiled Eggs',         13.0, 155, 'non_veg', '2 eggs'),
('Salmon',              25.0, 208, 'non_veg', '100g baked'),
('Egg Whites',          11.0,  52, 'non_veg', '3 egg whites'),
-- everyday Indian non-veg
('Egg Bhurji',          14.0, 180, 'non_veg', '2-egg scramble with onion/tomato'),
('Boiled Chicken',      27.0, 150, 'non_veg', '100g boneless'),
('Chicken Curry',       22.0, 240, 'non_veg', '1 medium serving (150g)'),
('Omelette',            12.0, 154, 'non_veg', '2-egg omelette'),
('Rohu Fish',           20.0, 140, 'non_veg', '100g cooked'),
('Catla Fish',          19.0, 130, 'non_veg', '100g cooked'),
-- ── shake ────────────────────────────────────────────────────
('Whey Protein Shake',  25.0, 130, 'shake',   '1 scoop (30g) in water'),
('Casein Shake',        24.0, 120, 'shake',   '1 scoop (33g) in milk'),
('Plant Protein Shake', 20.0, 140, 'shake',   '1 scoop (35g) in water'),
-- everyday Indian shakes/drinks
('Sattu Drink',         18.0, 180, 'shake',   '4 tbsp sattu in 300ml water with lemon'),
('Banana Milk Shake',   10.0, 265, 'shake',   '1 banana + 250ml full-fat milk'),
('Dahi Lassi',           8.0, 140, 'shake',   '250ml curd blended with water'),
('Peanut Butter Shake', 18.0, 310, 'shake',   '2 tbsp PB + 250ml milk + banana')
ON CONFLICT (name) DO NOTHING;


