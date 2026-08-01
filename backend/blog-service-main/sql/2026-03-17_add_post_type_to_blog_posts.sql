-- Add post_type column to blog_posts table
ALTER TABLE blog_posts
    ADD COLUMN IF NOT EXISTS post_type VARCHAR(32) NOT NULL DEFAULT 'BLOG';
