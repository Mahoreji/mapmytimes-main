-- V1__blog_service_baseline.sql
-- Baseline schema for blog-service with native JSONB support

-- Categories Table
CREATE TABLE IF NOT EXISTS categories (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    slug VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    parent_category_id VARCHAR(36),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

-- Tags Table
CREATE TABLE IF NOT EXISTS tags (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    slug VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

-- Blog Posts Table
CREATE TABLE IF NOT EXISTS blog_posts (
    id VARCHAR(36) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    excerpt TEXT,
    reading_time INTEGER,
    featured_image JSONB,
    content_blocks JSONB,
    table_of_contents JSONB,
    travel_meta JSONB,
    seo JSONB,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    visibility VARCHAR(50) NOT NULL DEFAULT 'PUBLIC',
    language VARCHAR(10) NOT NULL DEFAULT 'en',
    view_count BIGINT NOT NULL DEFAULT 0,
    share_count BIGINT NOT NULL DEFAULT 0,
    bookmark_count BIGINT NOT NULL DEFAULT 0,
    user_id VARCHAR(255) NOT NULL,
    allow_comments BOOLEAN NOT NULL DEFAULT TRUE,
    allow_likes BOOLEAN NOT NULL DEFAULT TRUE,
    is_featured BOOLEAN NOT NULL DEFAULT FALSE,
    is_trending BOOLEAN NOT NULL DEFAULT FALSE,
    post_type VARCHAR(50) NOT NULL DEFAULT 'BLOG',
    author_email VARCHAR(255),
    author_first_name VARCHAR(100),
    author_last_name VARCHAR(100),
    author_avatar_url TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    published_at TIMESTAMP WITHOUT TIME ZONE,
    scheduled_at TIMESTAMP WITHOUT TIME ZONE
);

-- Blog Post Categories (ElementCollection)
CREATE TABLE IF NOT EXISTS blog_post_categories (
    blog_post_id VARCHAR(36) NOT NULL,
    categories VARCHAR(255),
    CONSTRAINT fk_blog_post_categories FOREIGN KEY (blog_post_id) REFERENCES blog_posts(id) ON DELETE CASCADE
);

-- Blog Post Tags (ElementCollection)
CREATE TABLE IF NOT EXISTS blog_post_tags (
    blog_post_id VARCHAR(36) NOT NULL,
    tags VARCHAR(255),
    CONSTRAINT fk_blog_post_tags FOREIGN KEY (blog_post_id) REFERENCES blog_posts(id) ON DELETE CASCADE
);

-- Post Media Table
CREATE TABLE IF NOT EXISTS post_media (
    id VARCHAR(36) PRIMARY KEY,
    post_id VARCHAR(36) NOT NULL,
    media_url TEXT NOT NULL,
    media_type VARCHAR(50),
    caption VARCHAR(255),
    description TEXT,
    subtitle VARCHAR(255),
    subtitle_group_index INTEGER,
    user_id VARCHAR(255) NOT NULL,
    display_order INTEGER,
    uploaded_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT fk_post_media_post FOREIGN KEY (post_id) REFERENCES blog_posts(id) ON DELETE CASCADE
);

-- Post Comments Table
CREATE TABLE IF NOT EXISTS post_comments (
    id VARCHAR(36) PRIMARY KEY,
    post_id VARCHAR(36) NOT NULL,
    content TEXT NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    parent_comment_id VARCHAR(36),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT fk_post_comments_post FOREIGN KEY (post_id) REFERENCES blog_posts(id) ON DELETE CASCADE
);

-- Post Likes Table
CREATE TABLE IF NOT EXISTS post_likes (
    id VARCHAR(36) PRIMARY KEY,
    post_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    liked_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT fk_post_likes_post FOREIGN KEY (post_id) REFERENCES blog_posts(id) ON DELETE CASCADE,
    CONSTRAINT uk_post_likes_user UNIQUE (post_id, user_id)
);

-- Blog Settings Table
CREATE TABLE IF NOT EXISTS blog_settings (
    id VARCHAR(36) PRIMARY KEY,
    setting_key VARCHAR(255) NOT NULL UNIQUE,
    setting_value TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create Indexes
CREATE INDEX IF NOT EXISTS idx_blog_posts_user_id ON blog_posts(user_id);
CREATE INDEX IF NOT EXISTS idx_blog_posts_status ON blog_posts(status);
CREATE INDEX IF NOT EXISTS idx_blog_posts_slug ON blog_posts(slug);
CREATE INDEX IF NOT EXISTS idx_post_media_post_id ON post_media(post_id);
CREATE INDEX IF NOT EXISTS idx_post_comments_post_id ON post_comments(post_id);
CREATE INDEX IF NOT EXISTS idx_post_likes_post_id ON post_likes(post_id);
CREATE INDEX IF NOT EXISTS idx_post_likes_user_id ON post_likes(user_id);
