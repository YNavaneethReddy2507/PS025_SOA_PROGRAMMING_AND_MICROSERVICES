# PS025 – Real-Time Distributed Bidding & Auction Clearance Engine

A production-ready, high-concurrency distributed microservices platform for real-time auction bidding, atomic concurrency control, deterministic clearance calculation, and automated escrow payment settlements. Built with **Java 17+**, **Spring Boot 3.3.5**, **Spring Cloud 2023.0.3**, **Eureka**, **Spring Cloud Gateway**, **OpenFeign**, **Spring Security + JWT**, **Spring Data JPA**, **MySQL**, **JUnit 5**, **Mockito**, and **Docker**.

---

## 👥 Team Members

| ID / Registration No. | Name |
|---|---|
| **2400030020** | Yadamakanti Navaneeth Reddy |
| **2400030925** | Chithaluri Veera Hemanth |
| **2400032365** | Dhamothiran |

---

## 🏛️ Microservices Architecture

```
                                  +-------------------+
                                  |   Eureka Server   |
                                  |    (Port 8761)    |
                                  +---------+---------+
                                            ^
                 +--------------------------+--------------------------+
                 |                          |                          |
                 v                          v                          v
     +-----------------------+  +-----------------------+  +-----------------------+
     |      API Gateway      |  |     Auth Service      |  |    Auction Service    |
     |      (Port 8080)      |  |      (Port 8081)      |  |      (Port 8082)      |
     | [JwtAuthFilter/Router]|  |  [JWT / Users / RBAC] |  | [Lifecycle & Sched]   |
     +-----------+-----------+  +-----------+-----------+  +-----------+-----------+
                 |                          |                          |
                 +--------------------------+                          |
                 |                                                     | (Feign)
                 v                                                     v
     +-----------------------+                             +-----------------------+
     |    Bidding Service    | <======== (Feign) ========= |    Payment Service    |
     |      (Port 8083)      |                             |      (Port 8084)      |
     | [Real-Time Bids/Lock] |                             | [Escrow & Settlement] |
     +-----------------------+                             +-----------------------+
                 |                                                     |
                 v                                                     v
        [ bidding_db MySQL ]                                  [ payment_db MySQL ]
```

---

## 📋 Implementation Status & Phase Roadmap

| Phase | Milestone / Area | Status | Deliverables & Progress |
| :--- | :--- | :---: | :--- |
| **Phase 1** | **System Architecture & Multi-Module Setup** | ✅ **COMPLETED** | - Root Maven reactor POM managing 6 submodules with Spring Boot 3.3.5 & Spring Cloud 2023.0.3.<br>- Domain models, entities, DTOs, exception handlers, and repository layers for all services.<br>- JWT security utilities, BCrypt password hashing, and API Gateway route filters.<br>- Docker Compose multi-container configuration and MySQL initialization script (`init-mysql.sql`). |
| **Phase 2** | **Eureka Service Discovery** | ✅ **COMPLETED** | - Standalone Eureka Server running on port `8761` with self-preservation tuning.<br>- Eureka Discovery Clients configured across all 5 services with `prefer-ip-address: true`.<br>- Dynamic service lookup via Spring Cloud LoadBalancer (`lb://<service>`) & OpenFeign (`@FeignClient`).<br>- Zero hardcoded IP addresses across the entire codebase.<br>- Dynamic service registration integration tests (`EurekaServiceRegistrationTest`) & 42/42 tests passing. |
| **Phase 3** | **Authentication & JWT Security** | ✅ **COMPLETED** | - Auth Service with BCrypt password hashing, dual-identifier login (username/email), JWT creation & claims validation.<br>- Reusable JWT security configuration, custom authentication entry point & access denied handler.<br>- Zero token logging, configurable expiration & secrets, comprehensive exception handling.<br>- 34/34 tests passing in auth-service; 66/66 tests passing across all reactor modules.<br>- Exported Postman collection (`PS025_Phase3_Auth_Postman_Collection.json`) for full API verification. |
| **Phase 4** | **Gateway Routing, Rate Limiting & Filter Pipeline** | 🔄 **READY** | - Route predicates, request transformation, global CORS, and distributed rate limiting. |
| **Phase 5** | **Distributed Real-Time Engine & Resilience** | ⏳ **PLANNED** | - Concurrency stress testing, Circuit Breakers (Resilience4j), and transaction rollbacks. |

---

## 🚀 Services Overview

| Microservice | Port | Database Schema | Key Responsibilities |
|---|---|---|---|
| **Eureka Server** | `8761` | N/A | Service registration & discovery heartbeat monitoring |
| **API Gateway** | `8080` | N/A | Centralized routing, JWT authentication filter, context header propagation (`X-User-Id`, `X-User-Role`) |
| **Auth Service** | `8081` | `auth_db` | Registration, login, BCrypt password hashing, JWT creation, claims validation, RBAC |
| **Auction Service** | `8082` | `auction_db` | Auction creation, reserve price, lifecycle transitions (Draft/Active/Ended/Settled/Cancelled), automated clearance scheduler |
| **Bidding Service** | `8083` | `bidding_db` | Real-time high concurrency bid execution, atomic auction locking, sub-threshold bid rejection, deterministic clearance |
| **Payment Service** | `8084` | `payment_db` | Buyer/Seller wallets, escrow funds, 5% platform fee calculation, idempotent settlement ledger |

