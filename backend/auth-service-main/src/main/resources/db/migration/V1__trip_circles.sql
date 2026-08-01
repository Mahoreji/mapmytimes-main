-- V1__trip_circles.sql
-- Trip circles, micro-actions, polls, attribution and rate limiting

-- A) trip_circle
CREATE TABLE IF NOT EXISTS trip_circle (
    id VARCHAR(36) PRIMARY KEY,
    destination_id VARCHAR(36) NOT NULL,
    title VARCHAR(120) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    created_by_user_id VARCHAR(36) NOT NULL,
    visibility VARCHAR(20) NOT NULL DEFAULT 'DESTINATION_PUBLIC',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_circle_destination_dates
    ON trip_circle (destination_id, start_date, end_date);

CREATE INDEX IF NOT EXISTS idx_circle_status
    ON trip_circle (status, end_date);

-- B) trip_circle_member
CREATE TABLE IF NOT EXISTS trip_circle_member (
    id VARCHAR(36) PRIMARY KEY,
    circle_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    role VARCHAR(16) NOT NULL DEFAULT 'MEMBER',
    joined_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    left_at TIMESTAMPTZ NULL,
    CONSTRAINT fk_circle_member_circle FOREIGN KEY (circle_id) REFERENCES trip_circle(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_circle_member_user
    ON trip_circle_member (circle_id, user_id);

CREATE INDEX IF NOT EXISTS idx_member_user
    ON trip_circle_member (user_id, joined_at DESC);

CREATE INDEX IF NOT EXISTS idx_member_circle
    ON trip_circle_member (circle_id, joined_at DESC);

-- C) circle_post
CREATE TABLE IF NOT EXISTS circle_post (
    id VARCHAR(36) PRIMARY KEY,
    circle_id VARCHAR(36) NOT NULL,
    author_user_id VARCHAR(36) NOT NULL,
    post_type VARCHAR(24) NOT NULL,
    content TEXT NULL,
    media_url TEXT NULL,
    geo_lat DOUBLE PRECISION NULL,
    geo_lng DOUBLE PRECISION NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT fk_circle_post_circle FOREIGN KEY (circle_id) REFERENCES trip_circle(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_post_circle_time
    ON circle_post (circle_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_post_author_time
    ON circle_post (author_user_id, created_at DESC);

-- D) circle_poll
CREATE TABLE IF NOT EXISTS circle_poll (
    id VARCHAR(36) PRIMARY KEY,
    circle_id VARCHAR(36) NOT NULL,
    created_by_user_id VARCHAR(36) NOT NULL,
    question VARCHAR(180) NOT NULL,
    closes_at TIMESTAMPTZ NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_circle_poll_circle FOREIGN KEY (circle_id) REFERENCES trip_circle(id) ON DELETE CASCADE
);

-- E) circle_poll_option
CREATE TABLE IF NOT EXISTS circle_poll_option (
    id VARCHAR(36) PRIMARY KEY,
    poll_id VARCHAR(36) NOT NULL,
    option_text VARCHAR(120) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_poll_option_poll FOREIGN KEY (poll_id) REFERENCES circle_poll(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_option_poll
    ON circle_poll_option (poll_id, sort_order);

-- F) circle_poll_vote
CREATE TABLE IF NOT EXISTS circle_poll_vote (
    id VARCHAR(36) PRIMARY KEY,
    poll_id VARCHAR(36) NOT NULL,
    option_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    voted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_poll_vote_poll FOREIGN KEY (poll_id) REFERENCES circle_poll(id) ON DELETE CASCADE,
    CONSTRAINT fk_poll_vote_option FOREIGN KEY (option_id) REFERENCES circle_poll_option(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_poll_vote_user
    ON circle_poll_vote (poll_id, user_id);

CREATE INDEX IF NOT EXISTS idx_vote_poll
    ON circle_poll_vote (poll_id, voted_at DESC);

CREATE INDEX IF NOT EXISTS idx_vote_user
    ON circle_poll_vote (user_id, voted_at DESC);

-- G) booking_attribution
CREATE TABLE IF NOT EXISTS booking_attribution (
    id VARCHAR(36) PRIMARY KEY,
    booking_id VARCHAR(36) NOT NULL UNIQUE,
    booker_user_id VARCHAR(36) NOT NULL,
    circle_id VARCHAR(36) NULL,
    post_id VARCHAR(36) NULL,
    ref_user_id VARCHAR(36) NULL,
    amount NUMERIC(12,2) NOT NULL DEFAULT 0,
    eligible BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_attr_ref_time
    ON booking_attribution (ref_user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_attr_circle_time
    ON booking_attribution (circle_id, created_at DESC);

-- H) user_action_dedup
CREATE TABLE IF NOT EXISTS user_action_dedup (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    circle_id VARCHAR(36) NOT NULL,
    action_type VARCHAR(24) NOT NULL,
    action_date DATE NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_user_action_dedup
    ON user_action_dedup (user_id, circle_id, action_type, action_date);

-- I) rate_limit_counter
CREATE TABLE IF NOT EXISTS rate_limit_counter (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    bucket VARCHAR(32) NOT NULL,
    window_start TIMESTAMPTZ NOT NULL,
    count INT NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_rate_limit_counter
    ON rate_limit_counter (user_id, bucket, window_start);

-- Optional: last booking click per user per circle
CREATE TABLE IF NOT EXISTS circle_last_click (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    circle_id VARCHAR(36) NOT NULL,
    post_id VARCHAR(36) NULL,
    ref_user_id VARCHAR(36) NULL,
    clicked_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_circle_last_click_user
    ON circle_last_click (user_id);

CREATE INDEX IF NOT EXISTS idx_circle_last_click_circle
    ON circle_last_click (circle_id, clicked_at DESC);
