# PS025 – Real-Time Distributed Bidding & Auction Clearance Engine

A production-grade, high-concurrency distributed microservices platform for real-time auction bidding, atomic concurrency control, deterministic clearance calculation, and automated escrow payment settlements. Built with **Java 17+ (tested through Java 25)**, **Spring Boot 3.3.5**, **Spring Cloud 2023.0.3**, **Netflix Eureka**, **Spring Cloud Gateway**, **OpenFeign**, **Spring Security + JWT**, **Spring Data JPA**, **MySQL 8.0**, **JUnit 5**, **Mockito**, and **Docker**.

---

## 👥 Team Members

| ID / Registration No. | Name |
|---|---|
| **2400030020** | Yadamakanti Navaneeth Reddy |
| **2400030925** | Chithaluri Veera Hemanth |
| **2400032365** | Dhamothiran |

---

## 📋 Project Status: Phases 1–13 Complete (100%)

| Phase | Description | Deliverables | Status |
|---|---|---|:---:|
| **Phase 1** | Requirement Specification & Problem Analysis | `docs/requirements.md` | ✅ **Complete** |
| **Phase 2** | Architecture Design & Domain Modeling | `docs/architecture.md` | ✅ **Complete** |
| **Phase 3** | Authentication & User Service | `auth-service` (JWT, BCrypt, RBAC) | ✅ **Complete** |
| **Phase 4** | Eureka Service Registry | `eureka-server` (Port 8761) | ✅ **Complete** |
| **Phase 5** | Auction Catalog Service | `auction-service` (Port 8082) | ✅ **Complete** |
| **Phase 6** | Bidding Engine & Concurrency | `bidding-service` (Port 8083) | ✅ **Complete** |
| **Phase 7** | Clearance Engine & Winner Selection | Deterministic Price-Time Priority | ✅ **Complete** |
| **Phase 8** | Payment & Escrow Settlement Service | `payment-service` (Port 8084) | ✅ **Complete** |
| **Phase 9** | API Perimeter Gateway & Filters | `api-gateway` (Port 8080) | ✅ **Complete** |
| **Phase 10** | Distributed Communication & Discovery | OpenFeign, ErrorDecoders, Eureka | ✅ **Complete** |
| **Phase 11** | Security & Reliability Hardening | `docs/security.md`, Header Sanitization | ✅ **Complete** |
| **Phase 12** | Complete Testing & Validation | `docs/testing.md`, 117 Reactor Tests | ✅ **Complete** |
| **Phase 13** | Deployment & Technical Documentation | `docker-compose.yml`, `docs/deployment.md`, `docs/api.md` | ✅ **Complete** |

---

## 🎯 Problem Statement

Traditional auction platforms suffer from critical architectural vulnerabilities under high concurrency:
1. **Lost Updates & Race Conditions**: Simultaneous bids placed in the closing seconds ("bid sniping") cause dirty writes, invalid winning prices, or multiple contradictory winners.
2. **Monolithic Scalability Bottlenecks**: High-frequency read queries (e.g. refreshing current highest bids) overload transactional databases and degrade payment workflows.
3. **Security & Identity Spoofing**: Insecure systems trust client-supplied user IDs or expose payment histories to unauthorized third parties.
4. **Non-Deterministic Clearance**: Unpredictable tie-breaking leads to unfair winner determination when identical bids arrive concurrently.

**PS025** solves these challenges using a decentralized, event-ready microservices architecture combining per-auction in-memory fair mutex locks, programmatic transaction boundaries, deterministic price-time tie-breaking, schema-isolated MySQL persistence, and perimeter JWT token verification.

---

## ✨ Core Features

- **Decentralized Microservices**: 6 autonomous services running on distinct ports with zero shared database tables.
- **Dynamic Service Discovery**: Netflix Eureka registry with client-side load balancing and health checks.
- **Secure Perimeter API Gateway**: Spring Cloud Gateway enforcing JWT validation, header sanitization, context injection, global audit logging, and CORS.
- **Multi-Tier Hybrid Concurrency**: Sub-millisecond `ConcurrentHashMap<Long, ReentrantLock>` fair mutex locking combined with Spring `@Transactional` boundaries and JPA `@Version` optimistic locking.
- **Deterministic Price-Time Clearance**: Strict mathematical ordering: highest amount first; earliest accepted timestamp breaks ties; surrogate ID breaks sub-millisecond ties.
- **Automated Lifecycle Scheduler**: Background scheduled daemon sweeps expired auctions every 30 seconds and triggers clearance.
- **Idempotent Payment & Escrow**: Pluggable `PaymentProcessor` interface, wallet ledger, 5% automated platform commission calculation, and duplicate-safe retries.
- **Least Privilege Database Ownership**: Dedicated MySQL users (`auth_user`, `auction_user`, `bidding_user`, `payment_user`) restricted to their respective schemas.
- **Production-Ready Docker Containerization**: Multi-container Docker Compose setup with health dependency orchestration.

---