---

## 💡 Key Business & Technical Rules Implemented

1. **High Concurrency & Atomic Bidding**: Fine-grained auction locking and optimistic locking versioning prevent race conditions and dirty overwrites during rapid concurrent bids.
2. **Sub-Threshold Bid Prevention**: A bid is strictly rejected if `bidAmount < currentHighestBid + minBidIncrement`.
3. **Seller-Bidder Segregation**: Sellers are blocked from placing bids on their own auctions.
4. **Deterministic Winner Selection**:
   - **Primary**: Highest bid amount.
   - **Tie-Breaker**: Earliest bid timestamp (`ORDER BY bid_amount DESC, bid_timestamp ASC`).
   - **Reserve Check**: Winning bid must satisfy `winningBid >= reservePrice`. If reserve is not met, auction marks `NO_WINNER` without fund transfers.
5. **Idempotent Settlement Ledger**: Duplicate settlement requests for an auction return the existing completed transaction with zero double-debit risk.
6. **Independent Microservices & Database Isolation**: Zero shared database across services (`auth_db`, `auction_db`, `bidding_db`, `payment_db`).
7. **Clean DTO Boundaries & Exception Handling**: Controllers strictly consume and return DTOs (no JPA entities exposed); errors handled uniformly via `@RestControllerAdvice`.
8. **Pure Java Portability**: Pure POJO builder pattern, explicit constructor injection, and SLF4J logging for clean, warning-free compilation across Java 17 through Java 25+.
9. **Environment Variable Configuration**: Secrets and database credentials configured via environment variables with safe development defaults.
10. **Zero Token Logging & BCrypt Hashing**: Raw JWT tokens are strictly excluded from logs; passwords are salted and hashed via BCrypt before storage.

---

## 🧪 Test Suite & Verification Results

All 66 unit and integration tests execute cleanly with **0 failures and 0 errors**:

| Module | Test Classes | Tests Run | Result |
|---|---|---|---|
| **eureka-server** | `EurekaServerApplicationTests`, `EurekaServiceRegistrationTest` | 3 | **PASSED** |
| **api-gateway** | `ApiGatewayApplicationTests` | 2 | **PASSED** |
| **auth-service** | `AuthServiceApplicationTests`, `AuthControllerTest`, `JwtSecurityTest`, `AuthServiceImplTest` | 34 | **PASSED** |
| **auction-service** | `AuctionServiceApplicationTests`, `AuctionControllerTest`, `AuctionServiceImplTest` | 10 | **PASSED** |
| **bidding-service** | `BiddingServiceApplicationTests`, `BiddingControllerTest`, `BiddingServiceImplTest` | 9 | **PASSED** |
| **payment-service** | `PaymentServiceApplicationTests`, `PaymentControllerTest`, `PaymentServiceImplTest` | 8 | **PASSED** |
| **Total** | | **66** | **100% SUCCESS** |

---

## 🧭 Service Discovery & Startup Order (Phase 2)

All microservices register dynamically as Eureka discovery clients with **zero hardcoded service IPs**.

### Registered Services in Eureka

| Service | Port | Eureka Service ID | Discovery Mechanism |
|---|---|---|---|
| **Eureka Server** | `8761` | `EUREKA-SERVER` | Central Service Registry & Peer Awareness |
| **API Gateway** | `8080` | `API-GATEWAY` | Dynamic Route Resolution (`lb://<service>`) |
| **Auth Service** | `8081` | `AUTH-SERVICE` | Eureka Client (`@EnableDiscoveryClient`) |
| **Auction Service** | `8082` | `AUCTION-SERVICE` | OpenFeign dynamic lookup to `bidding-service` & `payment-service` |
| **Bidding Service** | `8083` | `BIDDING-SERVICE` | OpenFeign dynamic lookup to `auction-service` |
| **Payment Service** | `8084` | `PAYMENT-SERVICE` | Eureka Client (`@EnableDiscoveryClient`) |

### Recommended Startup Order

To guarantee clean service registration and avoid cold-lookup retries:

1. **MySQL Database (`3306`)**: Start MySQL server and run `init-mysql.sql` to initialize schemas (`auth_db`, `auction_db`, `bidding_db`, `payment_db`).
2. **Eureka Server (`8761`)**: Start `eureka-server` and verify dashboard at `http://localhost:8761`.
3. **Domain Services**:
   - `auth-service` (`8081`)
   - `payment-service` (`8084`)
