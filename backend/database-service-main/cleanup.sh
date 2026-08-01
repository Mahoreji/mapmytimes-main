#!/bin/bash

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo ""
echo -e "${BLUE}🧹 MapMyTour Database Service Cleanup Tool${NC}"
echo "==============================================="
echo ""
echo -e "${YELLOW}⚠️  This tool stops containers and optionally removes images and backup files.${NC}"
echo -e "${RED}⛔  It will NEVER delete ./data/ — your actual database files are always safe.${NC}"
echo ""

read -p "Stop and remove containers? (y/N): " -n 1 -r STOP_CONTAINERS
echo
if [[ $STOP_CONTAINERS =~ ^[Yy]$ ]]; then
    echo -e "${BLUE}🛑 Stopping containers...${NC}"
    docker-compose down --remove-orphans
    echo -e "${GREEN}✅ Containers stopped${NC}"
fi

echo ""
read -p "Remove Docker images? (y/N): " -n 1 -r REMOVE_IMAGES
echo
if [[ $REMOVE_IMAGES =~ ^[Yy]$ ]]; then
    echo -e "${BLUE}🗑️  Removing images...${NC}"
    # Remove images related to mapmytour or database-service
    docker rmi $(docker images -q --filter "reference=*mapmytour*") 2>/dev/null || echo "No matching images"
    echo -e "${GREEN}✅ Images removed${NC}"
fi

echo ""
read -p "Remove backup files in ./backups/? (y/N): " -n 1 -r REMOVE_BACKUPS
echo
if [[ $REMOVE_BACKUPS =~ ^[Yy]$ ]]; then
    echo -e "${BLUE}🗑️  Removing backup files...${NC}"
    # Keep the directory structure but remove files
    rm -rf ./backups/dev/postgres/* ./backups/dev/redis/* ./backups/prod/postgres/* ./backups/prod/redis/*
    echo -e "${GREEN}✅ Backup files removed (archive and reports kept)${NC}"
fi

echo ""
echo -e "${RED}════════════════════════════════════════════════════════${NC}"
echo -e "${RED}  ⛔  DATABASE DATA PROTECTION ZONE  ⛔${NC}"
echo -e "${RED}════════════════════════════════════════════════════════${NC}"
echo ""
echo "  The following directories contain your ACTUAL database files:"
echo "  • ./data/postgres_dev/"
echo "  • ./data/postgres_prod/"
echo "  • ./data/redis_dev/"
echo "  • ./data/redis_prod/"
echo ""
echo -e "${RED}  Deleting these means PERMANENT LOSS of all database data.${NC}"
echo -e "${RED}  There is NO undo. There is NO recovery unless you have backups.${NC}"
echo ""
echo "  To delete database data, type DELETE in capitals and press Enter."
echo "  To keep data safe, just press Enter."
echo ""
read -p "  Type DELETE to erase all database data, or Enter to skip: " CONFIRM_NUKE

if [ "$CONFIRM_NUKE" = "DELETE" ]; then
    echo ""
    echo -e "${RED}  Last chance. Type YES I AM SURE to confirm deletion:${NC}"
    read -p "  " FINAL_CONFIRM
    if [ "$FINAL_CONFIRM" = "YES I AM SURE" ]; then
        echo -e "${RED}🗑️  Deleting database data...${NC}"
        rm -rf ./data/postgres_dev/*
        rm -rf ./data/postgres_prod/*
        rm -rf ./data/redis_dev/*
        rm -rf ./data/redis_prod/*
        echo -e "${RED}💀 All database data deleted. Run ./setup.sh then docker-compose up -d to start fresh.${NC}"
    else
        echo -e "${GREEN}✅ Smart choice. Database data kept safe.${NC}"
    fi
else
    echo -e "${GREEN}✅ Database data is safe.${NC}"
fi

echo ""
echo -e "${BLUE}🧹 Running Docker cleanup (images and build cache only)...${NC}"
docker system prune -f

echo ""
echo -e "${GREEN}✅ Cleanup complete.${NC}"
echo ""
echo "Summary:"
echo "  Containers:     $([ "$STOP_CONTAINERS" = "y" ] || [ "$STOP_CONTAINERS" = "Y" ] && echo "Stopped" || echo "Untouched")"
echo "  Images:         $([ "$REMOVE_IMAGES" = "y" ] || [ "$REMOVE_IMAGES" = "Y" ] && echo "Removed" || echo "Kept")"
echo "  Backup files:   $([ "$REMOVE_BACKUPS" = "y" ] || [ "$REMOVE_BACKUPS" = "Y" ] && echo "Cleared" || echo "Kept")"
echo "  Database data:  $([ "$CONFIRM_NUKE" = "DELETE" ] && [ "$FINAL_CONFIRM" = "YES I AM SURE" ] && echo "DELETED" || echo "✅ SAFE")"
echo ""
echo "To restart: docker-compose up -d"
