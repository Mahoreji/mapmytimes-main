-- Migration: Add denormalized author metadata to blog_posts
-- Date: 2026-03-18

ALTER TABLE blog_posts ADD COLUMN IF NOT EXISTS author_email VARCHAR(255);
ALTER TABLE blog_posts ADD COLUMN IF NOT EXISTS author_first_name VARCHAR(255);
ALTER TABLE blog_posts ADD COLUMN IF NOT EXISTS author_last_name VARCHAR(255);
ALTER TABLE blog_posts ADD COLUMN IF NOT EXISTS author_avatar_url TEXT;

-- Update existing records with default metadata if needed (optional)
UPDATE blog_posts SET author_email = 'prakhar.mahore.1997@gmail.com', author_first_name = 'Prakhar', author_last_name = 'Mahore' WHERE author_email IS NULL;
