#!/bin/bash
set -e
echo "Configuring per-table autovacuum for OTA high-churn tables..."

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL

    -- Bookings table: high UPDATE churn
    ALTER TABLE bookings SET (
        autovacuum_vacuum_scale_factor = 0.01,
        autovacuum_analyze_scale_factor = 0.005,
        autovacuum_vacuum_cost_delay = 0,
        autovacuum_vacuum_threshold = 100
    );

    -- Fare holds: extreme high INSERT/UPDATE/DELETE churn
    ALTER TABLE fare_holds SET (
        autovacuum_vacuum_scale_factor = 0.005,
        autovacuum_analyze_scale_factor = 0.002,
        autovacuum_vacuum_cost_delay = 0
    );

    -- GDS cache: constant churn
    ALTER TABLE gds_cache SET (
        autovacuum_vacuum_scale_factor = 0.01,
        autovacuum_vacuum_cost_delay = 0
    );

EOSQL

echo "Autovacuum tuning complete."
