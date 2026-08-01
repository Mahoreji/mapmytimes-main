-- V1__gateway_security_baseline.sql
-- API Gateway Security Platform — baseline schema
-- Follows the same pattern as blog-service

-- ============================================================
-- Admin IP Whitelist Table
-- Stores whitelisted IP addresses for admin route access.
-- Redis holds the hot path; this table is the source of truth.
-- ============================================================
CREATE TABLE IF NOT EXISTS gateway_ip_whitelist (
    id               VARCHAR(36)  PRIMARY KEY,
    ip_address       VARCHAR(45)  NOT NULL UNIQUE,  -- supports both IPv4 and IPv6
    label            VARCHAR(255),                    -- human-friendly note (e.g. "devops-laptop")
    added_by         VARCHAR(255),                    -- user_id of the admin who added it
    expires_at       TIMESTAMP WITHOUT TIME ZONE,     -- NULL = never expires
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

-- ============================================================
-- Security Event Log Table
-- Persistent audit trail for all critical security events.
-- ============================================================
CREATE TABLE IF NOT EXISTS gateway_security_events (
    id           BIGSERIAL    PRIMARY KEY,
    event_time   TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    client_ip    VARCHAR(45)  NOT NULL,
    user_id      VARCHAR(255),
    endpoint     TEXT         NOT NULL,
    method       VARCHAR(10),
    action       VARCHAR(100) NOT NULL,   -- e.g. BOT_DETECTED, RATE_LIMIT_EXCEEDED
    result       VARCHAR(50)  NOT NULL,   -- e.g. BLOCKED, ALLOWED, THROTTLED
    threat_score INTEGER      NOT NULL DEFAULT 0,
    details      TEXT
);

-- ============================================================
-- Bot Ban Log Table
-- Tracks IPs that were auto-banned and their cooldown details.
-- ============================================================
CREATE TABLE IF NOT EXISTS gateway_bot_bans (
    id          VARCHAR(36)  PRIMARY KEY,
    ip_address  VARCHAR(45)  NOT NULL,
    banned_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    reason      TEXT,
    is_lifted   BOOLEAN NOT NULL DEFAULT FALSE,
    lifted_at   TIMESTAMP WITHOUT TIME ZONE,
    lifted_by   VARCHAR(255)
);

-- ============================================================
-- Indexes
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_ip_whitelist_ip       ON gateway_ip_whitelist(ip_address);
CREATE INDEX IF NOT EXISTS idx_ip_whitelist_active   ON gateway_ip_whitelist(is_active);
CREATE INDEX IF NOT EXISTS idx_ip_whitelist_expires  ON gateway_ip_whitelist(expires_at);

CREATE INDEX IF NOT EXISTS idx_sec_events_time       ON gateway_security_events(event_time);
CREATE INDEX IF NOT EXISTS idx_sec_events_client_ip  ON gateway_security_events(client_ip);
CREATE INDEX IF NOT EXISTS idx_sec_events_action     ON gateway_security_events(action);

CREATE INDEX IF NOT EXISTS idx_bot_bans_ip           ON gateway_bot_bans(ip_address);
CREATE INDEX IF NOT EXISTS idx_bot_bans_expires      ON gateway_bot_bans(expires_at);
