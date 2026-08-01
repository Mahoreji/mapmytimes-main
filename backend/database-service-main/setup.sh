#!/bin/bash
set -e

echo "🔧 MapMyTour Database Service — First Time Setup"
echo "================================================="
echo ""
echo "Creating host data directories (these hold your database data)..."

# Database data directories — NEVER delete these
mkdir -p data/postgres_dev
mkdir -p data/postgres_prod
mkdir -p data/redis_dev
mkdir -p data/redis_prod

# Backup directories
mkdir -p backups/dev/postgres
mkdir -p backups/dev/redis
mkdir -p backups/dev/archive
mkdir -p backups/prod/postgres
mkdir -p backups/prod/redis
mkdir -p backups/prod/archive
mkdir -p backups/reports
mkdir -p backups/wal

# SSL directory
mkdir -p ssl

# Log directory
mkdir -p logs

# Set permissions so PostgreSQL container (uid 999) can write to data dirs
# Note: On macOS, Docker Desktop handles file ownership differently, 
# but these permissions are good practice for Linux deployments.
chmod 700 data/postgres_dev data/postgres_prod
chmod 755 data/redis_dev data/redis_prod
chmod -R 755 backups/

echo ""
echo "✅ Directories created:"
echo "   ./data/postgres_dev   ← PostgreSQL dev data (NEVER DELETE)"
echo "   ./data/postgres_prod  ← PostgreSQL prod data (NEVER DELETE)"
echo "   ./data/redis_dev      ← Redis dev data (NEVER DELETE)"
echo "   ./data/redis_prod     ← Redis prod data (NEVER DELETE)"
echo "   ./backups/            ← All backup files"
echo ""
echo "Now run: docker-compose up -d"
echo ""
echo "⚠️  IMPORTANT: Add ./data/ to your .gitignore — it contains live database files"
