#!/bin/bash

# ===================================================================
# MapMyTour Database Service - Complete Setup Script
# Creates ALL files and configurations for Dev/Prod environment
# with automated backups, monitoring, and cron jobs
# ===================================================================

set -e

# Color codes for better output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[✓]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[⚠]${NC} $1"
}

print_error() {
    echo -e "${RED}[✗]${NC} $1"
}

print_header() {
    echo ""
    echo -e "${BLUE}============================================================${NC}"
    echo -e "${BLUE} $1 ${NC}"
    echo -e "${BLUE}============================================================${NC}"
}

print_section() {
    echo -e "${PURPLE}📁 $1${NC}"
}

# Banner
clear
echo -e "${CYAN}"
cat << "EOF"
╔══════════════════════════════════════════════════════════════╗
║                                                              ║
║               🗺️  MapMyTour Database Service                 ║
║                                                              ║
║           Complete Dev/Prod Setup with Automated            ║
║             Backups, Monitoring & Cron Jobs                 ║
║                                                              ║
╚══════════════════════════════════════════════════════════════╝
EOF
echo -e "${NC}"

print_header "Starting Complete Database Service Setup"

# Stop any running containers first
print_status "Stopping any existing containers..."
docker-compose down 2>/dev/null || true

# ===================================================================
# CREATE DIRECTORY STRUCTURE
# ===================================================================

print_header "Creating Project Directory Structure"

print_section "Creating main directories..."
mkdir -p {postgres,redis,backup-manager}/{scripts,config,cron}
mkdir -p postgres/init-scripts
mkdir -p redis/config/{dev,prod}
mkdir -p backups/{dev,prod}/{postgres,redis,archive,reports}
mkdir -p ssl
mkdir -p logs

print_status "✅ Directory structure created successfully"

# ===================================================================
# CREATE ENVIRONMENT CONFIGURATION
# ===================================================================

print_header "Creating Environment Configuration (.env)"

cat > .env << 'ENV_EOF'
# ===================================================================
# MapMyTour Database Service Configuration
# Updated for Dev/Prod Environment Separation
# ===================================================================

# ===========================================
# DEVELOPMENT ENVIRONMENT CONFIGURATION
# ===========================================

# PostgreSQL Development Configuration
POSTGRES_DEV_USER=admin_dev
POSTGRES_DEV_PASSWORD=MapMyTour@Dev885839!Secure
POSTGRES_DEV_DB=mapmytour_dev
POSTGRES_DEV_HOST=150.241.245.162
POSTGRES_DEV_EXTERNAL_PORT=5433

# Development Database Users (comma separated)
DB_DEV_USERS=mapmytour_app_dev,mapmytour_api_dev,mapmytour_analytics_dev
DB_DEV_USER_PASSWORDS=MapMyTour@AppDev885839,MapMyTour@ApiDev885839,MapMyTour@AnalyticsDev885839

# Additional Development Databases (comma separated)
ADDITIONAL_DEV_DBS=app_dev,analytics_dev,logs_dev,cache_dev

# Redis Development Configuration
REDIS_DEV_PASSWORD=MapMyTour@DevRedis885839!Secure
REDIS_DEV_HOST=150.241.245.162
REDIS_DEV_EXTERNAL_PORT=6380

# ===========================================
# PRODUCTION ENVIRONMENT CONFIGURATION
# ===========================================

# PostgreSQL Production Configuration
POSTGRES_PROD_USER=admin_prod
POSTGRES_PROD_PASSWORD=MapMyTour@Prod885839!SuperSecure
POSTGRES_PROD_DB=mapmytour_prod
POSTGRES_PROD_HOST=150.241.245.162
POSTGRES_PROD_EXTERNAL_PORT=5432

# Production Database Users (comma separated)
DB_PROD_USERS=mapmytour_app_prod,mapmytour_api_prod,mapmytour_analytics_prod
DB_PROD_USER_PASSWORDS=MapMyTour@AppProd885839!,MapMyTour@ApiProd885839!,MapMyTour@AnalyticsProd885839!

# Additional Production Databases (comma separated)
ADDITIONAL_PROD_DBS=app_prod,analytics_prod,logs_prod,cache_prod

# Redis Production Configuration
REDIS_PROD_PASSWORD=MapMyTour@ProdRedis885839!SuperSecure
REDIS_PROD_HOST=150.241.245.162
REDIS_PROD_EXTERNAL_PORT=6379

# ===========================================
# DOMAIN CONFIGURATION
# ===========================================
DOMAIN=mapmytour.in
POSTGRES_DEV_SUBDOMAIN=postgresql-dev
POSTGRES_PROD_SUBDOMAIN=postgresql-prod
REDIS_DEV_SUBDOMAIN=redis-dev
REDIS_PROD_SUBDOMAIN=redis-prod

# ===========================================
# BACKUP CONFIGURATION
# ===========================================

# Development Backup Schedule (Cron format)
# Daily at 2:00 AM
DEV_BACKUP_SCHEDULE=0 2 * * *

# Production Backup Schedule (Cron format)  
# Daily at 1:00 AM and 1:00 PM
PROD_BACKUP_SCHEDULE=0 1,13 * * *

# Backup Retention (days)
DEV_BACKUP_RETENTION=7
PROD_BACKUP_RETENTION=30

# Backup Compression
BACKUP_COMPRESSION=true

# ===========================================
# SSL CONFIGURATION
# ===========================================
SSL_ENABLED=false
SSL_CERT_PATH=./ssl/cert.pem
SSL_KEY_PATH=./ssl/key.pem

# ===========================================
# MONITORING & ALERTING
# ===========================================
ENABLE_MONITORING=true
SLACK_WEBHOOK_URL=
EMAIL_ALERTS=admin@mapmytour.in

# ===========================================
# PERFORMANCE TUNING
# ===========================================

# PostgreSQL Performance Settings
POSTGRES_MAX_CONNECTIONS_DEV=100
POSTGRES_MAX_CONNECTIONS_PROD=500
POSTGRES_SHARED_BUFFERS_DEV=256MB
POSTGRES_SHARED_BUFFERS_PROD=512MB

# Redis Performance Settings
REDIS_MAX_MEMORY_DEV=512mb
REDIS_MAX_MEMORY_PROD=1gb
REDIS_MAX_MEMORY_POLICY=allkeys-lru
ENV_EOF

print_status "✅ Environment configuration created"

# ===================================================================
# CREATE DOCKER COMPOSE FILE
# ===================================================================

print_header "Creating Docker Compose Configuration"

cat > docker-compose.yml << 'COMPOSE_EOF'
version: '3.8'

services:
  # PostgreSQL Development Environment
  postgres-dev:
    build:
      context: ./postgres
      dockerfile: Dockerfile
      args:
        ENVIRONMENT: dev
    container_name: mapmytour_postgres_dev
    environment:
      - POSTGRES_USER=${POSTGRES_DEV_USER}
      - POSTGRES_PASSWORD=${POSTGRES_DEV_PASSWORD}
      - POSTGRES_DB=${POSTGRES_DEV_DB}
      - DB_USERS=${DB_DEV_USERS}
      - DB_USER_PASSWORDS=${DB_DEV_USER_PASSWORDS}
      - ADDITIONAL_DBS=${ADDITIONAL_DEV_DBS}
      - ENVIRONMENT=dev
      - BACKUP_SCHEDULE=${DEV_BACKUP_SCHEDULE}
    volumes:
      - postgres_dev_data:/var/lib/postgresql/data
      - ./postgres/init-scripts:/docker-entrypoint-initdb.d:ro
      - ./backups/dev:/backups
      - ./postgres/cron:/etc/cron.d:ro
    ports:
      - "${POSTGRES_DEV_EXTERNAL_PORT}:5432"
    networks:
      - database_network
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_DEV_USER} -d ${POSTGRES_DEV_DB}"]
      interval: 30s
      timeout: 10s
      retries: 3

  # PostgreSQL Production Environment
  postgres-prod:
    build:
      context: ./postgres
      dockerfile: Dockerfile
      args:
        ENVIRONMENT: prod
    container_name: mapmytour_postgres_prod
    environment:
      - POSTGRES_USER=${POSTGRES_PROD_USER}
      - POSTGRES_PASSWORD=${POSTGRES_PROD_PASSWORD}
      - POSTGRES_DB=${POSTGRES_PROD_DB}
      - DB_USERS=${DB_PROD_USERS}
      - DB_USER_PASSWORDS=${DB_PROD_USER_PASSWORDS}
      - ADDITIONAL_DBS=${ADDITIONAL_PROD_DBS}
      - ENVIRONMENT=prod
      - BACKUP_SCHEDULE=${PROD_BACKUP_SCHEDULE}
    volumes:
      - postgres_prod_data:/var/lib/postgresql/data
      - ./postgres/init-scripts:/docker-entrypoint-initdb.d:ro
      - ./backups/prod:/backups
      - ./postgres/cron:/etc/cron.d:ro
    ports:
      - "${POSTGRES_PROD_EXTERNAL_PORT}:5432"
    networks:
      - database_network
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_PROD_USER} -d ${POSTGRES_PROD_DB}"]
      interval: 30s
      timeout: 10s
      retries: 3

  # Redis Development Environment
  redis-dev:
    build:
      context: ./redis
      dockerfile: Dockerfile
      args:
        ENVIRONMENT: dev
    container_name: mapmytour_redis_dev
    environment:
      - REDIS_PASSWORD=${REDIS_DEV_PASSWORD}
      - ENVIRONMENT=dev
      - BACKUP_SCHEDULE=${DEV_BACKUP_SCHEDULE}
    volumes:
      - redis_dev_data:/data
      - ./redis/config/dev:/usr/local/etc/redis:ro
      - ./backups/dev:/backups
      - ./redis/cron:/etc/cron.d:ro
    ports:
      - "${REDIS_DEV_EXTERNAL_PORT}:6379"
    networks:
      - database_network
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "redis-cli", "-a", "${REDIS_DEV_PASSWORD}", "ping"]
      interval: 30s
      timeout: 10s
      retries: 3

  # Redis Production Environment
  redis-prod:
    build:
      context: ./redis
      dockerfile: Dockerfile
      args:
        ENVIRONMENT: prod
    container_name: mapmytour_redis_prod
    environment:
      - REDIS_PASSWORD=${REDIS_PROD_PASSWORD}
      - ENVIRONMENT=prod
      - BACKUP_SCHEDULE=${PROD_BACKUP_SCHEDULE}
    volumes:
      - redis_prod_data:/data
      - ./redis/config/prod:/usr/local/etc/redis:ro
      - ./backups/prod:/backups
      - ./redis/cron:/etc/cron.d:ro
    ports:
      - "${REDIS_PROD_EXTERNAL_PORT}:6379"
    networks:
      - database_network
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "redis-cli", "-a", "${REDIS_PROD_PASSWORD}", "ping"]
      interval: 30s
      timeout: 10s
      retries: 3

  # Backup Manager Service
  backup-manager:
    build:
      context: ./backup-manager
      dockerfile: Dockerfile
    container_name: mapmytour_backup_manager
    environment:
      - POSTGRES_DEV_USER=${POSTGRES_DEV_USER}
      - POSTGRES_DEV_PASSWORD=${POSTGRES_DEV_PASSWORD}
      - POSTGRES_DEV_DB=${POSTGRES_DEV_DB}
      - POSTGRES_PROD_USER=${POSTGRES_PROD_USER}
      - POSTGRES_PROD_PASSWORD=${POSTGRES_PROD_PASSWORD}
      - POSTGRES_PROD_DB=${POSTGRES_PROD_DB}
      - REDIS_DEV_PASSWORD=${REDIS_DEV_PASSWORD}
      - REDIS_PROD_PASSWORD=${REDIS_PROD_PASSWORD}
      - DEV_BACKUP_SCHEDULE=${DEV_BACKUP_SCHEDULE}
      - PROD_BACKUP_SCHEDULE=${PROD_BACKUP_SCHEDULE}
    volumes:
      - ./backups:/backups
      - /var/run/docker.sock:/var/run/docker.sock:ro
    networks:
      - database_network
    restart: unless-stopped
    depends_on:
      - postgres-dev
      - postgres-prod
      - redis-dev
      - redis-prod

