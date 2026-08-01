# MapMyTour Database Disaster Recovery Runbook

## RTO / RPO Targets

| Scenario | RTO Target | RPO Target |
|---|---|---|
| Single container crash | 30 seconds (auto-restart) | 0 (data in ./data/) |
| Full server reboot | 3 minutes | 0 (data in ./data/) |
| Corrupt data in table | 30 minutes | 1 minute (minute backups) |
| Disk failure on server | 2 hours | 1 minute (S3 backups) |
| Full server loss | 4 hours | 1 minute (S3 backups) |

## Scenario 1: Container Crashed (auto-handled)

Docker restart policy `unless-stopped` handles this automatically.
Verify with: `docker-compose ps` — should show `Up` within 30 seconds.

## Scenario 2: Replication Lag / Replica Behind

```bash
# Check lag
docker exec mapmytour_postgres_prod psql -U admin_prod -c "
    SELECT now() - pg_last_xact_replay_timestamp() AS replication_lag;"

# If lag > 30 seconds, check replica logs
docker logs mapmytour_postgres_replica --tail=50

# Emergency: promote replica to primary if primary is gone
docker exec mapmytour_postgres_replica pg_ctl promote -D /var/lib/postgresql/data
```

## Scenario 3: Corrupt Data — Restore to Point in Time

```bash
# 1. Identify the last known good timestamp
ls -lt ./backups/prod/postgres/ | head -20

# 2. Stop the affected service (not the database)
docker-compose stop booking-service

# 3. Restore specific backup
./restore-from-backup.sh prod ./backups/prod/postgres/full_backup_prod_TIMESTAMP.sql.gz

# 4. Verify row counts
docker exec mapmytour_postgres_prod psql -U admin_prod -d mapmytour_prod -c "
    SELECT COUNT(*) FROM bookings;
    SELECT COUNT(*) FROM fare_holds WHERE status='active';"

# 5. Restart service
docker-compose start booking-service
```

## Scenario 4: Server Loss — Restore from S3

```bash
# 1. Provision new server, install Docker
# 2. Clone repo
git clone https://github.com/mapmytour/database-service.git
cd database-service
./setup.sh

# 3. Download latest backup from S3
aws s3 sync s3://${S3_BUCKET}/mapmytour-db/prod/$(date +%Y/%m/%d)/ ./backups/prod/postgres/
aws s3 sync s3://${S3_BUCKET}/mapmytour-db/prod/$(date +%Y/%m/%d)/redis/ ./backups/prod/redis/

# 4. Start services
docker-compose up -d

# 5. Wait for PostgreSQL to be ready
sleep 30

# 6. Restore latest backup
LATEST=$(ls -t ./backups/prod/postgres/full_backup_prod_*.gz | head -1)
./restore-from-backup.sh prod $LATEST

# 7. Verify and update DNS
```

## Emergency Contacts

| Role | Name | Contact |
|---|---|---|
| Lead Backend | Prakhar (CEO) | +91 88003 08446 |
| Database Admin | Animesh (Co-founder) | [add] |
| Hosting Support | [provider] | [add] |

## Post-Incident Checklist

- [ ] Identify root cause
- [ ] Verify all data integrity (row counts, recent bookings)
- [ ] Verify fare holds expired correctly (no orphaned holds)
- [ ] Verify Redis fare instance has no evictions
- [ ] Update incident log
- [ ] Implement preventive fix