4. **Core Auction & Bidding Engine**:
   - `auction-service` (`8082`)
   - `bidding-service` (`8083`)
5. **API Gateway (`8080`)**: Start `api-gateway` to pull full Eureka registry cache and begin routing incoming HTTP traffic.

---

## 🛠️ Build and Run

### Prerequisites
- Java 17 or higher (compatible with Java 17, 21, and 25)
- Maven 3.9+
- Docker & Docker Compose (optional for containerized deployment)

### 1. Build and Run All Tests
```bash
mvn clean test
```

### 2. Package and Install All Microservices
```bash
mvn clean install
```

---

## 🐳 Docker Deployment

Start the full microservices cluster with MySQL:

```bash
docker compose up --build -d
```

### Accessing Dashboards & Endpoints:
- **Eureka Dashboard:** [http://localhost:8761](http://localhost:8761)
- **API Gateway:** [http://localhost:8080](http://localhost:8080)
- **Actuator Health Checks:**
  - Gateway: `http://localhost:8080/actuator/health`
  - Auth: `http://localhost:8081/actuator/health`
  - Auction: `http://localhost:8082/actuator/health`
  - Bidding: `http://localhost:8083/actuator/health`
  - Payment: `http://localhost:8084/actuator/health`

To stop all containers:
```bash
docker compose down
```

---

## 📡 End-to-End API Workflow (cURL Examples)

All requests pass through the **API Gateway** on port `8080`.

### 1. Register Seller
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "alice_seller",
    "email": "alice@auction.com",
    "password": "Password123!",
    "role": "SELLER"
  }'
```

### 2. Register Buyer
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "bob_buyer",
    "email": "bob@auction.com",
    "password": "Password123!",
    "role": "BUYER"
  }'
```

### 3. Login to Obtain JWT Token
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "alice@auction.com",
    "password": "Password123!"
  }'
```

### 4. Create an Auction (As Seller)
```bash
curl -X POST http://localhost:8080/api/v1/auctions \
  -H "Authorization: Bearer <SELLER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Vintage Mechanical Chronograph 1968",
    "description": "Mint condition vintage chronograph with original box and papers.",
    "category": "Watches",
    "startingPrice": 500.00,
    "reservePrice": 1200.00,
    "minBidIncrement": 50.00,
    "startTime": "2026-09-01T10:00:00",
    "endTime": "2026-09-05T18:00:00"
  }'
```

### 5. Deposit Funds into Buyer Wallet
```bash
curl -X POST http://localhost:8080/api/v1/payments/wallet/deposit \
  -H "Authorization: Bearer <BUYER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 2500.00
  }'
```

### 6. Place Bids (As Buyer)
```bash
curl -X POST http://localhost:8080/api/v1/bids \
  -H "Authorization: Bearer <BUYER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "auctionId": 1,
    "bidAmount": 1300.00
  }'
```

### 7. View Bid History
```bash
curl -X GET http://localhost:8080/api/v1/bids/auction/1 \
  -H "Authorization: Bearer <BUYER_TOKEN>"
```

### 8. Trigger Clearance Settlement
```bash
curl -X POST http://localhost:8080/api/v1/auctions/1/clear \
  -H "Authorization: Bearer <SELLER_TOKEN>"
```

### 9. Check Buyer & Seller Wallets
```bash
curl -X GET http://localhost:8080/api/v1/payments/wallet \
  -H "Authorization: Bearer <SELLER_TOKEN>"
```

---

## 📁 Repository Structure

```
PS025-BidVelocity (d:\SOA)/
├── pom.xml                                  # Root Multi-Module POM
├── docker-compose.yml                       # Multi-Container Compose Orchestration
├── init-mysql.sql                           # Database Schema Initialization
├── README.md                                # Project Documentation & Guides
│
├── eureka-server/                           # Service Registry (Port 8761)
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/auction/eureka/
│       └── main/resources/application.yml
│
├── api-gateway/                             # Spring Cloud API Gateway (Port 8080)
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/auction/gateway/   # JWT Auth Filter & Route Validator
│       └── main/resources/application.yml
│
├── auth-service/                            # Identity, Users & JWT Provider (Port 8081)
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/auction/auth/      # Controller, Service, Entity, DTO, Security
│       └── main/resources/application.yml
│
├── auction-service/                         # Auction Engine & Lifecycle (Port 8082)
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/auction/auction/   # Feign Clients, Scheduler, Clearance Logic
│       └── main/resources/application.yml
│
├── bidding-service/                         # High-Concurrency Bidding Engine (Port 8083)
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/auction/bidding/   # Lock-Free & Atomic Bidding, Clearance
│       └── main/resources/application.yml
│
└── payment-service/                         # Wallets, Escrow & Settlement (Port 8084)
    ├── pom.xml
    └── src/
        ├── main/java/com/auction/payment/   # 5% Fee Calc, Idempotent Transaction Ledger
        └── main/resources/application.yml
```