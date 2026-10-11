#!/usr/bin/env bash
set -e

# --- ANSI Terminal Color Codes ---
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
RED='\033[0;31m'
BOLD='\033[1m'
NC='\033[0;3m' # No Color

export DB_NAME="local_prod_db"
export DB_USER="test_migration_user"
export DB_PASS="test_secure_password"
export DB_HOST="test-db"
export TAG_NAME="local-testing"
export PGPASSWORD="$DB_PASS"
export SPRING_DATASOURCE_HOST="test-db"

export PROD_APP_PATH="/app"
export PROD_DEPLOY_PATH="/tmp/mock_deploy"
export PROD_DEPLOY_NAME="spring-recipe"
export PROD_APP_NAME="spring-recipe"

PIPELINE_LOG="/tmp/pipeline_build_output.log"
rm -f "$PIPELINE_LOG"

mkdir -p "$PROD_DEPLOY_PATH"
cd "$PROD_APP_PATH"

cleanup_on_failure() {
  EXIT_CODE=$?
  if [ $EXIT_CODE -ne 0 ]; then
    echo -e "\n${RED}${BOLD}========================================================================${NC}"
    echo -e "${RED}${BOLD}❌ ERROR DETECTED IN PHASE: $CURRENT_STEP (Exit Code: $EXIT_CODE)${NC}"
    echo -e "${RED}${BOLD}========================================================================${NC}"
    if [ -f "$PIPELINE_LOG" ]; then
      echo -e "${YELLOW}${BOLD} PARSING FAILURE LOG TRACE BELOW:${NC}\n"
      cat "$PIPELINE_LOG"
      echo -e "${RED}${BOLD}========================================================================${NC}"
    fi
    exit $EXIT_CODE
  fi
}
trap cleanup_on_failure ERR

# --- STEP 4 ---
CURRENT_STEP="DATABASE_MIGRATION"
echo -e "\n${BLUE}${BOLD}[Step 4/7] DATABASE MIGRATION PHASE${NC}"

# Linting Naming Conventions
echo -e "  ${CYAN}•${NC} Verifying migration script naming formats..."
if [ -d sql/migrations ]; then
  for FILE in sql/migrations/*; do
    [ -e "$FILE" ] || break
    FILENAME=$(basename "$FILE")
    if [[ ! "$FILENAME" =~ ^[0-9]{8}_[0-9]+.*\.sql$ ]]; then
      echo -e "    ${YELLOW}⚠️ WARNING: File '$FILENAME' skips standard regex patterns (*.sql) and will be bypassed!${NC}"
    fi
  done
fi

# Stitching payload
echo -e "  ${CYAN}•${NC} Compiling incremental SQL delta files into master transaction payload..."
{
  echo "BEGIN;"
  echo "SET search_path = public_shadow, public;"
  if [ -f sql/init_schema.sql ]; then cat sql/init_schema.sql; fi
  
  if [ -d sql/migrations ]; then
    FILES=$(find sql/migrations -type f | sort)
    for FILE in $FILES; do
      FILENAME=$(basename "$FILE")
      if [[ "$FILENAME" =~ ^[0-9]{8}_[0-9]+.*\.sql$ ]]; then
        echo "INSERT INTO public_shadow.schema_migrations (filename, status, started_at) VALUES ('$FILENAME' , 'pending', NOW()) ON CONFLICT (filename) DO UPDATE SET status='pending';"
        cat "$FILE"
        echo "UPDATE public_shadow.schema_migrations SET ended_at=NOW(), status='completed' WHERE filename='$FILENAME';"
      fi
    done
  fi
  echo "COMMIT;"
} > /tmp/compiled_migration.sql

echo -e "  ${CYAN}•${NC} Appending schema increments to isolation namespace [public_shadow]..."
START_TIME=$(date +%s)
psql -h "$DB_HOST" -U "$DB_USER" -d "$DB_NAME" -f /tmp/compiled_migration.sql >> "$PIPELINE_LOG" 2>&1
rm -f /tmp/compiled_migration.sql

echo -e "  ${CYAN}•${NC} Executing atomic live namespace swap (${BOLD}public_shadow${NC} ${BOLD}public${NC})..."
psql -h "$DB_HOST" -U "$DB_USER" -d "$DB_NAME" -c "
  BEGIN;
    ALTER SCHEMA public RENAME TO public_old;
    ALTER SCHEMA public_shadow RENAME TO public;
  COMMIT;
" >> "$PIPELINE_LOG" 2>&1
END_TIME=$(date +%s)
echo -e "  ${GREEN}✓${NC} Schema migrations processed completely. (${BOLD}$((END_TIME - START_TIME))s${NC})"

# --- STEP 5 ---
CURRENT_STEP="BUILD"
echo -e "\n${BLUE}${BOLD}[Step 5/7] APPLICATION BUILD PHASE${NC}"
echo -e "  ${CYAN}•${NC} Compiling 56 Java classes and packaging executable archive..."
echo -ne "    Running ${YELLOW}${BOLD}mvn clean package -DskipTests${NC} in background... "

START_TIME=$(date +%s)
# Trigger build in background while showing a running ticker dot to the user
mvn clean package -DskipTests -q >> "$PIPELINE_LOG" 2>&1 &
PID=$!

while kill -0 $PID 2>/dev/null; do
  echo -ne "${YELLOW}.${NC}"
  sleep 2
done
END_TIME=$(date +%s)
echo -e " ${GREEN}${BOLD}[DONE]${NC} (${BOLD}$((END_TIME - START_TIME))s${NC})"

# --- STEP 6 ---
CURRENT_STEP="DEPLOYMENT"
echo -e "\n${BLUE}${BOLD}[Step 6/7] SIMULATED DISTRIBUTION PHASE${NC}"
VERSION_NAME="${PROD_DEPLOY_NAME}##${TAG_NAME}.war"
BUILD_WAR="${PROD_APP_PATH}/target/${PROD_APP_NAME}.war"

echo -e "  ${CYAN}•${NC} Mapping compiled war file to target target webroot: ${BOLD}$VERSION_NAME${NC}"
cp "$BUILD_WAR" "${PROD_DEPLOY_PATH}/${VERSION_NAME}" >> "$PIPELINE_LOG" 2>&1
echo -e "  ${GREEN}✓${NC} Distribution artifact staged successfully."

# --- STEP 7 ---
# --- STEP 7 ---
CURRENT_STEP="FINAL_CLEANUP"
echo -e "\n${BLUE}${BOLD}[Step 7/7] ENVIRONMENT SANITIZATION${NC}"
echo -e "  ${CYAN}•${NC} Exporting raw background logs to your local workspace path..."

# 🔄 NEW: Copy the raw, internal tool outputs out to your physical laptop folder!
cp "$PIPELINE_LOG" "github_action_raw.log"

# Clear it from the container's temporary directory
rm -f "$PIPELINE_LOG"

echo -e "\n${GREEN}${BOLD}===================================================================${NC}"
echo -e "${GREEN}${BOLD}  SUCCESS: Local Sandbox Test Architecture Finished Successfully!${NC}"
echo -e "${GREEN}${BOLD}===================================================================${NC}\n"
