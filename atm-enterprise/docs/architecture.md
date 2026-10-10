# ATM Enterprise — Architecture

## System context

```
                          ┌───────────────────────────┐
                          │  ATM hardware (or browser) │
                          └─────────────┬─────────────┘
                                        │ HTTPS
                          ┌─────────────▼─────────────┐
                          │  Ingress (nginx, atm.local)│
                          └─────────────┬─────────────┘
                                        │
                          ┌─────────────▼─────────────┐
                          │   API Gateway  (Spring    │
                          │   Cloud Gateway, :8080)    │
                          │   • JWT verification       │
                          │   • Rate limit / CORS     │
                          │   • Routing              │
                          └─────────────┬─────────────┘
                                        │
   ┌──────────────┬────────────────────┬┴───────────────────┬──────────────────┐
   │              │                    │                    │                  │
┌──▼─────────┐  ┌─▼──────────────┐  ┌──▼──────────────┐  ┌─▼──────────────┐ ┌▼─────────────┐
│ auth-svc   │  │ account-svc    │  │ transaction-svc │  │ card-svc       │ │ frontend     │
│ :8081      │  │ :8082          │  │ :8083           │  │ :8084          │ │ (React + Nginx)│
│            │  │                 │  │                  │  │                 │ │ :80           │
│ • PIN verify│  │ • balance       │  │ • withdraw       │  │ • card CRUD     │ └──────────────┘
│ • JWT issue │  │ • deposit       │  │ • transfer       │  │ • limit check   │
│ • change PIN│  │ • mini-stmt     │  │ • event publish  │  │ • block / unblock│
└────────────┘  └────────────────┘  └─────────┬────────┘  └─────────────────┘
                                              │
                                       ┌──────▼──────┐
                                       │  Kafka :9092│
                                       │  topics:    │
                                       │  • atm.tx   │
                                       │  • atm.audit│
                                       └──────┬──────┘
                                              │
   ┌─────────────┬──────────────────────┬────┴──────────────────────────┐
   │             │                      │                                │
┌──▼──────────┐  │                      │                                │
│ auth-svc    │  │  account-svc         │ transaction-svc                 │ card-svc
│ writes      │  │  reads + writes      │ reads + writes                 │ reads + writes
│ audit_logs  │  │ accounts             │ transactions, card_limits      │ cards, card_limits
└──────┬──────┘  │  account_audit       │ account_audit                  │
       │         │                      │                                │
       │         │                      │                                │
       └─────────┴─────────────────────┴────────────────────────────────┘
                                       │
                              ┌────────▼────────┐
                              │  PostgreSQL 16  │
                              │  db: atmdb      │
                              │  schema: atm   │
                              └─────────────────┘
```

## Service responsibilities

### auth-service (`:8081`)
- Stateless PIN verification service. Cards are looked up by `card_number`,
  the BCrypt `pin_hash` is compared against the supplied PIN.
- Issues JWTs (HS256) bound to a card / account, with a 1-hour expiry
  (configurable via `JWT_EXPIRATION_MS`).
- Tracks failed PIN attempts and increments `cards.failed_attempts`;
  after `PIN_MAX_ATTEMPTS` (3) the card is set to `BLOCKED`.
- Writes `audit_logs` rows for every PIN verification (success or failure).

### account-service (`:8082`)
- Owner of `accounts` table: balance reads, deposits, mini-statement
  (last N transactions for the account).
- All writes happen inside a DB transaction; `account_audit` is updated
  synchronously so audit and balance stay consistent.

### transaction-service (`:8083`)
- The money-movement service: `withdraw` and `transfer` operations.
- Calls `card-service` (verify card status + limits) and `account-service`
  (verify balance) before mutating anything.
- Writes the immutable `transactions` row, then publishes a
  `TransactionCompleted` (or `TransactionFailed`) event to the
  `atm.transactions` Kafka topic.
- Updates `card_limits.daily_spent` / `.monthly_spent` so subsequent
  operations can be pre-checked.

### card-service (`:8084`)
- CRUD over `cards` and `card_limits`.
- Computes remaining daily/monthly headroom; supports `block` /
  `unblock` card (sets `status`, `blocked_at`, `blocked_reason`).
- Reads are eventually-consistent from `cards`; writes are protected by
  optimistic locking on `updated_at`.

### apigateway (`:8080`)
- Single public entrypoint for `/api/*`.
- Validates JWT (shared HMAC secret) and forwards claims as headers
  (`X-Card-Number`, `X-Account-Number`) to downstream services.
- Applies per-route rate limiting (Bucket4j) and CORS.

### frontend (`:80`)
- React 18 + Vite SPA, served by Nginx.
- Calls the API gateway only; no service is directly addressed from the
  browser.

## Withdrawal data flow

1. Client → `POST /api/transactions/withdraw { cardNumber, pin, amount }`.
2. **apigateway** — verifies JWT; if missing, forwards to
   `auth-service` for PIN verification and a short-lived token.
3. **transaction-service** receives the request:
   a. calls **card-service** `GET /api/cards/{cardNumber}` to fetch card
      status, daily/monthly limit, and current `card_limits` spend;
   b. asserts `card.status == ACTIVE` and
      `daily_spent + amount <= daily_limit` and similarly for monthly;
   c. calls **account-service** `GET /api/accounts/{accountNumber}` to
      fetch `balance_before`;
   d. asserts `balance_before >= amount`.
4. **transaction-service** opens a DB transaction and:
   - inserts a `transactions` row with `status=PENDING`,
     `balance_before`, `balance_after = balance_before - amount`;
   - updates `accounts.balance = balance_after`;
   - updates `card_limits.daily_spent += amount`,
     `card_limits.monthly_spent += amount`;
   - inserts an `account_audit` row;
   - flips `transactions.status = SUCCESS`.
5. Commits, publishes `TransactionCompleted` event to `atm.transactions`.
6. Returns `{ referenceNumber, balanceAfter, status }` to the client.

If any assertion in step 3 fails, `transaction-service` inserts a
`transactions` row with `status=FAILED` + `remark`, publishes a
`TransactionFailed` event, and returns 4xx to the client without
touching `accounts.balance`.

## Non-functional properties

- **Horizontal scaling**: every service runs as a `Deployment` (2 replicas)
  fronted by a `ClusterIP` service, with an HPA targeting 70 % CPU up to
  10 replicas.
- **Statefulness**: only Postgres is stateful — a `StatefulSet` with a 5 Gi
  PVC and `pg_isready` readiness probes.
- **Security**: PINs never stored in plaintext (BCrypt cost-10 hashes),
  JWTs signed with a 256-bit secret mounted from a K8s Secret.
- **Audit**: every card-scoped action lands in `audit_logs`; every
  account-scoped money movement lands in `account_audit`; high-level
  events are also published to Kafka for downstream SIEM ingestion.
- **Idempotency**: clients may pass a `X-Idempotency-Key` header so a
  retried withdrawal / transfer with the same key returns the original
  `transactions.reference_number` instead of duplicating the operation.
