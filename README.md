# CPSystem

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-6DB33F?logo=springboot)
![Next.js](https://img.shields.io/badge/Next.js-16-black?logo=next.js)
![React](https://img.shields.io/badge/React-19-61DAFB?logo=react)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791?logo=postgresql)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3-FF6600?logo=rabbitmq)

Payment and billing management system with automated boleto generation via Banco Inter API and client communication via WhatsApp.

## Overview

CPSystem manages the full lifecycle of client billing — from client registration and payment group creation to automated boleto issuance, overdue tracking with late fees/interest, and WhatsApp notifications. Built for a Brazilian billing consultancy (assessoria de cobranca).

## Architecture

```
                    ┌──────────────────────────────────────────┐
                    │              Nginx (80/443)              │
                    │           Reverse Proxy + SSL            │
                    └──────┬─────────────────┬─────────────────┘
                           │                 │
                    ┌──────▼──────┐   ┌──────▼──────┐
                    │  Frontend   │   │   Backend   │
                    │  Next.js    │   │ Spring Boot │
                    │   :3000     │   │   :8080     │
                    └─────────────┘   └──┬────┬──┬──┘
                                         │    │  │
                      ┌──────────────────┘    │  └──────────────────┐
                      │                       │                     │
               ┌──────▼──────┐    ┌───────────▼────────┐   ┌───────▼───────┐
               │ PostgreSQL  │    │   Banco Inter API   │   │   RabbitMQ    │
               │    :5432    │    │  (OAuth2 + mTLS)    │   │    :5672      │
               └─────────────┘    └────────────────────┘   └───────┬───────┘
                                                                   │
                                                           ┌───────▼───────┐
                                                           │   WhatsApp    │
                                                           │   Service     │
                                                           └───────┬───────┘
                                                                   │
                                                           ┌───────▼───────┐
                                                           │ Evolution API │
                                                           │  (WhatsApp)   │
                                                           └───────────────┘
```

## Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Frontend | Next.js (App Router) | 16.1 |
| UI | React, Tailwind CSS v4, shadcn/ui | React 19 |
| Forms | React Hook Form + Zod | RHF 7, Zod 4 |
| Backend | Spring Boot, Spring Security + JWT | 4.0 |
| Language | Java | 21 |
| Database | PostgreSQL | 16 |
| Migrations | Flyway | — |
| Messaging | RabbitMQ | 3 |
| WhatsApp | Evolution API + custom Spring Boot service | — |
| Banking | Banco Inter API (OAuth2 + mTLS) | — |
| Proxy | Nginx | Alpine |
| Containers | Docker Compose | — |

## Project Structure

```
cp/
├── backend/                  # Spring Boot API
│   └── src/main/java/.../
│       ├── api/
│       │   ├── controller/   # REST controllers
│       │   ├── request/      # Request DTOs
│       │   └── response/     # Response DTOs
│       ├── boleto/           # Banco Inter integration
│       │   ├── controller/
│       │   ├── dto/
│       │   └── service/
│       │       └── strategy/ # BankStrategyFactory pattern
│       ├── exception/        # Global error handling
│       ├── model/            # JPA entities
│       ├── repository/       # Data access
│       ├── security/         # JWT auth
│       └── service/          # Business logic
├── frontend/                 # Next.js app
│   ├── app/                  # App Router pages
│   ├── components/
│   │   └── ui/               # shadcn/ui components
│   └── lib/
│       ├── api.ts            # API client (single entry point)
│       └── auth.ts           # JWT management
├── whatsapp-service/         # WhatsApp messaging microservice
├── nginx/                    # Nginx config + SSL certs
├── scripts/                  # Utility scripts
├── docker-compose.yml
└── env.example
```

## Getting Started

### Prerequisites

- Docker and Docker Compose
- (Optional for local dev) Java 21, Node.js 20+, PostgreSQL 16

### Quick Start with Docker

```bash
# Clone the repository
git clone <repo-url> && cd cp

# Configure environment
cp env.example .env
# Edit .env with your values (see Environment Variables below)

# Start all services
docker-compose up -d

# Access the application
# http://localhost (via Nginx)
```

### Verify

```bash
docker-compose ps                  # Check all services are running
docker-compose logs -f backend     # Tail backend logs
```

## Development

### Running without Docker

**Backend:**

```bash
cd backend

# Local profile — uses mock boleto strategy, no Banco Inter creds needed
./mvnw spring-boot:run -Dspring.profiles.active=local

# Requires: PostgreSQL running, env vars for DB connection
```

**Frontend:**

```bash
cd frontend
npm install
npm run dev    # http://localhost:3000
```

Set `NEXT_PUBLIC_API_URL=http://localhost:8080/api/v1` for local dev.

### Profiles

| Profile | Database | Boleto | Use Case |
|---------|----------|--------|----------|
| `local` | PostgreSQL | Mock | Local development |
| `dev` | PostgreSQL | Mock | Dev with real DB |
| `sandbox` | PostgreSQL | Banco Inter sandbox | Integration testing |
| `prod` | PostgreSQL | Banco Inter production | Production |

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `POSTGRES_DB` | Database name | `cobranca` |
| `POSTGRES_USER` | Database user | — |
| `POSTGRES_PASSWORD` | Database password | — |
| `SPRING_PROFILES_ACTIVE` | Spring profile | `prod` |
| `AUTH_USER_EMAIL` | Default admin email | — |
| `AUTH_USER_PASSWORD` | Default admin password | — |
| `JWT_SECRET` | JWT signing key (min 32 chars) | — |
| `JWT_EXPIRATION_MS` | Token TTL in ms | `3600000` |
| `NEXT_PUBLIC_API_URL` | API base URL for frontend | `/api/v1` |
| `ALLOWED_ORIGINS` | CORS allowed origins | — |
| `BANK_MOCK_ENABLED` | Force mock boleto strategy | `false` |
| `INTER_CLIENT_ID` | Banco Inter OAuth client ID | — |
| `INTER_CLIENT_SECRET` | Banco Inter OAuth client secret | — |
| `INTER_CERTIFICATE_PATH` | mTLS certificate path | — |
| `INTER_CERTIFICATE_PASSWORD` | Certificate password | — |
| `RABBITMQ_USERNAME` | RabbitMQ user | `guest` |
| `RABBITMQ_PASSWORD` | RabbitMQ password | `guest` |
| `EVOLUTION_API_KEY` | Evolution API auth key | — |
| `EVOLUTION_API_INSTANCE` | Evolution API instance name | `cpsystem` |

See [`env.example`](env.example) for the full template.

## Database

Schema is managed via **Flyway** migrations in `backend/src/main/resources/db/migration/`. Never edit existing migration files — always create new ones.

### Domain Model

```
Client (borrower)
  └── PaymentGroup (loan contract: payer info, monthly value, installment count)
        └── Payment (single installment: due date, status, overdue value)
              └── Boleto (bank slip linked to payment)
```

**Payment statuses:** `PENDING` → `PAID` / `PAID_LATE` / `OVERDUE`

A scheduled job runs daily to mark unpaid past-due payments as `OVERDUE` and recalculate overdue values with late fees and interest.

## API Overview

All endpoints are prefixed with `/api/v1` (via Nginx proxy). Authentication uses JWT Bearer tokens.

### Auth

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/v1/auth/login` | Authenticate, returns JWT |
| `GET` | `/v1/auth/validate` | Validate current token |
| `POST` | `/v1/auth/refresh` | Refresh expired token |

### Clients

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/v1/client` | Create client |
| `GET` | `/v1/client` | List clients |
| `GET` | `/v1/client/:id` | Get client by ID |
| `PUT` | `/v1/client/:id` | Update client |
| `DELETE` | `/v1/client/:id` | Delete client |

### Payment Groups

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/v1/payment-group` | List payment groups |
| `POST` | `/v1/payment-group` | Create payment group |
| `DELETE` | `/v1/payment-group/:id` | Delete payment group |

### Payments

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/v1/payment/all` | List all payments |
| `GET` | `/v1/payment` | List payments (filtered) |
| `PUT` | `/v1/payment/:id` | Update payment |
| `PATCH` | `/v1/payment/:id/mark-as-paid` | Mark payment as paid |

### Boletos

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/v1/boletos/payment/:id` | Get boleto for payment |
| `POST` | `/v1/boletos/payment/:id/generate` | Generate boleto |
| `POST` | `/v1/boletos/payment/:id/retry` | Retry failed boleto |

## Deployment

Production runs via Docker Compose with Nginx handling SSL termination and reverse proxying.

```bash
# Production deployment
cp env.example .env
chmod 600 .env
# Edit .env with production values

# Place SSL certs
cp fullchain.pem nginx/ssl/
cp privkey.pem nginx/ssl/

# Deploy
docker-compose up -d
```

## Scripts

| Script | Description |
|--------|-------------|
| `scripts/backup-db.sh` | Backup PostgreSQL to file (optionally to S3) |
| `scripts/restore-db.sh` | Restore database from backup |
| `scripts/docker-manage.sh` | Docker container management helpers |
| `scripts/validate-env.sh` | Validate `.env` file configuration |
| `scripts/copy-ssl-certs.sh` | Copy SSL certificates to Nginx volume |
| `scripts/test-boletos.sh` | Test boleto generation flow |

## License

Proprietary. All rights reserved.
