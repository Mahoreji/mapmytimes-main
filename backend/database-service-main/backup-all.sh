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
