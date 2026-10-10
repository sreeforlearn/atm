# ATM Enterprise — API Contract

All endpoints are exposed via the **apigateway** at `/api/*`. The gateway
validates the JWT (HS256, 1-hour expiry) for every non-login route and
forwards the verified claims (`X-Card-Number`, `X-Account-Number`)
as headers to downstream services.

- Base URL (local): `http://localhost:8080`
- Base URL (K8s):    `http://atm.local`
- Content-Type:     `application/json`
- Auth:              `Authorization: Bearer <jwt>` (omitted on login routes)
- Idempotency:       money-movement routes accept an optional
                     `X-Idempotency-Key: <uuid>` header

## Authentication

| Method | Path                       | Service       | Auth | Description                                                |
|--------|----------------------------|---------------|------|-------------------------------------------------------------|
| POST   | `/api/auth/login`          | auth-service  | -    | Exchange `{cardNumber, pin}` for a JWT                     |
| POST   | `/api/auth/verify-pin`     | auth-service  | JWT | Internal-only: confirm card+pin (used by other services)    |
| POST   | `/api/auth/refresh`        | auth-service  | JWT | Issue a new JWT before the current one expires             |
| POST   | `/api/auth/change-pin`     | auth-service  | JWT | Body `{oldPin, newPin}`; validates old, writes new BCrypt  |
| POST   | `/api/auth/logout`         | auth-service  | JWT | Blacklist the JWT until natural expiry (best-effort)       |
| GET    | `/api/auth/audit/{card}`   | auth-service  | JWT | Read recent `audit_logs` entries for the card              |

### Sample — login

```http
POST /api/auth/login
Content-Type: application/json

{
  "cardNumber": "4111111111111111",
  "pin": "1234"
}
```

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresIn": 3600,
  "cardNumber": "4111111111111111",
  "accountNumber": "ACCT1000000001"
}
```

## Card

| Method | Path                                   | Service      | Auth | Description                                              |
|--------|----------------------------------------|--------------|------|-----------------------------------------------------------|
| GET    | `/api/cards/{cardNumber}`              | card-service | JWT | Card details + limit headroom                            |
| POST   | `/api/cards`                           | card-service | JWT* | Provision a new card (admin only)                        |
| PATCH  | `/api/cards/{cardNumber}`              | card-service | JWT* | Update card (status / limits / holder) — admin only      |
| POST   | `/api/cards/{cardNumber}/block`        | card-service | JWT | Block a card (body `{reason}`)                           |
| POST   | `/api/cards/{cardNumber}/unblock`      | card-service | JWT*| Unblock a card — admin only                              |
| GET    | `/api/cards/{cardNumber}/limits`       | card-service | JWT | Current `daily_spent`, `monthly_spent`, remaining headroom|
| POST   | `/api/cards/{cardNumber}/limits/reset` | card-service | JWT*| Reset daily/monthly counters (admin / scheduled job)      |

`*` = admin-scoped JWT (claim `role=admin`) required.

## Account

| Method | Path                                                       | Service          | Auth | Description                                |
|--------|------------------------------------------------------------|------------------|------|--------------------------------------------|
| GET    | `/api/accounts/{accountNumber}`                            | account-service  | JWT | Fetch account + balance                   |
| GET    | `/api/accounts/{accountNumber}/statement?limit=20`        | account-service  | JWT | Mini-statement (last N transactions)      |
| GET    | `/api/accounts/{accountNumber}/audit?limit=50`            | account-service  | JWT | Read `account_audit` rows                 |
| POST   | `/api/accounts/deposit`                                    | account-service  | JWT | Deposit cash (body `{accountNumber, amount, remark}`) |
| POST   | `/api/accounts/{accountNumber}/block`                      | account-service  | JWT*| Freeze account (no debits allowed)        |
| POST   | `/api/accounts/{accountNumber}/unblock`                    | account-service  | JWT*| Unfreeze account                          |

## Transaction

| Method | Path                                       | Service              | Auth | Description                                        |
|--------|--------------------------------------------|----------------------|------|-----------------------------------------------------|
| POST   | `/api/transactions/withdraw`               | transaction-service  | JWT | Body `{cardNumber, pin, amount, remark?}`           |
| POST   | `/api/transactions/transfer`               | transaction-service  | JWT | Body `{fromAccount, toAccount, amount, pin, remark?}`|
| GET    | `/api/transactions/{reference}`            | transaction-service  | JWT | Look up a single transaction by `reference_number` |
| GET    | `/api/transactions?account=...&from=...&to=...` | transaction-service | JWT | Paginated history filtered by account / date range  |

### Sample — withdraw

```http
POST /api/transactions/withdraw
Authorization: Bearer eyJ...
Content-Type: application/json
X-Idempotency-Key: 4d2c1b7e-...

