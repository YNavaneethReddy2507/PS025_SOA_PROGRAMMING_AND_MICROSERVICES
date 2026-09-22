# PS025: REST API Reference & Specification

## 1. Overview & Gateway Routing

All client interactions should be directed through the **API Gateway** on port `8080`. The Gateway routes incoming requests to downstream microservices, validates JWT authentication, enforces rate limits and CORS, and injects validated user identities into downstream request headers.

| Service | Downstream Port | Gateway Route Prefix |
| :--- | :--- | :--- |
| **Auth Service** | `8081` | `/api/auth/**`, `/api/v1/auth/**`, `/api/users/**` |
| **Auction Service** | `8082` | `/api/auctions/**`, `/api/v1/auctions/**` |
| **Bidding Service** | `8083` | `/api/bids/**`, `/api/v1/bids/**` |
| **Payment Service** | `8084` | `/api/payments/**`, `/api/v1/payments/**` |

---

## 2. Standardized Error Response Format

All error responses across all microservices follow a uniform schema:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Bid amount must exceed current highest bid by minimum increment 10.00",
  "path": "/api/bids",
  "timestamp": "2026-09-22T16:30:00"
}
```

---

## 3. Auth Service API (`/api/auth`)

### 3.1 User Registration
**`POST /api/auth/register`**  
*Access: Public*

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "alice_bidder",
    "email": "alice@example.com",
    "password": "SecurePassword123!",
    "role": "BUYER"
  }'
```

**Response (201 Created):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "userId": 1,
  "username": "alice_bidder",
  "email": "alice@example.com",
  "role": "BUYER",
  "expiresInMs": 86400000
}
```

### 3.2 User Login
**`POST /api/auth/login`**  
*Access: Public*

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "identifier": "alice@example.com",
    "password": "SecurePassword123!"
  }'
```

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "userId": 1,
  "username": "alice_bidder",
  "email": "alice@example.com",
  "role": "BUYER",
  "expiresInMs": 86400000
}
```

### 3.3 Validate Token
**`POST /api/auth/validate`**  
*Access: Authenticated / Internal*

```bash
curl -X POST http://localhost:8080/api/auth/validate \
  -H "Authorization: Bearer <jwt_token>"
```

---

## 4. Auction Service API (`/api/auctions`)

### 4.1 Create Auction
**`POST /api/auctions`**  
*Access: Authenticated (SELLER, ADMIN)*

```bash
curl -X POST http://localhost:8080/api/auctions \
  -H "Authorization: Bearer <jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Vintage 1968 Omega Speedmaster",
    "description": "Mint condition chronometer with box and papers",
    "category": "Watches",
    "startingPrice": 1000.00,
    "minimumIncrement": 50.00,
    "reservePrice": 1500.00,
    "startTime": "2026-09-22T17:00:00",
    "endTime": "2026-09-25T17:00:00"
  }'
```

**Response (201 Created):**
```json
{
  "id": 101,
  "title": "Vintage 1968 Omega Speedmaster",
  "description": "Mint condition chronometer with box and papers",
  "category": "Watches",
  "startingPrice": 1000.00,
  "currentPrice": 1000.00,
  "minimumIncrement": 50.00,
  "reservePrice": 1500.00,
  "winnerId": null,
  "sellerId": 1,
  "status": "SCHEDULED",
  "startTime": "2026-09-22T17:00:00",
  "endTime": "2026-09-25T17:00:00",
  "createdAt": "2026-09-22T16:45:00",
  "updatedAt": "2026-09-22T16:45:00"
}
```

### 4.2 Query Auctions
**`GET /api/auctions?status=ACTIVE&category=Watches`**  
*Access: Public*

### 4.3 Get Auction By ID
**`GET /api/auctions/{id}`**  
*Access: Public*

### 4.4 Start Auction
**`POST /api/auctions/{id}/start`**  
*Access: Authenticated (Seller / Admin)*

### 4.5 Close & Clear Auction
**`POST /api/auctions/{id}/clear`**  
*Access: Internal / Seller / Admin*

**Response (200 OK):**
```json
{
  "auctionId": 101,
  "winningBidderId": 42,
  "winningAmount": 1650.00,
  "reserveMet": true,
  "clearedAt": "2026-09-25T17:00:01",
  "clearanceStatus": "CLEARED",
  "message": "Deterministic winner selected"
}
```

---

## 5. Bidding Service API (`/api/bids`)

### 5.1 Place a Bid
**`POST /api/bids`**  
*Access: Authenticated (BUYER, BIDDER)*

```bash
curl -X POST http://localhost:8080/api/bids \
  -H "Authorization: Bearer <jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "auctionId": 101,
    "bidAmount": 1100.00
  }'
```

**Response (201 Created):**
```json
{
  "id": 5001,
  "auctionId": 101,
  "bidderId": 42,
  "amount": 1100.00,
  "status": "ACCEPTED",
  "acceptedAt": "2026-09-22T17:15:30",
  "message": "Bid accepted successfully",
  "isWinning": true
}
```

### 5.2 Query Current Highest Bid
**`GET /api/bids/auction/{auctionId}/highest`**  
*Access: Public*

### 5.3 Query Auction Bid History
**`GET /api/bids/auction/{auctionId}`**  
*Access: Public*

---

## 6. Payment Service API (`/api/payments`)

### 6.1 Process Winner Payment
**`POST /api/payments`**  
*Access: Authenticated Winner*

```bash
curl -X POST http://localhost:8080/api/payments \
  -H "Authorization: Bearer <jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "auctionId": 101,
    "winnerId": 42,
    "amount": 1650.00,
    "paymentMethod": "CREDIT_CARD"
  }'
```

**Response (201 Created):**
```json
{
  "id": 801,
  "auctionId": 101,
  "winnerId": 42,
  "amount": 1650.00,
  "status": "SUCCESS",
  "transactionReference": "PAY-A8B9-4C3D-8899",
  "createdAt": "2026-09-25T17:05:00",
  "paidAt": "2026-09-25T17:05:01",
  "message": "Payment successfully processed"
}
```

### 6.2 Get Payment By ID
**`GET /api/payments/{id}`**  
*Access: Winner, Seller, Admin*

### 6.3 Get Payment By Auction ID
**`GET /api/payments/auction/{auctionId}`**  
*Access: Winner, Seller, Admin*

### 6.4 Wallet Deposit
**`POST /api/v1/payments/wallet/deposit`**  
*Access: Authenticated User*

```bash
curl -X POST http://localhost:8080/api/v1/payments/wallet/deposit \
  -H "Authorization: Bearer <jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 500.00
  }'
```

### 6.5 Query Wallet Balance
**`GET /api/v1/payments/wallet`**  
*Access: Authenticated User (Self)*
