-- V7__add_sections_table.sql
-- Adds the missing sections table + junction table for blog post <-> section relationships
-- Required by SectionController (@RequestMapping("/api/v1/blog/sections"))

CREATE TABLE IF NOT EXISTS sections (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    slug VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    icon VARCHAR(255),
    accent_color VARCHAR(16),
    sort_order INTEGER DEFAULT 0,
    parent_section_id VARCHAR(36),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_parent_section
        FOREIGN KEY (parent_section_id)
        REFERENCES sections(id)
        ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS blog_post_sections (
    blog_post_id VARCHAR(36) NOT NULL,
    sections VARCHAR(255) NOT NULL,
    CONSTRAINT fk_blog_post_sections_post
        FOREIGN KEY (blog_post_id)
        REFERENCES blog_posts(id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_sections_slug ON sections(slug);
CREATE INDEX IF NOT EXISTS idx_sections_parent ON sections(parent_section_id);
CREATE INDEX IF NOT EXISTS idx_blog_post_sections_post ON blog_post_sections(blog_post_id);
