#!/bin/bash
# test-migrations.sh - Manual Quality Gate Checker

echo "Running manual validation gate across all workspace migration files..."
echo "========================================================================"

VIOLATIONS=0

# Aligns perfectly with your spec linter rules by finding every .sql file in the project
while IFS= read -r FILE; do
    echo "Processing: $FILE"
    FILE_ERRORS=0

    # 1. Look for hardcoded schema prefixes
    if grep -qi '\bpublic\.' "$FILE"; then
        echo "PREFIX ERROR: Hardcoded 'public.' prefix detected."
        grep -n -I -i '\bpublic\.' "$FILE" | sed 's/^/      Line /'
        FILE_ERRORS=$((FILE_ERRORS + 1))
    fi

    # 2. Extract content while stripping comments and blank rows
    CLEAN_SQL=$(sed -E 's/\/\*.*\*\/|\-\-.*//g' "$FILE" | grep -v '^[[:space:]]*$')

    if [ ! -z "$CLEAN_SQL" ]; then
        FIRST_COMMAND=$(echo "$CLEAN_SQL" | head -n 1 | tr '[:upper:]' '[:lower:]' | tr -d '[:space:]')
        LAST_COMMAND=$(echo "$CLEAN_SQL" | tail -n 1 | tr '[:upper:]' '[:lower:]' | tr -d '[:space:]')

        # Check for open transaction block
        if [[ "$FIRST_COMMAND" != "begin;" && "$FIRST_COMMAND" != "begin" ]]; then
            echo "TRANSACTION ERROR: File does not open with 'BEGIN;'"
            echo "Found instead: '$(echo "$CLEAN_SQL" | head -n 1)'"
            FILE_ERRORS=$((FILE_ERRORS + 1))
        fi

        # Check for close transaction block
        if [[ "$LAST_COMMAND" != "commit;" && "$LAST_COMMAND" != "commit" ]]; then
            echo "TRANSACTION ERROR: File does not close with 'COMMIT;'"
            echo "Found instead: '$(echo "$CLEAN_SQL" | tail -n 1)'"
            FILE_ERRORS=$((FILE_ERRORS + 1))
        fi
    else
        echo " WARNING: File appears to be completely empty."
    fi

    if [ $FILE_ERRORS -gt 0 ]; then
        VIOLATIONS=$((VIOLATIONS + 1))
    fi
    echo "------------------------------------------------------------------------"
done < <(find . -type f -name "*.sql" -not -path "*/target/*")

if [ $VIOLATIONS -gt 0 ]; then
    echo "VALIDATION FAILED: $VIOLATIONS file(s) violate your production deployment rules."
    exit 1
else
    echo "SUCCESS: All SQL files are clean, portable, and transactionally safe!"
    exit 0
fi

