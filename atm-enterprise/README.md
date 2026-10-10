# ATM Enterprise

A reference implementation of an **enterprise-grade ATM system** built as a
microservices architecture: 5 Spring Boot services behind an API gateway,
a React SPA, Postgres 16, Kafka, all containerized and deployable to
Kubernetes in a single `kubectl apply` sweep.

---

## Table of contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Tech stack](#tech-stack)
4. [Project structure](#project-structure)
5. [Prerequisites](#prerequisites)
6. [Quick start (Docker Compose)](#quick-start-docker-compose)
7. [Local dev with hot-reload](#local-dev-with-hot-reload)
8. [Database](#database)
9. [Kafka](#kafka)
10. [Kubernetes deployment](#kubernetes-deployment)
11. [CI/CD](#cicd)
12. [API endpoints](#api-endpoints)
13. [Demo credentials](#demo-credentials)
14. [Configuration](#configuration)
15. [Observability](#observability)
16. [Troubleshooting](#troubleshooting)
17. [Contributing](#contributing)
18. [License](#license)

---

## Overview

ATM Enterprise simulates the back-end of a retail bank's ATM network.
Customers authenticate with a card number + PIN, then can:

- check balance
- withdraw cash (subject to daily/monthly limits)
- deposit cash
- transfer funds to another account
- view mini-statement / transaction history
- change PIN

The system is split into five independently deployable services, fronted by
a single Spring Cloud Gateway that handles JWT validation, rate-limiting,
and request routing.

| Service               | Port | Responsibility                                              |
|-----------------------|------|-------------------------------------------------------------|
| `auth-service`        | 8081 | PIN verification, JWT issuance, login-attempt audit         |
| `account-service`     | 8082 | Balance, deposit, mini-statement, account status            |
| `transaction-service` | 8083 | Withdraw, transfer, ledger writes, Kafka event producer    |
| `card-service`        | 8084 | Card CRUD, daily/monthly limit checks, block / unblock     |
| `apigateway`           | 8080 | Edge router, JWT verification, rate limit, CORS            |
| `frontend`            | 80   | React SPA served by Nginx                                    |

All state lives in PostgreSQL (`atmdb` database, `atm` schema). Cross-service
events (every withdrawal, deposit, transfer) are published to Kafka topics
`atm.transactions` and `atm.audit` for downstream consumers / audit fan-out.

---

## Architecture

```
                            +-----------------+
                            |   ATM hardware  |
                            |  (or web SPA)   |
                            +--------+--------+
                                     |
                          https://atm.local
                                     |
                       +-------------+-------------+
                       |    nginx Ingress (K8s)    |
                       +-------------+-------------+
                                     |
                       +-------------+-------------+
                       |   apigateway  (Spring    |
                       |   Cloud Gateway, :8080)   |
                       +-------------+-------------+
                                     |
        +---------------+------------+-------------+----------------+
        |               |            |             |                |
+-------v------+ +-------v------+ +---v---------+ +--v---------+ +--v---------+
| auth-service| | account-svc  | | transaction | | card-svc  | | frontend  |
|  :8081       | | :8082        | | -service    | | :8084      | | (React)   |
| JWT, PIN     | | balance,     | | :8083       | | limits,   | | Nginx :80 |
| verify       | | deposit      | | withdraw,   | | block     | +-----------+
|              | | mini-stmt     | | transfer    | |           |
+------+-------+ +-------+------+ +------+-------+ +-----+-----+
       |                 |              |                |
       |                 |              v                |
       |                 |       +------+------+         |
       |                 |       |   Kafka :9092|        |
       |                 |       | (txn, audit |        |
       |                 |       |   topics)   |        |
       |                 |       +-------------+        |
       |                 |              |              |
       +--------+--------+---+----------+--------------+
                |              |
            +---v--------------v----+
            |  PostgreSQL 16 (atmdb)|
            |  schema: atm          |
            +-----------------------+
```

A **withdrawal** end-to-end:

1. Client POSTs to `/api/transactions/withdraw` with `{cardNumber, pin, amount}`.
2. `apigateway` verifies JWT (or short-circuits an unauthenticated flow for
   login routes).
3. `transaction-service` calls `card-service` to verify PIN and check the
   card is `ACTIVE` with available daily/monthly limit headroom.
4. `transaction-service` calls `account-service` to read `balance_before`
   and assert sufficient funds.
5. Within a single DB transaction: insert into `transactions`, decrement
   `accounts.balance`, increment `card_limits.daily_spent` / `.monthly_spent`.
6. Publish `TransactionCompleted` event to `atm.transactions`.
7. Insert into `account_audit` (denormalized audit trail).
8. Return the new balance + reference number to the client.

---

## Tech stack

| Layer        | Technology                                              |
|--------------|---------------------------------------------------------|
| Backend      | Java 17, Spring Boot 3.x, Spring Cloud Gateway         |
| Frontend     | React 18, Vite 5, TypeScript 5, Tailwind CSS           |
| Database     | PostgreSQL 16 (alpine image)                            |
| Messaging    | Apache Kafka 3.7 (KRaft mode, bitnami image)            |
| Container    | Docker, docker-compose 3.9                             |
| Orchestrator | Kubernetes 1.28+ (tested on kind, minikube, k3s)        |
| CI/CD        | GitHub Actions (per-service), Jenkins (full pipeline)   |
| Security     | JWT (HS256), BCrypt PIN hashing, CodeQL scanning         |
| Observability| Spring Actuator, Micrometer/Prometheus endpoints        |

---

## Project structure

```
atm-enterprise/
├── README.md                  (this file)
├── LICENSE                    (MIT)
├── docker-compose.yml         (local stack)
├── docker-compose.dev.yml     (hot-reload override)
├── .env.example
├── .gitignore
│
├── database/
│   └── init/
│       ├── 01_create_schema.sql
│       ├── 02_create_tables.sql
│       └── 03_seed_data.sql
│
├── k8s/
│   ├── namespaces/atm-namespace.yaml
│   ├── config/{configmap.yaml, secrets.yaml}
│   ├── postgres/{statefulset.yaml, service.yaml, init-configmap.yaml}
│   ├── auth-service/{deployment.yaml, service.yaml, hpa.yaml}
│   ├── account-service/{deployment.yaml, service.yaml, hpa.yaml}
│   ├── transaction-service/{deployment.yaml, service.yaml, hpa.yaml}
│   ├── card-service/{deployment.yaml, service.yaml, hpa.yaml}
│   ├── apigateway/{deployment.yaml, service.yaml, hpa.yaml}
│   ├── frontend/{deployment.yaml, service.yaml}
│   ├── ingress/ingress.yaml
│   └── kafka.yaml              (Kafka + Zookeeper, single file)
│
├── cicd/
│   ├── github-actions/{auth,account,transaction,card,apigateway,
│   │                    frontend,codeql}.yml
│   └── jenkins/{Jenkinsfile, Jenkinsfile.pr, jenkins-casc.yaml, README.md}
│
├── services/                  (per-service Maven modules)
│   ├── auth-service/
│   ├── account-service/
│   ├── transaction-service/
│   ├── card-service/
│   └── apigateway/
│
├── frontend/                  (React SPA)
│
└── docs/
    ├── architecture.md
    └── api-contract.md
```

---

## Prerequisites

- **JDK 17** — `java -version`
- **Maven 3.9+** — `mvn -v` (or use the included wrapper `./mvnw`)
- **Node 20+** — `node -v` (frontend builds)
- **Docker 24+** — `docker version`
- **docker-compose v2** — `docker compose version`
- **kubectl** — `kubectl version --client`
- **kind / minikube / k3s** — local Kubernetes cluster
- **Helm 3** — optional, for installing the nginx ingress controller

On macOS:
```bash
brew install openjdk@17 maven node docker kubectl helm
brew install --cask docker
brew install minikube
```

On Debian/Ubuntu:
```bash
sudo apt install openjdk-17-jdk maven nodejs npm docker.io docker-compose kubectl
curl -sLo kind "$(curl -s https://kind.sigs.k8s.io/dl/latest.txt)/kind-linux-amd64"
```

---

## Quick start (Docker Compose)

```bash
git clone https://github.com/atm-enterprise/atm-enterprise.git
cd atm-enterprise
cp .env.example .env           # edit if you want
docker compose up --build
```

Wait ~60 s for Postgres to apply the init scripts and Kafka to elect a
controller. Then:

- Frontend → http://localhost
- API gateway → http://localhost:8080
- Postgres → localhost:5432 (user `atm`, pass `atm123`, db `atmdb`)
- Kafka → localhost:9092

Smoke test:
```bash
# health
curl http://localhost:8080/actuator/health

# login as Alice (card / pin 1234)
curl -X POST http://localhost:8080/api/auth/login \
     -H 'Content-Type: application/json' \
     -d '{"cardNumber":"4111111111111111","pin":"1234"}'
```

---

## Local dev with hot-reload

```bash
docker compose -f docker-compose.yml -f docker-compose.dev.yml up
```

- Backend services run `mvn spring-boot:run` with `SPRING_PROFILES_ACTIVE=dev`.
- Remote JVM debug ports exposed: 8001 (auth), 8002 (account), 8003 (txn),
  8004 (card), 8000 (gateway).
- Frontend runs Vite with HMR on http://localhost:5173.
- Source folders are bind-mounted — editing code triggers auto-rebuild.

---

## Database

Schema lives in `database/init/`:

1. `01_create_schema.sql` — creates the `atmdb` database and `atm` schema,
   enables `pgcrypto` (for `gen_random_uuid()`).
2. `02_create_tables.sql` — `cards`, `accounts`, `transactions`,
   `audit_logs`, `account_audit`, `card_limits` + indexes.
3. `03_seed_data.sql` — 3 demo cards, 3 accounts, sample transactions.

Both the Docker image (via `/docker-entrypoint-initdb.d` mount) and the
K8s StatefulSet (via the `postgres-init` ConfigMap) consume these scripts
on first boot. To wipe & re-seed locally:

```bash
docker compose down -v
docker compose up
```

---

## Kafka

The compose stack runs a single Kafka broker in KRaft mode (`broker,controller`).
Topics are auto-created on first produce. The two application topics:

- `atm.transactions` — `TransactionCompleted`, `TransactionFailed`
- `atm.audit` — `AuditEvent` (card-scoped)

Inspect from host:
```bash
docker exec -it atm-kafka kafka-topics.sh --bootstrap-server localhost:9092 --list
docker exec -it atm-kafka kafka-console-consumer.sh \
    --bootstrap-server localhost:9092 --topic atm.transactions --from-beginning
```

---

## Kubernetes deployment

### 1. Start a local cluster

```bash
kind create cluster --name atm-dev
# or
minikube start --cpus=4 --memory=8g
```

### 2. Install nginx ingress controller

```bash
# kind
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml

# minikube
minikube addons enable ingress
```

### 3. Apply manifests in order

```bash
kubectl apply -f k8s/namespaces/
kubectl apply -f k8s/config/
kubectl apply -f k8s/postgres/
kubectl apply -f k8s/kafka.yaml
kubectl apply -f k8s/auth-service/
kubectl apply -f k8s/account-service/
kubectl apply -f k8s/transaction-service/
kubectl apply -f k8s/card-service/
kubectl apply -f k8s/apigateway/
kubectl apply -f k8s/frontend/
kubectl apply -f k8s/ingress/
```

Or one-liner:
```bash
kubectl apply -f k8s/namespaces/ -f k8s/config/ -f k8s/postgres/ \
              -f k8s/kafka.yaml -f k8s/auth-service/ -f k8s/account-service/ \
              -f k8s/transaction-service/ -f k8s/card-service/ -f k8s/apigateway/ \
              -f k8s/frontend/ -f k8s/ingress/
```

### 4. Add host entry

```bash
echo "$(minikube ip) atm.local" | sudo tee -a /etc/hosts
# or
echo "127.0.0.1 atm.local" | sudo tee -a /etc/hosts
kubectl port-forward -n ingress-nginx svc/ingress-nginx-controller 80:80 &
```

Open http://atm.local in the browser.

### 5. Verify

```bash
kubectl -n atm-dev get pods
kubectl -n atm-dev get hpa
kubectl -n atm-dev logs -l app.kubernetes.io/name=auth-service --tail=50
```

Each microservice ships ` HorizontalPodAutoscaler`s: 2–10 replicas,
CPU target 70%. The postgres is a `StatefulSet` with a 5 Gi PVC.

---

## CI/CD

### GitHub Actions (per-service)

| File                                              | Triggers                             |
|---------------------------------------------------|--------------------------------------|
| `.github/workflows/auth-service.yml`              | push/PR on `services/auth-service/**`|
| `.github/workflows/account-service.yml`           | push/PR on `services/account-service/**` |
| `.github/workflows/transaction-service.yml`       | push/PR on `services/transaction-service/**` |
| `.github/workflows/card-service.yml`              | push/PR on `services/card-service/**`|
| `.github/workflows/apigateway.yml`                | push/PR on `services/apigateway/**`  |
| `.github/workflows/frontend.yml`                  | push/PR on `frontend/**`             |
| `.github/workflows/codeql.yml`                    | schedule + push to main              |

Each pipeline runs `build → test → docker-build → docker-push` (push only
on main) and tags images as `<service>:latest` + `<service>:${{ github.sha }}`.

Required repo secrets:
- `DOCKER_USERNAME`
- `DOCKER_PASSWORD`

### Jenkins (full stack)

See [`cicd/jenkins/README.md`](cicd/jenkins/README.md) for setup. The
pipeline builds all five backend services + frontend in parallel, runs
tests in parallel, runs SonarQube analysis, pushes images to Docker Hub,
and finally `kubectl apply` + `rollout status` against the atm-dev cluster.

Required Jenkins credentials (IDs):
- `docker-hub` (username/password)
- `docker-hub-registry` (string, e.g. `docker.io/myuser`)
- `sonar-token`
- `kubeconfig-atm`

PR pipeline (`Jenkinsfile.pr`) only runs build + test on the changed
services, keeping feedback fast.

---

## API endpoints

Full table in [`docs/api-contract.md`](docs/api-contract.md). Highlights:

| Method | Path                                | Service              |
|--------|-------------------------------------|----------------------|
| POST   | `/api/auth/login`                   | auth-service         |
| POST   | `/api/auth/verify-pin`              | auth-service         |
| POST   | `/api/auth/change-pin`              | auth-service         |
| GET    | `/api/accounts/{accountNumber}`     | account-service      |
| POST   | `/api/accounts/deposit`             | account-service      |
| GET    | `/api/accounts/{accountNumber}/statement` | account-service |
| POST   | `/api/transactions/withdraw`        | transaction-service  |
| POST   | `/api/transactions/transfer`        | transaction-service  |
| GET    | `/api/transactions/{reference}`     | transaction-service  |
| GET    | `/api/cards/{cardNumber}`           | card-service         |
| POST   | `/api/cards/{cardNumber}/block`     | card-service         |

---

## Demo credentials

| Card number         | PIN  | Holder       | Balance (INR) |
|---------------------|------|--------------|---------------|
| `4111111111111111` | 1234 | Alice Sharma | 50,000        |
| `4222222222222222` | 1234 | Bob Verma    | 25,000        |
| `4333333333333333` | 1234 | Carol Iyer   | 1,00,000      |

> These PINs are stored as BCrypt hashes in `database/init/03_seed_data.sql`.
> The hashes are publicly known demo values — never reuse them in production.

DB credentials for the dev stack: `atm` / `atm123` (db `atmdb`).

---

## Configuration

### Environment variables

| Variable                  | Default                | Used by                          |
|---------------------------|------------------------|----------------------------------|
| `POSTGRES_USER`           | atm                    | Postgres container               |
| `POSTGRES_PASSWORD`       | atm123                 | Postgres container               |
| `POSTGRES_DB`             | atmdb                  | Postgres container               |
| `JWT_SECRET`              | (see .env.example)     | auth-service, apigateway         |
| `JWT_EXPIRATION_MS`       | 3600000 (1h)           | auth-service                     |
| `KAFKA_BROKERS`           | kafka:9092             | transaction-service              |
| `KAFKA_TOPIC_TRANSACTIONS`| atm.transactions       | transaction-service              |
| `KAFKA_TOPIC_AUDIT`       | atm.audit              | transaction-service              |

### K8s ConfigMap / Secret

`k8s/config/configmap.yaml` exposes the non-sensitive values;
`k8s/config/secrets.yaml` carries `DB_USER`, `DB_PASSWORD`,
`JWT_SECRET` (base64-encoded). Rotate the JWT secret before
going to production:

```bash
JWT=$(openssl rand -base64 32)
kubectl -n atm-dev create secret generic atm-secrets \
    --from-literal=DB_USER=atm \
    --from-literal=DB_PASSWORD=$(openssl rand -base64 24) \
    --from-literal=JWT_SECRET=$JWT \
    --dry-run=client -o yaml | kubectl apply -f -
```

---

## Observability

Every backend service exposes:

- `GET /actuator/health`         — liveness + readiness
- `GET /actuator/health/liveness` — Kubernetes liveness probe target
- `GET /actuator/health/readiness`— Kubernetes readiness probe target
- `GET /actuator/info`           — build metadata
- `GET /actuator/metrics`        — Micrometer metrics
- `GET /actuator/prometheus`     — Prometheus exposition format

Wire these into Prometheus + Grafana (out of scope for this repo, but
the `actuator/prometheus` endpoint is exposed by `atm-config`).

---

## Troubleshooting

| Symptom                                                    | Cause / Fix                                                            |
|------------------------------------------------------------|------------------------------------------------------------------------|
| `docker compose up` hangs building services               | First Maven build downloads the internet; let it finish or pre-build   |
| Postgres init scripts not applied                         | Volume has stale data; `docker compose down -v` then `up`              |
| `connection refused` on port 8080                         | apigateway still booting; `docker compose logs apigateway`             |
| Kafka pod `CrashLoopBackOff` on K8s                        | Increase memory limit or check KRaft quorum env vars                    |
| Ingress returns 503 / 404                                  | Wait for nginx controller + atm-dev pods to be `Running`                |
| `atm.local` does not resolve                              | Add to `/etc/hosts`: `127.0.0.1 atm.local`                            |
| HPA stuck at `unknown` for current CPU                     | Metrics server missing on the cluster: `minikube addons enable metrics-server` |
| Login returns 401 with demo PIN                            | Init scripts didn't run; check postgres logs and re-seed                 |
| `kubectl apply` reports `namespace not found`              | Apply `k8s/namespaces/` first, wait 2 s, then apply the rest             |
| Build fails with `package does not exist`                 | Run `mvn -N install` at repo root once to install the parent POM       |

### Useful one-liners

```bash
# Tail all backend service logs
kubectl -n atm-dev logs -l app.kubernetes.io/component=backend --tail=200

# Shell into Postgres pod
kubectl -n atm-dev exec -it postgres-0 -- psql -U atm atmdb

# Force reschedule a service
kubectl -n atm-dev rollout restart deployment/auth-service

# Watch HPA
kubectl -n atm-dev get hpa -w
```

---

## Contributing

1. Fork the repo, branch off `main`.
2. Match the per-service pre-commit hook (`mvn -B verify -pl services/<svc>`).
3. Open a PR — the GitHub Actions workflow + Jenkins `Jenkinsfile.pr` will run
   automatically.
4. Two approvals required for merge to `main`.
5. Once merged, the per-service workflow pushes the image and Jenkins deploys
   to `atm-dev`.

For larger architectural changes, open an RFC issue first.

---

## License

[MIT](LICENSE) — © 2025 ATM Enterprise Contributors.
