-- V3__add_admin_initial_whitelist.sql
-- Whitelist initial administrative IPs for persistent access across deployments.

INSERT INTO gateway_ip_whitelist (id, ip_address, label, added_by, expires_at, is_active, created_at, updated_at)
VALUES (
    '8f2c929b-4675-4b21-b321-c3b01a1a1a1a', 
    '2409:4090:204e:c02d:a06c:5b40:f007:8770', 
    'Primary Admin (System Bootstrap)', 
    'system-assistant', 
    NULL, 
    TRUE, 
    NOW(), 
    NOW()
) ON CONFLICT (ip_address) DO UPDATE SET is_active = TRUE, expires_at = NULL, updated_at = NOW();
