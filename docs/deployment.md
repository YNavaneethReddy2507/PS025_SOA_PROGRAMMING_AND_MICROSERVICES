# PS025: Deployment & Containerization Manual

## 1. Overview & Architecture Topology

The **PS025 Real-Time Distributed Bidding & Auction Clearance Engine** is architectured as a containerized microservices ecosystem. It features decentralized persistence, autonomous service discovery, an intelligent API perimeter gateway, and distributed business services.

```
 +-----------------------------------------------------------------------------------------+
 |                                  Docker Bridge Network                                  |
 |                                    (auction-network)                                    |
 |                                                                                         |
 |    +-------------------+                                                                |
 |    |   Eureka Server   | <===========================================+                  |
 |    |    (Port 8761)    |                                             |                  |
 |    +---------+---------+                                             | (Service Reg)    |
 |              ^                                                       |                  |
 |              |                                                       |                  |
 |    +---------+---------+         +-------------------+         +-----+-------------+    |
 |    |    API Gateway    | ------> |   Auth Service    | ------> |     MySQL 8.0     |    |
 |    |    (Port 8080)    |         |    (Port 8081)    |         |    (Port 3306)    |    |
 |    +----+----+----+----+         +-------------------+         |                   |    |
 |         |    |    |                                            | - auth_db         |    |
 |         |    |    +------------> +-------------------+         | - auction_db      |    |
 |         |    |                   |  Auction Service  | ------> | - bidding_db      |    |
 |         |    |                   |    (Port 8082)    |         | - payment_db      |    |
 |         |    |                   +---------+---------+         |                   |    |
 |         |    |                             |                   +-------------------+    |
 |         |    |                             | (Feign)                     ^              |
 |         |    +-----------------+           v                             |              |
 |         |                      | +-------------------+                   |              |
 |         +----------------------+-|  Bidding Service  | ------------------+              |
 |                                | |    (Port 8083)    |                                  |
 |                                | +-------------------+                                  |
 |                                |           |                                            |
 |                                |           | (Feign)                                    |
 |                                v           v                                            |
 |                          +-------------------+                                          |
 |                          |  Payment Service  | --------------------------+              |
 |                          |    (Port 8084)    |                                          |
 |                          +-------------------+                                          |
 +-----------------------------------------------------------------------------------------+
```

---

## 2. Prerequisites

Before deploying the containerized ecosystem, ensure the following tools are installed:

| Tool | Minimum Version | Verification Command |
| :--- | :--- | :--- |
| **Java Development Kit (JDK)** | 17 (Supports up to JDK 25) | `java -version` |
| **Apache Maven** | 3.8.0+ | `mvn -v` |
| **Docker Engine** | 20.10.0+ | `docker --version` |
| **Docker Compose** | v2.0.0+ | `docker compose version` |

---

## 3. Database Isolation & Schema Ownership

Each service maintains its own isolated database schema. No service can directly query or modify another service's tables. Access is controlled via dedicated MySQL service users:

| Service | Schema | Database User | Permissions |
| :--- | :--- | :--- | :--- |
| **Auth Service** | `auth_db` | `auth_user` | `ALL PRIVILEGES ON auth_db.*` |
| **Auction Service** | `auction_db` | `auction_user` | `ALL PRIVILEGES ON auction_db.*` |
| **Bidding Service** | `bidding_db` | `bidding_user` | `ALL PRIVILEGES ON bidding_db.*` |
| **Payment Service** | `payment_db` | `payment_user` | `ALL PRIVILEGES ON payment_db.*` |
| **Administrative** | All Schemas | `root` | `ALL PRIVILEGES ON *.*` |

