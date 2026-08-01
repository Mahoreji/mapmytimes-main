#!/bin/bash
# =======================================================================
# validate-backup.sh — MapMyTour Automated Backup Integrity Check
# =======================================================================
set -euo pipefail

LOG_FILE="/app/logs/backup-manager.log"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

log_message() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [VALIDATE] $1" | tee -a "$LOG_FILE"
}

validate_file() {
    local file=$1
    local name=$(basename "$file")

    if [ ! -f "$file" ]; then
        log_message "❌ CRITICAL: File not found: $file"
        return 1
    fi

    # 1. Gzip Integrity Check
    if ! gzip -t "$file" 2>/dev/null; then
        log_message "❌ CRITICAL: Backup file is CORRUPT (Gzip check failed): $name"
        return 1
    fi

    # 2. MD5 Checksum Validation (if .md5 file exists)
    if [ -f "$file.md5" ]; then
        log_message "🔍 Validating MD5 checksum for $name..."
        if md5sum -c "$file.md5" &>/dev/null; then
            log_message "✅ MD5 Match for $name"
        else
            log_message "❌ CRITICAL: MD5 MISMATCH for $name"
            return 1
        fi
    else
        log_message "⚠️ WARNING: MD5 checksum file missing for $name"
    fi

    return 0
}

log_message "Starting backup validation..."

# Validate Production PostgreSQL Backup
LATEST_PG=$(find /backups/prod/postgres -name "full_backup_*.gz" | sort | tail -1)
if [ -n "$LATEST_PG" ]; then
    if validate_file "$LATEST_PG"; then
        log_message "✅ PostgreSQL production backup is valid: $(basename $LATEST_PG)"
    else
        log_message "❌ PostgreSQL production backup VALIDATION FAILED!"
        echo "BACKUP_FAILED_PG,$TIMESTAMP,$(basename $LATEST_PG)" >> /backups/validation-failures.csv
    fi
else
    log_message "⚠️ No PostgreSQL production backup found to validate."
fi

# Validate Production Redis Backup
LATEST_REDIS=$(find /backups/prod/redis -name "dump_*.gz" | sort | tail -1)
if [ -n "$LATEST_REDIS" ]; then
    if validate_file "$LATEST_REDIS"; then
        log_message "✅ Redis production backup is valid: $(basename $LATEST_REDIS)"
    else
        log_message "❌ Redis production backup VALIDATION FAILED!"
        echo "BACKUP_FAILED_REDIS,$TIMESTAMP,$(basename $LATEST_REDIS)" >> /backups/validation-failures.csv
    fi
else
    log_message "⚠️ No Redis production backup found to validate."
fi

log_message "Backup validation workflow complete"
