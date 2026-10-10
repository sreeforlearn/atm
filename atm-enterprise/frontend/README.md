# ATM Enterprise — Frontend

A modern, customer-facing ATM web UI built with **React 18 + TypeScript + Vite + Tailwind CSS**.

## Tech stack

| Concern         | Library                                |
| --------------- | -------------------------------------- |
| Framework       | React 18 + TypeScript                  |
| Build tool      | Vite 5                                 |
| Styling         | Tailwind CSS 3                         |
| Routing         | React Router v6                        |
| HTTP            | Axios (with interceptors)              |
| Client state    | Zustand (persisted)                    |
| Server state    | @tanstack/react-query                  |
| Notifications   | react-toastify                         |
| Icons           | lucide-react                           |
| Charts          | recharts                               |

## Project layout

```
frontend/
├── index.html
├── vite.config.ts
├── tailwind.config.js
├── Dockerfile
├── nginx.conf
└── src/
    ├── main.tsx
    ├── App.tsx
    ├── styles/index.css
    ├── types/index.ts
    ├── store/authStore.ts
    ├── services/
    │   ├── api.ts
    │   ├── auth.service.ts
    │   ├── account.service.ts
    │   ├── transaction.service.ts
    │   └── card.service.ts
    ├── hooks/
    │   ├── useAuth.ts
    │   └── useTransactions.ts
    ├── utils/
    │   ├── format.ts
    │   └── validation.ts
    ├── components/
    │   ├── Layout/  (DashboardLayout, Sidebar, Topbar, ProtectedRoute)
    │   ├── ui/      (Button, Card, Input, Modal, Loader, BalanceCard)
    │   └── Transaction/ (TransactionTable, TransactionForm)
    └── pages/
        ├── Login.tsx
        ├── Dashboard.tsx
        ├── Balance.tsx
        ├── Withdraw.tsx
        ├── Deposit.tsx
        ├── Transfer.tsx
        ├── Transactions.tsx
        ├── PinChange.tsx
        └── NotFound.tsx
```

## Local development

```bash
# 1. Install deps
npm install

# 2. Start dev server (port 5173)
npm run dev
```

The dev server proxies `/api/*` to `http://localhost:8080` (API Gateway).

### Demo credentials

| Card number | PIN  |
| ----------- | --- |
| 4111111111111111 | 1234 |
| 4242424242424242 | 9999 |

## Production build

```bash
npm run build      # outputs to dist/
npm run preview    # preview built bundle locally
```

## Docker

```bash
docker build -t atm-frontend:latest .
docker run --rm -p 8080:80 atm-frontend:latest
# UI available at http://localhost:8080
```

The container ships an nginx config that:
- serves the SPA with history fallback,
- proxies `/api/` to the `apigateway:8080` service in the docker network.

## Scripts

| Script          | Purpose                              |
| --------------- | ------------------------------------ |
| `npm run dev`   | Vite dev server (HMR)                 |
| `npm run build` | Type-check + production build         |
| `npm run preview` | Preview production build           |
| `npm run lint`  | ESLint over `src/`                   |
