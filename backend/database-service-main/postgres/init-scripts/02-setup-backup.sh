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
