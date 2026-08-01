-- V2__convert_to_jsonb.sql
-- Convert TEXT columns to JSONB for blog-service

DO $$ 
BEGIN
    -- blog_posts table
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'blog_posts' AND column_name = 'featured_image' AND data_type = 'text') THEN
        ALTER TABLE blog_posts ALTER COLUMN featured_image TYPE JSONB USING featured_image::jsonb;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'blog_posts' AND column_name = 'content_blocks' AND data_type = 'text') THEN
        ALTER TABLE blog_posts ALTER COLUMN content_blocks TYPE JSONB USING content_blocks::jsonb;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'blog_posts' AND column_name = 'table_of_contents' AND data_type = 'text') THEN
        ALTER TABLE blog_posts ALTER COLUMN table_of_contents TYPE JSONB USING table_of_contents::jsonb;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'blog_posts' AND column_name = 'travel_meta' AND data_type = 'text') THEN
        ALTER TABLE blog_posts ALTER COLUMN travel_meta TYPE JSONB USING travel_meta::jsonb;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'blog_posts' AND column_name = 'seo' AND data_type = 'text') THEN
        ALTER TABLE blog_posts ALTER COLUMN seo TYPE JSONB USING seo::jsonb;
    END IF;

    -- Ensure author_avatar_url is TEXT (which is already case, but for completeness)
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'blog_posts' AND column_name = 'author_avatar_url' AND data_type != 'text') THEN
        ALTER TABLE blog_posts ALTER COLUMN author_avatar_url TYPE TEXT;
    END IF;

END $$;
