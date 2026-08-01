#!/bin/bash
set -euo pipefail

# =======================================================================
# PostgreSQL Backup Script - MapMyTour Platform
# =======================================================================

# Load environment variables if needed
POSTGRES_USER=${POSTGRES_USER:-admin_prod}
POSTGRES_DB=${POSTGRES_DB:-mapmytour_prod}
ENVIRONMENT=${ENVIRONMENT:-prod}

TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_DIR="/backups/$ENVIRONMENT/postgres"
mkdir -p "$BACKUP_DIR"

LOCKFILE="/tmp/postgres_backup.lock"

# Use flock to prevent concurrent backups
(
    if ! flock -n 9; then
        echo "[$(date '+%Y-%m-%d %H:%M:%S')] ❌ Backup already in progress. Exiting."
        exit 1
    fi

    echo "[$(date '+%Y-%m-%d %H:%M:%S')] 🗄️ Starting PostgreSQL backup for $ENVIRONMENT..."

    BACKUP_FILE="$BACKUP_DIR/full_backup_$TIMESTAMP.sql.gz"

    if pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -F c -Z 9 > "$BACKUP_FILE"; then
        echo "[$(date '+%Y-%m-%d %H:%M:%S')] ✅ Backup successful: $BACKUP_FILE"
        
        # Generate MD5 checksum
        echo "[$(date '+%Y-%m-%d %H:%M:%S')] 🔍 Generating MD5 checksum..."
        md5sum "$BACKUP_FILE" > "$BACKUP_FILE.md5"
        echo "[$(date '+%Y-%m-%d %H:%M:%S')] ✅ MD5 generated: $(cat $BACKUP_FILE.md5)"
    else
        echo "[$(date '+%Y-%m-%d %H:%M:%S')] ❌ Backup FAILED!"
        exit 1
    fi

    # Local safety buffer
    find "$BACKUP_DIR" -name "*.gz" -mmin +60 -delete
    find "$BACKUP_DIR" -name "*.md5" -mmin +60 -delete

) 9>"$LOCKFILE"
