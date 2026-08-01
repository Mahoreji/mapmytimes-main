-- Add booking_id and payment_id columns to social_notifications table
ALTER TABLE social_notifications 
ADD COLUMN booking_id VARCHAR(50),
ADD COLUMN payment_id VARCHAR(50),
ADD COLUMN action_url TEXT;

-- Add indexes for performance
CREATE INDEX idx_social_notifications_booking_id ON social_notifications(booking_id);
CREATE INDEX idx_social_notifications_payment_id ON social_notifications(payment_id);
