#!/bin/bash
set -e
echo "Creating OTA core schema and indexes..."

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL

    -- ============================================================
    -- AIRPORT REFERENCE TABLE (with PostGIS geo data)
    -- ============================================================
    CREATE TABLE IF NOT EXISTS airports (
        id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
        iata_code       CHAR(3) NOT NULL UNIQUE,
        icao_code       CHAR(4) UNIQUE,
        name            TEXT NOT NULL,
        city            TEXT NOT NULL,
        country_code    CHAR(2) NOT NULL,
        timezone        TEXT NOT NULL,
        location        GEOGRAPHY(POINT, 4326),    -- PostGIS point
        elevation_ft    INTEGER,
        is_active       BOOLEAN DEFAULT true,
        created_at      TIMESTAMPTZ DEFAULT NOW()
    );

    -- GiST index for geo proximity search ("airports near me")
    CREATE INDEX IF NOT EXISTS idx_airports_location ON airports USING GIST (location);
    CREATE INDEX IF NOT EXISTS idx_airports_iata ON airports (iata_code);
    CREATE INDEX IF NOT EXISTS idx_airports_country ON airports (country_code);
    -- GIN + trgm for fuzzy name search
    CREATE INDEX IF NOT EXISTS idx_airports_name_trgm ON airports USING GIN (name gin_trgm_ops);
    CREATE INDEX IF NOT EXISTS idx_airports_city_trgm ON airports USING GIN (city gin_trgm_ops);

    -- ============================================================
    -- FARE HOLDS — OTA critical table
    -- Price is legally committed for 20 minutes during checkout
    -- ============================================================
    CREATE TABLE IF NOT EXISTS fare_holds (
        id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
        booking_ref     TEXT NOT NULL UNIQUE,
        user_id         UUID NOT NULL,
        flight_ids      UUID[] NOT NULL,            -- array of flight segment IDs
        adult_count     SMALLINT NOT NULL DEFAULT 1,
        child_count     SMALLINT NOT NULL DEFAULT 0,
        infant_count    SMALLINT NOT NULL DEFAULT 0,
        base_fare       NUMERIC(12,2) NOT NULL,
        taxes           NUMERIC(12,2) NOT NULL,
        total_fare      NUMERIC(12,2) NOT NULL,
        currency        CHAR(3) NOT NULL DEFAULT 'INR',
        exchange_rate   NUMERIC(10,6),              -- rate at time of hold
        gds_pnr         TEXT,                        -- GDS/supplier PNR
        gds_source      TEXT,                        -- 'amadeus','sabre','galileo','direct'
        held_at         TIMESTAMPTZ DEFAULT NOW(),
        expires_at      TIMESTAMPTZ NOT NULL DEFAULT (NOW() + INTERVAL '20 minutes'),
        released_at     TIMESTAMPTZ,
        status          TEXT NOT NULL DEFAULT 'active'
                            CHECK (status IN ('active','converted','expired','released')),
        metadata        JSONB DEFAULT '{}'
    );

    -- Critical indexes for fare hold management
    CREATE INDEX IF NOT EXISTS idx_fare_holds_expires ON fare_holds (expires_at)
        WHERE status = 'active';    -- partial index — only active holds
    CREATE INDEX IF NOT EXISTS idx_fare_holds_user ON fare_holds (user_id, status);
    CREATE INDEX IF NOT EXISTS idx_fare_holds_booking_ref ON fare_holds (booking_ref);

    -- ============================================================
    -- BOOKINGS — partitioned by booking_date (range partition)
    -- ============================================================
    CREATE TABLE IF NOT EXISTS bookings (
        id              UUID DEFAULT uuid_generate_v4(),
        booking_ref     TEXT NOT NULL,
        fare_hold_id    UUID,
        user_id         UUID NOT NULL,
        agent_id        UUID,                       -- B2B agent if applicable
        booking_date    DATE NOT NULL,              -- PARTITION KEY
        travel_date     DATE NOT NULL,
        return_date     DATE,
        trip_type       TEXT NOT NULL CHECK (trip_type IN ('one_way','round_trip','multi_city')),
        status          TEXT NOT NULL DEFAULT 'pending'
                            CHECK (status IN ('pending','payment_pending','confirmed','cancelled','completed','refunded')),
        total_fare      NUMERIC(12,2) NOT NULL,
        currency        CHAR(3) NOT NULL DEFAULT 'INR',
        payment_ref     TEXT,
        cancellation_reason TEXT,
        cancelled_at    TIMESTAMPTZ,
        created_at      TIMESTAMPTZ DEFAULT NOW(),
        updated_at      TIMESTAMPTZ DEFAULT NOW(),
        metadata        JSONB DEFAULT '{}'
    ) PARTITION BY RANGE (booking_date);

    -- Create partitions for current year and next year
    DO $$
    DECLARE
        yr INTEGER := EXTRACT(YEAR FROM NOW())::INTEGER;
        m INTEGER;
    BEGIN
        FOR m IN 1..12 LOOP
            EXECUTE format(
                'CREATE TABLE IF NOT EXISTS bookings_%s_%s PARTITION OF bookings
                 FOR VALUES FROM (%L) TO (%L)',
                yr, LPAD(m::TEXT, 2, '0'),
                format('%s-%s-01', yr, LPAD(m::TEXT, 2, '0')),
                format('%s-%s-01',
                    CASE WHEN m = 12 THEN yr+1 ELSE yr END,
                    CASE WHEN m = 12 THEN '01' ELSE LPAD((m+1)::TEXT, 2, '0') END
                )
            );
        END LOOP;
        -- Next year partitions
        FOR m IN 1..12 LOOP
            EXECUTE format(
                'CREATE TABLE IF NOT EXISTS bookings_%s_%s PARTITION OF bookings
                 FOR VALUES FROM (%L) TO (%L)',
                yr+1, LPAD(m::TEXT, 2, '0'),
                format('%s-%s-01', yr+1, LPAD(m::TEXT, 2, '0')),
                format('%s-%s-01',
                    CASE WHEN m = 12 THEN yr+2 ELSE yr+1 END,
                    CASE WHEN m = 12 THEN '01' ELSE LPAD((m+1)::TEXT, 2, '0') END
                )
            );
        END LOOP;
    END $$;

    -- Indexes on bookings (applied to all partitions)
    CREATE INDEX IF NOT EXISTS idx_bookings_user ON bookings (user_id, booking_date);
    CREATE INDEX IF NOT EXISTS idx_bookings_agent ON bookings (agent_id, booking_date) WHERE agent_id IS NOT NULL;
    CREATE INDEX IF NOT EXISTS idx_bookings_status ON bookings (status, booking_date) WHERE status NOT IN ('completed','refunded');
    CREATE INDEX IF NOT EXISTS idx_bookings_ref ON bookings (booking_ref);
    -- BRIN index for date-range queries (extremely efficient for append-only data)
    CREATE INDEX IF NOT EXISTS idx_bookings_travel_date_brin ON bookings USING BRIN (travel_date);
    CREATE INDEX IF NOT EXISTS idx_bookings_updated_at_brin ON bookings USING BRIN (updated_at);

    -- ============================================================
    -- PRICE HISTORY — high-volume, time-series, BRIN-indexed
    -- ============================================================
    CREATE TABLE IF NOT EXISTS price_history (
        id              BIGSERIAL,
        route_key       TEXT NOT NULL,              -- 'BOM-DEL-2024-01-15'
        supplier        TEXT NOT NULL,
        cabin_class     CHAR(1) NOT NULL DEFAULT 'Y', -- Y=economy, C=business, F=first
        price           NUMERIC(10,2) NOT NULL,
        currency        CHAR(3) NOT NULL DEFAULT 'INR',
        seats_available SMALLINT,
        recorded_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
        PRIMARY KEY (id, recorded_at)
    ) PARTITION BY RANGE (recorded_at);

    -- Monthly partitions for price history
    DO $$
    DECLARE yr INTEGER := EXTRACT(YEAR FROM NOW())::INTEGER; m INTEGER;
    BEGIN
        FOR m IN 1..12 LOOP
            EXECUTE format('CREATE TABLE IF NOT EXISTS price_history_%s_%s
                PARTITION OF price_history FOR VALUES FROM (%L) TO (%L)',
                yr, LPAD(m::TEXT,2,'0'),
                format('%s-%s-01',yr,LPAD(m::TEXT,2,'0')),
                format('%s-%s-01',
                    CASE WHEN m=12 THEN yr+1 ELSE yr END,
                    CASE WHEN m=12 THEN '01' ELSE LPAD((m+1)::TEXT,2,'0') END));
        END LOOP;
    END $$;

    -- BRIN is perfect for time-series
    CREATE INDEX IF NOT EXISTS idx_price_history_recorded_brin ON price_history USING BRIN (recorded_at);
    CREATE INDEX IF NOT EXISTS idx_price_history_route ON price_history (route_key, recorded_at DESC);

    -- ============================================================
    -- EXCHANGE RATES — for multi-currency OTA
    -- ============================================================
    CREATE TABLE IF NOT EXISTS exchange_rates (
        id              BIGSERIAL PRIMARY KEY,
        from_currency   CHAR(3) NOT NULL,
        to_currency     CHAR(3) NOT NULL,
        rate            NUMERIC(14,8) NOT NULL,
        source          TEXT NOT NULL DEFAULT 'rbi',
        effective_date  DATE NOT NULL,
        created_at      TIMESTAMPTZ DEFAULT NOW(),
        UNIQUE (from_currency, to_currency, effective_date, source)
    );

    CREATE INDEX IF NOT EXISTS idx_exchange_rates_pair ON exchange_rates
        (from_currency, to_currency, effective_date DESC);

    -- ============================================================
    -- GDS CACHE — staging area for live GDS inventory
    -- ============================================================
    CREATE TABLE IF NOT EXISTS gds_cache (
        id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
        cache_key       TEXT NOT NULL UNIQUE,           -- hash of search params
        gds_source      TEXT NOT NULL,                  -- 'amadeus','sabre','galileo'
        request_params  JSONB NOT NULL,
        response_data   JSONB NOT NULL,
        result_count    INTEGER DEFAULT 0,
        cached_at       TIMESTAMPTZ DEFAULT NOW(),
        expires_at      TIMESTAMPTZ NOT NULL DEFAULT (NOW() + INTERVAL '5 minutes'),
        hit_count       INTEGER DEFAULT 0
    );

    CREATE INDEX IF NOT EXISTS idx_gds_cache_expires ON gds_cache (expires_at);
    CREATE INDEX IF NOT EXISTS idx_gds_cache_key ON gds_cache (cache_key);

    -- ============================================================
    -- FUNCTIONS
    -- ============================================================
    
    -- Cleanup GDS cache
    CREATE OR REPLACE FUNCTION cleanup_gds_cache() RETURNS INTEGER AS $$
    DECLARE deleted INTEGER;
    BEGIN
        DELETE FROM gds_cache WHERE expires_at < NOW();
        GET DIAGNOSTICS deleted = ROW_COUNT;
        RETURN deleted;
    END;
    $$ LANGUAGE plpgsql;

    -- Expire fare holds
    CREATE OR REPLACE FUNCTION expire_fare_holds() RETURNS INTEGER AS $$
    DECLARE expired_count INTEGER;
    BEGIN
        UPDATE fare_holds
        SET status = 'expired', released_at = NOW()
        WHERE status = 'active'
          AND expires_at < NOW();
        GET DIAGNOSTICS expired_count = ROW_COUNT;

        RETURN expired_count;
    END;
    $$ LANGUAGE plpgsql;

    -- UPDATED_AT trigger
    CREATE OR REPLACE FUNCTION update_updated_at()
    RETURNS TRIGGER AS $$
    BEGIN NEW.updated_at = NOW(); RETURN NEW; END;
    $$ LANGUAGE plpgsql;

    CREATE TRIGGER bookings_updated_at
        BEFORE UPDATE ON bookings
        FOR EACH ROW EXECUTE FUNCTION update_updated_at();

EOSQL

echo "OTA schema created successfully."