{
  "cardNumber": "4111111111111111",
  "pin": "1234",
  "amount": 2000.00,
  "remark": "ATM cash withdrawal"
}
```

```json
{
  "referenceNumber": "8d9a...uuid",
  "status": "SUCCESS",
  "amount": 2000.00,
  "balanceBefore": 50000.00,
  "balanceAfter": 48000.00,
  "timestamp": "2025-10-10T12:34:56Z"
}
```

### Sample — transfer

```http
POST /api/transactions/transfer
Authorization: Bearer eyJ...
Content-Type: application/json
X-Idempotency-Key: 1f6e8b3a-...

{
  "fromAccount": "ACCT1000000001",
  "toAccount":   "ACCT2000000002",
  "amount":      1500.00,
  "pin":         "1234",
  "remark":      "Rent share"
}
```

## Audit

| Method | Path                                 | Service              | Auth | Description                                |
|--------|--------------------------------------|----------------------|------|---------------------------------------------|
| GET    | `/api/audit/cards/{cardNumber}`      | auth-service          | JWT* | Card-scoped `audit_logs` (admin)           |
| GET    | `/api/audit/accounts/{accountNumber}`| account-service       | JWT* | Account-scoped `account_audit` (admin)     |
| GET    | `/api/audit/transactions?from=...`   | transaction-service   | JWT* | Cross-card transaction search (admin)      |

## Health & ops (apigateway)

| Method | Path                          | Service    | Auth | Description                              |
|--------|-------------------------------|------------|------|-------------------------------------------|
| GET    | `/actuator/health`             | any        | -    | Liveness + readiness composite            |
| GET    | `/actuator/health/liveness`    | any        | -    | K8s liveness probe target                 |
| GET    | `/actuator/health/readiness`  | any        | -    | K8s readiness probe target               |
| GET    | `/actuator/info`               | any        | -    | Build metadata                           |
| GET    | `/actuator/metrics`            | any        | -    | Micrometer metric list                   |
| GET    | `/actuator/prometheus`         | any        | -    | Prometheus exposition format              |

## Error envelope

Every error response is a single JSON object:

```json
{
  "timestamp": "2025-10-10T12:34:56Z",
  "status": 400,
  "error": "BAD_REQUEST",
  "message": "Amount exceeds daily card limit",
  "path": "/api/transactions/withdraw",
  "reference": "8d9a...uuid"
}
```

Common statuses:

| HTTP | Meaning                                         |
|------|--------------------------------------------------|
| 200  | OK                                               |
| 201  | Created (deposit / withdraw / transfer recorded) |
| 400  | Validation / business-rule failure                |
| 401  | Missing / invalid / expired JWT                  |
| 403  | Authenticated but not authorized (admin route)   |
| 404  | Resource not found                               |
| 409  | Idempotency-key conflict                          |
| 422  | Card blocked / account frozen                    |
| 429  | Rate limited by apigateway                        |
| 500  | Unexpected server error (logged with `reference`)|
