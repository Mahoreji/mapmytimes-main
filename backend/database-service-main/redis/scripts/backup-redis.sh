#!/bin/bash
set -euo pipefail

# =======================================================================
# Redis Backup Script - MapMyTour Platform
# =======================================================================

ENVIRONMENT=${ENVIRONMENT:-prod}
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_DIR="/backups/$ENVIRONMENT/redis"
mkdir -p "$BACKUP_DIR"

LOCKFILE="/tmp/redis_backup.lock"

# Use flock to prevent concurrent backups
(
    if ! flock -n 9; then
        echo "[$(date '+%Y-%m-%d %H:%M:%S')] ❌ Backup already in progress. Exiting."
        exit 1
    fi

    echo "[$(date '+%Y-%m-%d %H:%M:%S')] 🗄️ Starting Redis backup for $ENVIRONMENT..."

    # Use BGSAVE instead of SAVE to avoid blocking Redis performance
    if [ ! -z "${REDIS_PASSWORD:-}" ]; then
        redis-cli -a "$REDIS_PASSWORD" BGSAVE
    else
        redis-cli BGSAVE
    fi

    # Wait for BGSAVE to complete (poll INFO Persistence)
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] ⏳ Waiting for BGSAVE to complete..."
    while true; do
        if [ ! -z "${REDIS_PASSWORD:-}" ]; then
            STATUS=$(redis-cli -a "$REDIS_PASSWORD" INFO Persistence | grep "rdb_bgsave_in_progress" | cut -d: -f2 | tr -d '\r')
        else
            STATUS=$(redis-cli INFO Persistence | grep "rdb_bgsave_in_progress" | cut -d: -f2 | tr -d '\r')
        fi
        
        if [ "$STATUS" == "0" ]; then
            break
        fi
        sleep 2
    done

    # Copy and compress the dump file
    if [ -f /data/dump.rdb ]; then
        BACKUP_FILE="$BACKUP_DIR/dump_$TIMESTAMP.rdb.gz"
        cp /data/dump.rdb "$BACKUP_DIR/dump_$TIMESTAMP.rdb"
        gzip -f "$BACKUP_DIR/dump_$TIMESTAMP.rdb"
        
        echo "[$(date '+%Y-%m-%d %H:%M:%S')] ✅ Redis backup successful: $BACKUP_FILE"
        
        # Generate MD5 checksum
        echo "[$(date '+%Y-%m-%d %H:%M:%S')] 🔍 Generating MD5 checksum..."
        md5sum "$BACKUP_FILE" > "$BACKUP_FILE.md5"
        echo "[$(date '+%Y-%m-%d %H:%M:%S')] ✅ MD5 generated: $(cat $BACKUP_FILE.md5)"
    else
        echo "[$(date '+%Y-%m-%d %H:%M:%S')] ❌ dump.rdb NOT FOUND at /data/dump.rdb"
        exit 1
    fi

    # Local safety buffer
    find "$BACKUP_DIR" -name "*.gz" -mmin +60 -delete
    find "$BACKUP_DIR" -name "*.md5" -mmin +60 -delete

) 9>"$LOCKFILE"
