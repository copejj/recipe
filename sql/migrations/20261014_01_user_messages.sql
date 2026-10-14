BEGIN;
CREATE TABLE user_messages (
    user_message_id SERIAL PRIMARY KEY,
    user_id INT REFERENCES users(user_id) ON DELETE CASCADE, -- NULL means a "Global Broadcast" to everyone
    message_text TEXT NOT NULL,
    alert_type VARCHAR(50) DEFAULT 'info', -- 'info', 'success', 'danger', 'warning'
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Index to optimize fetching active messages quickly
CREATE INDEX idx_user_messages_lookup ON user_messages (user_id, expires_at, created_at DESC);
COMMIT;