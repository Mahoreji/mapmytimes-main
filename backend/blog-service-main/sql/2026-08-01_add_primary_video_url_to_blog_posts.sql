-- Add primary_video_url column to blog_posts (required for video news feature — YouTube/Instagram embed URLs)
-- Apply to blog-service database BEFORE deploying frontend build.
-- Column matches BlogPost.java @Column(name="primary_video_url", columnDefinition="TEXT").

ALTER TABLE blog_posts ADD COLUMN IF NOT EXISTS primary_video_url TEXT;
