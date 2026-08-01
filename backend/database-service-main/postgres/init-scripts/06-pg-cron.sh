#!/bin/bash
set -e

# Only set up pg_cron on production
if [ "$ENVIRONMENT" != "prod" ]; then
    echo "Skipping pg_cron setup for $ENVIRONMENT environment"
    exit 0
fi

echo "Setting up pg_cron scheduled jobs for production OTA..."

# Wait for postgres to be ready
until psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "select 1" > /dev/null 2>&1; do
  echo "Waiting for PostgreSQL to be ready for pg_cron setup..."
  sleep 2
done

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL

    CREATE EXTENSION IF NOT EXISTS pg_cron;

    -- Expire fare holds every minute
    SELECT cron.schedule('expire-fare-holds', '* * * * *',
        'SELECT expire_fare_holds()');

    -- Clean up expired GDS cache every 5 minutes
    SELECT cron.schedule('cleanup-gds-cache', '*/5 * * * *',
        'SELECT cleanup_gds_cache()');

    -- Kill queries running longer than 60 seconds (except superuser)
    SELECT cron.schedule('kill-long-queries', '* * * * *', $$
        SELECT pg_terminate_backend(pid)
        FROM pg_stat_activity
        WHERE state = 'active'
          AND query_start < NOW() - INTERVAL '60 seconds'
          AND usename != 'postgres'
          AND query NOT ILIKE '%pg_stat_activity%'
    $$);

    -- Weekly VACUUM ANALYZE (Sunday 3 AM IST)
    SELECT cron.schedule('weekly-vacuum', '0 21 * * 6', 
        'VACUUM ANALYZE bookings, fare_holds, gds_cache');

    -- Daily stats reset
    SELECT cron.schedule('reset-pg-stats', '0 18 * * *', 
        'SELECT pg_stat_statements_reset()');

EOSQL

echo "pg_cron jobs scheduled."
