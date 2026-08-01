#!/bin/bash
set -e

echo "🔴 Starting Redis ${ENVIRONMENT} Environment..."

# Create directory structure
mkdir -p /backups/redis
mkdir -p /backups/archive
mkdir -p /scripts
mkdir -p /var/log/cron
chmod -R 777 /backups /scripts /var/log/cron

# Create Redis config copy to modify
mkdir -p /etc/redis
cp /usr/local/etc/redis/redis.conf /etc/redis/redis.conf

# Set Redis password if provided
if [ -n "$REDIS_PASSWORD" ]; then
    echo "requirepass $REDIS_PASSWORD" >> /etc/redis/redis.conf
    echo "Redis password configured for ${ENVIRONMENT}"
fi

# Setup cron jobs
setup_cron() {
    cat > /etc/cron.d/redis-backup << EOF
# Redis backup cron — MapMyTour ${ENVIRONMENT}

# Backup every minute
* * * * * /scripts/backup-redis.sh >> /var/log/cron/backup.log 2>&1

# Cleanup every 5 minutes
*/5 * * * * /scripts/cleanup-backups.sh >> /var/log/cron/cleanup.log 2>&1

# Health check every minute
* * * * * /scripts/health-check.sh >> /var/log/cron/health.log 2>&1

EOF
    chmod 0644 /etc/cron.d/redis-backup
    # crond -f -d 8 &  <-- Disabled to allow backup-manager to orchestrate
    echo "✅ Cron disabled — backup-manager will orchestrate"
}

# Create backup script with fixed BGSAVE wait
create_backup_script() {
    cat > /scripts/backup-redis.sh << 'EOF'
#!/bin/bash
set -euo pipefail

# Configuration
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_DIR="${BACKUP_DIR:-/backups/redis}"
ARCHIVE_DIR="${ARCHIVE_DIR:-/backups/archive}"
LOG_FILE="/var/log/cron/backup.log"
LOCK_FILE="/tmp/redis_backup.lock"
# ENVIRONMENT is provided by container env

log_message() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [${ENVIRONMENT:-redis}] $1"
}

# Validation
if [ -z "$BACKUP_DIR" ] || [ -z "$ARCHIVE_DIR" ]; then
    log_message "❌ ABORT: Backup or Archive directory variables are empty"
    exit 1
fi

mkdir -p "$BACKUP_DIR" "$ARCHIVE_DIR"

redis_auth() {
    if [ -n "${REDIS_PASSWORD:-}" ]; then
        redis-cli -a "$REDIS_PASSWORD" "$@" 2>/dev/null
    else
        redis-cli "$@"
    fi
}

log_message "Initiating Redis backup (Dir: $BACKUP_DIR)"

# Atomic backup operation
(
  flock -n 200 || { log_message "⚠️ Another backup is already running. Skipping."; exit 0; }

  # Trigger and wait for BGSAVE
  BEFORE=$(redis_auth LASTSAVE)
  redis_auth BGSAVE > /dev/null 2>&1
  
  ATTEMPTS=0
  while [ "$(redis_auth LASTSAVE)" -le "$BEFORE" ] && [ $ATTEMPTS -lt 60 ]; do
      sleep 1
      ATTEMPTS=$(($ATTEMPTS + 1))
  done

  # Verify BGSAVE status
  if ! redis_auth INFO Persistence | grep -q "rdb_last_bgsave_status:ok"; then
      log_message "❌ Redis BGSAVE failed or status is not 'ok'. Aborting."
      exit 1
  fi

  # Backup RDB
  if [ -f "/data/dump.rdb" ]; then
      TARGET_RDB="${BACKUP_DIR}/dump_${ENVIRONMENT:-unknown}_${TIMESTAMP}.rdb.gz"
      cp /data/dump.rdb "${TARGET_RDB%.gz}"
      gzip -f "${TARGET_RDB%.gz}"
      # Generate Checksum (Relative Path)
      (cd "$BACKUP_DIR" && md5sum "$(basename "$TARGET_RDB")" > "$(basename "$TARGET_RDB").md5")
      log_message "✅ RDB backup verified: $(basename "$TARGET_RDB")"
  fi

  # Backup AOF
  if [ -f "/data/appendonly.aof" ]; then
      TARGET_AOF="${BACKUP_DIR}/aof_${ENVIRONMENT:-unknown}_${TIMESTAMP}.aof.gz"
      cp /data/appendonly.aof "${TARGET_AOF%.gz}"
      gzip -f "${TARGET_AOF%.gz}"
      # Generate Checksum (Relative Path)
      (cd "$BACKUP_DIR" && md5sum "$(basename "$TARGET_AOF")" > "$(basename "$TARGET_AOF").md5")
      log_message "✅ AOF backup verified: $(basename "$TARGET_AOF")"
  fi
) 200>"$LOCK_FILE"
EOF
    chmod +x /scripts/backup-redis.sh
}

