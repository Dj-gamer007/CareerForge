#!/usr/bin/env bash
# =========================================================================
# CareerForge Nightly Database Backup Script
# Usage: ./scripts/backup-db.sh [/optional/backup/directory]
# =========================================================================
set -euo pipefail

BACKUP_DIR="${1:-/var/backups/careerforge}"
TIMESTAMP="$(date +"%Y%m%d_%H%M%S")"
CONTAINER_NAME="careerforge-mysql"
DB_NAME="${DB_NAME:-careerforge_prod}"

mkdir -p "${BACKUP_DIR}"

echo "[INFO] Starting backup of database '${DB_NAME}' at $(date)..."

# Verify MySQL container is running
if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    echo "[ERROR] Container '${CONTAINER_NAME}' is not running! Aborting backup." >&2
    exit 1
fi

BACKUP_FILE="${BACKUP_DIR}/careerforge_backup_${TIMESTAMP}.sql.gz"

# Execute mysqldump inside container and stream compressed output
docker exec "${CONTAINER_NAME}" sh -c 'exec mysqldump --single-transaction --quick --lock-tables=false -u root -p"$MYSQL_ROOT_PASSWORD" '"${DB_NAME}" | gzip > "${BACKUP_FILE}"

# Set secure file permissions
chmod 600 "${BACKUP_FILE}"

echo "[INFO] Backup successfully created at: ${BACKUP_FILE} ($(du -h "${BACKUP_FILE}" | cut -f1))"

# Prune backups older than 7 days
echo "[INFO] Cleaning up backups older than 7 days..."
find "${BACKUP_DIR}" -type f -name "careerforge_backup_*.sql.gz" -mtime +7 -delete

echo "[INFO] Backup maintenance completed successfully at $(date)."
