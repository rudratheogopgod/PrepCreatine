-- V12: Seed questions (empty — questions generated on-demand by AI)
-- This migration exists as a placeholder.
-- AI-generated questions are inserted via QuizGenerationService at runtime.
-- To seed manual questions, INSERT INTO questions (...) here.

-- Example structure (do not uncomment in production seeding):
-- INSERT INTO questions (exam_id, subject_id, topic_id, level, type, question_text,
--     option_a, option_b, option_c, option_d, correct_answer, explanation, is_ai_generated)
-- VALUES ('jee', 'physics', 'jee-physics-mechanics-newtons-laws', 1, 'mcq',
--     'Which of Newton''s laws states F=ma?', 'First', 'Second', 'Third', 'Fourth',
--     'B', 'Newton''s Second Law: Force = mass × acceleration.', FALSE);

SELECT 'V12 seed migration applied — question bank starts empty.' AS status;
