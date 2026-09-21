# PS025 – Real-Time Distributed Bidding & Auction Clearance Engine

A production-grade, high-concurrency distributed microservices platform for real-time auction bidding, atomic concurrency control, deterministic clearance calculation, and automated escrow payment settlements. Built with **Java 17+ (tested through Java 25)**, **Spring Boot 3.3.5**, **Spring Cloud 2023.0.3**, **Netflix Eureka**, **Spring Cloud Gateway**, **OpenFeign**, **Spring Security + JWT**, **Spring Data JPA**, **MySQL**, **JUnit 5**, **Mockito**, and **Docker**.

---

## 👥 Team Members

| ID / Registration No. | Name |
|---|---|
| **2400030020** | Yadamakanti Navaneeth Reddy |
| **2400030925** | Chithaluri Veera Hemanth |
| **2400032365** | Dhamothiran |

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

## 🚀 Microservices Catalog

| Microservice | Port | Database Schema | Key Responsibilities |
|---|---|---|---|
| **Eureka Server** | `8761` | N/A | Central dynamic service registry, peer awareness, and heartbeat liveness monitoring. |
| **API Gateway** | `8080` | N/A | Reverse proxy, perimeter JWT authentication filter, context header propagation (`X-User-Id`, `X-User-Email`, `X-User-Role`), global audit logging (`GlobalLoggingFilter`), and CORS. |
| **Auth Service** | `8081` | `auth_db` | User registration, dual-identifier login (email/username), BCrypt password hashing, JWT creation & token validation, role-based access control (BUYER, SELLER, ADMIN). |
| **Auction Service** | `8082` | `auction_db` | Auction creation, reserve price, lifecycle state transitions (ACTIVE, CLOSED, CANCELLED), deterministic winner finalization, automated clearance scheduler, and Feign inter-service settlement trigger. |
| **Bidding Service** | `8083` | `bidding_db` | Real-time high concurrency bid placement, per-auction hybrid locking, minimum increment checks, sub-threshold rejection, and deterministic clearance calculation. |
| **Payment Service** | `8084` | `payment_db` | Winner payment processing (`POST /api/payments`), pluggable `PaymentProcessor` & `MockPaymentProcessor`, idempotent settlement ledger, platform fee calculation (5%), and wallet transactions. |

---

## 💡 Key Architectural & Technical Highlights

### 1. Multi-Tier Hybrid Concurrency Control
To eliminate race conditions and dirty overwrites under high-frequency simultaneous bidding:
- **Per-Auction Fair In-Memory Lock**: `ConcurrentHashMap<Long, ReentrantLock>` configured with fair scheduling (`new ReentrantLock(true)`).
- **Programmatic Transactional Enclosure**: `TransactionTemplate` executes strictly *inside* the locked critical section (`lock.lock() -> transactionTemplate.execute(...) -> lock.unlock()`), ensuring uncommitted state is never exposed to concurrent threads.
- **Database Optimistic Locking**: JPA `@Version` column prevents lost updates across distributed replicas.

### 2. Strict Deterministic Winner Selection & Clearance
- **Primary Rank**: Highest bid amount (`amount DESC`).
- **Tie-Breaker**: Earliest accepted timestamp (`acceptedAt ASC`).
- **Secondary Tie-Breaker**: Deterministic lowest primary key (`id ASC`).
- **Reserve Price Rule**: If `winningBid < reservePrice`, the auction terminates with status `CLOSED` and `winnerId = null` (no winner, zero fund transfers).

### 3. Pluggable Payment Processor & Idempotency
- **Decoupled Design**: Abstract `PaymentProcessor` interface implemented by `MockPaymentProcessor`, ready to be swapped with Stripe, PayPal, or Razorpay without touching business logic.
- **Idempotent Transactions**: All payment operations check existing completed transactions by `auctionId` before executing charges, preventing duplicate deductions.
- **PCI-DSS Compliance / Sensitive Data Masking**: Raw card details and CVVs are never logged or stored; only masked references (`**** 4242`) and cryptographically random transaction IDs (`PAY-...`) are retained.

### 4. Defense-in-Depth Security
- **Perimeter Gateway Authentication**: `AuthenticationFilter` validates JWT signatures, extracts claims, and mutates downstream request headers with `X-User-Id`, `X-User-Email`, and `X-User-Role`.
- **Downstream Independent Verification**: Microservices independently re-verify JWT tokens from the `Authorization: Bearer <token>` header to prevent lateral privilege escalation if gateway perimeter checks are bypassed.

