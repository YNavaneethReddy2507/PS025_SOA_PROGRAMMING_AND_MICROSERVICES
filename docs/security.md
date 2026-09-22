# PS025: Security & Reliability Hardening Architecture

## 1. Executive Summary

This document details the security and reliability hardening framework implemented across the PS025 Real-Time Distributed Bidding & Auction Clearance Engine. The architecture employs a defense-in-depth model across the API Gateway, Auth Service, Auction Service, Bidding Service, Payment Service, and Service Discovery layers.

---

## 2. End-to-End Authorization Matrix

| Endpoint | HTTP Method | Permitted Roles | Authentication Required | Ownership / Verification Rules |
| :--- | :--- | :--- | :--- | :--- |
| `/api/auth/register` | `POST` | Public | No | Unique email and username enforced; passwords hashed with BCrypt. |
| `/api/auth/login` | `POST` | Public | No | BCrypt password matching; returns signed stateless JWT. |
| `/api/auth/validate` | `POST` | Authenticated | Yes (JWT) | Validates JWT token signature, expiration, and claims. |
| `/api/auctions` | `POST` | `SELLER`, `ADMIN` | Yes | Authenticated user ID automatically assigned as `sellerId`. Client cannot inject `sellerId`. |
| `/api/auctions` | `GET` | Public | No | Public read access to active and scheduled auctions. |
| `/api/auctions/{id}` | `GET` | Public | No | Public read access to auction details. |
| `/api/auctions/seller/{sellerId}` | `GET` | Public | No | Filter auctions by seller. |
| `/api/auctions/{id}` | `PUT` | `SELLER`, `ADMIN` | Yes | Caller must match `auction.sellerId` or possess `ROLE_ADMIN`. Auction cannot be `CLOSED` or `CANCELLED`. |
| `/api/auctions/{id}` | `DELETE` | `SELLER`, `ADMIN` | Yes | Caller must match `auction.sellerId` or possess `ROLE_ADMIN`. Auction cannot be `ACTIVE` or `CLOSED`. |
| `/api/auctions/{id}/start` | `POST` | `SELLER`, `ADMIN` | Yes | Caller must match `auction.sellerId` or possess `ROLE_ADMIN`. |
| `/api/auctions/{id}/activate` | `POST` | `SELLER`, `ADMIN` | Yes | Caller must match `auction.sellerId` or possess `ROLE_ADMIN`. |
| `/api/auctions/{id}/close` | `POST` | `SELLER`, `ADMIN` | Yes | Caller must match `auction.sellerId` or possess `ROLE_ADMIN`. Mutex serialized and idempotent. |
| `/api/auctions/{id}/cancel` | `POST` | `SELLER`, `ADMIN` | Yes | Caller must match `auction.sellerId` or possess `ROLE_ADMIN`. |
| `/api/bids` | `POST` | `BIDDER`, `BUYER`, `ADMIN` | Yes | `bidderId` extracted strictly from validated JWT claims. Seller cannot bid on own auction. |
| `/api/bids/{id}` | `GET` | Authenticated / Internal | Yes | Retrieve individual bid record. |
| `/api/auctions/{auctionId}/bids` | `GET` | Public | No | Public transparent bid history. |
| `/api/auctions/{auctionId}/highest`| `GET` | Public | No | Current highest bid query. |
| `/api/payments` | `POST` | `BUYER`, `WINNER` | Yes | Only the declared auction winner can process payment. Amount must match winning bid exactly. |
| `/api/payments/{id}` | `GET` | `WINNER`, `SELLER`, `ADMIN` | Yes | Restricted to payment winner, auction seller, or platform admin. |
| `/api/payments/auction/{auctionId}`| `GET` | `WINNER`, `SELLER`, `ADMIN` | Yes | Restricted to payment winner, auction seller, or platform admin. |
| `/api/v1/payments/wallet` | `GET` | Authenticated | Yes | User can only retrieve their own wallet balance. |
| `/api/v1/payments/wallet/{userId}` | `GET` | `ADMIN`, Self | Yes | Restricted to wallet owner or platform administrator. |
| `/api/v1/payments/wallet/deposit` | `POST` | Authenticated | Yes | Deposit funds into authenticated user's wallet. |
| `/api/v1/payments/settle` | `POST` | Internal / Service | Internal | Mutex locked, idempotent clearance settlement between buyer & seller. |

---

## 3. JWT Security & Token Governance

