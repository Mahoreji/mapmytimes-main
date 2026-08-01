#!/bin/bash
set -e

# =======================================================================
# ENTERPRISE DISASTER RECOVERY — CLOUD SYNC
# =======================================================================
# This script syncs local archives to remote cloud storage (S3/GCS/Azure).
# Configuration: Configure 'rclone' first using 'rclone config'.
# =======================================================================

LOG_FILE="/app/logs/backup-manager.log"
REMOTE_NAME="cloud-storage" # As configured in rclone config
REMOTE_PATH="mapmytour-backups/database-service"

log_message() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [CLOUD-SYNC] $1" | tee -a "$LOG_FILE"
}

log_message "🚀 Starting offsite cloud sync..."

# Sync both environments
for env in dev prod; do
    SOURCE_DIR="/backups/$env/archive"
    DEST_PATH="$REMOTE_NAME:$REMOTE_PATH/$env/archive"

    if [ -d "$SOURCE_DIR" ]; then
        log_message "Syncing $env archives to $DEST_PATH..."
        
        # 'sync' ensures remote matches local (deletes old files on remote)
        # 'copy' only adds new files (safer for backups)
        if rclone copy "$SOURCE_DIR" "$DEST_PATH" --update --use-mmap --transfers 4; then
            log_message "✅ $env sync successful"
        else
            log_message "❌ $env sync failed"
            # In production, this should trigger an alert
        fi
    fi
done

log_message "📊 Cloud sync process completed"
