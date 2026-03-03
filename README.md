# ScaleMart: Fullstack Java (Scalability & Performance Focus)

Production-style starter project built for interview-ready scalability discussions:

- Stateless Spring Boot APIs with JWT auth
- Horizontal scaling with Nginx load balancing (`api-1`, `api-2`)
- Redis caching and distributed rate limiting
- PostgreSQL with explicit indexing
- Async order pipeline with Kafka + worker service
- Prometheus + Grafana monitoring
- k6 load test script with P95 threshold

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              CLIENTS                                        │
├─────────────────────────────────────────────────────────────────────────────┤
│  Browser (React via Nginx) Prometheus             Grafana                  │
│  http://localhost:8080      http://localhost:9090  http://localhost:3000  │
└──────────┬──────────────────┬──────────────────────┬───────────────────────┘
           │                  │                      │
           │                  │ Scrapes Metrics       │ Dashboards
           ▼                  ▼                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           NGINX LOAD BALANCER                                │
│                        http://localhost:8080                                │
│                    ┌──────────────┬──────────────┐                          │
│                    │   api-1:8080  │   api-2:8080 │                          │
└────────────────────┴──────┬───────┴──────┬───────┴──────────────────────────┘
                            │              │
         ┌──────────────────┼──────────────┼──────────────────┐
         │                  │              │                  │
         ▼                  ▼              ▼                  ▼
┌─────────────────┐ ┌──────────────┐ ┌──────────────┐ ┌─────────────────────┐
│  SPRING BOOT   │ │  SPRING BOOT │ │    REDIS    │ │   POSTGRESQL       │
│    API-1       │ │    API-2     │ │   (Cache)   │ │   (Database)        │
│                │ │              │ │  Port 6379  │ │   Port 5432        │
│  - Auth        │ │  - Auth     │ │             │ │                     │
│  - Products    │ │  - Products │ │  Caching    │ │  - Users            │
│  - Orders      │ │  - Orders   │ │  Rate Limit │ │  - Products         │
│  - Cart        │ │  - Cart     │ │             │ │  - Orders           │
└───────┬────────┘ └──────┬───────┘ └──────────────┘ └─────────────────────┘
        │                 │
        │   Publishes     │
        │   Events        │
        ▼                 ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          REDPANDA (KAFKA)                                    │
│                         Port 9092                                            │
│   ┌──────────────────────────────┐                                        │
│   │      order.created topic      │  ← Async order processing             │
│   └──────────────────────────────┘                                        │
└────────────────────────────┬───────────────────────────────────────────────┘
                             │ Consumes
                             ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                       WORKER SERVICE                                        │
│   - Processes order events                                                  │
│   - Updates order status                                                   │
│   - Event-driven architecture                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Stack

- Java 21, Spring Boot 3
- PostgreSQL 16
- Redis 7
- Redpanda (Kafka API compatible)
- React + Vite
- Prometheus + Grafana
- Nginx
- k6 (load testing)

## Quick Start

1. Start all services:

```
powershell
.\start.ps1
```

Windows CMD alternative:

```
start.bat
```

Use from another device on same Wi-Fi/LAN:

- The script prints a LAN URL like `http://192.168.x.x:8080`

Use from different internet networks:

```
powershell
.\start.ps1 -SharePublic
```

- Keep the tunnel terminal open and share the generated URL.

2. Open services:

- App + API Gateway: `http://localhost:8080`
- Grafana: `http://localhost:3000` (`admin` / `admin`)

3. Optional load test:

```
powershell
docker compose --profile loadtest run --rm k6
```

## Interview-Ready Talking Points

- **Stateless auth**: no in-memory session, JWT + any-node routing
- **Cache strategy**: Redis-backed `@Cacheable` for hot product reads
- **DB performance**: indexes on high-cardinality/frequently queried columns
- **Reliability**: LB + multiple API instances prevents single-node collapse
- **Async processing**: Kafka-based event-driven order workflow
- **SLO framing**: k6 script enforces `p95 < 200ms` threshold

## APIs