volumes:
  postgres_dev_data:
    driver: local
  postgres_prod_data:
    driver: local
  redis_dev_data:
    driver: local
  redis_prod_data:
    driver: local

networks:
  database_network:
    driver: bridge
COMPOSE_EOF

print_status "✅ Docker Compose configuration created"

# ===================================================================
# CREATE POSTGRESQL CONFIGURATION
# ===================================================================

print_header "Creating PostgreSQL Configuration"

print_section "Creating PostgreSQL Dockerfile..."
cat > postgres/Dockerfile << 'PG_DOCKERFILE'
FROM postgres:15-alpine

# Build argument for environment
ARG ENVIRONMENT=dev

# Install additional tools and cron
RUN apk add --no-cache \
    curl \
    bash \
    postgresql-contrib \
    dcron \
    tzdata \
    zip \
    gzip

# Set timezone
ENV TZ=Asia/Kolkata
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Create directories
RUN mkdir -p /backups && chmod 777 /backups
RUN mkdir -p /scripts && chmod 755 /scripts
RUN mkdir -p /var/log/cron && chmod 755 /var/log/cron

# Copy initialization scripts
COPY init-scripts/ /docker-entrypoint-initdb.d/
RUN chmod +x /docker-entrypoint-initdb.d/*.sh

# Copy backup and maintenance scripts
COPY scripts/ /scripts/
RUN chmod +x /scripts/*.sh

# Set environment variable
ENV ENVIRONMENT=${ENVIRONMENT}

# Copy custom entrypoint
COPY entrypoint.sh /usr/local/bin/custom-entrypoint.sh
RUN chmod +x /usr/local/bin/custom-entrypoint.sh

# Create log file for cron
RUN touch /var/log/cron/cron.log

EXPOSE 5432

# Use custom entrypoint that starts both postgres and cron
ENTRYPOINT ["/usr/local/bin/custom-entrypoint.sh"]
PG_DOCKERFILE

print_section "Creating PostgreSQL entrypoint script..."
cat > postgres/entrypoint.sh << 'PG_ENTRYPOINT'
#!/bin/bash
set -e

echo "🐘 Starting PostgreSQL ${ENVIRONMENT} Environment..."

# Create backup directory structure
mkdir -p /backups/postgres
mkdir -p /backups/archive
chmod -R 777 /backups

# Setup cron jobs based on environment
setup_cron() {
    echo "⏰ Setting up cron jobs for ${ENVIRONMENT} environment..."
    
    # Create cron job file
    cat > /etc/cron.d/postgres-backup << EOF
# PostgreSQL Backup Cron Job for ${ENVIRONMENT}
SHELL=/bin/bash
PATH=/usr/local/sbin:/usr/local/bin:/sbin:/bin:/usr/sbin:/usr/bin

# Backup schedule from environment variable
${BACKUP_SCHEDULE} root /scripts/backup-postgres.sh >> /var/log/cron/backup.log 2>&1

# Weekly cleanup - every Sunday at 3 AM
0 3 * * 0 root /scripts/cleanup-backups.sh >> /var/log/cron/cleanup.log 2>&1

# Health check - every hour
0 * * * * root /scripts/health-check.sh >> /var/log/cron/health.log 2>&1

EOF

    # Set proper permissions
    chmod 0644 /etc/cron.d/postgres-backup
    
    # Start cron daemon
    echo "Starting cron daemon..."
    crond -f -d 8 &
    
    echo "✅ Cron jobs configured successfully"
}

# Create backup script
create_backup_script() {
    echo "📝 Creating backup script for ${ENVIRONMENT}..."
    
    cat > /scripts/backup-postgres.sh << 'EOF'
#!/bin/bash
set -e

# Configuration
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_DIR="/backups/postgres"
ARCHIVE_DIR="/backups/archive"
LOG_FILE="/var/log/cron/backup.log"

# Create directories
mkdir -p "$BACKUP_DIR" "$ARCHIVE_DIR"

# Logging function
log_message() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

log_message "🗄️ Starting PostgreSQL backup for ${ENVIRONMENT} environment..."

# Create full database backup
BACKUP_FILE="${BACKUP_DIR}/full_backup_${ENVIRONMENT}_${TIMESTAMP}.sql"
log_message "Creating full backup: $(basename $BACKUP_FILE)"

if pg_dumpall -U "$POSTGRES_USER" > "$BACKUP_FILE"; then
    log_message "✅ Full backup created successfully"
    
    # Compress the backup
    gzip "$BACKUP_FILE"
    BACKUP_FILE="${BACKUP_FILE}.gz"
    log_message "✅ Backup compressed: $(basename $BACKUP_FILE)"
    
    # Create individual database backups
    log_message "Creating individual database backups..."
    
    # Get list of databases
    DATABASES=$(psql -U "$POSTGRES_USER" -t -c "SELECT datname FROM pg_database WHERE datistemplate = false AND datname != 'postgres';" | sed '/^$/d' | sed 's/^ *//')
    
    for db in $DATABASES; do
        DB_BACKUP_FILE="${BACKUP_DIR}/db_${db}_${ENVIRONMENT}_${TIMESTAMP}.sql"
        if pg_dump -U "$POSTGRES_USER" "$db" > "$DB_BACKUP_FILE"; then
            gzip "$DB_BACKUP_FILE"
            log_message "✅ Database '$db' backed up and compressed"
        else
            log_message "❌ Failed to backup database '$db'"
        fi
    done
    
    # Create metadata file
    cat > "${BACKUP_DIR}/backup_info_${TIMESTAMP}.txt" << METADATA
Backup Information
==================
Environment: ${ENVIRONMENT}
Timestamp: ${TIMESTAMP}
Date: $(date '+%Y-%m-%d %H:%M:%S %Z')
PostgreSQL Version: $(psql -U "$POSTGRES_USER" -t -c "SELECT version();" | head -1)
Databases Backed Up: $(echo $DATABASES | tr '\n' ', ')
Backup Size: $(du -sh "$BACKUP_FILE" | cut -f1)
Server: $(hostname)
METADATA
    
    log_message "✅ Backup metadata created"
    
    # Archive old backups (move backups older than 7 days to archive)
    find "$BACKUP_DIR" -name "*.gz" -mtime +7 -exec mv {} "$ARCHIVE_DIR/" \;
    find "$BACKUP_DIR" -name "*.txt" -mtime +7 -exec mv {} "$ARCHIVE_DIR/" \;
    
    log_message "📦 Old backups archived"
    
else
    log_message "❌ Backup failed!"
    exit 1
fi

log_message "🎉 Backup process completed successfully"
EOF
    
    chmod +x /scripts/backup-postgres.sh
}

# Create cleanup script
create_cleanup_script() {
    echo "🧹 Creating cleanup script..."
    
    cat > /scripts/cleanup-backups.sh << 'EOF'
#!/bin/bash
set -e

LOG_FILE="/var/log/cron/cleanup.log"
BACKUP_DIR="/backups/postgres"
ARCHIVE_DIR="/backups/archive"

log_message() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

log_message "🧹 Starting backup cleanup for ${ENVIRONMENT}..."

# Remove backups older than retention period
case "$ENVIRONMENT" in
    "prod")
        RETENTION_DAYS=30
        ;;
    "dev")
        RETENTION_DAYS=7
        ;;
    *)
        RETENTION_DAYS=7
        ;;
esac

log_message "Retention period: $RETENTION_DAYS days"

# Clean up archive directory
DELETED_COUNT=$(find "$ARCHIVE_DIR" -name "*.gz" -mtime +$RETENTION_DAYS -delete -print | wc -l)
log_message "Deleted $DELETED_COUNT old backup files from archive"

# Clean up metadata files
find "$ARCHIVE_DIR" -name "*.txt" -mtime +$RETENTION_DAYS -delete

# Clean up log files older than 30 days
find /var/log/cron -name "*.log" -mtime +30 -exec truncate -s 0 {} \;

log_message "✅ Cleanup completed"
EOF
    
    chmod +x /scripts/cleanup-backups.sh
}

# Create health check script
create_health_check() {
    echo "🏥 Creating health check script..."
    
    cat > /scripts/health-check.sh << 'EOF'
#!/bin/bash
LOG_FILE="/var/log/cron/health.log"

log_message() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" >> "$LOG_FILE"
}

# Check PostgreSQL connectivity
if pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB" >/dev/null 2>&1; then
    log_message "✅ PostgreSQL ${ENVIRONMENT} is healthy"
else
    log_message "❌ PostgreSQL ${ENVIRONMENT} health check failed"
fi

# Check disk space
DISK_USAGE=$(df /var/lib/postgresql/data | awk 'NR==2 {print $5}' | sed 's/%//')
log_message "💾 Disk usage: ${DISK_USAGE}%"

# Check active connections
CONNECTIONS=$(psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -t -c "SELECT count(*) FROM pg_stat_activity;" 2>/dev/null | xargs)
log_message "🔗 Active connections: ${CONNECTIONS}"
EOF
    
    chmod +x /scripts/health-check.sh
}

# Main execution
echo "🔧 Setting up PostgreSQL ${ENVIRONMENT} environment..."

# Create all scripts
create_backup_script
create_cleanup_script
create_health_check

# Setup cron if BACKUP_SCHEDULE is provided
if [ ! -z "$BACKUP_SCHEDULE" ]; then
    setup_cron
else
    echo "⚠️ No backup schedule provided, skipping cron setup"
fi

echo "✅ PostgreSQL ${ENVIRONMENT} setup completed"

# Start PostgreSQL with the original entrypoint
exec docker-entrypoint.sh postgres
PG_ENTRYPOINT

print_section "Creating PostgreSQL initialization scripts..."
cat > postgres/init-scripts/01-init-db.sh << 'PG_INIT'
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
PG_INIT

cat > postgres/init-scripts/02-setup-backup.sh << 'PG_BACKUP_INIT'
#!/bin/bash
set -e

echo "🔧 Setting up PostgreSQL backup functionality for ${ENVIRONMENT}..."

# Create backup script
cat > /backups/backup-postgres.sh << 'BACKUP_EOF'
#!/bin/bash
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_DIR="/backups/postgres"
mkdir -p $BACKUP_DIR

echo "🗄️ Creating PostgreSQL backup..."
pg_dumpall -U $POSTGRES_USER > "$BACKUP_DIR/full_backup_$TIMESTAMP.sql"
echo "✅ Backup created: $BACKUP_DIR/full_backup_$TIMESTAMP.sql"