### 5. Robust OpenFeign Inter-Service Communication
- **Eureka-Based Dynamic Lookup**: All Feign clients resolve target services dynamically via service names (`auction-service`, `bidding-service`, `payment-service`) with zero hardcoded IPs.
- **Custom Feign Error Decoders**: `CustomFeignErrorDecoder` instances in `auction-service`, `bidding-service`, and `payment-service` intercept downstream HTTP errors and translate them into domain exceptions (`ResourceNotFoundException`, `InvalidBidException`, `ServiceUnavailableException`).
- **Resilience Timeouts**: Connect and read timeouts configured to 5000ms across all participating clients.

### 6. Database-per-Service Pattern
- Total schema isolation: `auth_db`, `auction_db`, `bidding_db`, and `payment_db`.
- No cross-database joins or shared tables.
- Cross-boundary state is exchanged strictly via typed DTOs over REST and OpenFeign.

---

## 📡 Complete REST API Reference

All requests pass through the **API Gateway** on port `8080` (or directly via individual service ports for internal/testing access).

### Auth Service (`/api/auth` or `/api/v1/auth`)

| Method | Endpoint | Auth | Description |
|---|---|:---:|---|
| `POST` | `/api/auth/register` | Public | Register a new user (SELLER, BUYER, ADMIN) |
| `POST` | `/api/auth/login` | Public | Authenticate with email/username + password and obtain JWT token |
| `GET` | `/api/auth/validate` | Bearer Token | Validate JWT token authenticity and expiration |
| `GET` | `/api/users/profile` | Bearer Token | Retrieve authenticated user profile |

### Auction Service (`/api/auctions` or `/api/v1/auctions`)

| Method | Endpoint | Auth | Description |
|---|---|:---:|---|
| `POST` | `/api/auctions` | Seller / Admin | Create a new auction listing |
| `GET` | `/api/auctions` | Public | List all auctions (optional filter by `status`, `category`) |
| `GET` | `/api/auctions/{id}` | Public | Retrieve detailed auction metadata |
| `GET` | `/api/auctions/seller/{sellerId}` | Public | Retrieve all auctions listed by a specific seller |
| `PUT` | `/api/auctions/{id}` | Seller / Admin | Update draft auction parameters |
| `DELETE` | `/api/auctions/{id}` | Seller / Admin | Delete draft auction |
| `POST` | `/api/auctions/{id}/start` | Seller / Admin | Transition auction to `ACTIVE` state |
| `POST` | `/api/auctions/{id}/close` | Seller / Admin | Close auction and determine winning bidder |
| `POST` | `/api/auctions/{id}/cancel` | Seller / Admin | Cancel auction |
| `PUT` | `/api/auctions/{id}/highest-bid` | Internal / Feign | Update highest bid and current price |
| `POST` | `/api/auctions/{id}/clear` | Internal / Feign | Execute deterministic clearance and trigger settlement |

### Bidding Service (`/api/bids` or `/api/v1/bids`)

| Method | Endpoint | Auth | Description |
|---|---|:---:|---|
| `POST` | `/api/bids` | Buyer Token | Place a real-time bid on an active auction |
| `GET` | `/api/bids/{id}` | Bearer Token | Retrieve individual bid record |
| `GET` | `/api/auctions/{auctionId}/bids` | Public | Retrieve all bids for an auction |
| `GET` | `/api/auctions/{auctionId}/highest` | Public | Retrieve current highest accepted bid |
| `GET` | `/api/v1/bids/auction/{auctionId}/clearance` | Internal / Feign | Compute deterministic clearance result |

### Payment Service (`/api/payments` or `/api/v1/payments`)

| Method | Endpoint | Auth | Description |
|---|---|:---:|---|
| `POST` | `/api/payments` | Winner Token | Process winning bidder payment via pluggable processor |
| `GET` | `/api/payments/{id}` | Bearer Token | Retrieve payment status and transaction reference |
| `GET` | `/api/payments/auction/{auctionId}` | Bearer Token | Retrieve payment details for a specific auction |
| `POST` | `/api/v1/payments/wallet/deposit` | Bearer Token | Deposit funds into sandbox wallet |
| `GET` | `/api/v1/payments/wallet` | Bearer Token | Retrieve current user wallet balance |
| `POST` | `/api/v1/payments/settle` | Internal / Feign | Settle auction escrow payout to seller (5% platform fee) |
| `GET` | `/api/v1/payments/transactions/{txId}` | Bearer Token | Retrieve settlement ledger transaction |

---

## 🧪 Comprehensive Verification & Test Suite

The entire multi-module reactor executes cleanly with **131 tests passing, 0 failures, and 0 errors**:

