#!/bin/bash
set -e

echo "🐘 Initializing PostgreSQL databases and users for ${ENVIRONMENT} environment..."

# Function to create database if it doesn't exist
create_database() {
    local db_name=$1
    echo "Creating database: $db_name"
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
        SELECT 'CREATE DATABASE $db_name' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '$db_name')\gexec
        GRANT ALL PRIVILEGES ON DATABASE $db_name TO $POSTGRES_USER;
EOSQL
}

# Function to create user if it doesn't exist
create_user() {
    local username=$1
    local password=$2
    echo "Creating user: $username"
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
        DO \$\$
        BEGIN
            IF NOT EXISTS (SELECT FROM pg_catalog.pg_user WHERE usename = '$username') THEN
                CREATE USER $username WITH PASSWORD '$password';
            END IF;
        END
        \$\$;
        ALTER USER $username CREATEDB;
        GRANT ALL PRIVILEGES ON DATABASE $POSTGRES_DB TO $username;
EOSQL
}

# Create additional databases
if [ ! -z "$ADDITIONAL_DBS" ]; then
    IFS=',' read -ra DATABASES <<< "$ADDITIONAL_DBS"
    for db in "${DATABASES[@]}"; do
        db=$(echo $db | xargs) # trim whitespace
        if [ ! -z "$db" ]; then
            create_database "$db"
        fi
    done
fi

# Create additional users
if [ ! -z "$DB_USERS" ] && [ ! -z "$DB_USER_PASSWORDS" ]; then
    IFS=',' read -ra USERS <<< "$DB_USERS"
    IFS=',' read -ra PASSWORDS <<< "$DB_USER_PASSWORDS"
    
    for i in "${!USERS[@]}"; do
        user=$(echo ${USERS[$i]} | xargs) # trim whitespace
        password=$(echo ${PASSWORDS[$i]} | xargs) # trim whitespace
        if [ ! -z "$user" ] && [ ! -z "$password" ]; then
            create_user "$user" "$password"
        fi
    done
fi

echo "✅ PostgreSQL initialization completed for ${ENVIRONMENT}!"
