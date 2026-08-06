-- V6__add_missing_post_columns.sql
-- Adds missing columns that were added to BlogPost entity but not to the database

ALTER TABLE blog_posts ADD COLUMN IF NOT EXISTS primary_video_url TEXT;
ALTER TABLE blog_posts ADD COLUMN IF NOT EXISTS section_slug VARCHAR(64);
