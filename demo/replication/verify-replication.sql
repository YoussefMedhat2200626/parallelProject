-- Replication & Partitioning Verification Queries

-- 1. CHECK REPLICATION STATUS (run on REPLICA — port 3307)
-- Connect:  mysql -h 127.0.0.1 -P 3307 -u root -prootpass marketplace
SHOW SLAVE STATUS\G
-- Look for:
--   Slave_IO_Running:  Yes         replica is connected to master
--   Slave_SQL_Running: Yes         replica is applying changes
--   Seconds_Behind_Master: 0      replica is fully caught up
--   Last_Error: (empty)           no errors

-- 2. VERIFY MASTER BINARY LOG IS ACTIVE (run on MASTER — port 3306)
SHOW MASTER STATUS;
-- Shows current binlog file and position being streamed to replica

SHOW BINARY LOGS;
-- Lists all binlog files on master


-- 3. LIVE REPLICATION TEST
-- Write on master  should appear on replica within milliseconds

-- Step A: On MASTER (port 3306) — insert a test user
INSERT INTO users (username, email, password_hash, full_name)
VALUES ('replication_test', 'reptest@example.com', 'testhash', 'Replication Test');

-- Step B: On REPLICA (port 3307) — should already be there
SELECT user_id, username, email, full_name
FROM users
WHERE username = 'replication_test';

-- Step C: Clean up on MASTER (will also replicate the delete)
DELETE FROM users WHERE username = 'replication_test';


-- 4. PARTITION DISTRIBUTION — verify rows spread across partitions
-- Users table: rows per partition
SELECT PARTITION_NAME, TABLE_ROWS
FROM information_schema.PARTITIONS
WHERE TABLE_SCHEMA = 'marketplace' AND TABLE_NAME = 'users'
ORDER BY PARTITION_NAME;

-- Items table: rows per partition
SELECT PARTITION_NAME, TABLE_ROWS
FROM information_schema.PARTITIONS
WHERE TABLE_SCHEMA = 'marketplace' AND TABLE_NAME = 'items'
ORDER BY PARTITION_NAME;

-- Transactions table: which monthly partitions have data
SELECT PARTITION_NAME, TABLE_ROWS
FROM information_schema.PARTITIONS
WHERE TABLE_SCHEMA = 'marketplace' AND TABLE_NAME = 'transactions'
  AND TABLE_ROWS > 0
ORDER BY PARTITION_NAME;

-- 5. PARTITION PRUNING — prove MariaDB only scans the right partition
-- EXPLAIN shows which partition is hit for a time-range query
EXPLAIN PARTITIONS
SELECT * FROM transactions
WHERE created_at BETWEEN '2026-05-01' AND '2026-05-31';
-- Should show: partitions = p202605 

-- 6. ALL PARTITION DETAILS FOR EVERY TABLE
SELECT
    TABLE_NAME,
    PARTITION_NAME,
    PARTITION_METHOD,
    PARTITION_EXPRESSION,
    TABLE_ROWS
FROM information_schema.PARTITIONS
WHERE TABLE_SCHEMA = 'marketplace'
  AND PARTITION_NAME IS NOT NULL
ORDER BY TABLE_NAME, PARTITION_NAME;
