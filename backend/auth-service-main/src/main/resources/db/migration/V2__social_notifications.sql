-- V2__social_notifications.sql
-- Table for storing social interaction notifications (likes, comments)

CREATE TABLE IF NOT EXISTS social_notifications (
    id VARCHAR(36) PRIMARY KEY,
    recipient_user_id VARCHAR(36) NOT NULL,
    sender_user_id VARCHAR(36) NOT NULL,
    type VARCHAR(24) NOT NULL, -- SOCIAL_LIKE, SOCIAL_COMMENT
    message TEXT NOT NULL,
    post_id VARCHAR(36) NULL,
    user_name VARCHAR(120) NULL,
    user_avatar TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_social_notification_recipient
    ON social_notifications (recipient_user_id, created_at DESC);