# Keep only last 7 days of backups
find $BACKUP_DIR -name "full_backup_*.sql" -mtime +7 -delete
BACKUP_EOF

chmod +x /backups/backup-postgres.sh

echo "✅ PostgreSQL backup setup completed for ${ENVIRONMENT}!"
PG_BACKUP_INIT

print_status "✅ PostgreSQL configuration created"

# ===================================================================
# CREATE REDIS CONFIGURATION
# ===================================================================

print_header "Creating Redis Configuration"

print_section "Creating Redis Dockerfile..."
cat > redis/Dockerfile << 'REDIS_DOCKERFILE'
FROM redis:7-alpine

# Build argument for environment
ARG ENVIRONMENT=dev

# Install additional tools and cron
RUN apk add --no-cache \
    curl \
    bash \
    dcron \
    tzdata \
    zip \
    gzip

# Set timezone
ENV TZ=Asia/Kolkata
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Create directories
RUN mkdir -p /backups && chmod 777 /backups
RUN mkdir -p /scripts && chmod 755 /scripts
RUN mkdir -p /var/log/cron && chmod 755 /var/log/cron

# Copy Redis configuration based on environment
COPY config/${ENVIRONMENT}/redis.conf /usr/local/etc/redis/redis.conf

# Copy backup and maintenance scripts
COPY scripts/ /scripts/
RUN chmod +x /scripts/*.sh

# Set environment variable
ENV ENVIRONMENT=${ENVIRONMENT}

# Copy and set custom entrypoint
COPY entrypoint.sh /usr/local/bin/custom-entrypoint.sh
RUN chmod +x /usr/local/bin/custom-entrypoint.sh

# Create log files
RUN touch /var/log/cron/cron.log
RUN touch /var/log/cron/backup.log
RUN touch /var/log/cron/health.log

EXPOSE 6379

ENTRYPOINT ["/usr/local/bin/custom-entrypoint.sh"]
REDIS_DOCKERFILE

print_section "Creating Redis entrypoint script..."
cat > redis/entrypoint.sh << 'REDIS_ENTRYPOINT'
#!/bin/bash
set -e

echo "🔴 Starting Redis ${ENVIRONMENT} Environment..."

# Create backup directory structure
mkdir -p /backups/redis
mkdir -p /backups/archive
chmod -R 777 /backups

# Create Redis config copy to modify
cp /usr/local/etc/redis/redis.conf /tmp/redis.conf

# Set Redis password if provided
if [ ! -z "$REDIS_PASSWORD" ]; then
    echo "requirepass $REDIS_PASSWORD" >> /tmp/redis.conf
    echo "Redis password configured for ${ENVIRONMENT}"
fi

# Setup cron jobs
setup_cron() {
    echo "⏰ Setting up cron jobs for Redis ${ENVIRONMENT} environment..."
    
    # Create cron job file
    cat > /etc/cron.d/redis-backup << EOF
# Redis Backup Cron Job for ${ENVIRONMENT}
SHELL=/bin/bash
PATH=/usr/local/sbin:/usr/local/bin:/sbin:/bin:/usr/sbin:/usr/bin

# Backup schedule from environment variable
${BACKUP_SCHEDULE} root /scripts/backup-redis.sh >> /var/log/cron/backup.log 2>&1

# Weekly cleanup - every Sunday at 4 AM
0 4 * * 0 root /scripts/cleanup-backups.sh >> /var/log/cron/cleanup.log 2>&1

# Health check - every hour at 30 minutes
30 * * * * root /scripts/health-check.sh >> /var/log/cron/health.log 2>&1

EOF

    # Set proper permissions
    chmod 0644 /etc/cron.d/redis-backup
    
    # Start cron daemon
    echo "Starting cron daemon..."
    crond -f -d 8 &
    
    echo "✅ Redis cron jobs configured successfully"
}

# Create backup script
create_backup_script() {
    echo "📝 Creating Redis backup script for ${ENVIRONMENT}..."
    
    cat > /scripts/backup-redis.sh << 'EOF'
#!/bin/bash
set -e

# Configuration
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_DIR="/backups/redis"
ARCHIVE_DIR="/backups/archive"
LOG_FILE="/var/log/cron/backup.log"

# Create directories
mkdir -p "$BACKUP_DIR" "$ARCHIVE_DIR"

# Logging function
log_message() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

log_message "🗄️ Starting Redis backup for ${ENVIRONMENT} environment..."

# Function to authenticate Redis CLI
redis_auth() {
    if [ ! -z "$REDIS_PASSWORD" ]; then
        redis-cli -a "$REDIS_PASSWORD" "$@"
    else
        redis-cli "$@"
    fi
}

# Trigger Redis save
log_message "Triggering Redis BGSAVE..."
if redis_auth BGSAVE | grep -q "Background saving started"; then
    log_message "✅ Background save initiated"
    
    # Wait for save to complete
    while [ "$(redis_auth LASTSAVE)" == "$(redis_auth LASTSAVE)" ]; do
        sleep 1
    done
    log_message "✅ Background save completed"
else
    log_message "❌ Failed to initiate background save"
    exit 1
fi

# Backup RDB file
if [ -f "/data/dump.rdb" ]; then
    RDB_BACKUP="${BACKUP_DIR}/dump_${ENVIRONMENT}_${TIMESTAMP}.rdb"
    cp /data/dump.rdb "$RDB_BACKUP"
    gzip "$RDB_BACKUP"
    log_message "✅ RDB backup created and compressed: $(basename ${RDB_BACKUP}.gz)"
else
    log_message "⚠️ No RDB file found"
fi

# Backup AOF file
if [ -f "/data/appendonly.aof" ]; then
    AOF_BACKUP="${BACKUP_DIR}/appendonly_${ENVIRONMENT}_${TIMESTAMP}.aof"
    cp /data/appendonly.aof "$AOF_BACKUP"
    gzip "$AOF_BACKUP"
    log_message "✅ AOF backup created and compressed: $(basename ${AOF_BACKUP}.gz)"
else
    log_message "⚠️ No AOF file found"
fi

# Get Redis info and create metadata
REDIS_INFO=$(redis_auth INFO server | grep -E "(redis_version|uptime_in_days|used_memory_human)")

cat > "${BACKUP_DIR}/backup_info_${TIMESTAMP}.txt" << METADATA
Redis Backup Information
========================
Environment: ${ENVIRONMENT}
Timestamp: ${TIMESTAMP}
Date: $(date '+%Y-%m-%d %H:%M:%S %Z')
$REDIS_INFO
Server: $(hostname)
Backup Files:
$(ls -la ${BACKUP_DIR}/*${TIMESTAMP}* 2>/dev/null || echo "No backup files found")
METADATA

log_message "✅ Backup metadata created"

# Archive old backups
find "$BACKUP_DIR" -name "*.gz" -mtime +7 -exec mv {} "$ARCHIVE_DIR/" \;
find "$BACKUP_DIR" -name "*.txt" -mtime +7 -exec mv {} "$ARCHIVE_DIR/" \;

log_message "📦 Old backups archived"
log_message "🎉 Redis backup process completed successfully"
EOF
    
    chmod +x /scripts/backup-redis.sh
}

# Create cleanup script
create_cleanup_script() {
    echo "🧹 Creating Redis cleanup script..."
    
    cat > /scripts/cleanup-backups.sh << 'EOF'
#!/bin/bash
set -e

LOG_FILE="/var/log/cron/cleanup.log"
BACKUP_DIR="/backups/redis"
ARCHIVE_DIR="/backups/archive"

log_message() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

log_message "🧹 Starting Redis backup cleanup for ${ENVIRONMENT}..."

# Set retention period based on environment
case "$ENVIRONMENT" in
    "prod")
        RETENTION_DAYS=30
        ;;
    "dev")
        RETENTION_DAYS=7
        ;;
    *)
        RETENTION_DAYS=7
        ;;
esac

log_message "Retention period: $RETENTION_DAYS days"

# Clean up archive directory
DELETED_COUNT=$(find "$ARCHIVE_DIR" -name "*.gz" -mtime +$RETENTION_DAYS -delete -print | wc -l)
log_message "Deleted $DELETED_COUNT old backup files from archive"

# Clean up metadata files
find "$ARCHIVE_DIR" -name "*.txt" -mtime +$RETENTION_DAYS -delete

# Clean up log files older than 30 days
find /var/log/cron -name "*.log" -mtime +30 -exec truncate -s 0 {} \;

log_message "✅ Redis cleanup completed"
EOF
    
    chmod +x /scripts/cleanup-backups.sh
}

# Create health check script
create_health_check() {
    echo "🏥 Creating Redis health check script..."
    
    cat > /scripts/health-check.sh << 'EOF'
#!/bin/bash
LOG_FILE="/var/log/cron/health.log"

log_message() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" >> "$LOG_FILE"
}

# Function to authenticate Redis CLI
redis_auth() {
    if [ ! -z "$REDIS_PASSWORD" ]; then
        redis-cli -a "$REDIS_PASSWORD" "$@" 2>/dev/null
    else
        redis-cli "$@"
    fi
}

# Check Redis connectivity
if redis_auth ping | grep -q "PONG"; then
    log_message "✅ Redis ${ENVIRONMENT} is healthy"
    
    # Get Redis stats
    MEMORY_USAGE=$(redis_auth INFO memory | grep "used_memory_human" | cut -d: -f2 | tr -d '\r')
    CONNECTED_CLIENTS=$(redis_auth INFO clients | grep "connected_clients" | cut -d: -f2 | tr -d '\r')
    UPTIME=$(redis_auth INFO server | grep "uptime_in_days" | cut -d: -f2 | tr -d '\r')
    
    log_message "📊 Memory usage: ${MEMORY_USAGE}, Clients: ${CONNECTED_CLIENTS}, Uptime: ${UPTIME} days"
else
    log_message "❌ Redis ${ENVIRONMENT} health check failed"
fi

# Check disk space
DISK_USAGE=$(df /data | awk 'NR==2 {print $5}' | sed 's/%//')
log_message "💾 Disk usage: ${DISK_USAGE}%"
EOF
    
    chmod +x /scripts/health-check.sh
}

# Main execution
echo "🔧 Setting up Redis ${ENVIRONMENT} environment..."

# Create all scripts
create_backup_script
create_cleanup_script
create_health_check

# Setup cron if BACKUP_SCHEDULE is provided
if [ ! -z "$BACKUP_SCHEDULE" ]; then
    setup_cron
else
    echo "⚠️ No backup schedule provided, skipping cron setup"
fi

echo "✅ Redis ${ENVIRONMENT} setup completed!"

# Start Redis with the modified config
exec redis-server /tmp/redis.conf
REDIS_ENTRYPOINT

print_section "Creating Redis development configuration..."
cat > redis/config/dev/redis.conf << 'REDIS_DEV_CONF'
# Redis Configuration for MapMyTour Development Environment

# Basic Settings
port 6379
bind 0.0.0.0
protected-mode no

# Memory Management - Development Settings
maxmemory 512mb
maxmemory-policy allkeys-lru

# Persistence Settings - More frequent saves for dev
save 300 10
save 60 1000
save 30 10000

# RDB Settings
rdbcompression yes
rdbchecksum yes
dbfilename dump.rdb
dir /data

# AOF Settings - Enabled for data safety
appendonly yes
appendfilename "appendonly.aof"
appendfsync everysec
no-appendfsync-on-rewrite no
auto-aof-rewrite-percentage 100
auto-aof-rewrite-min-size 64mb

# Logging - More verbose for development
loglevel notice
logfile "/data/redis-dev.log"
syslog-enabled yes
syslog-ident redis-dev

# Performance Settings - Development optimized
tcp-keepalive 300
timeout 300
tcp-backlog 511
databases 16

# Client Settings
maxclients 100

# Slow Log - Track slow queries
slowlog-log-slower-than 10000
slowlog-max-len 128

# Development specific settings
stop-writes-on-bgsave-error yes
rdbcompression yes
rdbchecksum yes

# Latency Monitoring
latency-monitor-threshold 100

# Memory Usage Tracking
maxmemory-samples 5

# Key Expiration
activerehashing yes

# Client Output Buffer Limits
client-output-buffer-limit normal 0 0 0
client-output-buffer-limit replica 256mb 64mb 60
client-output-buffer-limit pubsub 32mb 8mb 60

# Hash Settings
hash-max-ziplist-entries 512
hash-max-ziplist-value 64

# List Settings
list-max-ziplist-size -2
list-compress-depth 0

# Set Settings
set-max-intset-entries 512

# Sorted Set Settings
zset-max-ziplist-entries 128
zset-max-ziplist-value 64

# HyperLogLog Settings
hll-sparse-max-bytes 3000

# Streams Settings
stream-node-max-bytes 4096
stream-node-max-entries 100

# Active Rehashing
activerehashing yes

# AOF Rewrite Settings
aof-rewrite-incremental-fsync yes

# RDB-AOF Hybrid Persistence
aof-use-rdb-preamble yes
REDIS_DEV_CONF

print_section "Creating Redis production configuration..."
cat > redis/config/prod/redis.conf << 'REDIS_PROD_CONF'
# Redis Configuration for MapMyTour Production Environment

# Basic Settings
port 6379
bind 0.0.0.0
protected-mode no

# Memory Management - Production Settings
maxmemory 1gb
maxmemory-policy allkeys-lru

# Persistence Settings - Production optimized
save 900 1
save 300 10
save 60 10000

# RDB Settings
rdbcompression yes
rdbchecksum yes
dbfilename dump.rdb
dir /data

# AOF Settings - Critical for production data safety
appendonly yes
appendfilename "appendonly.aof"
appendfsync everysec
no-appendfsync-on-rewrite no
auto-aof-rewrite-percentage 100
auto-aof-rewrite-min-size 128mb

# Logging - Production level
loglevel notice
logfile "/data/redis-prod.log"
syslog-enabled yes
syslog-ident redis-prod

# Performance Settings - Production optimized
tcp-keepalive 300
timeout 0
tcp-backlog 511
databases 16

# Client Settings
maxclients 1000

# Slow Log - Production monitoring
slowlog-log-slower-than 10000
slowlog-max-len 128

# Production Security Settings
stop-writes-on-bgsave-error yes
rdbcompression yes
rdbchecksum yes

# Latency Monitoring - Critical for production
latency-monitor-threshold 100

# Memory Usage Tracking
maxmemory-samples 5

# Key Expiration
activerehashing yes

# Client Output Buffer Limits - Production sized
client-output-buffer-limit normal 0 0 0
client-output-buffer-limit replica 512mb 128mb 60
client-output-buffer-limit pubsub 64mb 16mb 60

# Hash Settings - Production optimized
hash-max-ziplist-entries 512
hash-max-ziplist-value 64

# List Settings
list-max-ziplist-size -2
list-compress-depth 0

# Set Settings
set-max-intset-entries 512

# Sorted Set Settings
zset-max-ziplist-entries 128
zset-max-ziplist-value 64

# HyperLogLog Settings
hll-sparse-max-bytes 3000

# Streams Settings
stream-node-max-bytes 4096
stream-node-max-entries 100

# Active Rehashing
activerehashing yes

# AOF Rewrite Settings
aof-rewrite-incremental-fsync yes

# RDB-AOF Hybrid Persistence
aof-use-rdb-preamble yes

# Production Specific Optimizations
# Disable some commands for security
rename-command FLUSHDB ""
rename-command FLUSHALL ""
rename-command DEBUG ""

# Enable key space notifications for monitoring
notify-keyspace-events Ex

# TLS/SSL Settings (if needed)
# tls-port 6380
# tls-cert-file /etc/ssl/redis.crt
# tls-key-file /etc/ssl/redis.key

# Modules (if any)
# loadmodule /path/to/module.so
REDIS_PROD_CONF

print_status "✅ Redis configuration created"

# ===================================================================
# CREATE BACKUP MANAGER
# ===================================================================

print_header "Creating Backup Manager"

print_section "Creating Backup Manager Dockerfile..."
cat > backup-manager/Dockerfile << 'BM_DOCKERFILE'
FROM alpine:3.18

# Install required packages
RUN apk add --no-cache \
    bash \
    curl \
    docker-cli \
    postgresql-client \
    redis \
    dcron \
    tzdata \
    zip \
    gzip \
    tar \
    rsync \
    python3 \
    py3-pip

# Set timezone
ENV TZ=Asia/Kolkata
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Create working directories
RUN mkdir -p /app/scripts
RUN mkdir -p /app/logs
RUN mkdir -p /backups
RUN mkdir -p /var/log/cron

WORKDIR /app

# Copy scripts
COPY scripts/ /app/scripts/
RUN chmod +x /app/scripts/*.sh

# Copy configuration
COPY config/ /app/config/

# Install Python dependencies for monitoring
COPY requirements.txt /app/
RUN pip3 install -r requirements.txt

# Create cron jobs directory
RUN mkdir -p /etc/cron.d

# Create log files
RUN touch /var/log/cron/backup-manager.log
RUN touch /app/logs/backup-manager.log

# Copy entrypoint
COPY entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh

# Set environment variables
ENV PYTHONPATH=/app
ENV PATH="/app/scripts:${PATH}"

ENTRYPOINT ["/app/entrypoint.sh"]
BM_DOCKERFILE

print_section "Creating Backup Manager requirements..."
cat > backup-manager/requirements.txt << 'BM_REQUIREMENTS'
# Backup Manager Python Dependencies
# For monitoring, alerting, and reporting

# HTTP requests for webhooks and API calls
requests==2.31.0

# Email notifications
smtplib-starttls==1.0

# JSON handling and data processing
simplejson==3.19.1

# Date and time handling
python-dateutil==2.8.2

# Configuration file parsing
configparser==6.0.0

# Template engine for reports
jinja2==3.1.2

# CSV processing
pandas==2.0.3

# System monitoring
psutil==5.9.5

# Scheduler for backup orchestration
schedule==1.2.0

# Database connections for health checks
psycopg2-binary==2.9.7
redis==4.6.0

# File compression and archiving
py7zr==0.20.6

# Encryption for sensitive data
cryptography==41.0.3

# Metrics and monitoring
prometheus-client==0.17.1
BM_REQUIREMENTS

print_section "Creating Backup Manager entrypoint..."
cat > backup-manager/entrypoint.sh << 'BM_ENTRYPOINT'
#!/bin/bash
set -e

echo "🔧 Starting MapMyTour Backup Manager..."

# Create directory structure
mkdir -p /backups/{dev,prod}/{postgres,redis,archive,reports}
chmod -R 755 /backups

# Setup logging
setup_logging() {
    echo "📝 Setting up logging..."
    
    # Ensure log files exist
    touch /app/logs/backup-manager.log
    touch /app/logs/monitoring.log
    touch /app/logs/alerts.log
    
    # Setup log rotation
    cat > /etc/logrotate.d/backup-manager << EOF
/app/logs/*.log {
    daily
    rotate 30
    compress
    delaycompress
    missingok
    notifempty
    copytruncate
}
EOF
}

# Setup cron jobs for backup management
setup_cron() {
    echo "⏰ Setting up backup manager cron jobs..."
    
    # Create master backup orchestration cron
    cat > /etc/cron.d/backup-manager << EOF
# Backup Manager Cron Jobs
SHELL=/bin/bash
PATH=/usr/local/sbin:/usr/local/bin:/sbin:/bin:/usr/sbin:/usr/bin:/app/scripts

# Development environment backup orchestration
${DEV_BACKUP_SCHEDULE} root /app/scripts/orchestrate-backup.sh dev >> /app/logs/backup-manager.log 2>&1

# Production environment backup orchestration  
${PROD_BACKUP_SCHEDULE} root /app/scripts/orchestrate-backup.sh prod >> /app/logs/backup-manager.log 2>&1

# Daily backup verification and reporting
0 6 * * * root /app/scripts/verify-backups.sh >> /app/logs/backup-manager.log 2>&1

# Weekly backup cleanup and archival
0 3 * * 0 root /app/scripts/archive-backups.sh >> /app/logs/backup-manager.log 2>&1

# Hourly monitoring
0 * * * * root /app/scripts/monitor-services.sh >> /app/logs/monitoring.log 2>&1

# Daily reports
0 8 * * * root /app/scripts/generate-reports.sh >> /app/logs/backup-manager.log 2>&1

EOF
    
    chmod 0644 /etc/cron.d/backup-manager
}

# Create backup orchestration script
create_orchestration_script() {
    echo "🎼 Creating backup orchestration script..."
    
    cat > /app/scripts/orchestrate-backup.sh << 'EOF'
#!/bin/bash
set -e

ENVIRONMENT=$1
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
LOG_FILE="/app/logs/backup-manager.log"

if [ -z "$ENVIRONMENT" ]; then
    echo "Usage: orchestrate-backup.sh <dev|prod>"
    exit 1
fi

log_message() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [$ENVIRONMENT] $1" | tee -a "$LOG_FILE"
}

log_message "🚀 Starting backup orchestration for $ENVIRONMENT environment"

# Check if containers are running
check_container() {
    local container=$1
    if docker ps --format "table {{.Names}}" | grep -q "$container"; then
        log_message "✅ Container $container is running"
        return 0
    else
        log_message "❌ Container $container is not running"
        return 1
    fi
}

# Execute backup in container
execute_backup() {
    local container=$1
    local script=$2
    
    log_message "🗄️ Executing backup in $container..."
    
    if docker exec "$container" "$script"; then
        log_message "✅ Backup completed successfully in $container"
        return 0
    else
        log_message "❌ Backup failed in $container"
        return 1
    fi
}

# Main orchestration logic
POSTGRES_CONTAINER="mapmytour_postgres_$ENVIRONMENT"
REDIS_CONTAINER="mapmytour_redis_$ENVIRONMENT"

BACKUP_SUCCESS=true

# Check containers
if ! check_container "$POSTGRES_CONTAINER"; then
    BACKUP_SUCCESS=false
fi

if ! check_container "$REDIS_CONTAINER"; then
    BACKUP_SUCCESS=false
fi

# Execute backups if containers are running
if [ "$BACKUP_SUCCESS" = true ]; then
    # PostgreSQL Backup
    if ! execute_backup "$POSTGRES_CONTAINER" "/scripts/backup-postgres.sh"; then
        BACKUP_SUCCESS=false
    fi
    
    # Redis Backup
    if ! execute_backup "$REDIS_CONTAINER" "/scripts/backup-redis.sh"; then
        BACKUP_SUCCESS=false
    fi
    
    # Create consolidated backup report
    /app/scripts/create-backup-report.sh "$ENVIRONMENT" "$TIMESTAMP"
    
    if [ "$BACKUP_SUCCESS" = true ]; then
        log_message "🎉 All backups completed successfully for $ENVIRONMENT"
    else
        log_message "⚠️ Some backups failed for $ENVIRONMENT"
    fi
else
    log_message "❌ Backup orchestration failed - containers not available"
fi

# Update backup status
echo "$ENVIRONMENT,$TIMESTAMP,$BACKUP_SUCCESS" >> /backups/backup-status.csv

log_message "📊 Backup orchestration completed for $ENVIRONMENT"
EOF
    
    chmod +x /app/scripts/orchestrate-backup.sh
}

# Create backup verification script
create_verification_script() {
    echo "🔍 Creating backup verification script..."
    
    cat > /app/scripts/verify-backups.sh << 'EOF'
#!/bin/bash
set -e

LOG_FILE="/app/logs/backup-manager.log"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

log_message() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [VERIFY] $1" | tee -a "$LOG_FILE"
}

log_message "🔍 Starting backup verification process"

# Verify backups exist and are valid
verify_environment_backups() {
    local env=$1
    local backup_dir="/backups/$env"
    
    log_message "Verifying $env environment backups..."
    
    # Check PostgreSQL backups
    POSTGRES_BACKUPS=$(find "$backup_dir/postgres" -name "*.gz" -mtime -1 | wc -l)
    if [ "$POSTGRES_BACKUPS" -gt 0 ]; then
        log_message "✅ Found $POSTGRES_BACKUPS recent PostgreSQL backups for $env"
    else
        log_message "❌ No recent PostgreSQL backups found for $env"
    fi
    
    # Check Redis backups
    REDIS_BACKUPS=$(find "$backup_dir/redis" -name "*.gz" -mtime -1 | wc -l)
    if [ "$REDIS_BACKUPS" -gt 0 ]; then
        log_message "✅ Found $REDIS_BACKUPS recent Redis backups for $env"
    else
        log_message "❌ No recent Redis backups found for $env"
    fi
    
    # Test backup integrity (sample test)
    LATEST_PG_BACKUP=$(find "$backup_dir/postgres" -name "*.gz" -mtime -1 | head -1)
    if [ ! -z "$LATEST_PG_BACKUP" ]; then
        if gzip -t "$LATEST_PG_BACKUP" 2>/dev/null; then
            log_message "✅ PostgreSQL backup integrity check passed for $env"
        else
            log_message "❌ PostgreSQL backup integrity check failed for $env"
        fi
    fi
}

# Verify both environments
verify_environment_backups "dev"
verify_environment_backups "prod"

# Generate verification report
cat > "/backups/verification_report_$TIMESTAMP.txt" << REPORT
Backup Verification Report
==========================
Date: $(date '+%Y-%m-%d %H:%M:%S')
Verification ID: $TIMESTAMP

Development Environment:
- PostgreSQL Backups: $(find /backups/dev/postgres -name "*.gz" -mtime -1 | wc -l)
- Redis Backups: $(find /backups/dev/redis -name "*.gz" -mtime -1 | wc -l)

Production Environment:
- PostgreSQL Backups: $(find /backups/prod/postgres -name "*.gz" -mtime -1 | wc -l)
- Redis Backups: $(find /backups/prod/redis -name "*.gz" -mtime -1 | wc -l)

Total Backup Size:
- Dev: $(du -sh /backups/dev 2>/dev/null | cut -f1)
- Prod: $(du -sh /backups/prod 2>/dev/null | cut -f1)

REPORT

log_message "📊 Backup verification completed"
EOF
    
    chmod +x /app/scripts/verify-backups.sh
}

# Create monitoring script
create_monitoring_script() {
    echo "📊 Creating monitoring script..."
    
    cat > /app/scripts/monitor-services.sh << 'EOF'
#!/bin/bash
set -e

LOG_FILE="/app/logs/monitoring.log"

log_message() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [MONITOR] $1" >> "$LOG_FILE"
}

# Check container health
check_containers() {
    local containers=("mapmytour_postgres_dev" "mapmytour_postgres_prod" "mapmytour_redis_dev" "mapmytour_redis_prod")
    
    for container in "${containers[@]}"; do
        if docker ps --format "table {{.Names}}" | grep -q "$container"; then
            # Check container health
            HEALTH=$(docker inspect --format='{{.State.Health.Status}}' "$container" 2>/dev/null || echo "unknown")
            log_message "Container $container: running, health: $HEALTH"
        else
            log_message "❌ Container $container: not running"
        fi
    done
}

# Check disk usage
check_disk_usage() {
    BACKUP_DISK_USAGE=$(df /backups | awk 'NR==2 {print $5}' | sed 's/%//')
    log_message "💾 Backup disk usage: ${BACKUP_DISK_USAGE}%"
    
    if [ "$BACKUP_DISK_USAGE" -gt 85 ]; then
        log_message "⚠️ High disk usage detected: ${BACKUP_DISK_USAGE}%"
    fi
}

# Check backup freshness
check_backup_freshness() {
    local environments=("dev" "prod")
    
    for env in "${environments[@]}"; do
        PG_FRESH=$(find "/backups/$env/postgres" -name "*.gz" -mtime -1 | wc -l)
        REDIS_FRESH=$(find "/backups/$env/redis" -name "*.gz" -mtime -1 | wc -l)
        
        log_message "📊 $env backups - PostgreSQL: $PG_FRESH, Redis: $REDIS_FRESH (last 24h)"
        
        if [ "$PG_FRESH" -eq 0 ] || [ "$REDIS_FRESH" -eq 0 ]; then
            log_message "⚠️ Missing recent backups for $env environment"
        fi
    done
}

# Main monitoring
log_message "🔍 Starting service monitoring"
check_containers
check_disk_usage
check_backup_freshness
log_message "✅ Monitoring check completed"
EOF
    
    chmod +x /app/scripts/monitor-services.sh
}

# Create utility scripts
create_utility_scripts() {
    echo "🔧 Creating utility scripts..."
    
    # Create backup report script
    cat > /app/scripts/create-backup-report.sh << 'EOF'
#!/bin/bash
set -e

ENVIRONMENT=$1
TIMESTAMP=$2
BACKUP_DIR="/backups/$ENVIRONMENT"

if [ -z "$ENVIRONMENT" ] || [ -z "$TIMESTAMP" ]; then
    echo "Usage: create-backup-report.sh <environment> <timestamp>"
    exit 1
fi

REPORT_FILE="$BACKUP_DIR/backup_summary_$TIMESTAMP.txt"

cat > "$REPORT_FILE" << REPORT
MapMyTour Database Backup Summary
=================================
Environment: $ENVIRONMENT
Timestamp: $TIMESTAMP
Date: $(date '+%Y-%m-%d %H:%M:%S %Z')
Server: $(hostname)

PostgreSQL Backup Status:
$(find "$BACKUP_DIR/postgres" -name "*$TIMESTAMP*" -type f | while read file; do
    echo "  - $(basename "$file"): $(du -sh "$file" | cut -f1)"
done)

Redis Backup Status:
$(find "$BACKUP_DIR/redis" -name "*$TIMESTAMP*" -type f | while read file; do
    echo "  - $(basename "$file"): $(du -sh "$file" | cut -f1)"
done)

Total Backup Size: $(du -sh "$BACKUP_DIR" | cut -f1)
Available Space: $(df -h "$BACKUP_DIR" | awk 'NR==2 {print $4}')

REPORT

echo "Backup report created: $REPORT_FILE"
EOF
    
    chmod +x /app/scripts/create-backup-report.sh
    
    # Create archive management script
    cat > /app/scripts/archive-backups.sh << 'EOF'
#!/bin/bash
set -e

LOG_FILE="/app/logs/backup-manager.log"

log_message() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [ARCHIVE] $1" | tee -a "$LOG_FILE"
}

log_message "📦 Starting weekly backup archival process"

# Archive old backups
for env in dev prod; do
    BACKUP_DIR="/backups/$env"
    ARCHIVE_DIR="$BACKUP_DIR/archive"
    
    log_message "Processing $env environment archival..."
    
    # Set retention based on environment
    if [ "$env" = "prod" ]; then
        RETENTION_DAYS=30
    else
        RETENTION_DAYS=7
    fi
    
    # Create weekly archive
    WEEK_ARCHIVE="$ARCHIVE_DIR/weekly_$(date +%Y%W).tar.gz"
    
    # Find files older than retention period
    OLD_FILES=$(find "$BACKUP_DIR" -name "*.gz" -mtime +$RETENTION_DAYS)
    
    if [ ! -z "$OLD_FILES" ]; then
        log_message "Creating weekly archive for $env: $(basename "$WEEK_ARCHIVE")"
        
        # Create archive of old files
        echo "$OLD_FILES" | tar -czf "$WEEK_ARCHIVE" -T -
        
        # Remove original files after archiving
        echo "$OLD_FILES" | xargs rm -f
        
        log_message "✅ Archived and cleaned up old backups for $env"
    else
        log_message "No old backups to archive for $env"
    fi
done

log_message "📦 Weekly archival process completed"
EOF
    
    chmod +x /app/scripts/archive-backups.sh
    
    # Create report generation script
    cat > /app/scripts/generate-reports.sh << 'EOF'
#!/bin/bash
set -e

LOG_FILE="/app/logs/backup-manager.log"
TIMESTAMP=$(date +"%Y%m%d")
REPORT_DIR="/backups/reports"

mkdir -p "$REPORT_DIR"

log_message() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [REPORT] $1" | tee -a "$LOG_FILE"
}

log_message "📈 Generating daily backup report"

# Generate comprehensive daily report
cat > "$REPORT_DIR/daily_report_$TIMESTAMP.html" << 'HTML'
<!DOCTYPE html>
<html>
<head>
    <title>MapMyTour Database Backup Report</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .header { background-color: #2196F3; color: white; padding: 20px; text-align: center; }
        .section { margin: 20px 0; padding: 15px; border: 1px solid #ddd; }
        .success { background-color: #d4edda; border-color: #c3e6cb; }
        .warning { background-color: #fff3cd; border-color: #ffeaa7; }
        .error { background-color: #f8d7da; border-color: #f5c6cb; }
        table { width: 100%; border-collapse: collapse; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #f2f2f2; }
    </style>
</head>
<body>
    <div class="header">
        <h1>MapMyTour Database Backup Report</h1>
        <p>Generated on: $(date '+%Y-%m-%d %H:%M:%S')</p>
    </div>
HTML

# Add environment status for both dev and prod
for env in dev prod; do
    cat >> "$REPORT_DIR/daily_report_$TIMESTAMP.html" << HTML
    <div class="section">
        <h2>$(echo $env | tr '[:lower:]' '[:upper:]') Environment Status</h2>
        <table>
            <tr><th>Service</th><th>Status</th><th>Last Backup</th><th>Backup Count (24h)</th></tr>
HTML

    # PostgreSQL status
    PG_CONTAINER="mapmytour_postgres_$env"
    if docker ps --format "table {{.Names}}" | grep -q "$PG_CONTAINER"; then
        PG_STATUS="✅ Running"
    else
        PG_STATUS="❌ Stopped"
    fi
    
    PG_LAST_BACKUP=$(find "/backups/$env/postgres" -name "*.gz" | sort | tail -1 | xargs basename 2>/dev/null || echo "None")
    PG_COUNT=$(find "/backups/$env/postgres" -name "*.gz" -mtime -1 | wc -l)
    
    cat >> "$REPORT_DIR/daily_report_$TIMESTAMP.html" << HTML
            <tr><td>PostgreSQL</td><td>$PG_STATUS</td><td>$PG_LAST_BACKUP</td><td>$PG_COUNT</td></tr>
HTML

    # Redis status
    REDIS_CONTAINER="mapmytour_redis_$env"
    if docker ps --format "table {{.Names}}" | grep -q "$REDIS_CONTAINER"; then
        REDIS_STATUS="✅ Running"
    else
        REDIS_STATUS="❌ Stopped"
    fi
    
    REDIS_LAST_BACKUP=$(find "/backups/$env/redis" -name "*.gz" | sort | tail -1 | xargs basename 2>/dev/null || echo "None")
    REDIS_COUNT=$(find "/backups/$env/redis" -name "*.gz" -mtime -1 | wc -l)
    
    cat >> "$REPORT_DIR/daily_report_$TIMESTAMP.html" << HTML
            <tr><td>Redis</td><td>$REDIS_STATUS</td><td>$REDIS_LAST_BACKUP</td><td>$REDIS_COUNT</td></tr>
        </table>
    </div>
HTML
done

# Add storage information
TOTAL_BACKUP_SIZE=$(du -sh /backups 2>/dev/null | cut -f1)
DISK_USAGE=$(df /backups | awk 'NR==2 {print $5}')

cat >> "$REPORT_DIR/daily_report_$TIMESTAMP.html" << HTML
    <div class="section">
        <h2>Storage Information</h2>
        <table>
            <tr><th>Metric</th><th>Value</th></tr>
            <tr><td>Total Backup Size</td><td>$TOTAL_BACKUP_SIZE</td></tr>
            <tr><td>Disk Usage</td><td>$DISK_USAGE</td></tr>
        </table>
    </div>
    
    <div class="section">
        <h2>Recent Activity</h2>
        <pre>$(tail -50 /app/logs/backup-manager.log)</pre>
    </div>
</body>
</html>
HTML

log_message "📊 Daily report generated: $REPORT_DIR/daily_report_$TIMESTAMP.html"
EOF
    
    chmod +x /app/scripts/generate-reports.sh
}

# Main setup execution
echo "🔧 Setting up Backup Manager components..."

setup_logging
create_orchestration_script
create_verification_script
create_monitoring_script
create_utility_scripts

# Setup cron jobs
setup_cron

# Start cron daemon
echo "⏰ Starting cron daemon..."
crond -f -d 8 &

echo "✅ Backup Manager setup completed successfully!"
echo "📊 Monitoring services and backup orchestration..."

# Keep container running
tail -f /app/logs/backup-manager.log
BM_ENTRYPOINT

print_status "✅ Backup Manager configuration created"

# ===================================================================
# CREATE MANAGEMENT SCRIPTS
# ===================================================================

print_header "Creating Management Scripts"

print_section "Creating backup-all.sh script..."
cat > backup-all.sh << 'BACKUP_ALL'
#!/bin/bash

echo "🗄️ Starting complete database backup for both environments..."

TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

echo ""
echo "=========================================="
echo "     MapMyTour Database Backup Tool"
echo "=========================================="
echo ""

# Function to check if container is running
check_container() {
    local container=$1
    if docker ps --format "table {{.Names}}" | grep -q "$container"; then
        echo "✅ $container is running"
        return 0
    else
        echo "❌ $container is not running"
        return 1
    fi
}

echo "🔍 Checking container status..."
CONTAINERS=("mapmytour_postgres_dev" "mapmytour_postgres_prod" "mapmytour_redis_dev" "mapmytour_redis_prod")
ALL_RUNNING=true

for container in "${CONTAINERS[@]}"; do
    if ! check_container "$container"; then
        ALL_RUNNING=false
    fi
done

if [ "$ALL_RUNNING" = false ]; then
    echo ""
    echo "⚠️ Some containers are not running. Starting services..."
    docker-compose up -d
    sleep 10
fi

echo ""
echo "📊 Backing up Development Environment..."
echo "----------------------------------------"
docker exec mapmytour_postgres_dev /scripts/backup-postgres.sh
docker exec mapmytour_redis_dev /scripts/backup-redis.sh

echo ""
echo "📊 Backing up Production Environment..."
echo "---------------------------------------"
docker exec mapmytour_postgres_prod /scripts/backup-postgres.sh
docker exec mapmytour_redis_prod /scripts/backup-redis.sh

echo ""
echo "📦 Creating consolidated backup archive..."
tar -czf "complete_backup_$TIMESTAMP.tar.gz" -C ./backups .

echo ""
echo "✅ Complete backup process finished!"
echo "=========================================="
echo ""
echo "📁 Backup locations:"
echo "  • Development: ./backups/dev/"
echo "  • Production: ./backups/prod/"
echo "  • Archive: complete_backup_$TIMESTAMP.tar.gz"
echo ""
echo "📊 Backup summary:"
echo "  • Dev PostgreSQL: $(find ./backups/dev/postgres -name "*.gz" | wc -l) files"
echo "  • Dev Redis: $(find ./backups/dev/redis -name "*.gz" | wc -l) files"
echo "  • Prod PostgreSQL: $(find ./backups/prod/postgres -name "*.gz" | wc -l) files"
echo "  • Prod Redis: $(find ./backups/prod/redis -name "*.gz" | wc -l) files"
echo ""
echo "💾 Total backup size: $(du -sh ./backups | cut -f1)"
echo "🎉 Backup completed successfully!"
BACKUP_ALL

print_section "Creating check-status.sh script..."
cat > check-status.sh << 'STATUS_CHECK'
#!/bin/bash

echo ""
echo "🔍 MapMyTour Database Service Status Check"
echo "=============================================="
echo ""

# Color codes
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Function to get container status
get_container_status() {
    local container=$1
    if docker ps --format "table {{.Names}}" | grep -q "$container"; then
        health=$(docker inspect --format='{{.State.Health.Status}}' "$container" 2>/dev/null || echo "unknown")
        uptime=$(docker inspect --format='{{.State.StartedAt}}' "$container" 2>/dev/null | cut -d'T' -f1)
        echo -e "${GREEN}✅ Running${NC} (Health: $health, Since: $uptime)"
    else
        echo -e "${RED}❌ Stopped${NC}"
    fi
}

echo "📦 Container Status:"
echo "--------------------"
containers=("mapmytour_postgres_dev" "mapmytour_postgres_prod" "mapmytour_redis_dev" "mapmytour_redis_prod" "mapmytour_backup_manager")

for container in "${containers[@]}"; do
    echo -n "  $container: "
    get_container_status "$container"
done

echo ""
echo "🌐 Service Endpoints:"
echo "---------------------"
echo "  • PostgreSQL Dev: postgresql-dev.mapmytour.in:5433 (localhost:5433)"
echo "  • PostgreSQL Prod: postgresql-prod.mapmytour.in:5432 (localhost:5432)" 
echo "  • Redis Dev: redis-dev.mapmytour.in:6380 (localhost:6380)"
echo "  • Redis Prod: redis-prod.mapmytour.in:6379 (localhost:6379)"

echo ""
echo "🔑 Connection Examples:"
echo "-----------------------"
echo "  # PostgreSQL Development"
echo "  psql -h localhost -p 5433 -U admin_dev -d mapmytour_dev"
echo ""
echo "  # PostgreSQL Production"
echo "  psql -h localhost -p 5432 -U admin_prod -d mapmytour_prod"
echo ""
echo "  # Redis Development"
echo "  redis-cli -h localhost -p 6380 -a [password]"
echo ""
echo "  # Redis Production"
echo "  redis-cli -h localhost -p 6379 -a [password]"

echo ""
echo "📊 Recent Backups (Last 24 hours):"
echo "-----------------------------------"
echo "Development Environment:"
dev_pg_count=$(find ./backups/dev/postgres -name "*.gz" -mtime -1 2>/dev/null | wc -l)
dev_redis_count=$(find ./backups/dev/redis -name "*.gz" -mtime -1 2>/dev/null | wc -l)
echo "  • PostgreSQL: $dev_pg_count backups"
echo "  • Redis: $dev_redis_count backups"

if [ $dev_pg_count -gt 0 ]; then
    latest_dev_pg=$(find ./backups/dev/postgres -name "*.gz" -mtime -1 | sort | tail -1 | xargs basename 2>/dev/null)
    echo "    Latest: $latest_dev_pg"
fi

echo ""
echo "Production Environment:"
prod_pg_count=$(find ./backups/prod/postgres -name "*.gz" -mtime -1 2>/dev/null | wc -l)
prod_redis_count=$(find ./backups/prod/redis -name "*.gz" -mtime -1 2>/dev/null | wc -l)
echo "  • PostgreSQL: $prod_pg_count backups"
echo "  • Redis: $prod_redis_count backups"

if [ $prod_pg_count -gt 0 ]; then
    latest_prod_pg=$(find ./backups/prod/postgres -name "*.gz" -mtime -1 | sort | tail -1 | xargs basename 2>/dev/null)
    echo "    Latest: $latest_prod_pg"
fi

echo ""
echo "💾 Storage Usage:"
echo "-----------------"
if [ -d "./backups" ]; then
    echo "  • Total backup size: $(du -sh ./backups 2>/dev/null | cut -f1)"
    echo "  • Dev environment: $(du -sh ./backups/dev 2>/dev/null | cut -f1)"
    echo "  • Prod environment: $(du -sh ./backups/prod 2>/dev/null | cut -f1)"
fi
echo "  • Available space: $(df -h . | awk 'NR==2 {print $4}')"

echo ""
echo "📈 Recent Reports:"
echo "------------------"
if [ -d "./backups/reports" ]; then
    latest_report=$(find ./backups/reports -name "*.html" | sort | tail -1)
    if [ ! -z "$latest_report" ]; then
        echo "  • Latest report: $(basename "$latest_report")"
        echo "    Open: file://$(realpath "$latest_report")"
    else
        echo "  • No reports generated yet"
    fi
else
    echo "  • Reports directory not found"
fi

echo ""
echo "🔧 Quick Actions:"
echo "-----------------"
echo "  • Start services: docker-compose up -d"
echo "  • Stop services: docker-compose down"
echo "  • Manual backup: ./backup-all.sh"
echo "  • View logs: docker-compose logs -f [service]"
echo "  • Restore backup: ./restore-from-backup.sh <env> <file>"

echo ""
echo "📋 System Health:"
echo "-----------------"
# Check Docker
if command -v docker &> /dev/null; then
    echo -e "  • Docker: ${GREEN}✅ Available${NC}"
else
    echo -e "  • Docker: ${RED}❌ Not found${NC}"
fi

# Check Docker Compose
if command -v docker-compose &> /dev/null; then
    echo -e "  • Docker Compose: ${GREEN}✅ Available${NC}"
else
    echo -e "  • Docker Compose: ${RED}❌ Not found${NC}"
fi

# Check disk space
disk_usage=$(df . | awk 'NR==2 {print $5}' | sed 's/%//')
if [ "$disk_usage" -lt 80 ]; then
    echo -e "  • Disk usage: ${GREEN}✅ ${disk_usage}%${NC}"
elif [ "$disk_usage" -lt 90 ]; then
    echo -e "  • Disk usage: ${YELLOW}⚠️ ${disk_usage}%${NC}"
else
    echo -e "  • Disk usage: ${RED}❌ ${disk_usage}%${NC}"
fi

echo ""
echo "✅ Status check completed!"
echo "=============================================="
STATUS_CHECK

print_section "Creating restore-from-backup.sh script..."
cat > restore-from-backup.sh << 'RESTORE_SCRIPT'
#!/bin/bash

# Color codes
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo ""
echo -e "${BLUE}🔄 MapMyTour Database Restore Tool${NC}"
echo "============================================="
echo ""

if [ -z "$1" ] || [ -z "$2" ]; then
    echo -e "${RED}❌ Usage: ./restore-from-backup.sh <environment> <backup_file>${NC}"
    echo ""
    echo "Parameters:"
    echo "  environment: dev or prod"
    echo "  backup_file: path to backup file"
    echo ""
    echo "Examples:"
    echo "  ./restore-from-backup.sh dev ./backups/dev/postgres/full_backup_dev_20240711_020001.sql.gz"
    echo "  ./restore-from-backup.sh prod ./backups/prod/redis/dump_prod_20240711_010001.rdb.gz"
    echo ""
    echo "Available backups:"
    echo "Development PostgreSQL:"
    find ./backups/dev/postgres -name "*.gz" 2>/dev/null | head -5 | while read file; do
        echo "  - $file"
    done
    echo ""
    echo "Production PostgreSQL:"
    find ./backups/prod/postgres -name "*.gz" 2>/dev/null | head -5 | while read file; do
        echo "  - $file"
    done
    exit 1
fi

ENVIRONMENT=$1
BACKUP_FILE=$2

if [ ! -f "$BACKUP_FILE" ]; then
    echo -e "${RED}❌ Backup file not found: $BACKUP_FILE${NC}"
    exit 1
fi

echo -e "${YELLOW}⚠️ WARNING: This will restore data from backup and may overwrite existing data!${NC}"
echo ""
echo "Environment: $ENVIRONMENT"
echo "Backup file: $BACKUP_FILE"
echo ""
read -p "Are you sure you want to continue? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Restore cancelled."
    exit 1
fi

echo ""
echo -e "${BLUE}🔄 Starting restore process...${NC}"

# Determine container names
POSTGRES_CONTAINER="mapmytour_postgres_$ENVIRONMENT"
REDIS_CONTAINER="mapmytour_redis_$ENVIRONMENT"

# Check if containers are running
check_container() {
    local container=$1
    if docker ps --format "table {{.Names}}" | grep -q "$container"; then
        return 0
    else
        return 1
    fi
}

echo "🔍 Checking container status..."

# Restore based on file type
if [[ "$BACKUP_FILE" == *"postgres"* ]] || [[ "$BACKUP_FILE" == *".sql"* ]]; then
    echo "📊 Restoring PostgreSQL backup..."
    
    if ! check_container "$POSTGRES_CONTAINER"; then
        echo -e "${RED}❌ PostgreSQL container not running: $POSTGRES_CONTAINER${NC}"
        echo "Starting container..."
        docker-compose up -d postgres-$ENVIRONMENT
        sleep 5
    fi
    
    echo "🗄️ Restoring PostgreSQL data..."
    if [[ "$BACKUP_FILE" == *".gz" ]]; then
        if gunzip -c "$BACKUP_FILE" | docker exec -i "$POSTGRES_CONTAINER" psql -U admin_${ENVIRONMENT} -d mapmytour_${ENVIRONMENT}; then
            echo -e "${GREEN}✅ PostgreSQL restore completed successfully${NC}"
        else
            echo -e "${RED}❌ PostgreSQL restore failed${NC}"
            exit 1
        fi
    else
        if docker exec -i "$POSTGRES_CONTAINER" psql -U admin_${ENVIRONMENT} -d mapmytour_${ENVIRONMENT} < "$BACKUP_FILE"; then
            echo -e "${GREEN}✅ PostgreSQL restore completed successfully${NC}"
        else
            echo -e "${RED}❌ PostgreSQL restore failed${NC}"
            exit 1
        fi
    fi
    
elif [[ "$BACKUP_FILE" == *"redis"* ]] || [[ "$BACKUP_FILE" == *".rdb"* ]] || [[ "$BACKUP_FILE" == *".aof"* ]]; then
    echo "🔴 Restoring Redis backup..."
    
    if ! check_container "$REDIS_CONTAINER"; then
        echo -e "${RED}❌ Redis container not running: $REDIS_CONTAINER${NC}"
        echo "Starting container..."
        docker-compose up -d redis-$ENVIRONMENT
        sleep 5
    fi
    
    echo "🗄️ Restoring Redis data..."
    
    # Stop Redis temporarily for safe restore
    echo "Stopping Redis service temporarily..."
    docker exec "$REDIS_CONTAINER" redis-cli shutdown nosave || true
    sleep 2
    
    # Extract and copy backup file
    if [[ "$BACKUP_FILE" == *".gz" ]]; then
        TEMP_FILE=$(mktemp)
        gunzip -c "$BACKUP_FILE" > "$TEMP_FILE"
        docker cp "$TEMP_FILE" "$REDIS_CONTAINER:/data/"
        rm "$TEMP_FILE"
    else
        docker cp "$BACKUP_FILE" "$REDIS_CONTAINER:/data/"
    fi
    
    # Restart Redis
    echo "Restarting Redis service..."
    docker restart "$REDIS_CONTAINER"
    sleep 5
    
    # Verify Redis is running
    if docker exec "$REDIS_CONTAINER" redis-cli ping | grep -q "PONG"; then
        echo -e "${GREEN}✅ Redis restore completed successfully${NC}"
    else
        echo -e "${RED}❌ Redis restore failed${NC}"
        exit 1
    fi
    
else
    echo -e "${RED}❌ Unknown backup file type: $BACKUP_FILE${NC}"
    echo "Supported types: PostgreSQL (.sql, .sql.gz) and Redis (.rdb, .aof, .rdb.gz, .aof.gz)"
    exit 1
fi

echo ""
echo -e "${GREEN}✅ Restore completed successfully for $ENVIRONMENT environment${NC}"
echo ""
echo "🔍 Verification:"
echo "  • Check container status: ./check-status.sh"
echo "  • Test connections with your applications"
echo "  • Review logs: docker-compose logs -f"
echo ""
echo "📊 Restore summary:"
echo "  • Environment: $ENVIRONMENT"
echo "  • Backup file: $(basename "$BACKUP_FILE")"
echo "  • Completed at: $(date '+%Y-%m-%d %H:%M:%S')"
RESTORE_SCRIPT

print_section "Creating cleanup.sh script..."
cat > cleanup.sh << 'CLEANUP_SCRIPT'
#!/bin/bash

# Color codes
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo ""
echo -e "${BLUE}🧹 MapMyTour Database Service Cleanup Tool${NC}"
echo "==============================================="
echo ""

echo -e "${YELLOW}⚠️ WARNING: This will stop and remove all database containers and volumes!${NC}"
echo ""
echo "This will:"
echo "  • Stop all running containers"
echo "  • Remove containers and networks"
echo "  • Optionally remove Docker images"
echo "  • Optionally remove backup files"
echo "  • Optionally remove data volumes (PERMANENT DATA LOSS)"
echo ""

read -p "Are you sure you want to continue? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Cleanup cancelled."
    exit 0
fi

echo ""
echo -e "${BLUE}🛑 Stopping and removing containers...${NC}"
docker-compose down --remove-orphans

echo ""
echo -e "${BLUE}🔧 Removing networks...${NC}"
docker network prune -f

echo ""
read -p "Remove Docker images as well? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${BLUE}🗑️ Removing Docker images...${NC}"
    docker rmi $(docker images -q --filter "reference=*mapmytour*" --filter "reference=*database-service*") 2>/dev/null || echo "No matching images found"
fi

echo ""
read -p "Remove backup files as well? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${BLUE}🗑️ Removing backup files...${NC}"
    rm -rf ./backups/*
    echo -e "${GREEN}✅ Backup files removed${NC}"
fi

echo ""
echo -e "${RED}⚠️ DANGER ZONE: Remove data volumes? This will permanently delete ALL database data!${NC}"
read -p "Remove data volumes (PERMANENT DATA LOSS)? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${RED}🗑️ Removing data volumes...${NC}"
    docker volume rm database-service_postgres_dev_data 2>/dev/null || true
    docker volume rm database-service_postgres_prod_data 2>/dev/null || true
    docker volume rm database-service_redis_dev_data 2>/dev/null || true
    docker volume rm database-service_redis_prod_data 2>/dev/null || true
    echo -e "${GREEN}✅ Data volumes removed${NC}"
fi

echo ""
read -p "Remove configuration files and logs? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${BLUE}🗑️ Removing configuration files...${NC}"
    rm -rf ./logs/*
    rm -f complete_backup_*.tar.gz
    echo -e "${GREEN}✅ Configuration files and logs removed${NC}"
fi

echo ""
echo -e "${BLUE}🧹 Running Docker system cleanup...${NC}"
docker system prune -f

echo ""
echo -e "${GREEN}✅ Cleanup completed successfully!${NC}"
echo ""
echo "📋 Summary:"
echo "  • Containers: Stopped and removed"
echo "  • Networks: Cleaned up"
echo "  • Images: $(if [[ $REPLY =~ ^[Yy]$ ]]; then echo "Removed"; else echo "Kept"; fi)"
echo "  • Backups: $(if [[ $REPLY =~ ^[Yy]$ ]]; then echo "Removed"; else echo "Kept"; fi)"
echo "  • Data volumes: $(if [[ $REPLY =~ ^[Yy]$ ]]; then echo "Removed"; else echo "Kept"; fi)"
echo ""
echo "🚀 To recreate the service:"
echo "  • Run: ./setup.sh (if you removed config files)"
echo "  • Run: docker-compose up -d"
CLEANUP_SCRIPT

# Make all scripts executable
chmod +x *.sh

print_status "✅ Management scripts created"

# ===================================================================
# FINAL SETUP AND INSTRUCTIONS
# ===================================================================

print_header "Final Setup and Permissions"

print_section "Setting up permissions..."
# Set proper permissions
find . -name "*.sh" -exec chmod +x {} \;
chmod -R 755 postgres/init-scripts/
chmod -R 755 postgres/scripts/
chmod -R 755 redis/scripts/
chmod -R 755 backup-manager/scripts/

print_status "✅ All permissions set correctly"

print_section "Creating project documentation..."
cat > README.md << 'README_EOF'
# MapMyTour Database Service

Complete Dev/Prod database setup with automated backups, monitoring, and cron jobs.

## Quick Start

```bash
# 1. Start all services
docker-compose up -d

# 2. Check status
./check-status.sh

# 3. Manual backup
./backup-all.sh
```

## Service Access

- **PostgreSQL Dev**: localhost:5433 (postgresql-dev.mapmytour.in:5433)
- **PostgreSQL Prod**: localhost:5432 (postgresql-prod.mapmytour.in:5432)
- **Redis Dev**: localhost:6380 (redis-dev.mapmytour.in:6380)
- **Redis Prod**: localhost:6379 (redis-prod.mapmytour.in:6379)

## Management Commands

- `./check-status.sh` - Check service status
- `./backup-all.sh` - Manual backup all environments
- `./restore-from-backup.sh <env> <file>` - Restore from backup
- `./cleanup.sh` - Complete cleanup

## Automated Features

- Daily backups with cron jobs
- Health monitoring and checks
- Automatic cleanup and archival
- Daily HTML reports
- Container health checks

## Configuration

Edit `.env` file to update passwords and settings before production use.
README_EOF

print_status "✅ Documentation created"

# ===================================================================
# COMPLETION MESSAGE
# ===================================================================

clear
echo -e "${CYAN}"
cat << "EOF"
╔══════════════════════════════════════════════════════════════╗
║                                                              ║
║            🎉 SETUP COMPLETED SUCCESSFULLY! 🎉               ║
║                                                              ║
║          MapMyTour Database Service is Ready                 ║
║                                                              ║
╚══════════════════════════════════════════════════════════════╝
EOF
echo -e "${NC}"

print_header "🎯 Setup Summary"

echo -e "${GREEN}✅ All components created successfully:${NC}"
echo ""
echo "📁 Project Structure:"
echo "  ├── docker-compose.yml        # Main orchestration"
echo "  ├── .env                      # Environment configuration"
echo "  ├── postgres/                 # PostgreSQL setup"
echo "  │   ├── Dockerfile"
echo "  │   ├── entrypoint.sh"
echo "  │   └── init-scripts/"
echo "  ├── redis/                    # Redis setup"
echo "  │   ├── Dockerfile"
echo "  │   ├── entrypoint.sh"
echo "  │   └── config/{dev,prod}/"
echo "  ├── backup-manager/           # Centralized backup management"
echo "  │   ├── Dockerfile"
echo "  │   ├── entrypoint.sh"
echo "  │   └── scripts/"
echo "  ├── backups/                  # Backup storage"
echo "  │   ├── dev/{postgres,redis,archive}/"
echo "  │   ├── prod/{postgres,redis,archive}/"
echo "  │   └── reports/"
echo "  ├── backup-all.sh             # Manual backup script"
echo "  ├── check-status.sh           # Status monitoring"
echo "  ├── restore-from-backup.sh    # Restore utility"
echo "  ├── cleanup.sh                # Cleanup script"
echo "  └── README.md                 # Documentation"

echo ""
echo -e "${BLUE}🔧 Service Configuration:${NC}"
echo "  • PostgreSQL Dev Environment   (Port: 5433)"
echo "  • PostgreSQL Prod Environment  (Port: 5432)"
echo "  • Redis Dev Environment        (Port: 6380)"
echo "  • Redis Prod Environment       (Port: 6379)"
echo "  • Backup Manager Service       (Automated)"

echo ""
echo -e "${PURPLE}⏰ Automated Backup Schedule:${NC}"
echo "  • Development: Daily at 2:00 AM"
echo "  • Production: Daily at 1:00 AM & 1:00 PM"
echo "  • Verification: Daily at 6:00 AM"
echo "  • Reports: Daily at 8:00 AM"
echo "  • Cleanup: Weekly on Sunday at 3:00 AM"

echo ""
echo -e "${CYAN}📊 Retention Policies:${NC}"
echo "  • Development: 7 days"
echo "  • Production: 30 days"
echo "  • Archives: Managed automatically"

echo ""
print_header "🚀 Next Steps"

echo -e "${YELLOW}1. Review and Update Configuration:${NC}"
echo "   nano .env"
echo "   # Update passwords and settings for production use"
echo ""

echo -e "${YELLOW}2. Start All Services:${NC}"
echo "   docker-compose up -d"
echo ""

echo -e "${YELLOW}3. Check Service Status:${NC}"
echo "   ./check-status.sh"
echo ""

echo -e "${YELLOW}4. Test Database Connections:${NC}"
echo "   # PostgreSQL Development"
echo "   psql -h localhost -p 5433 -U admin_dev -d mapmytour_dev"
echo ""
echo "   # PostgreSQL Production"
echo "   psql -h localhost -p 5432 -U admin_prod -d mapmytour_prod"
echo ""
echo "   # Redis Development"
echo "   redis-cli -h localhost -p 6380 -a [dev_password]"
echo ""
echo "   # Redis Production"
echo "   redis-cli -h localhost -p 6379 -a [prod_password]"

echo ""
print_header "🌐 Network Configuration"

echo -e "${YELLOW}Set up DNS records:${NC}"
echo "  postgresql-dev.mapmytour.in    A    150.241.245.162"
echo "  postgresql-prod.mapmytour.in   A    150.241.245.162"
echo "  redis-dev.mapmytour.in         A    150.241.245.162"
echo "  redis-prod.mapmytour.in        A    150.241.245.162"

echo ""
echo -e "${YELLOW}Configure firewall:${NC}"
echo "  ufw allow 5432  # PostgreSQL Prod"
echo "  ufw allow 5433  # PostgreSQL Dev"
echo "  ufw allow 6379  # Redis Prod"
echo "  ufw allow 6380  # Redis Dev"

echo ""
print_header "🔧 Management Commands"

echo -e "${GREEN}Daily Operations:${NC}"
echo "  ./check-status.sh              # Check all services"
echo "  ./backup-all.sh                # Manual backup"
echo "  docker-compose logs -f         # View all logs"
echo ""

echo -e "${GREEN}Backup & Restore:${NC}"
echo "  ./backup-all.sh                # Backup all environments"
echo "  ./restore-from-backup.sh dev [file]   # Restore development"
echo "  ./restore-from-backup.sh prod [file]  # Restore production"
echo ""

echo -e "${GREEN}Monitoring:${NC}"
echo "  ./check-status.sh              # Service status"
echo "  docker-compose ps              # Container status"
echo "  docker-compose logs backup-manager    # Backup logs"
echo "  open ./backups/reports/daily_report_[date].html  # Daily reports"

echo ""
print_header "📈 Monitoring & Reports"

echo -e "${CYAN}Automated Monitoring:${NC}"
echo "  • Health checks every hour"
echo "  • Backup verification daily"
echo "  • Daily HTML reports in ./backups/reports/"
echo "  • Container health monitoring"
echo "  • Disk usage monitoring"

echo ""
echo -e "${CYAN}Log Locations:${NC}"
echo "  • Container logs: docker-compose logs [service]"
echo "  • Backup logs: docker exec [container] tail -f /var/log/cron/backup.log"
echo "  • Manager logs: docker exec mapmytour_backup_manager tail -f /app/logs/backup-manager.log"

echo ""
print_header "⚠️ Important Security Notes"

echo -e "${RED}Before Production Use:${NC}"
echo "  • Update ALL passwords in .env file"
echo "  • Use strong passwords (16+ characters)"
echo "  • Configure SSL/TLS certificates"
echo "  • Set up proper firewall rules"
echo "  • Test backup and restore procedures"
echo "  • Configure monitoring alerts"

echo ""
echo -e "${RED}Default Passwords (CHANGE THESE):${NC}"
echo "  • PostgreSQL Dev: MapMyTour@Dev2024!Secure"
echo "  • PostgreSQL Prod: MapMyTour@Prod2024!SuperSecure"
echo "  • Redis Dev: MapMyTour@DevRedis2024!Secure"
echo "  • Redis Prod: MapMyTour@ProdRedis2024!SuperSecure"

echo ""
print_header "🔄 Quick Start Commands"

echo -e "${GREEN}To start using immediately:${NC}"
echo ""
echo "# 1. Start all services"
echo "docker-compose up -d"
echo ""
echo "# 2. Wait for services to initialize (30 seconds)"
echo "sleep 30"
echo ""
echo "# 3. Check status"
echo "./check-status.sh"
echo ""
echo "# 4. Test connections"
echo "docker exec mapmytour_postgres_dev psql -U admin_dev -d mapmytour_dev -c 'SELECT version();'"
echo "docker exec mapmytour_redis_dev redis-cli -a [password] ping"

echo ""
print_header "📚 Additional Resources"

echo -e "${BLUE}Documentation:${NC}"
echo "  • README.md - Complete documentation"
echo "  • .env - Configuration reference"
echo "  • Daily reports in ./backups/reports/"

echo ""
echo -e "${BLUE}Troubleshooting:${NC}"
echo "  • Check container logs: docker-compose logs [service]"
echo "  • Restart specific service: docker-compose restart [service]"
echo "  • Reset everything: ./cleanup.sh && docker-compose up -d"

echo ""
print_header "🎊 Setup Complete!"

echo ""
echo -e "${GREEN}🎉 MapMyTour Database Service has been successfully set up!${NC}"
echo ""
echo -e "${CYAN}Your enterprise-grade database service includes:${NC}"
echo "  ✅ Separate Dev/Prod environments"
echo "  ✅ Automated daily backups with cron jobs"
echo "  ✅ Health monitoring and alerting"
echo "  ✅ Backup verification and reporting"
echo "  ✅ Easy restore functionality"
echo "  ✅ Container orchestration with Docker"
echo "  ✅ Production-ready configurations"

echo ""
echo -e "${YELLOW}Ready to start? Run these commands:${NC}"
echo ""
echo -e "${BLUE}nano .env${NC}                    # Update passwords"
echo -e "${BLUE}docker-compose up -d${NC}        # Start services"
echo -e "${BLUE}./check-status.sh${NC}           # Verify everything works"

echo ""
echo -e "${GREEN}Happy coding! 🚀${NC}"
echo ""

# ===================================================================
# FINAL VALIDATION
# ===================================================================

print_header "🔍 Final Validation"

print_section "Validating created files..."

# Check if all required files were created
REQUIRED_FILES=(
    "docker-compose.yml"
    ".env"
    "postgres/Dockerfile"
    "postgres/entrypoint.sh"
    "postgres/init-scripts/01-init-db.sh"
    "postgres/init-scripts/02-setup-backup.sh"
    "redis/Dockerfile"
    "redis/entrypoint.sh"
    "redis/config/dev/redis.conf"
    "redis/config/prod/redis.conf"
    "backup-manager/Dockerfile"
    "backup-manager/entrypoint.sh"
    "backup-manager/requirements.txt"
    "backup-all.sh"
    "check-status.sh"
    "restore-from-backup.sh"
    "cleanup.sh"
    "README.md"
)

VALIDATION_SUCCESS=true

for file in "${REQUIRED_FILES[@]}"; do
    if [ -f "$file" ]; then
        print_status "✅ $file"
    else
        print_error "❌ $file - MISSING"
        VALIDATION_SUCCESS=false
    fi
done

# Check directories
REQUIRED_DIRS=(
    "postgres/init-scripts"
    "redis/config/dev"
    "redis/config/prod"
    "backup-manager/scripts"
    "backups/dev/postgres"
    "backups/dev/redis"
    "backups/prod/postgres"
    "backups/prod/redis"
    "backups/reports"
)

for dir in "${REQUIRED_DIRS[@]}"; do
    if [ -d "$dir" ]; then
        print_status "✅ $dir/"
    else
        print_error "❌ $dir/ - MISSING"
        VALIDATION_SUCCESS=false
    fi
done

echo ""
if [ "$VALIDATION_SUCCESS" = true ]; then
    print_status "🎉 All files and directories created successfully!"
else
    print_error "⚠️ Some files or directories are missing. Please check the setup."
fi

echo ""
echo -e "${CYAN}================================================================${NC}"
echo -e "${CYAN}    MapMyTour Database Service Setup Completed Successfully!    ${NC}"
echo -e "${CYAN}================================================================${NC}"
echo ""