The schemas and user permissions are automatically provisioned upon initial container startup via [init-mysql.sql](file:///d:/SOA/init-mysql.sql).

---

## 4. Environment Variables Reference

Environment settings can be customized in `.env` (copy from [.env.example](file:///d:/SOA/.env.example)):

| Variable | Description | Default Value |
| :--- | :--- | :--- |
| `MYSQL_ROOT_PASSWORD` | Root administrative password for MySQL | `rootpassword` |
| `MYSQL_PORT` | Host exposed port for MySQL | `3306` |
| `AUTH_DB_USER` / `AUTH_DB_PASSWORD` | Database credentials for Auth Service | `auth_user` / `auth_pass` |
| `AUCTION_DB_USER` / `AUCTION_DB_PASSWORD` | Database credentials for Auction Service | `auction_user` / `auction_pass` |
| `BIDDING_DB_USER` / `BIDDING_DB_PASSWORD` | Database credentials for Bidding Service | `bidding_user` / `bidding_pass` |
| `PAYMENT_DB_USER` / `PAYMENT_DB_PASSWORD` | Database credentials for Payment Service | `payment_user` / `payment_pass` |
| `EUREKA_PORT` | Port for Eureka Service Registry | `8761` |
| `GATEWAY_PORT` | Port for Spring Cloud API Gateway | `8080` |
| `AUTH_PORT` | Port for Auth Service | `8081` |
| `AUCTION_PORT` | Port for Auction Service | `8082` |
| `BIDDING_PORT` | Port for Bidding Service | `8083` |
| `PAYMENT_PORT` | Port for Payment Service | `8084` |
| `JWT_SECRET` | 256-bit cryptographic HMAC key for JWT signing | `404E635266...5970` |
| `JWT_EXPIRATION_MS` | Token lifespan in milliseconds (24h) | `86400000` |
| `PLATFORM_FEE_PERCENTAGE` | Platform commission taken from seller payout | `0.05` (5%) |

---

## 5. Step-by-Step Deployment Guide

### Step 1: Clone the Repository
```bash
git clone https://github.com/YNavaneethReddy2507/PS025_SOA_PROGRAMMING_AND_MICROSERVICES.git
cd PS025_SOA_PROGRAMMING_AND_MICROSERVICES
```

### Step 2: Build Executable Microservice Artifacts
Compile and package all 6 microservice JAR files in a single pass:
```bash
mvn clean package -DskipTests
```
*This produces production Spring Boot runnable `.jar` files in each service's `target/` directory.*

### Step 3: Prepare Environment Variables
```bash
cp .env.example .env
```
*(Optionally modify passwords or port mappings in `.env`)*

### Step 4: Launch the Microservices Cluster
```bash
docker compose up --build -d
```

### Step 5: Startup Sequence & Health Verification
The containers launch with automated dependency health checks:
1. `auction-mysql` starts first and executes `init-mysql.sql`. Health status is monitored via `mysqladmin ping`.
2. `eureka-server` starts and reports healthy via `http://localhost:8761/actuator/health`.
3. `auth-service`, `auction-service`, `bidding-service`, and `payment-service` start once MySQL and Eureka are fully healthy.
4. `api-gateway` starts and routes incoming external traffic to all registered microservices.

---

## 6. Verification Sequence

Verify each service layer in order:

### 1. Service Discovery Dashboard (Eureka)
Open in browser:
```
http://localhost:8761
```
*Verify that the following 5 application instances are listed under "Instances currently registered with Eureka":*
- `API-GATEWAY`
- `AUTH-SERVICE`
- `AUCTION-SERVICE`
- `BIDDING-SERVICE`
- `PAYMENT-SERVICE`

### 2. API Gateway Actuator Health
```bash
curl http://localhost:8080/actuator/health
```
*Expected Response:* `{"status":"UP"}`

### 3. Auth Service Health
```bash
curl http://localhost:8081/actuator/health
```
*Expected Response:* `{"status":"UP"}`

### 4. Auction Service Health
```bash
curl http://localhost:8082/actuator/health
```
*Expected Response:* `{"status":"UP"}`

### 5. Bidding Service Health
```bash
curl http://localhost:8083/actuator/health
```
*Expected Response:* `{"status":"UP"}`

### 6. Payment Service Health
```bash
curl http://localhost:8084/actuator/health
```
*Expected Response:* `{"status":"UP"}`

---

## 7. Container Management Commands

### Viewing Logs
```bash
# Stream logs for all containers
docker compose logs -f

# Stream logs for a specific service
docker compose logs -f bidding-service
docker compose logs -f api-gateway
docker compose logs -f auction-mysql
```

### Container Status & Resource Utilization
```bash
# View active container status and health states
docker compose ps

# View real-time CPU and Memory consumption
docker stats
```

### Stopping the Ecosystem
```bash
# Stop all containers preserving persistent database data
docker compose stop

# Stop and remove containers, networks
docker compose down

# Stop and purge database volumes (clean reset)
docker compose down -v
```

---

## 8. Operational Troubleshooting

| Symptom | Probable Cause | Resolution |
| :--- | :--- | :--- |
| `target/*.jar not found during docker build` | Maven build was skipped before running Docker Compose | Run `mvn clean package -DskipTests` at the repository root first. |
| `Port already allocated (e.g. 3306 or 8080)` | A local service or MySQL instance is bound to the port | Modify the exposed port in `.env` (e.g. `MYSQL_PORT=3307`, `GATEWAY_PORT=8888`). |
| `Services not appearing in Eureka dashboard` | Discovery client registration interval | Eureka heartbeats poll on 10s intervals; wait 15–20 seconds after container initialization. |
| `Connection refused: mysql:3306` | MySQL container still initializing schemas | Healthcheck enforces dependency readiness. Check `docker compose logs auction-mysql`. |
