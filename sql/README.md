# 🗄️ Database Migration Guidelines

This directory houses the schema initializations and sequential migration scripts for the production application database. Changes made here interact directly with the **Shadow Schema Swap** pattern configured in the deployment pipeline.

## 🗂️ Directory Layout

```text
sql/
├── README.md               <-- This file
├── init_schema.sql         <-- Base structure definitions
└── migrations/             <-- Incremental production changes
    ├── 20261010_01_init.sql
    └── 20261010_02_add_users.sql
```

---

## 🛑 Crucial Migration Rules

### 1. Mandatory File Extension (`.sql`)
The automation runner strictly checks files inside the `migrations/` directory using a regex pattern. 
* **Rule:** Every filename must match this pattern exactly: `YYYYMMDD_NN_descriptive_name.sql`
* **Warning:** If you omit the `.sql` extension (e.g., `20261010_03_adding_disabled_by`), the file **will be silently skipped** by the core loop, though the pipeline will throw a yellow warning banner (`⚠️`) in your GitHub Actions logs.

### 2. File-Level Transactions (`BEGIN` / `COMMIT`)
* **The Good News:** The deployment workflow wraps the entire execution block in a global transaction. If any single script fails, the entire stack rolls back out of the shadow space.
* **Best Practice:** For local development testing and raw reproducibility, manually envelope your custom delta scripts in structural blocks:
  ```sql
  BEGIN;
  
  ALTER TABLE public.users ADD COLUMN IF NOT EXISTS disabled_by_user_id integer;
  
  COMMIT;
  ```

### 3. Namespace/Schema Routing (`public`)
Write your raw scripts normally using the standard `public` namespace prefix (e.g., `public.users`). 
* The deployment engine dynamically sets the routing environment to `public_shadow` on the fly. You do not need to manually change names inside your files.

---

## 🛠️ Local Troubleshooting

If a production build fails during `Step 4/7 (DATABASE_MIGRATION)`:
1. The server's global environment trap automatically handles cleanup by executing `DROP SCHEMA IF EXISTS public_shadow CASCADE;`.
2. Your live website remains untouched under the original `public` schema.
3. Check the GitHub Action log to locate the specific file or syntax causing the execution to exit early. Fix the target script file locally, commit the changes, and cut a fresh release tag.
