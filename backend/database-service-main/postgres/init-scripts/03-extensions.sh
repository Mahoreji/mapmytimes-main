#!/bin/bash
set -e
echo "Installing PostgreSQL extensions for OTA platform..."

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL

    -- Already installed by base image
    CREATE EXTENSION IF NOT EXISTS postgis;
    CREATE EXTENSION IF NOT EXISTS postgis_topology;
    CREATE EXTENSION IF NOT EXISTS vector;

    -- Text search for destination autocomplete
    CREATE EXTENSION IF NOT EXISTS pg_trgm;        -- fuzzy search: "Mumabi" -> "Mumbai"
    CREATE EXTENSION IF NOT EXISTS unaccent;        -- "zurich" matches "Zürich"
    CREATE EXTENSION IF NOT EXISTS btree_gin;       -- composite GIN indexes

    -- Performance and monitoring
    CREATE EXTENSION IF NOT EXISTS pg_stat_statements;   -- CRITICAL: query performance tracking
    CREATE EXTENSION IF NOT EXISTS pg_buffercache;        -- buffer cache inspection

    -- UUID generation
    CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

    -- Full text search improvement
    CREATE EXTENSION IF NOT EXISTS dict_xsyn;

    -- Scheduling (will be enabled by pg_cron init script later)
    -- CREATE EXTENSION IF NOT EXISTS pg_cron;

    -- Configure pg_stat_statements
    ALTER SYSTEM SET pg_stat_statements.track = 'all';
    ALTER SYSTEM SET pg_stat_statements.max = 10000;
    ALTER SYSTEM SET pg_stat_statements.track_utility = on;

    -- Configure unaccent as default text search dictionary
    CREATE TEXT SEARCH CONFIGURATION IF NOT EXISTS public.unaccent_config (COPY = pg_catalog.english);
    ALTER TEXT SEARCH CONFIGURATION public.unaccent_config
        ALTER MAPPING FOR hword, hword_part, word WITH unaccent, english_stem;

EOSQL

echo "Extensions installed successfully."
