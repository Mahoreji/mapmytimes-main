-- V4__increase_column_lengths.sql
-- Increase column lengths for title, slug, and other fields that may exceed 255 characters

-- Update blog_posts table
ALTER TABLE blog_posts ALTER COLUMN title TYPE TEXT;
ALTER TABLE blog_posts ALTER COLUMN slug TYPE TEXT;
ALTER TABLE blog_posts ALTER COLUMN author_email TYPE TEXT;

-- Update post_media table
ALTER TABLE post_media ALTER COLUMN caption TYPE TEXT;
ALTER TABLE post_media ALTER COLUMN subtitle TYPE TEXT;

-- Update blog_post_categories table
ALTER TABLE blog_post_categories ALTER COLUMN categories TYPE TEXT;

-- Update blog_post_tags table
ALTER TABLE blog_post_tags ALTER COLUMN tags TYPE TEXT;
