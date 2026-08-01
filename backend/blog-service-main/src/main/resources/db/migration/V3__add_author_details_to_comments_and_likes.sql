-- Add author details to post_comments
ALTER TABLE post_comments ADD COLUMN author_email VARCHAR(255);
ALTER TABLE post_comments ADD COLUMN author_first_name VARCHAR(255);
ALTER TABLE post_comments ADD COLUMN author_last_name VARCHAR(255);
ALTER TABLE post_comments ADD COLUMN author_avatar_url TEXT;

-- Add author details to post_likes
ALTER TABLE post_likes ADD COLUMN author_email VARCHAR(255);
ALTER TABLE post_likes ADD COLUMN author_first_name VARCHAR(255);
ALTER TABLE post_likes ADD COLUMN author_last_name VARCHAR(255);
ALTER TABLE post_likes ADD COLUMN author_avatar_url TEXT;
