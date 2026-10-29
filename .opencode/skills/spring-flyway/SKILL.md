---
name: spring-flyway
description: Guide Flyway database migrations in Spring Boot including naming conventions, versioning strategy, safe migration patterns, module-aware migrations, and troubleshooting. Use when creating or managing database schema migrations.
version: 0.1.0
tags: [database, migrations, flyway, schema]
triggers:
  - creating or managing database migrations
  - naming Flyway migration files
  - configuring Flyway in Spring Boot
  - setting up module-aware migrations with Spring Modulith
  - troubleshooting migration failures or checksum mismatches
  - writing safe DDL or DML migration scripts
spring_versions:
  boot: ">=3.2"
depends_on: [spring-boot]
---

# Spring Flyway

Conventions for managing database schema migrations with Flyway in Spring Boot 3.2+.

**Do NOT Load** for Liquibase or manual schema management — this skill is Flyway-specific.

## Migration File Naming

```
V<version>__<description>.sql
│ │            │
│ │            └── Snake_case description (double underscore separator)
│ └── Version number
└── V = versioned, R = repeatable
```

### Examples

```
db/migration/
├── V1__create_orders_table.sql
├── V2__add_status_to_orders.sql
├── V3__create_order_items_table.sql
├── V4__add_index_on_order_status.sql
├── V5__insert_default_order_statuses.sql
├── R__create_order_summary_view.sql        # Repeatable — re-runs when content changes
└── R__grant_read_permissions.sql
```

### Versioning Strategy

| Strategy | Format | Pros | Cons | Use When |
|----------|--------|------|------|----------|
| Sequential integers | `V1`, `V2`, `V3` | Simple, readable | Merge conflicts on version number | Small team, single branch |
| Timestamp | `V20260312143000` | No conflicts | Harder to read, unordered by intent | Multiple teams, feature branches |

**Recommendation:** Use sequential integers for small teams. Switch to timestamps when merge conflicts on version numbers become frequent.

### Repeatable Migrations

Prefix with `R__` — Flyway re-executes them whenever their checksum changes. Use for:

- **Views** — `R__create_order_summary_view.sql`
- **Stored procedures/functions** — `R__update_order_totals_function.sql`
- **Permissions** — `R__grant_read_permissions.sql`

Repeatable migrations run **after** all versioned migrations, in alphabetical order.

## Spring Boot Configuration

```yaml
spring:
  flyway:
    enabled: true                              # Default: true when Flyway is on classpath
    locations: classpath:db/migration           # Default location
    baseline-on-migrate: false                  # Set true only for existing databases without Flyway history
    validate-on-migrate: true                   # Validates applied migrations haven't been modified
    out-of-order: false                         # Set true only during feature branch merges
```

### Test Configuration

```yaml
# application-test.yml
spring:
  flyway:
    clean-disabled: false                       # Allow clean in tests (NEVER in production)
```

## Writing Safe Migrations

### Adding a Column

```sql
-- V6__add_email_to_customers.sql

-- Safe: nullable column, no default needed
ALTER TABLE customers ADD COLUMN email VARCHAR(255);
```

```sql
-- V7__add_priority_to_orders.sql

-- Safe: NOT NULL with DEFAULT — existing rows get the default value
ALTER TABLE orders ADD COLUMN priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL';
```

### Adding an Index

```sql
-- V8__add_index_on_customer_email.sql

-- Use CONCURRENTLY on PostgreSQL to avoid locking the table
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_customers_email ON customers (email);
```

Note: `CONCURRENTLY` cannot run inside a transaction. Set `spring.flyway.postgresql.transactional-lock=false` or use a separate migration with a Flyway callback to disable transactions for this migration.

### Renaming a Column (Safe Multi-Step)

Don't rename in one migration — it breaks running application instances that still reference the old name.

```sql
-- V9__add_full_name_to_customers.sql (deploy 1)
ALTER TABLE customers ADD COLUMN full_name VARCHAR(255);
UPDATE customers SET full_name = name;

-- Application code: read from full_name, write to both name and full_name
```

```sql
-- V10__drop_name_from_customers.sql (deploy 2, after all instances use full_name)
ALTER TABLE customers DROP COLUMN name;
```

### Data Migrations

```sql
-- V11__migrate_order_status_values.sql

-- Idempotent: only updates rows that haven't been migrated
UPDATE orders SET status = 'COMPLETED' WHERE status = 'DONE';
UPDATE orders SET status = 'CANCELLED' WHERE status = 'CANCELED';
```

For large tables, batch the update:

```sql
-- Process in chunks to avoid long-running transactions
DO $$
DECLARE
    batch_size INT := 10000;
    rows_updated INT;
BEGIN
    LOOP
        UPDATE orders SET status = 'COMPLETED'
        WHERE status = 'DONE'
        AND id IN (SELECT id FROM orders WHERE status = 'DONE' LIMIT batch_size);

        GET DIAGNOSTICS rows_updated = ROW_COUNT;
        EXIT WHEN rows_updated = 0;
        COMMIT;
    END LOOP;
END $$;
```

