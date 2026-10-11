#!/usr/bin/env bash
set -e

export PGPASSWORD="test_secure_password"
DB_NAME="local_prod_db"
DB_USER="test_migration_user"

# 🔄 FIXED: Create the target runtime permission role inside your local test box
psql -h localhost -U "$DB_USER" -d "$DB_NAME" -c "CREATE ROLE recipe_app_user;" 2>/dev/null || true

echo "[Step 1/3] Seeding local migration logging architectures..."
# 🔄 FIXED: Force the container to hold a pre-existing public baseline to catch production-level schema duplicate errors
psql -h localhost -U "$DB_USER" -d "$DB_NAME" -c "CREATE SCHEMA IF NOT EXISTS public;"

psql -h localhost -U "$DB_USER" -d "$DB_NAME" -t -c "CREATE TABLE IF NOT EXISTS public.schema_migrations (filename varchar(255) PRIMARY KEY, status varchar(50), started_at timestamp, ended_at timestamp);"
psql -h localhost -U "$DB_USER" -d "$DB_NAME" -t -c "DROP SCHEMA IF EXISTS public_old CASCADE; DROP SCHEMA IF EXISTS public_shadow CASCADE;"

echo "[Step 2/3] Extracting production snapshot cleanly using native container tools..."
pg_dump -h localhost -U "$DB_USER" -d "$DB_NAME" --no-subscriptions --no-publications -x -O -n public -f /tmp/live_full_snapshot.sql

echo "[Step 3/3] Restoring snapshot into a fresh, isolated shadow schema namespace..."
psql -h localhost -U "$DB_USER" -d "$DB_NAME" -c "CREATE SCHEMA public_shadow AUTHORIZATION $DB_USER;"
psql -h localhost -U "$DB_USER" -d "$DB_NAME" --set search_path=public_shadow -f /tmp/live_full_snapshot.sql
rm -f /tmp/live_full_snapshot.sql
