#!/bin/bash
set -e

echo "🔧 Starting MapMyTour Backup Manager..."

# Create directory structure
mkdir -p /backups/{dev,prod}/{postgres,redis,archive,reports}
mkdir -p /app/scripts /app/logs
chmod -R 755 /backups /app/scripts /app/logs

# Setup cron jobs (Enterprise Schedule)
setup_cron() {
    # Specify Alpine-compatible crontab location
    CRON_FILE="/var/spool/cron/crontabs/root"
    mkdir -p "$(dirname "$CRON_FILE")"

    # CRITICAL: Eliminate ghost cron jobs from other possible locations
    rm -f /etc/cron.d/backup-manager /etc/crontab /etc/cron.d/* 2>/dev/null || true

    cat > "$CRON_FILE" << 'EOF'
# MapMyTour Production Backup Schedule
# Production Targets (Enterprise Cadence)
*/15 * * * * /app/scripts/orchestrate-backup.sh prod >> /app/logs/backup-manager.log 2>&1
*/10 * * * * /app/scripts/orchestrate-backup.sh prod_fares >> /app/logs/backup-manager.log 2>&1

# Development Targets (Daily at 3 AM)
0 3 * * * /app/scripts/orchestrate-backup.sh dev >> /app/logs/backup-manager.log 2>&1

# Reliability tasks (Every 15 minutes)
*/15 * * * * /app/scripts/cleanup-minute-backups.sh >> /app/logs/backup-manager.log 2>&1
*/15 * * * * /app/scripts/verify-backups.sh >> /app/logs/backup-manager.log 2>&1

# Maintenance & Reports
0 8 * * * /app/scripts/generate-reports.sh >> /app/logs/backup-manager.log 2>&1
0 3 * * 0 /app/scripts/archive-backups.sh >> /app/logs/backup-manager.log 2>&1
0 * * * * /app/scripts/sync-to-cloud.sh >> /app/logs/backup-manager.log 2>&1
EOF
    chmod 0600 "$CRON_FILE"
    echo "✅ Backup-manager cron configured at $CRON_FILE"
}

# Create cleanup script for minute-backups
create_cleanup_minute_backups() {
    cat > /app/scripts/cleanup-minute-backups.sh << 'EOF'
#!/bin/bash
set -e

LOG_FILE="/app/logs/backup-manager.log"

log_message() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [CLEANUP] $1"
}

log_message "Running minute-backup cleanup..."

for env in dev prod; do
    PG_DIR="/backups/$env/postgres"
    REDIS_RDB_DIR="/backups/$env/redis"
    ARCHIVE_DIR="/backups/$env/archive"

    mkdir -p "$PG_DIR" "$REDIS_RDB_DIR" "$ARCHIVE_DIR"

    # PostgreSQL — keep last 120 minute-backups
    PG_TOTAL=$(find "$PG_DIR" -name "full_backup_*.gz" 2>/dev/null | wc -l)
    if [ "$PG_TOTAL" -gt 120 ]; then
        EXCESS=$(($PG_TOTAL - 120))
        find "$PG_DIR" -name "full_backup_*.gz" | sort | head -n "$EXCESS" | while read f; do
            mv "$f" "$ARCHIVE_DIR/"
        done
        log_message "$env PostgreSQL: archived $EXCESS files (keeping last 120)"
    fi

    # Redis RDB — keep last 120
    RDB_TOTAL=$(find "$REDIS_RDB_DIR" -name "dump_*.gz" 2>/dev/null | wc -l)
    if [ "$RDB_TOTAL" -gt 120 ]; then
        EXCESS=$(($RDB_TOTAL - 120))
        find "$REDIS_RDB_DIR" -name "dump_*.gz" | sort | head -n "$EXCESS" | while read f; do
            mv "$f" "$ARCHIVE_DIR/"
        done
        log_message "$env Redis RDB: archived $EXCESS files (keeping last 120)"
    fi

    # Delete from archive based on environment retention
    if [ "$env" = "prod" ]; then
        DELETED=$(find "$ARCHIVE_DIR" -name "*.gz" -mtime +7 -delete -print | wc -l)
        log_message "$env archive: deleted $DELETED files older than 7 days"
    else
        DELETED=$(find "$ARCHIVE_DIR" -name "*.gz" -mmin +1440 -delete -print | wc -l)
        log_message "$env archive: deleted $DELETED files older than 24 hours"
    fi
done

log_message "Cleanup complete"
EOF
    chmod +x /app/scripts/cleanup-minute-backups.sh
}

# Create orchestration script
create_orchestration_script() {
    cat > /app/scripts/orchestrate-backup.sh << 'EOF'
#!/bin/bash
set -euo pipefail

ENVIRONMENT="${1:-}"
LOG_FILE="/app/logs/backup-manager.log"

if [ -z "$ENVIRONMENT" ]; then
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [ERROR] Usage: orchestrate-backup.sh <dev|prod|prod_fares>"
    exit 1
fi

log_message() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [$ENVIRONMENT-ORCH] $1"
}