### Creating Tables

```sql
-- V12__create_shipments_table.sql

CREATE TABLE shipments (
    id          BIGSERIAL PRIMARY KEY,
    order_id    BIGINT NOT NULL REFERENCES orders(id),
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    shipped_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_shipments_order_id ON shipments (order_id);
CREATE INDEX idx_shipments_status ON shipments (status);
```

- **Always index foreign keys** — JPA won't create them automatically
- **Use `TIMESTAMPTZ`** (PostgreSQL) — stores timezone-aware timestamps
- **Add `created_at`/`updated_at`** with defaults — the database handles them even outside JPA

## Module-Aware Migrations (Spring Modulith)

When using Spring Modulith with module-aware Flyway:

```yaml
spring:
  modulith:
    runtime:
      flyway-enabled: true
```

```
db/migration/
├── __root/                      # Root migrations — shared schema
│   └── V1__create_event_publication.sql
├── order/                       # Order module — own flyway_schema_history_order table
│   ├── V1__create_orders.sql
│   └── V2__add_status_index.sql
└── inventory/                   # Inventory module — own flyway_schema_history_inventory
    └── V1__create_inventory.sql
```

- Version numbers are **scoped per module** — each module starts at V1
- Migrations execute in **module dependency order**
- `@ApplicationModuleTest` only runs migrations for the tested module and its dependencies
- Root migrations (`__root/`) use the default `flyway_schema_history` table

See the spring-modulith skill's `references/runtime.md` for full configuration.

## Determining the Next Version Number

Before creating a migration, check the highest existing version:

```
db/migration/
├── V1__init.sql
├── V2__create_orders.sql
├── ...
└── V37__add_customer_preferences.sql    # Highest → next is V38
```

For module-aware migrations, check per module:

```
db/migration/order/
├── V1__create_orders.sql
└── V2__add_status_index.sql             # Highest → next is V3
```

## Troubleshooting

### Checksum Mismatch

```
Migration checksum mismatch for migration version X
```

Someone modified an already-applied migration. **Never edit applied migrations.** Fix:

1. If in development: `flyway repair` to update the stored checksum
2. If in production: create a new migration with the correct change

### Out-of-Order Migrations

When feature branches create version gaps (V5 merged before V4):

```yaml
spring.flyway.out-of-order: true    # Temporarily, until branches are merged
```

Reset to `false` afterward. Better: use timestamp-based versioning to avoid this entirely.

### Failed Migration

Flyway marks the migration as failed in `flyway_schema_history`. On PostgreSQL, DDL is transactional so the schema is clean. Fix the migration SQL and run `flyway repair` to remove the failed entry, then re-run.

On MySQL, DDL is **not** transactional — failed migrations may leave partial schema changes. Manually clean up before retrying.

## Key Decisions

### SQL vs Java Migrations

| Type | File | Use When |
|------|------|----------|
| SQL | `V1__name.sql` | **Always — default choice.** Schema changes, data migrations |
| Java | `V1__Name.java` | Complex logic requiring application code (rare: data transformation, external API calls) |

Prefer SQL. Java migrations couple your migration history to application code versions.

### Flyway Clean

| Environment | `clean-disabled` | Reason |
|-------------|-------------------|--------|
| Production | `true` (default) | **Never clean production** — it drops all objects |
| Test | `false` | Useful for resetting test databases |
| Local dev | `false` | Convenient for fresh starts |

## NEVER

- **Never edit an applied migration** — Flyway checksums detect changes and fail. Always create a new migration for corrections
- **Never add a NOT NULL column without a DEFAULT on existing tables** — the migration fails because existing rows have no value. Add with a DEFAULT or make it nullable first, backfill, then add the constraint
- **Never run long-running DML in a single transaction** — large UPDATE/DELETE operations lock tables and block the application. Batch the changes
- **Never drop a column in the same deploy that stops writing to it** — running instances still reference the old column. Use a two-deploy strategy: stop writing first, drop in the next release
- **Never use `flyway clean` in production** — it drops every object in the schema. Keep `clean-disabled: true` (the default)
- **Never skip migrations to "catch up"** — if a migration fails, fix it. Don't delete it and fold the change into the next one — this breaks environments that already applied the original
- **Never mix DDL and large DML in one migration** — DDL acquires schema locks, DML acquires row locks. Combining them extends lock duration. Separate into two migrations
- **Never rely on `ddl-auto=update` instead of Flyway** — Hibernate's schema update cannot drop columns, rename safely, add indexes conditionally, or handle data migrations. Flyway is mandatory for production

## References

- [Flyway Documentation](https://documentation.red-gate.com/flyway)
- [Spring Boot Flyway Properties](https://docs.spring.io/spring-boot/appendix/application-properties/index.html#appendix.application-properties.data-migration)
- [Spring Modulith Module-Aware Flyway](https://docs.spring.io/spring-modulith/reference/runtime.html#flyway)