| Module | Test Classes | Tests Run | Result |
|---|---|:---:|:---:|
| **eureka-server** | `EurekaServerApplicationTests`, `EurekaServiceRegistrationTest` | 4 | **100% PASSED** |
| **api-gateway** | `ApiGatewayApplicationTests`, `AuthenticationFilterTest`, `GlobalLoggingFilterTest`, `GatewayRoutingTest` | 8 | **100% PASSED** |
| **auth-service** | `AuthServiceApplicationTests`, `AuthControllerTest`, `JwtSecurityTest`, `AuthServiceImplTest` | 34 | **100% PASSED** |
| **auction-service** | `AuctionServiceApplicationTests`, `AuctionControllerTest`, `AuctionServiceImplTest`, `AuctionConcurrencyTest`, `AuctionFeignErrorDecoderTest`, `AuctionInterServiceCommunicationTest` | 38 | **100% PASSED** |
| **bidding-service** | `BiddingServiceApplicationTests`, `BiddingControllerTest`, `BiddingServiceImplTest`, `BiddingConcurrencyTest`, `BiddingFeignErrorDecoderTest` | 25 | **100% PASSED** |
| **payment-service** | `PaymentServiceApplicationTests`, `PaymentControllerTest`, `PaymentServiceImplTest`, `PaymentWinnerTest`, `PaymentFeignErrorDecoderTest` | 22 | **100% PASSED** |
| **Total Reactor** | **All 7 Modules** | **131** | **100% SUCCESS** |

---

## 🛠️ Build and Execution Instructions

### Prerequisites
- **Java 17+** (Compatible through Java 25)
- **Maven 3.9+**
- **Docker & Docker Compose** (Optional, for containerized multi-service deployment)
- **MySQL 8.0+** (When running outside Docker)

### 1. Build and Run All Unit & Integration Tests
```bash
mvn clean test
```

### 2. Package All Microservices
```bash
mvn clean package -DskipTests
```

### 3. Recommended Local Startup Order
1. **Eureka Server**:
   ```bash
   cd eureka-server && mvn spring-boot:run
   ```
2. **Auth Service**:
   ```bash
   cd auth-service && mvn spring-boot:run
   ```
3. **Auction Service**:
   ```bash
   cd auction-service && mvn spring-boot:run
   ```
4. **Bidding Service**:
   ```bash
   cd bidding-service && mvn spring-boot:run
   ```
5. **Payment Service**:
   ```bash
   cd payment-service && mvn spring-boot:run
   ```
6. **API Gateway**:
   ```bash
   cd api-gateway && mvn spring-boot:run
   ```

Verify Eureka dashboard at: [http://localhost:8761](http://localhost:8761)

---

## 🐳 Docker Deployment

Start the complete cluster (MySQL + Eureka + Gateway + 4 Microservices) in a single command:

```bash
docker compose up --build -d
```

### Monitor Container Status:
```bash
docker compose ps
```

### Stop All Services:
```bash
docker compose down
```

---

## 📡 End-to-End API Workflow Example (cURL)

All requests pass through the **API Gateway** on port `8080`.

### 1. Register Seller & Buyer
```bash
# Register Seller
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"seller_alice","email":"alice@auction.com","password":"Password123!","role":"SELLER"}'

# Register Buyer
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"buyer_bob","email":"bob@auction.com","password":"Password123!","role":"BUYER"}'
```

### 2. Login to Obtain JWT Token
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"bob@auction.com","password":"Password123!"}'
```

### 3. Create and Activate Auction (As Seller)
```bash
curl -X POST http://localhost:8080/api/auctions \
  -H "Authorization: Bearer <SELLER_JWT>" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "MacBook Pro M3 Max",
    "description": "Mint condition 64GB RAM 1TB SSD",
    "category": "Electronics",
    "startingPrice": 1000.00,
    "reservePrice": 1500.00,
    "minimumIncrement": 50.00,
    "startTime": "2026-09-20T00:00:00",
    "endTime": "2026-09-30T00:00:00"
  }'

curl -X POST http://localhost:8080/api/auctions/1/start \
  -H "Authorization: Bearer <SELLER_JWT>"
```

### 4. Place Bid (As Buyer)
```bash
curl -X POST http://localhost:8080/api/bids \
  -H "Authorization: Bearer <BUYER_JWT>" \
  -H "Content-Type: application/json" \
  -d '{
    "auctionId": 1,
    "amount": 1600.00
  }'
```

### 5. Close Auction & Settle Payment
```bash
# Seller closes auction
curl -X POST http://localhost:8080/api/auctions/1/close \
  -H "Authorization: Bearer <SELLER_JWT>"

# Winning buyer pays
curl -X POST http://localhost:8080/api/payments \
  -H "Authorization: Bearer <BUYER_JWT>" \
  -H "Content-Type: application/json" \
  -d '{
    "auctionId": 1,
    "winnerId": 2,
    "amount": 1600.00,
    "paymentMethod": "CREDIT_CARD",
    "maskedCardNumber": "**** **** **** 4242"
  }'
```