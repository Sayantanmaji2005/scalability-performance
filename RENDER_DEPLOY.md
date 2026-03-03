# Render Deployment Guide

This project includes a Render Blueprint at `render.yaml` for full-stack deployment.

## Quick Deploy (One-Click)

### Option 1: From Render Dashboard

1. **Push to GitHub**: Ensure your code is pushed to a GitHub repository.
2. **Open Render Dashboard**: Go to [dashboard.render.com](https://dashboard.render.com).
3. **Create Blueprint**: Click `New` → `Blueprint`.
4. **Connect Repository**: Select your GitHub repository (`Sayantanmaji2005/scalability-performance`).
5. **Deploy**: Click `Apply Blueprint` - all services will be created automatically.

### Option 2: From GitHub (Recommended)

1. Go to your GitHub repository.
2. Navigate to the `Settings` tab → `Environments`.
3. Click `New environment` → `Render`.
4. Follow the OAuth authorization flow.
5. Click `Deploy` on any commit to trigger automatic deployment.

## What Gets Deployed

| Service | Type | Description |
|---------|------|-------------|
| `scalemart-frontend` | Static | React frontend served from CDN |
| `scalemart-api` | Web | Spring Boot REST API |
| `scalemart-worker` | Web | Kafka consumer for order processing |
| `scalemart-redpanda` | Web | Kafka-compatible message broker |
| `scalemart-redis` | Key-Value | Session & cache storage |
| `scalemart-postgres` | PostgreSQL | Primary database |

## Environment Variables

The blueprint automatically configures:

### API Service (`scalemart-api`)
| Variable | Source | Description |
|----------|--------|-------------|
| `JWT_SECRET` | Auto-generated | Secret key for JWT signing |
| `DATABASE_URL` | From PostgreSQL | Database connection string |
| `SPRING_DATA_REDIS_HOST` | From Redis | Redis host |
| `SPRING_DATA_REDIS_PORT` | From Redis | Redis port |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | From Redpanda | Kafka broker address |
| `APP_CORS_ALLOWED_ORIGINS` | Configured | Frontend domains |
| `APP_SEED_DEMO_USERS` | Configured | Enable demo users |

### Worker Service (`scalemart-worker`)
| Variable | Source | Description |
|----------|--------|-------------|
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | From Redpanda | Kafka broker address |

## Verify Deployment

### 1. Check Frontend
Open your frontend URL: `https://<your-frontend>.onrender.com`

### 2. Test API Health
```
GET https://<your-api>.onrender.com/actuator/health
```
Expected response: `{"status":"UP"}`

### 3. Test Worker Health
```
GET https://<your-worker>.onrender.com/actuator/health
```
Expected response: `{"status":"UP"}`

### 4. Create Test Order
1. Register a new user via the UI
2. Add products to cart
3. Checkout to create an order
4. Verify order is processed by checking worker logs

## Troubleshooting

### Service Won't Start

1. **Check Logs**: Click on the service in Render Dashboard → `Logs`.
2. **Common Issues**:
   - Missing environment variables
   - Database migration failures
   - Redis connection issues

### Kafka (Redpanda) Issues
If `scalemart-redpanda` is unstable on free resources:
1. Upgrade from `free` to `starter` plan
2. Or use external Kafka service (e.g., Confluent Cloud)

### CORS Errors
If frontend can't reach API:
1. Check `APP_CORS_ALLOWED_ORIGINS` includes your frontend URL
2. Verify API is responding to health check

### Database Connection
If API fails to connect:
1. Check `DATABASE_URL` is properly set
2. Verify PostgreSQL is in same region as API
3. Check Flyway migrations in logs

## Manual Configuration (Alternative)

If you prefer manual setup instead of blueprint:

1. **Create PostgreSQL**: `New` → `PostgreSQL` → Configure and create
2. **Create Redis**: `New` → `Key/Value` → Configure and create
3. **Create Redpanda**: `New` → `Web Service` → Use `redpandadata/redpanda:v24.2.18` image
4. **Create API**: `New` → `Web Service` → Connect your repo, set env vars
5. **Create Worker**: Same as API but different repo path
6. **Create Frontend**: `New` → `Static Site` → Configure build

## Supported Environment Variables

### Spring Boot Application
| Variable | Default | Description |
|----------|---------|-------------|
| `PORT` | 8080 | Server port (Render provides this) |
| `DATABASE_URL` | - | PostgreSQL connection string |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | localhost:9092 | Kafka broker address |
| `SPRING_DATA_REDIS_HOST` | localhost | Redis host |
| `SPRING_DATA_REDIS_PORT` | 6379 | Redis port |
| `JWT_SECRET` | (required) | JWT signing secret |

### Application Features
| Variable | Default | Description |
|----------|---------|-------------|
| `APP_CORS_ALLOWED_ORIGINS` | localhost | CORS allowed origins |
| `APP_SEED_DEMO_USERS` | false | Create demo users on startup |
| `APP_MAIL_ENABLED` | false | Enable email features |

## Architecture

```
┌─────────────────┐     ┌─────────────┐     ┌─────────────────┐
│  Frontend       │────▶│  API        │────▶│  PostgreSQL     │
│  (Static)       │     │  (Spring)   │     │  (Database)     │
└─────────────────┘     └──────┬──────┘     └─────────────────┘
                               │
                               ▼
                        ┌─────────────┐     ┌─────────────────┐
                        │  Redpanda   │────▶│  Worker         │
                        │  (Kafka)    │     │  (Kafka Client) │
                        └─────────────┘     └─────────────────┘
                               │
                               ▼
                        ┌─────────────┐
                        │  Redis      │
                        │  (Cache)    │
                        └─────────────┘
```

## Security Notes

- `JWT_SECRET` is auto-generated during blueprint deployment
- Redis is internal-only (`ipAllowList: []`)
- CORS is configured to allow `*.onrender.com` domains
- Change default demo credentials in production

