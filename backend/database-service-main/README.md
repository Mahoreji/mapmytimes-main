# MapMyTour Database Service — High Availability & Zero Data Loss

This service provides a production-grade PostgreSQL (PostGIS) and Redis environment with high-frequency backups and guaranteed data persistence.

## 🚀 Quick Start (First Time Only)

```bash
# Step 1: Create all required host directories
./setup.sh

# Step 2: Start all services
docker-compose up -d

# Step 3: Verify everything is running
./check-status.sh
```

## 🛠️ Developer Workflow

```bash
# After any code change — rebuild images without touching data:
docker-compose up -d --build

# Restart a single service:
docker-compose restart postgres-prod

# View logs:
docker-compose logs -f postgres-prod

# Check backup status (runs every minute automatically):
./check-status.sh
```

## 🛡️ Data Safety Guide

| ✅ SAFE Commands (Data is kept) | ❌ DANGEROUS Commands (Data LOSS) |
|---|---|
| `docker-compose up -d` | `docker-compose down -v` |
| `docker-compose up -d --build` | `docker volume prune` |
| `docker-compose down` | `docker system prune --volumes` |
| `docker-compose restart` | `rm -rf ./data/` |
| Server reboot | |

### Where Your Data Lives
All database data is stored in real folders on the host machine, not inside Docker volumes. This means your data survives even if Docker is uninstalled.

```
./data/
├── postgres_dev/    ← PostgreSQL dev data (real files)
├── postgres_prod/   ← PostgreSQL prod data (real files)
├── redis_dev/       ← Redis dev data
└── redis_prod/      ← Redis prod data
```

## ⏱️ Backup System (Every-Minute)

Backups run **every minute** automatically for all environments. The system maintains a 2-hour window of high-frequency backups in "hot storage" and archives the rest.

```
./backups/
├── dev/
│   ├── postgres/    ← Last 120 minute-backups (2 hours)
│   ├── redis/       ← Last 120 minute-backups (2 hours)
│   └── archive/     ← Older backups (deleted after 24h for dev)
└── prod/
    ├── postgres/    ← Last 120 minute-backups (2 hours)
    ├── redis/       ← Last 120 minute-backups (2 hours)
    └── archive/     ← Older backups (deleted after 7 days for prod)
```

### Restore from a Minute-Backup

```bash
# List available backups
ls -lt ./backups/prod/postgres/ | head -20

# Restore latest
LATEST=$(ls -t ./backups/prod/postgres/full_backup_prod_*.gz | head -1)
./restore-from-backup.sh prod $LATEST
```

## 🏢 Enterprise Scaling (OTA-Ready)

For high-traffic production environments, this service includes enterprise-grade scaling and monitoring.

### Connection Pooling (PgBouncer)
To handle thousands of concurrent application connections without overloading PostgreSQL, use the PgBouncer endpoint:
- **Port**: `6432`
- **Host**: `pgbouncer` (internal) or `localhost` (external)
- **Benefits**: Reduced memory usage, faster connection times, and managed connection limits.

### Monitoring Exporters
Real-time metrics are exported for Prometheus/Grafana:
- **PostgreSQL Metrics**: `localhost:9187/metrics`
- **Redis Metrics**: `localhost:9121/metrics`

### Disaster Recovery (Offsite Sync)
Automated hourly sync to cloud storage is configured via `rclone`.
1. Enter the `backup-manager` container: `docker exec -it mapmytour_backup_manager bash`
2. Configure your cloud provider: `rclone config`
3. Archives will sync to the cloud every hour automatically.

## 🔐 Enterprise Security & Auditing

### Audit Logging (pgAudit)
Production PostgreSQL has **pgAudit** enabled. All database actions are logged to the container logs for compliance:
- View audit logs: `docker-compose logs -f postgres-prod | grep AUDIT`

### Resource Isolation
Each service is capped with CPU and Memory limits to prevent a single component from crashing the entire host (e.g., memory leaks or runaway queries).

### SSL/TLS Hardening
For production endpoints, it is highly recommended to:
1. Place certificates in the `./ssl/` directory (ignored by git).
2. Update `.env` to point to these certificates.
3. Configure `pgbouncer` or `postgres` to require SSL for all external connections.

## 🧹 Cleanup
To safely stop services and optionally clean up images/backups, use the provided tool:
```bash
./cleanup.sh
```
Deleting the actual database data (`./data/`) requires explicit confirmation by typing `DELETE`.
