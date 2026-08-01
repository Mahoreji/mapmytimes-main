-- V2__security_log_pruning.sql
-- Production Maintenance: Automated pruning for the security audit logs
-- Ensures the gateway_security_events table doesn't grow indefinitely.

-- ============================================================
-- 1. Create a function to prune old security events
-- ============================================================
CREATE OR REPLACE FUNCTION gateway_prune_security_events(retention_days INTEGER DEFAULT 30)
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM gateway_security_events
    WHERE event_time < (NOW() - (retention_days || ' days')::INTERVAL);
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- ============================================================
-- 2. Create a function to prune expired whitelist entries
-- ============================================================
CREATE OR REPLACE FUNCTION gateway_prune_expired_whitelist()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM gateway_ip_whitelist
    WHERE expires_at IS NOT NULL AND expires_at < NOW();
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- ============================================================
-- 3. Add column for 'event_type' for better filtering in V2
-- ============================================================
DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='gateway_security_events' AND column_name='event_type') THEN
        ALTER TABLE gateway_security_events ADD COLUMN event_type VARCHAR(50) DEFAULT 'GENERAL';
    END IF;
END $$;

-- Note: In a full production environment, these functions would be called 
-- via a pg_cron job or an external scheduler.