1. **HMAC-SHA256 Cryptographic Signature Validation**:
   - Every token is cryptographically signed using a 256-bit secret key.
   - Any signature tampering immediately raises a `SignatureException` and results in HTTP 401 Unauthorized at API Gateway or HTTP 403 at individual microservices.
2. **Expiration Enforcement**:
   - Tokens carry an explicit `exp` timestamp. Expired tokens are rejected (`ExpiredJwtException`).
3. **Malformed Token Rejection**:
   - Irregular payloads, altered headers, or malformed strings throw `MalformedJwtException` and are rejected prior to business logic execution.
4. **Missing Token Rejection**:
   - Gateway route validator enforces that any request to secured paths without a valid `Authorization: Bearer <token>` header is rejected with HTTP 401.
5. **No JWT Logging**:
   - Raw tokens are never logged. Loggers record only the validation outcome and sanitized reason (e.g., `Token expired`, `Signature mismatch`).
6. **Environment-Based JWT Secret**:
   - The secret key is externalized using the `${JWT_SECRET}` environment variable with default fallback for development only. Production deployments supply strong random secrets via Kubernetes secrets or cloud vaults.

---

## 4. Password Security Policy

1. **Adaptive BCrypt Hashing**:
   - User passwords are automatically salted and hashed using `BCryptPasswordEncoder` with default work factor (strength 10).
2. **Zero Plaintext Persistence**:
   - Plaintext passwords are never persisted to the database under any circumstance.
3. **Zero Plaintext Logging**:
   - Request logs and exception traces exclude plaintext credentials. Login attempts log only sanitized progress (e.g., `Processing user login attempt`).

---

## 5. Authorization & Spoofing Defense

1. **Auction Ownership Protection**:
   - `AuctionServiceImpl.verifyOwnershipOrAdmin` strictly enforces that `userId != null` and `auction.sellerId.equals(userId)` unless the caller holds `ROLE_ADMIN`.
   - Modifying, deleting, activating, or canceling an auction belonging to another user is rejected with HTTP 403 Forbidden.
2. **Bidder Impersonation Prevention**:
   - `PlaceBidRequest` strictly omits `bidderId` from client-provided JSON payloads.
   - Downstream `bidding-service` extracts bidder identity directly from the cryptographically verified JWT token claims (`claims.get("userId")`).
   - Self-bidding is strictly prevented: sellers attempting to bid on their own auctions are rejected with HTTP 400 Bad Request (`SellerCannotBidException`).
3. **Header Spoofing Prevention at API Gateway**:
   - External clients cannot forge `X-User-Id`, `X-User-Email`, or `X-User-Role` headers.
   - `AuthenticationFilter` strips all incoming `X-User-*` headers from untrusted clients before processing.
   - Valid JWT claims are injected into downstream request headers only after token validation succeeds.
4. **Payment & Wallet Privacy**:
   - Payment records and wallet balances are restricted. Users cannot inspect other users' payment records or balances. Only auction winners, sellers, and administrators can query payment endpoints.

---

## 6. Distributed Reliability & Fault Tolerance

1. **Feign Timeouts**:
   - Inter-service Feign clients are configured with explicit 5000ms connect and read timeouts:
     ```yaml
     spring:
       cloud:
         openfeign:
           client:
             config:
               default:
                 connect-timeout: 5000
                 read-timeout: 5000
     ```
2. **Service Unavailable Handling**:
   - When downstream services are unreachable or time out, custom error decoders catch Feign exceptions and map them to HTTP 503 Service Unavailable or fallback execution without crashing the caller.
3. **Concurrency Control & Idempotency**:
   - Distributed mutex locks (`ConcurrentHashMap<Long, ReentrantLock>`) guard auction state transitions, concurrent bids, and payment settlements per auction ID.
   - Duplicate payment requests safely return the existing transaction record with identical transaction reference.
4. **Transaction Boundaries**:
   - All state-modifying database operations are wrapped in Spring `@Transactional` boundaries, ensuring atomic all-or-nothing execution.
5. **Standardized Error Handling**:
   - Every service implements a `@RestControllerAdvice` global exception handler producing standardized, sanitized `ErrorResponse` objects with `status`, `error`, `message`, `path`, and `timestamp`.
   - Internal database exceptions and sensitive stack traces are suppressed from client responses.

---

## 7. Cross-Origin Resource Sharing (CORS)

1. **API Gateway CORS**:
   - Global CORS configuration in API Gateway allows specified HTTP methods (`GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`) and headers.
2. **Auth Service CORS**:
   - `SecurityConfig` in `auth-service` configures a `CorsConfigurationSource` bean to allow controlled origin headers and credentialed preflight requests.
