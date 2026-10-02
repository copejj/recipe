-- Create the enum type for migration status if it doesn't exist
DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'migration_status') THEN
        CREATE TYPE migration_status AS ENUM ('pending', 'completed', 'failed');
    END IF;
END $$;

-- Create the tracking table
CREATE TABLE IF NOT EXISTS schema_migrations (
    id SERIAL PRIMARY KEY,
    filename VARCHAR(255) NOT NULL UNIQUE,
    status migration_status DEFAULT 'pending',
    started_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMPTZ,
    error_message TEXT
);

-- Trigger function: Automatically set status to 'completed' when ended_at is filled
CREATE OR REPLACE FUNCTION update_migration_status()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.ended_at IS NOT NULL THEN
        NEW.status := 'completed';
        NEW.error_message := NULL;
    ELSIF NEW.error_message IS NOT NULL THEN
        NEW.status := 'failed';
    ELSE
        NEW.status := 'pending';
        NEW.error_message := NULL;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Bind trigger to table (handles duplicates via EXCEPTION)
DO $$ 
BEGIN
    CREATE TRIGGER trg_migration_status
    BEFORE UPDATE ON schema_migrations
    FOR EACH ROW EXECUTE FUNCTION update_migration_status();
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;
