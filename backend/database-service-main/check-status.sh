#!/bin/bash

# Color codes
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo ""
echo -e "${BLUE}🔍 MapMyTour Database Service Status Check${NC}"
echo "=============================================="
echo ""

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
    printf "  %-30s " "$container:"
    get_container_status "$container"
done

echo ""
echo "⏱️  Minute Backup Health:"
echo "-------------------------"
for env in dev prod; do
    PG_COUNT=$(find ./backups/$env/postgres -name "*.gz" 2>/dev/null | wc -l)
    REDIS_COUNT=$(find ./backups/$env/redis -name "*.gz" 2>/dev/null | wc -l)
    LATEST_PG=$(find ./backups/$env/postgres -name "*.gz" 2>/dev/null | sort | tail -1 | xargs basename 2>/dev/null || echo "none")
    LATEST_REDIS=$(find ./backups/$env/redis -name "*.gz" 2>/dev/null | sort | tail -1 | xargs basename 2>/dev/null || echo "none")
    
    # Create reference file for age check (1 minute ago)
    # macOS compatible date command
    if [[ "$OSTYPE" == "darwin"* ]]; then
        touch -t $(date -v-1M +%Y%m%d%H%M) /tmp/1min_ago 2>/dev/null || true
    else
        touch -t $(date -d '1 minute ago' +%Y%m%d%H%M) /tmp/1min_ago 2>/dev/null || true
    fi
    
    PG_FRESH=$(find ./backups/$env/postgres -name "*.gz" -newer /tmp/1min_ago 2>/dev/null | wc -l)
    REDIS_FRESH=$(find ./backups/$env/redis -name "*.gz" -newer /tmp/1min_ago 2>/dev/null | wc -l)

    echo -e "  ${BLUE}$env environment:${NC}"
    echo -n "    PostgreSQL: $PG_COUNT backups stored | Latest: $LATEST_PG"
    if [ "$PG_FRESH" -gt 0 ]; then echo -e " ${GREEN}(FRESH)${NC}"; else echo -e " ${RED}(STALE)${NC}"; fi
    
    echo -n "    Redis:      $REDIS_COUNT backups stored | Latest: $LATEST_REDIS"
    if [ "$REDIS_FRESH" -gt 0 ]; then echo -e " ${GREEN}(FRESH)${NC}"; else echo -e " ${RED}(STALE)${NC}"; fi
done

echo ""
echo "💾 Data Directory Status (your actual database files):"
echo "-------------------------------------------------------"
for dir in data/postgres_dev data/postgres_prod data/redis_dev data/redis_prod; do
    if [ -d "./$dir" ]; then
        SIZE=$(du -sh ./$dir 2>/dev/null | cut -f1)
        echo -e "  ${GREEN}✅${NC} ./$dir — $SIZE"
    else
        echo -e "  ${RED}❌${NC} ./$dir — MISSING (run ./setup.sh)"
    fi
done

echo ""
echo "📊 Storage Usage:"
echo "-----------------"
if [ -d "./backups" ]; then
    echo "  • Total backup size: $(du -sh ./backups 2>/dev/null | cut -f1)"
    echo "  • Available space:   $(df -h . | awk 'NR==2 {print $4}')"
fi

echo ""
echo "🔧 Quick Actions:"
echo "-----------------"
echo "  • Start services:     docker-compose up -d"
echo "  • Rebuild & Start:    docker-compose up -d --build"
echo "  • Stop services:      docker-compose down"
echo "  • View logs:          docker-compose logs -f [service]"
echo "  • Cleanup tool:       ./cleanup.sh"

echo ""
echo -e "${GREEN}✅ Status check completed!${NC}"
echo "=============================================="
