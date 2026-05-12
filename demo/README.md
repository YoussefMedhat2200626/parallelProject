# Distributed Online Marketplace

A full-stack distributed marketplace system built with Java 21, Spring Boot 3.3.5, MariaDB 11.4, and Thymeleaf — for CSE352s Parallel and Distributed Systems (Spring 2026).

## Prerequisites

- Java 21
- Maven 3.9+
- Docker Desktop (for MariaDB database)

## Quick Start

> **Important:** Always run commands from the `demo/` directory (where `pom.xml` is).

```powershell
# 1. Navigate to the project directory (quotes needed because of space in path)
cd "D:\2026 parallel\parallelProject\demo"

# 2. Start the MariaDB database container
docker compose up -d

# 3. Initialize the database (first time only)
Get-Content "src\main\resources\schema.sql" | docker exec -i marketplace-db mariadb -u marketplace_user -pmarketplace_pass marketplace
Get-Content "src\main\resources\data.sql" | docker exec -i marketplace-db mariadb -u marketplace_user -pmarketplace_pass marketplace

# 4. Start the application
mvn spring-boot:run
```

**App URL:** http://localhost:8080

## Common Commands

```powershell
# Build the project
mvn clean package -DskipTests

# Run the app
mvn spring-boot:run

# Run via JAR
java -jar target/distributed-marketplace-1.0.0.jar

# Run tests
mvn clean test
```

## Restarting the Server

```powershell
# 1. Press Ctrl+C in the terminal to stop the server

# 2. If port 8080 is still in use, find and kill the process:
netstat -ano | findstr ":8080"
taskkill /F /PID <PID_NUMBER>

# 3. Restart
mvn spring-boot:run
```

## Managing the Database

```powershell
# Start MariaDB container
docker compose up -d

# Stop MariaDB container
docker compose down

# Check if MariaDB is running
docker ps

# Connect to MariaDB shell
docker exec -it marketplace-db mariadb -u marketplace_user -pmarketplace_pass marketplace

# Verify tables exist
docker exec marketplace-db mariadb -u marketplace_user -pmarketplace_pass -e "SHOW TABLES" marketplace
```

## Project Structure

```
demo/
├── docker-compose.yml              # MariaDB 11.4 container
├── pom.xml                          # Maven build config
├── src/main/java/com/marketplace/
│   ├── MarketplaceApplication.java  # Entry point
│   ├── config/                      # SOAP web service config
│   ├── controller/                  # Thymeleaf web controllers
│   │   ├── AuthController.java      #   Login, Register, Logout
│   │   ├── DashboardController.java #   Dashboard overview
│   │   ├── ItemController.java      #   Item CRUD + CSV import
│   │   ├── MarketplaceController.java # Search + Purchase
│   │   ├── WalletController.java    #   Deposits
│   │   └── ReportController.java    #   Transaction reports
│   ├── rest/                        # REST API endpoints
│   │   ├── ItemRestController.java  #   GET /api/v1/items/search
│   │   ├── AccountRestController.java # GET /api/v1/accounts/{id}
│   │   └── InventoryRestController.java # GET/PUT /api/v1/inventory/{id}
│   ├── soap/                        # SOAP web service endpoint
│   ├── service/                     # Business logic layer
│   ├── repository/                  # Spring Data JPA repositories
│   └── entity/                      # JPA entity classes
├── src/main/resources/
│   ├── application.yml              # Spring Boot config
│   ├── schema.sql                   # DDL with partitioning
│   ├── data.sql                     # Seed data (3 users, 8 items)
│   ├── marketplace.xsd             # SOAP schema
│   ├── templates/                   # Thymeleaf HTML pages
│   └── static/css/style.css         # Dark theme stylesheet
```

## Features

### Core (10 Required)
1. Create account (with 2FA OTP verification)
2. Login / Logout
3. Add / Edit / Remove items
4. Deposit cash into wallet
5. Search items by name, brand, category
6. Purchase items (fund transfer + inventory decrement)
7. Dashboard with balance, purchases, sales
8. Inventory management
9. Transaction reports with date filtering
10. REST + SOAP web services

### REST APIs (3)
| Endpoint | Description |
|----------|-------------|
| `GET /api/v1/items/search?q=` | Search items |
| `GET /api/v1/accounts/{userId}` | Account info |
| `GET/PUT /api/v1/inventory/{itemId}` | Inventory management |

### SOAP Services (3)
| Operation | Description |
|-----------|-------------|
| `getTransactionReport` | Reports by date range |
| `purchaseItem` | Purchase with 2FA |
| `getUserInfo` | User account info |

WSDL: http://localhost:8080/ws/marketplace.wsdl

### Bonus Features (3)
- **2FA** — OTP codes for registration and purchases
- **CSV Import** — Bulk product upload via CSV file
- **External Store Interface** — API key-based store integration

### Distributed Database
All tables use MariaDB **native partitioning**:
- `users`, `wallets`, `otp_codes`, `deposit_ledger` → HASH by user_id (4 partitions)
- `items` → HASH by seller_id (4 partitions)
- `inventory` → HASH by item_id (4 partitions)
- `transactions` → RANGE by month (monthly partitions)

## Troubleshooting

| Problem | Solution |
|---------|----------|
| `no POM in this directory` | Run from `demo/` folder: `cd "D:\2026 parallel\parallelProject\demo"` |
| `Port 8080 already in use` | Kill old process: `netstat -ano \| findstr ":8080"` then `taskkill /F /PID <PID>` |
| `Table doesn't exist` | Run the schema.sql init command (see Quick Start step 3) |
| `Docker container conflict` | `docker rm -f marketplace-db` then `docker compose up -d` |
| `Cannot connect to database` | Make sure Docker Desktop is running and MariaDB container is up |
