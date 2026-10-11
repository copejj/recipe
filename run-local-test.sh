#!/usr/bin/env bash
set -e

# --- ANSI Terminal Color Codes ---
GREEN='\033[0;32m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
YELLOW='\033[0;33m'
BOLD='\033[1m'
NC='\033[0m'

clear
echo -e "${BLUE}${BOLD}========================================================================${NC}"
echo -e "${BLUE}${BOLD} LAUNCHING LOCAL SANDBOX RUNNER FOR PIPELINE INTEGRITY SCRIPT${NC}"
echo -e "${BLUE}${BOLD}========================================================================${NC}"

echo -e "\n${BLUE}${BOLD}[Step 1/7] INITIALIZING CONTAINER FRAMEWORK${NC}"
echo -e "  ${CYAN}•${NC} Tearing down previous sandbox environments..."
docker compose -f docker-compose.test.yml down -v --remove-orphans > /dev/null 2>&1

echo -e "  ${CYAN}•${NC} Provisioning isolated database and app server cluster containers..."
docker compose -f docker-compose.test.yml up -d --wait > /dev/null 2>&1
echo -e "  ${GREEN}✓${NC} Docker stack network connections online."

echo -e "\n${BLUE}${BOLD}[Step 2/7] DATABASE REPLICATION AND SNAPSHOT PHASE${NC}"
echo -ne "  ${CYAN}•${NC} Waiting for PostgreSQL database engine cluster to accept connections... "

until docker compose -f docker-compose.test.yml exec -T test-db psql -h localhost -U "test_migration_user" -d "local_prod_db" -c "select 1" > /dev/null 2>&1; do
  echo -ne "${YELLOW}.${NC}"
  sleep 1
done
echo -e " ${GREEN}${BOLD}[CONNECTED]${NC}"

echo -e "  ${CYAN}•${NC} Mirroring live schema baseline constraints..."
docker compose -f docker-compose.test.yml exec -T test-db bash -s < .github-test/init-database.sh > /dev/null 2>&1
echo -e "  ${GREEN}✓${NC} Baseline database clone complete."

echo -e "\n${BLUE}${BOLD}[Step 3/7] PROVISIONING DEPENDENCIES${NC}"
echo -e "  ${CYAN}•${NC} Synchronizing apt mirrors inside Ubuntu sandbox environment..."
docker compose -f docker-compose.test.yml exec -T test-server apt-get update -y > /tmp/apt_setup.log 2>&1
echo -e "  ${CYAN}•${NC} Injecting ${BOLD}Git${NC}, ${BOLD}OpenJDK 21${NC}, and ${BOLD}Maven${NC} toolchains..."
docker compose -f docker-compose.test.yml exec -T test-server apt-get install -y git openjdk-21-jdk maven postgresql-client > /tmp/apt_setup.log 2>&1
rm -f /tmp/apt_setup.log
echo -e "  ${GREEN}✓${NC} Runtime toolchains prepared successfully."

echo -e "  ${GREEN}✓${NC} Launching application migration and Maven build step..."

# This streams the inner script and pipes its output directly to a log file on your laptop
docker compose -f docker-compose.test.yml exec -T test-server bash -s < .github-test/run-migrations-and-build.sh 
