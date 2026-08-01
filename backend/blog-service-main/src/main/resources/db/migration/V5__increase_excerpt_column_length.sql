-- V5__increase_excerpt_column_length.sql
-- Explicitly increase excerpt column length to TEXT to support longer blog summaries

ALTER TABLE blog_posts ALTER COLUMN excerpt TYPE TEXT;
