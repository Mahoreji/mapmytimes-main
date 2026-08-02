CREATE TABLE sections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(128) NOT NULL UNIQUE,
    slug VARCHAR(128) NOT NULL UNIQUE,
    description TEXT,
    icon VARCHAR(64),
    accent_color VARCHAR(16),
    sort_order INTEGER DEFAULT 0,
    parent_section_id UUID,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now()
);

ALTER TABLE blog_posts ADD COLUMN IF NOT EXISTS section_slug VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_blog_posts_section_slug ON blog_posts(section_slug);
