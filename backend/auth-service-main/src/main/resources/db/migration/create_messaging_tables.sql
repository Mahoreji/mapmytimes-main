-- Create tables for messaging and expense features
-- Run this script if tables are not auto-created by Hibernate

-- Group Messages Table
CREATE TABLE IF NOT EXISTS group_messages (
    id VARCHAR(36) PRIMARY KEY,
    group_id VARCHAR(36) NOT NULL,
    sender_id VARCHAR(36) NOT NULL,
    message TEXT NOT NULL,
    message_type VARCHAR(50),
    attachment_url VARCHAR(500),
    location_data VARCHAR(200),
    status VARCHAR(50) DEFAULT 'SENT',
    read_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_group_message_group FOREIGN KEY (group_id) REFERENCES travel_groups(id) ON DELETE CASCADE,
    CONSTRAINT fk_group_message_sender FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Direct Messages Table
CREATE TABLE IF NOT EXISTS direct_messages (
    id VARCHAR(36) PRIMARY KEY,
    sender_id VARCHAR(36) NOT NULL,
    recipient_id VARCHAR(36) NOT NULL,
    message TEXT NOT NULL,
    message_type VARCHAR(50),
    attachment_url VARCHAR(500),
    location_data VARCHAR(200),
    status VARCHAR(50) DEFAULT 'SENT',
    read_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_direct_message_sender FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_direct_message_recipient FOREIGN KEY (recipient_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Group Expenses Table
CREATE TABLE IF NOT EXISTS group_expenses (
    id VARCHAR(36) PRIMARY KEY,
    group_id VARCHAR(36) NOT NULL,
    paid_by VARCHAR(36) NOT NULL,
    description VARCHAR(200) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    category VARCHAR(50),
    expense_date TIMESTAMP NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING',
    receipt_url VARCHAR(200),
    notes VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_group_expense_group FOREIGN KEY (group_id) REFERENCES travel_groups(id) ON DELETE CASCADE,
    CONSTRAINT fk_group_expense_paid_by FOREIGN KEY (paid_by) REFERENCES users(id) ON DELETE CASCADE
);

-- Expense Participants Table
CREATE TABLE IF NOT EXISTS expense_participants (
    id VARCHAR(36) PRIMARY KEY,
    expense_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    share_amount DECIMAL(10, 2) NOT NULL,
    paid_amount DECIMAL(10, 2) DEFAULT 0,
    balance DECIMAL(10, 2) DEFAULT 0,
    status VARCHAR(50) DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_expense_participant_expense FOREIGN KEY (expense_id) REFERENCES group_expenses(id) ON DELETE CASCADE,
    CONSTRAINT fk_expense_participant_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_expense_participant UNIQUE (expense_id, user_id)
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_group_messages_group ON group_messages(group_id);
CREATE INDEX IF NOT EXISTS idx_group_messages_sender ON group_messages(sender_id);
CREATE INDEX IF NOT EXISTS idx_group_messages_created ON group_messages(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_direct_messages_sender ON direct_messages(sender_id);
CREATE INDEX IF NOT EXISTS idx_direct_messages_recipient ON direct_messages(recipient_id);
CREATE INDEX IF NOT EXISTS idx_direct_messages_created ON direct_messages(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_direct_messages_status ON direct_messages(recipient_id, status);

CREATE INDEX IF NOT EXISTS idx_group_expenses_group ON group_expenses(group_id);
CREATE INDEX IF NOT EXISTS idx_group_expenses_paid_by ON group_expenses(paid_by);
CREATE INDEX IF NOT EXISTS idx_group_expenses_date ON group_expenses(expense_date DESC);

CREATE INDEX IF NOT EXISTS idx_expense_participants_expense ON expense_participants(expense_id);
CREATE INDEX IF NOT EXISTS idx_expense_participants_user ON expense_participants(user_id);
CREATE INDEX IF NOT EXISTS idx_expense_participants_status ON expense_participants(user_id, status);

