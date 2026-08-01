-- Migration: Add Enhanced OTA Features to Support Tickets
-- Description: Adds SLA tracking, escalation, booking integration, and multi-language support fields
-- Version: 2.0
-- Date: 2026-01-09

-- SLA Management Fields
ALTER TABLE support_tickets 
ADD COLUMN IF NOT EXISTS first_response_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS response_time_minutes BIGINT,
ADD COLUMN IF NOT EXISTS resolution_time_minutes BIGINT,
ADD COLUMN IF NOT EXISTS sla_response_time_minutes INTEGER,
ADD COLUMN IF NOT EXISTS sla_resolution_time_minutes INTEGER,
ADD COLUMN IF NOT EXISTS sla_response_met BOOLEAN,
ADD COLUMN IF NOT EXISTS sla_resolution_met BOOLEAN;

-- Booking System Integration Fields
ALTER TABLE support_tickets 
ADD COLUMN IF NOT EXISTS booking_id VARCHAR(255),
ADD COLUMN IF NOT EXISTS booking_reference VARCHAR(255);

-- Multi-language Support Field
ALTER TABLE support_tickets 
ADD COLUMN IF NOT EXISTS language VARCHAR(10) DEFAULT 'en';

-- Escalation Fields
ALTER TABLE support_tickets 
ADD COLUMN IF NOT EXISTS escalation_level INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS escalated_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS escalation_reason VARCHAR(500);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_support_tickets_booking_id ON support_tickets(booking_id);
CREATE INDEX IF NOT EXISTS idx_support_tickets_language ON support_tickets(language);
CREATE INDEX IF NOT EXISTS idx_support_tickets_escalation_level ON support_tickets(escalation_level);
CREATE INDEX IF NOT EXISTS idx_support_tickets_first_response_at ON support_tickets(first_response_at);

-- Add comments for documentation
COMMENT ON COLUMN support_tickets.first_response_at IS 'Timestamp when agent first responded to the ticket';
COMMENT ON COLUMN support_tickets.response_time_minutes IS 'Time to first response in minutes';
COMMENT ON COLUMN support_tickets.resolution_time_minutes IS 'Time to resolution in minutes';
COMMENT ON COLUMN support_tickets.sla_response_time_minutes IS 'SLA target for response time based on priority';
COMMENT ON COLUMN support_tickets.sla_resolution_time_minutes IS 'SLA target for resolution time based on priority';
COMMENT ON COLUMN support_tickets.sla_response_met IS 'Whether response SLA was met';
COMMENT ON COLUMN support_tickets.sla_resolution_met IS 'Whether resolution SLA was met';
COMMENT ON COLUMN support_tickets.booking_id IS 'Link to booking system - booking ID';
COMMENT ON COLUMN support_tickets.booking_reference IS 'Human-readable booking reference number';
COMMENT ON COLUMN support_tickets.language IS 'Language code (en, es, fr, etc.) for multi-language support';
COMMENT ON COLUMN support_tickets.escalation_level IS 'Current escalation level (0 = none, 1-5 = escalated)';
COMMENT ON COLUMN support_tickets.escalated_at IS 'Timestamp when ticket was last escalated';
COMMENT ON COLUMN support_tickets.escalation_reason IS 'Reason for escalation';