- `POST /api/v1/auth/login`
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/verify-email`
- `POST /api/v1/auth/resend-verification`
- `POST /api/v1/auth/forgot-password`
- `POST /api/v1/auth/reset-password`
- `POST /api/v1/auth/change-password` (AUTH)
- `GET /api/v1/products/{id}`
- `GET /api/v1/products/trending`
- `POST /api/v1/orders`
- `GET /api/v1/orders` (history)
- `GET /api/v1/orders/status/{status}`
- `GET /api/v1/admin/users` (ADMIN)
- `PATCH /api/v1/admin/users/{id}/enabled` (ADMIN)
- `PATCH /api/v1/admin/users/{id}/role` (ADMIN)
- `GET /api/v1/admin/audit-logs?page=0&size=25&actor=&target=&action=&from=&to=` (ADMIN, paginated/filterable)
- `GET /api/v1/admin/audit-logs/export?limit=500&actor=&target=&action=&from=&to=` (ADMIN, CSV export)

## Demo Credentials

- Username: `demo@scalemart.dev`
- Password: `DemoPass123!`
- Admin Username: `admin@scalemart.dev`
- Admin Password: `AdminPass123!`

These accounts are seeded only when `APP_SEED_DEMO_USERS=true` (enabled in local `docker-compose.yml` and disabled in production examples).

You can also create new users from the web UI using the **Register** button.

## Advanced Features Added

- Web signup flow with verification-first login (`/auth/register`)
- Email verification workflow (`/auth/register` + `/auth/verify-email`)
- Resend verification token support (`/auth/resend-verification`)
- Password recovery (`/auth/forgot-password` + `/auth/reset-password`)
- Authenticated password change (`/auth/change-password`)
- Order history panel with status filtering (`ALL/PENDING/PROCESSING/COMPLETED/FAILED`)
- Idempotency key generation surfaced in UI for safer retries
- Admin user management panel (for ADMIN):
  - user search + filters
  - enable/disable user
  - promote/demote role
- Admin audit log panel:
  - actor/target/action/date filters
  - pagination
  - CSV export
  - lists recent admin actions
  - actor, target, action, details, timestamp
- Guardrails:
  - cannot disable your own logged-in account
  - cannot remove your own ADMIN role
  - cannot disable/demote the last enabled admin account

For local development, auth endpoints can return debug verification/reset tokens.
Disable this for public deployment with:

```
powershell
$env:APP_AUTH_EXPOSE_DEBUG_TOKENS="false"
```

## SMTP Setup (Real Email)

Set these env vars before `docker compose up --build -d`:

```
powershell
$env:APP_MAIL_ENABLED="true"
$env:APP_MAIL_FROM="no-reply@your-domain.com"
$env:APP_MAIL_BASE_URL="https://your-domain.com"
$env:SPRING_MAIL_HOST="smtp.your-provider.com"
$env:SPRING_MAIL_PORT="587"
$env:SPRING_MAIL_USERNAME="your-smtp-user"
$env:SPRING_MAIL_PASSWORD="your-smtp-password"
$env:SPRING_MAIL_SMTP_AUTH="true"
$env:SPRING_MAIL_SMTP_STARTTLS_ENABLE="true"
$env:APP_AUTH_EXPOSE_DEBUG_TOKENS="false"
```

## Public Deployment

1. Copy env template and set real values:

```
powershell
Copy-Item .env.public.example .env
```

2. Or set environment variables directly (example):

```
powershell
$env:APP_CORS_ALLOWED_ORIGINS="https://your-domain.com"
$env:VITE_API_BASE="/api/v1"
$env:JWT_SECRET="<long-random-base64-secret>"
$env:APP_AUTH_EXPOSE_DEBUG_TOKENS="false"
$env:APP_SEED_DEMO_USERS="false"
```

3. Build and start:

```
powershell
docker compose -f docker-compose.yml -f docker-compose.prod.yml up --build -d
```

4. Publish `443` behind your cloud load balancer/reverse proxy (HTTPS), and point DNS to that host.

## Key Features Demonstrated

| Feature | Technology | Interview Value |
|---------|------------|-----------------|
| Authentication | JWT + Refresh Tokens | Stateless, scalable auth |
| Caching | Redis | Hot path optimization |
| Load Balancing | Nginx | Horizontal scaling |
| Async Processing | Kafka/Redpanda | Event-driven architecture |
| Database | PostgreSQL + Indexes | Query optimization |
| Monitoring | Prometheus + Grafana | Observability |
| Rate Limiting | Redis | DoS protection |
| Load Testing | k6 | Performance validation |
