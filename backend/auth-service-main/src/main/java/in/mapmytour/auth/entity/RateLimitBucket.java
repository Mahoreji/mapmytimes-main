package in.mapmytour.auth.entity;

/**
 * Buckets for DB-backed rate limiting of trip circle features.
 */
public enum RateLimitBucket {
    CIRCLE_CREATE,
    CIRCLE_POST,
    CIRCLE_POLL
}
