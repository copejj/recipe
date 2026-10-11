#!/usr/bin/env bash
set -e

export PGPASSWORD="test_secure_password"
DB_NAME="local_prod_db"
DB_USER="test_migration_user"

# 🔄 FIXED: Added -h localhost to force network routing inside the container
echo "[Step 2/7] Seeding local migration logging architectures..."
psql -h localhost -U "$DB_USER" -d "$DB_NAME" -t -c "CREATE TABLE IF NOT EXISTS public.schema_migrations (filename varchar(255) PRIMARY KEY, status varchar(50), started_at timestamp, ended_at timestamp);"
psql -h localhost -U "$DB_USER" -d "$DB_NAME" -t -c "DROP SCHEMA IF EXISTS public_old CASCADE; DROP SCHEMA IF EXISTS public_shadow CASCADE;"

echo "[Step 2/7] Extracting production snapshot cleanly using native container tools..."
pg_dump -h localhost -U "$DB_USER" -d "$DB_NAME" --no-subscriptions --no-publications -x -O -n public -f /tmp/live_full_snapshot.sql

echo "[Step 2/7] Restoring snapshot into a fresh, isolated shadow schema namespace..."
psql -h localhost -U "$DB_USER" -d "$DB_NAME" -c "CREATE SCHEMA public_shadow AUTHORIZATION $DB_USER;"
psql -h localhost -U "$DB_USER" -d "$DB_NAME" --set search_path=public_shadow -f /tmp/live_full_snapshot.sql
rm -f /tmp/live_full_snapshot.sql