## 🏛️ System Architecture

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
     | [GlobalLoggingFilter] |  |  [JWT / Users / RBAC] |  | [Lifecycle & Sched]   |
     | [AuthenticationFilter]|  +-----------+-----------+  +-----------+-----------+
     +-----------+-----------+              |                          |
                 |                          v                          |
                 |                 [ auth_db MySQL ]                   | (Feign)
                 |                                                     v
                 +--------------------------+----------------> +-----------------------+
                 |                          |                  |    Payment Service    |
                 v                          |                  |      (Port 8084)      |
     +-----------------------+              |                  | [Pluggable Processor] |
     |    Bidding Service    | <============+== (Feign) ====== | [Escrow & Settlement] |
     |      (Port 8083)      |                                 +-----------+-----------+
     | [Real-Time Bids/Lock] |                                             |
     +-----------+-----------+                                             v
                 |                                                [ payment_db MySQL ]
                 v
        [ bidding_db MySQL ]
```

---

## 🛠️ Technology Stack

| Category | Technology |
|---|---|
| **Language & Platform** | Java 17 LTS (Tested up to JDK 25) |
| **Microservices Framework** | Spring Boot 3.3.5 |
| **Cloud & Routing** | Spring Cloud 2023.0.3, Spring Cloud Gateway |
| **Service Discovery** | Spring Cloud Netflix Eureka |
| **Inter-Service Communication** | Spring Cloud OpenFeign, Resilience Decoders |
| **Security & Auth** | Spring Security 6, JJWT 0.12.6 (HMAC-SHA256), BCrypt |
| **Persistence & ORM** | Spring Data JPA, Hibernate 6.5, HikariCP |
| **Database** | MySQL 8.0 (Schema-per-Service isolation) / H2 In-Memory (Testing) |
| **Testing & Mocking** | JUnit 5, Mockito, Spring Boot Test, MockMvc |
| **Containerization** | Docker, Docker Compose, Eclipse Temurin 17 Alpine JRE |

---

## 🚀 Microservices Catalog & Ports

| Microservice | Port | Schema | Dedicated User | Responsibilities |
|---|---|---|---|---|
| **Eureka Server** | `8761` | N/A | N/A | Service discovery registry, peer awareness, and liveness monitoring. |
| **API Gateway** | `8080` | N/A | N/A | Perimeter routing, JWT authentication, header sanitization, CORS, audit logging. |
| **Auth Service** | `8081` | `auth_db` | `auth_user` | User registration, dual-identifier login, BCrypt hashing, JWT issuance. |
| **Auction Service** | `8082` | `auction_db` | `auction_user` | Auction catalog, reserve pricing, lifecycle states, clearance trigger. |
| **Bidding Service** | `8083` | `bidding_db` | `bidding_user` | Real-time concurrent bidding, mutex serialization, deterministic clearance. |
| **Payment Service** | `8084` | `payment_db` | `payment_user` | Winner payments, pluggable payment processor, idempotent wallet settlements. |

---

## 📡 REST API Reference Summary

All client calls route through the API Gateway at `http://localhost:8080`.

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/auth/register` | Public | Register new buyer, seller, or admin user. |
| `POST` | `/api/auth/login` | Public | Authenticate with username/email and receive JWT. |
| `POST` | `/api/auth/validate` | Authenticated | Validate JWT signature, expiration, and claims. |
| `POST` | `/api/auctions` | Seller, Admin | Create a new scheduled auction with reserve price. |
| `GET` | `/api/auctions` | Public | Query auctions with optional status and category filters. |
| `GET` | `/api/auctions/{id}` | Public | Retrieve detailed auction record. |
| `POST` | `/api/auctions/{id}/start` | Seller, Admin | Transition auction status from `SCHEDULED` to `ACTIVE`. |
| `POST` | `/api/auctions/{id}/clear` | Internal, Seller | Trigger clearance calculation and initiate settlement. |
| `POST` | `/api/bids` | Buyer, Admin | Place a new bid (identity extracted from JWT). |
| `GET` | `/api/bids/auction/{auctionId}` | Public | Retrieve all accepted bids for an auction. |
| `GET` | `/api/bids/auction/{auctionId}/highest` | Public | Query current highest bid. |
| `POST` | `/api/payments` | Winner | Process winner payment for an auction. |
| `GET` | `/api/payments/{id}` | Winner, Seller, Admin | Retrieve payment record by ID. |
| `POST` | `/api/v1/payments/wallet/deposit` | Authenticated | Deposit funds into authenticated user's wallet. |
| `GET` | `/api/v1/payments/wallet` | Authenticated | Retrieve authenticated user's wallet balance. |

*For complete API schemas, curl examples, and response bodies, see [docs/api.md](docs/api.md).*

---

## 🔒 Security Architecture

1. **Perimeter Authentication**: The API Gateway intercepts requests to secured endpoints, validates the HMAC-SHA256 signature and expiration of the JWT token, strips any client-supplied `X-User-*` headers, and injects validated claims (`X-User-Id`, `X-User-Email`, `X-User-Role`).
2. **Defense-in-Depth Validation**: Downstream services independently validate JWT tokens when the `Authorization` header is present.
3. **Auction Ownership**: Sellers can only modify, start, or cancel auctions they own (`auction.sellerId == userId`), unless they possess `ROLE_ADMIN`.
4. **Anti-Spoofing Bidding**: The bidding payload (`PlaceBidRequest`) strictly omits `bidderId`; the identity is extracted from the cryptographic JWT claims. Sellers cannot bid on their own auctions.
5. **Payment & Wallet Privacy**: Payment records and wallet balances are restricted strictly to the winner, seller, or platform admin.
6. **Password Governance**: Passwords are encrypted with `BCryptPasswordEncoder`. Zero plaintext passwords or JWT tokens are stored or logged.

*For the complete Authorization Matrix and security review, see [docs/security.md](docs/security.md).*

---

## ⚡ Concurrency & Clearance Strategy

1. **In-Memory Fair Mutex Locking**: Each auction is guarded by a dedicated `ReentrantLock(true)` stored in a `ConcurrentHashMap<Long, ReentrantLock>`. Bids for auction $A$ do not block bids for auction $B$.
2. **Transaction Enclosure**: The database transaction begins *after* acquiring the lock and commits *before* releasing the lock:
   $$\text{lock.lock()} \longrightarrow \text{TransactionTemplate.execute(...)} \longrightarrow \text{lock.unlock()}$$
3. **Deterministic Tie-Breaking (Price-Time Priority)**:
   - **Primary**: Highest bid amount (`amount DESC`)
   - **Secondary**: Earliest accepted timestamp (`acceptedAt ASC`)
   - **Tertiary**: Lowest primary key (`id ASC`)
4. **Post-Close Defense**: The instant an auction transitions to `CLOSED`, subsequent bids are rejected with `AuctionNotActiveException`.
5. **Payment Idempotency**: Payment settlements check existing transactions under lock before processing charges.

---

## 🧪 Testing Suite & Validation

The comprehensive test suite contains **117 automated tests** spanning unit, security, concurrency, and end-to-end integration workflows:

```bash
# Run the complete test suite across all 7 modules
mvn clean test
```

### Test Results Breakdown:
- **Unit Tests**: Registration, login, auction creation, state machine, valid/invalid bids, fee calculation.
- **Security Tests**: Missing JWT, expired JWT, tampered tokens, unauthorized auction edits, header spoofing.
- **Concurrency Tests**: 2 simultaneous bids, 10 simultaneous bids, **50+ simultaneous bids**, identical bid amounts, simultaneous bid + close race condition, and concurrent duplicate payments.
- **End-to-End Integration**: Complete lifecycle flow (`Create -> Start -> Bid 1 -> Bid 2 -> Close -> Clearance -> Settle Payment -> Reject Post-Close Bids`).

| Module | Tests Run | Failures | Errors | Skipped | Status |
|---|:---:|:---:|:---:|:---:|:---:|
| **Eureka Server** | 1 | 0 | 0 | 0 | `SUCCESS` |
| **API Gateway** | 6 | 0 | 0 | 0 | `SUCCESS` |
| **Auth Service** | 24 | 0 | 0 | 0 | `SUCCESS` |
| **Auction Service** | 23 | 0 | 0 | 0 | `SUCCESS` |
| **Bidding Service** | 31 | 0 | 0 | 0 | `SUCCESS` |
| **Payment Service** | 32 | 0 | 0 | 0 | `SUCCESS` |
| **TOTAL** | **117** | **0** | **0** | **0** | **`BUILD SUCCESS`** |

*For complete test execution metrics, see [docs/testing.md](docs/testing.md).*

---

## 🐳 Docker Deployment Guide

### 1. Build Executable JARs
```bash
mvn clean package -DskipTests
```

### 2. Configure Environment
```bash
cp .env.example .env
```

### 3. Launch with Docker Compose
```bash
docker compose up --build -d
```

### 4. Verify Service Health
- **Eureka Dashboard**: [http://localhost:8761](http://localhost:8761)
- **API Gateway**: `curl http://localhost:8080/actuator/health`
- **Auth Service**: `curl http://localhost:8081/actuator/health`
- **Auction Service**: `curl http://localhost:8082/actuator/health`
- **Bidding Service**: `curl http://localhost:8083/actuator/health`
- **Payment Service**: `curl http://localhost:8084/actuator/health`

### 5. Stop the Cluster
```bash
docker compose down
```

*For complete deployment instructions, see [docs/deployment.md](docs/deployment.md).*

---

## 📚 Technical Documentation Index

- **[System Requirements Specification](docs/requirements.md)**: Problem analysis, actors, use cases, non-functional requirements.
- **[Distributed Architecture Specification](docs/architecture.md)**: Domain models, sequence diagrams, concurrency architecture, container topology.
- **[REST API Reference](docs/api.md)**: Complete endpoint catalog, request/response bodies, error codes, curl examples.
- **[Security & RBAC Architecture](docs/security.md)**: Authorization matrix, JWT policy, password governance, defense-in-depth model.
- **[Testing & Validation Report](docs/testing.md)**: Test execution matrix, concurrency benchmarks, verification guarantees.
- **[Deployment & Operations Manual](docs/deployment.md)**: Containerization guide, Docker Compose, environment variables, healthchecks.