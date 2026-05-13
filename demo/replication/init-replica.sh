#!/bin/bash
set -e

MASTER_HOST="marketplace-db-master"
MASTER_PORT=3306
MASTER_USER="replicator"
MASTER_PASS="replicatorpass"
ROOT_PASS="rootpass"

echo "[replica-init] Waiting for master to be ready..."
until mysqladmin ping -h "$MASTER_HOST" -P "$MASTER_PORT" --silent 2>/dev/null; do
  sleep 2
done
echo "[replica-init] Master is up."

# Get current master binary log position
MASTER_STATUS=$(mysql -h "$MASTER_HOST" -P "$MASTER_PORT" \
  -u root -p"$ROOT_PASS" \
  -e "SHOW MASTER STATUS\G" 2>/dev/null)

BINLOG_FILE=$(echo "$MASTER_STATUS" | grep "File:" | awk '{print $2}')
BINLOG_POS=$(echo  "$MASTER_STATUS" | grep "Position:" | awk '{print $2}')

echo "[replica-init] Master binlog: $BINLOG_FILE @ $BINLOG_POS"

# Configure this server as a replica of the master
mysql -u root -p"$ROOT_PASS" <<-EOSQL
    STOP SLAVE;

    CHANGE MASTER TO
        MASTER_HOST     = '$MASTER_HOST',
        MASTER_PORT     = $MASTER_PORT,
        MASTER_USER     = '$MASTER_USER',
        MASTER_PASSWORD = '$MASTER_PASS',
        MASTER_LOG_FILE = '$BINLOG_FILE',
        MASTER_LOG_POS  =  $BINLOG_POS;

    START SLAVE;
EOSQL

echo "[replica-init] Replication started."

# Quick health check 
sleep 3
mysql -u root -p"$ROOT_PASS" -e "SHOW SLAVE STATUS\G" 2>/dev/null | \
  grep -E "Slave_IO_Running|Slave_SQL_Running|Seconds_Behind_Master|Last_Error"