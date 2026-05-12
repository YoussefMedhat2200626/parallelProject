# Parallel Distributed System (MVP)

An end-to-end Java 21 Maven implementation of a distributed-system MVP with API-gateway orchestration, wallet + ledger transaction processing, marketplace trade execution, 2FA validation, and resilience patterns.

## Prerequisites

- Java 21
- Maven 3.9+
- Docker (optional, for container build/run)

## Implemented Components

- API Gateway: routes and orchestrates wallet + marketplace flows
- Wallet Subsystem: thread-safe account balances and transfers
- Transaction Ledger: append-only ledger entries for auditability
- Marketplace Service: trade execution with retry handling
- Security (2FA): OTP generation/validation with expiry and single-use semantics
- Fault Tolerance: retry executor and circuit breaker for unstable dependencies

## Code Layout

- `src/main/java/com/example/Main.java`: parallel simulation runner
- `src/main/java/com/example/domain/`: gateway/trade/ledger domain models
- `src/main/java/com/example/service/`: gateway, wallet, ledger, marketplace, 2FA services
- `src/main/java/com/example/resilience/`: retry and circuit-breaker implementations
- `src/main/resources/logback.xml`: logging configuration
- `src/test/java/com/example/`: integration and concurrency tests
- `pom.xml`: Java 21 build + dependencies/plugins
- `Dockerfile`: multi-stage container build

## Run Locally

```bash
mvn clean package
java -jar target/demo-1.0-SNAPSHOT.jar
```

The application will execute parallel marketplace trades, validate OTPs, and print final wallet balances plus ledger totals.

## Run Tests

```bash
mvn clean test
```

Current test coverage includes:

- Gateway integration (success + invalid OTP path)
- OTP lifecycle (single-use + expiry)
- Wallet concurrency under parallel transfers
- Main simulation smoke test

## Build Docker Image

```bash
docker build -t demo-cli:local .
```

## Run Docker Container

```bash
docker run --rm demo-cli:local
```