# Cleanup script for Redis
create_cleanup_script() {
    cat > /scripts/cleanup-backups.sh << 'EOF'
#!/bin/bash
set -euo pipefail

BACKUP_DIR="${BACKUP_DIR:-/backups/redis}"
ARCHIVE_DIR="${ARCHIVE_DIR:-/backups/archive}"
LOG_FILE="/var/log/cron/cleanup.log"

log_message() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [CLEANUP] $1" >> "$LOG_FILE"
}

if [ -z "$BACKUP_DIR" ] || [ -z "$ARCHIVE_DIR" ]; then
    log_message "❌ ABORT: Cleanup path resolution failed."
    exit 1
fi

# Keep last 120 RDB backups
RDB_TOTAL=$(find "$BACKUP_DIR" -name "dump_*.gz" 2>/dev/null | wc -l || echo 0)
if [ "$RDB_TOTAL" -gt 120 ]; then
    EXCESS=$(($RDB_TOTAL - 120))
    find "$BACKUP_DIR" -name "dump_*.gz" | sort | head -n "$EXCESS" | while read f; do
        [ -f "$f" ] && mv "$f" "$ARCHIVE_DIR/"
    done
    log_message "Archived $EXCESS old RDB backups"
fi

# Keep last 120 AOF backups
AOF_TOTAL=$(find "$BACKUP_DIR" -name "aof_*.gz" 2>/dev/null | wc -l || echo 0)
if [ "$AOF_TOTAL" -gt 120 ]; then
    EXCESS=$(($AOF_TOTAL - 120))
    find "$BACKUP_DIR" -name "aof_*.gz" | sort | head -n "$EXCESS" | while read f; do
        [ -f "$f" ] && mv "$f" "$ARCHIVE_DIR/"
    done
    log_message "Archived $EXCESS old AOF backups"
fi

# Delete from archive based on environment
if [ "${ENVIRONMENT:-}" = "prod" ] || [ "${ENVIRONMENT:-}" = "prod_fares" ]; then
    find "$ARCHIVE_DIR" -name "*.gz" -mtime +7 -delete 2>/dev/null || true
else
    find "$ARCHIVE_DIR" -name "*.gz" -mmin +1440 -delete 2>/dev/null || true
fi
EOF
    chmod +x /scripts/cleanup-backups.sh
}

# Health check script
create_health_check() {
    cat > /scripts/health-check.sh << 'EOF'
#!/bin/bash
set -euo pipefail
LOG_FILE="/var/log/cron/health.log"

log_message() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [HEALTH] $1" >> "$LOG_FILE"
}

redis_auth() {
    if [ -n "${REDIS_PASSWORD:-}" ]; then
        redis-cli -a "$REDIS_PASSWORD" "$@" 2>/dev/null
    else
        redis-cli "$@"
    fi
}

if redis_auth ping | grep -q "PONG"; then
    log_message "✅ Redis is healthy"
else
    log_message "❌ Redis health check failed"
fi
EOF
    chmod +x /scripts/health-check.sh
}

# Main execution
create_backup_script
create_cleanup_script
create_health_check
# setup_cron  <-- Disabled to allow backup-manager to orchestrate

echo "✅ Redis entrypoint setup complete"

# Start Redis
exec redis-server /etc/redis/redis.conf
