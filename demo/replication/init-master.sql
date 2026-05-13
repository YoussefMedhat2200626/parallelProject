-- Create a dedicated replication user (principle of least privilege)
CREATE USER IF NOT EXISTS 'replicator'@'%' IDENTIFIED BY 'replicatorpass';

-- Grant ONLY the replication slave privilege — nothing else
GRANT REPLICATION SLAVE ON *.* TO 'replicator'@'%';

FLUSH PRIVILEGES;