# Resolve targets cleanly
case "$ENVIRONMENT" in
    dev)
        PROJECT="MapMyTour-Dev"
        POSTGRES_CONTAINER="mapmytour_postgres_dev"
        REDIS_CONTAINER="mapmytour_redis_dev"
        BACKUP_ROOT="/backups/dev"
        ;;
    prod)
        PROJECT="MapMyTour-Prod"
        POSTGRES_CONTAINER="mapmytour_postgres_prod"
        REDIS_CONTAINER="mapmytour_redis_prod"
        BACKUP_ROOT="/backups/prod"
        ;;
    prod_fares)
        PROJECT="MapMyTour-Prod-Fares"
        POSTGRES_CONTAINER=""  # Dedicated redis instance
        REDIS_CONTAINER="mapmytour_redis_fares_prod"
        BACKUP_ROOT="/backups/prod/redis-fares"
        ;;
    *)
        log_message "❌ ABORT: Invalid environment input: '$ENVIRONMENT'"
        exit 1
        ;;
esac

# Validate variables
if [ -z "$REDIS_CONTAINER" ] || [ -z "$BACKUP_ROOT" ]; then
    log_message "❌ ABORT: Critical target variables are empty for $ENVIRONMENT"
    exit 1
fi

LOCK_FILE="/tmp/backup_${ENVIRONMENT}.lock"

# Execute with locking
log_message "Initiating atomic backup for $PROJECT..."
(
    flock -n 200 || { log_message "⚠️ Backup for $PROJECT already in progress. Skipping."; exit 0; }

    # 1. Postgres Backup (if applicable)
    if [ -n "$POSTGRES_CONTAINER" ]; then
        if docker ps --format '{{.Names}}' | grep -q "^${POSTGRES_CONTAINER}$"; then
            log_message "Starting Postgres backup for $POSTGRES_CONTAINER..."
            if ! docker exec "$POSTGRES_CONTAINER" /scripts/backup-postgres.sh >> "$LOG_FILE" 2>&1; then
                log_message "❌ Postgres backup failed for $ENVIRONMENT"
                if [ -n "${SLACK_WEBHOOK_URL:-}" ]; then
                    curl -X POST -H 'Content-type: application/json' --data "{\"text\":\"🚨 Postgres Backup FAILED: $POSTGRES_CONTAINER\"}" "$SLACK_WEBHOOK_URL" || true
                fi
            fi
        else
            log_message "⚠️ $POSTGRES_CONTAINER is not running - skipping."
        fi
    fi

    # 2. Redis Backup
    if docker ps --format '{{.Names}}' | grep -q "^${REDIS_CONTAINER}$"; then
        log_message "Starting Redis backup for $REDIS_CONTAINER..."
        if ! docker exec "$REDIS_CONTAINER" /scripts/backup-redis.sh >> "$LOG_FILE" 2>&1; then
            log_message "❌ Redis backup failed for $ENVIRONMENT"
        fi
    else
        log_message "⚠️ $REDIS_CONTAINER is not running - skipping."
    fi

) 200>"$LOCK_FILE"

log_message "Orchestration phase complete for $PROJECT."
EOF
    chmod +x /app/scripts/orchestrate-backup.sh
}

# Create other required scripts
create_placeholder_scripts() {
    # verify-backups.sh
    cat > /app/scripts/verify-backups.sh << 'EOF'
#!/bin/bash
set -euo pipefail
LOG_FILE="/app/logs/backup-manager.log"
log_message() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] [VERIFY] $1" | tee -a "$LOG_FILE"; }

log_message "🔍 Reviewing backup integrity..."
# Explicit check for all known environments
    for root in /backups/dev /backups/prod /backups/prod/redis-fares; do
        if [ ! -d "$root" ]; then continue; fi
        find "$root" -name "*.gz" -mmin -60 | while read f; do
        if [ -z "$f" ]; then continue; fi
        if [ -f "${f}.md5" ]; then
            if (cd $(dirname "$f") && md5sum -c "$(basename "$f").md5") >> "$LOG_FILE" 2>&1; then
                log_message "✅ $f verified (MD5 Match)"
            else
                log_message "❌ $f INTEGRITY ERROR (MD5 Mismatch!)"
            fi
        else
            if gzip -t "$f" 2>/dev/null; then
                log_message "⚠️ $f valid but missing MD5"
            else
                log_message "❌ $f CORRUPT (No MD5)"
            fi
        fi
    done
done
EOF
    chmod +x /app/scripts/verify-backups.sh

    # Cleanup script: fixed and robust
    cat > /app/scripts/cleanup-minute-backups.sh << 'EOF'
#!/bin/bash
set -euo pipefail
LOG_FILE="/app/logs/backup-manager.log"
log_message() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] [CLEANUP] $1" | tee -a "$LOG_FILE"; }

log_message "Starting retention cleanup..."

cleanup_root() {
    local dir=$1
    if [ -d "$dir" ]; then
        find "$dir" -name "*.gz" -mmin +120 -delete 2>/dev/null || true
    fi
}

# Cleanup minute backups (keep 2 hours = 120 mins)
cleanup_root "/backups/dev/postgres"
cleanup_root "/backups/dev/redis"
cleanup_root "/backups/prod/postgres"
cleanup_root "/backups/prod/redis"
cleanup_root "/backups/prod/redis-fares/redis"

log_message "Retention cleanup complete."
EOF
    chmod +x /app/scripts/cleanup-minute-backups.sh
}

# Main execution
mkdir -p /app/logs
touch /app/logs/backup-manager.log
create_cleanup_minute_backups
create_orchestration_script
create_placeholder_scripts
setup_cron

echo "✅ Backup-manager setup complete"
crond -f &
tail -F /app/logs/backup-manager.log
