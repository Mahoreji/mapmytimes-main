#!/bin/bash
set -e

echo "🐘 Starting PostgreSQL ${ENVIRONMENT} Environment..."

# Create directory structure
mkdir -p /backups/postgres
mkdir -p /backups/archive
mkdir -p /scripts
mkdir -p /var/log/cron
chmod -R 777 /backups /scripts /var/log/cron

# Setup cron jobs
setup_cron() {
    cat > /etc/cron.d/postgres-backup << 'EOF'
# PostgreSQL backup cron — MapMyTour ${ENVIRONMENT}
SHELL=/bin/bash
PATH=/usr/local/sbin:/usr/local/bin:/sbin:/bin:/usr/sbin:/usr/bin

# Backup every minute (disabled: backup-manager orchestrates)
# * * * * * root /scripts/backup-postgres.sh >> /var/log/cron/backup.log 2>&1

# Cleanup minute-backups every 5 minutes (keep last 120 = 2 hour window)
# */5 * * * * root /scripts/cleanup-backups.sh >> /var/log/cron/cleanup.log 2>&1

# Health check every minute
# * * * * * root /scripts/health-check.sh >> /var/log/cron/health.log 2>&1

EOF
    chmod 0644 /etc/cron.d/postgres-backup
}

# Create backup script
create_backup_script() {
    cat > /scripts/backup-postgres.sh << 'EOF'
#!/bin/bash
set -euo pipefail

# Configuration
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_DIR="${BACKUP_DIR:-/backups/postgres}"
LOG_FILE="/var/log/cron/backup.log"
LOCK_FILE="/tmp/postgres_backup.lock"
# ENVIRONMENT, POSTGRES_USER, POSTGRES_PASSWORD provided by container env

log_message() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [${ENVIRONMENT:-postgres}] $1"
}

# Validation
if [ -z "$BACKUP_DIR" ] || [ -z "$POSTGRES_USER" ] || [ -z "$POSTGRES_PASSWORD" ]; then
    log_message "❌ ABORT: Critical configuration variables are missing."
    exit 1
fi

mkdir -p "$BACKUP_DIR"

export PGPASSWORD="$POSTGRES_PASSWORD"

log_message "Initiating Postgres backup (Dir: $BACKUP_DIR)..."

# Atomic backup operation
(
  flock -n 200 || { log_message "⚠️ Another operation is already running. Skipping."; exit 0; }

  BACK_FILE="${BACKUP_DIR}/full_backup_${ENVIRONMENT:-unknown}_${TIMESTAMP}.sql"
  
  if pg_dumpall -U "$POSTGRES_USER" > "$BACK_FILE" 2>> "$LOG_FILE"; then
      if [ -s "$BACK_FILE" ]; then
          gzip -f "$BACK_FILE"
          # Generate Checksum (Relative Path)
          (cd "$BACKUP_DIR" && md5sum "$(basename "${BACK_FILE}.gz")" > "$(basename "${BACK_FILE}.gz").md5")
          log_message "✅ Backup verified: $(basename "${BACK_FILE}.gz")"
      else
          log_message "❌ ABORT: Backup file is empty."
          rm -f "$BACK_FILE"
          exit 1
      fi
  else
      log_message "❌ ABORT: pg_dumpall failed."
      rm -f "$BACK_FILE"
      exit 1
  fi
) 200>"$LOCK_FILE"
EOF
    chmod +x /scripts/backup-postgres.sh
}

# Cleanup script
create_cleanup_script() {
    cat > /scripts/cleanup-backups.sh << 'EOF'
#!/bin/bash
set -euo pipefail
BACKUP_DIR="${BACKUP_DIR:-/backups/postgres}"
LOG_FILE="/var/log/cron/cleanup.log"

log_message() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [CLEANUP] $1" >> "$LOG_FILE"
}

if [ -z "$BACKUP_DIR" ]; then
    log_message "❌ ABORT: Backup directory variable is empty."
    exit 1
fi

# Keep last 120 files
TOTAL=$(find "$BACKUP_DIR" -name "*.gz" 2>/dev/null | wc -l || echo 0)
if [ "$TOTAL" -gt 120 ]; then
    EXCESS=$((TOTAL - 120))
    find "$BACKUP_DIR" -name "*.gz" | sort | head -n "$EXCESS" | xargs rm -f
    log_message "Deleted $EXCESS old backups (keeping last 120)"
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

export PGPASSWORD="${POSTGRES_PASSWORD:-}"

if pg_isready -U "${POSTGRES_USER:-postgres}" -d "${POSTGRES_DB:-postgres}" >/dev/null 2>&1; then
    log_message "✅ PostgreSQL is healthy"
else
    log_message "❌ PostgreSQL health check failed"
fi
EOF
    chmod +x /scripts/health-check.sh
}

# Main execution
create_backup_script
create_cleanup_script
create_health_check
# setup_cron  <-- Disabled to allow backup-manager to orchestrate

echo "✅ PostgreSQL entrypoint setup complete"

# Cron is disabled here; backup-manager will trigger /scripts/backup-postgres.sh via docker exec
exec docker-entrypoint.sh postgres
