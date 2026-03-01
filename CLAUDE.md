# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Full-stack payment and billing management system with boleto generation via Banco Inter API. Built with Next.js frontend, Spring Boot backend, and PostgreSQL database, deployed via Docker Compose with Nginx reverse proxy.

## Commands

### Frontend (`frontend/`)

```bash
npm run dev      # Start dev server on localhost:3000
npm run build    # Build optimized bundle
npm run lint     # Run ESLint
```

### Backend (`backend/`)

```bash
# Run with different profiles
./mvnw spring-boot:run -Dspring.profiles.active=local     # Mock mode (no external deps)
./mvnw spring-boot:run -Dspring.profiles.active=dev       # Dev with real DB
./mvnw spring-boot:run -Dspring.profiles.active=sandbox   # Banco Inter sandbox
./mvnw spring-boot:run -Dspring.profiles.active=prod      # Production

./mvnw clean package   # Build JAR
./mvnw test            # Run tests
```

### Docker (full stack)

```bash
docker-compose up -d       # Start all services
docker-compose down        # Stop all services
docker-compose logs -f backend   # Tail backend logs
```

## Architecture

```
Nginx (80/443) → Frontend (3000) + Backend (8080) → PostgreSQL
                                  Backend → Banco Inter API (OAuth2/SSL)
```

**Frontend**: Next.js 16 App Router, React 19, Tailwind CSS v4, shadcn/ui (Radix UI), React Hook Form + Zod, TypeScript.

**Backend**: Spring Boot 4, Java 21, Spring Security + JWT (JJWT), Spring Data JPA, Flyway migrations, Maven.

## Key Architectural Patterns

### Backend Layer Structure
```
api/controller/  →  service/  →  repository/ (JPA)  →  PostgreSQL
                                 boleto/service/     →  Banco Inter API
```

- **Strategy Pattern** for boleto generation: `BankStrategyFactory` selects between `InterBoletoStrategy` (real API) and `MockBoletoStrategy` (testing) based on profile/config.
- **DTOs** are strictly separated: `api/request/`, `api/response/`, `boleto/dto/`.
- **Global exception handler** in `exception/` catches and formats all API errors.
- Database schema managed via **Flyway** in `src/main/resources/db/migration/`.

### Frontend API Client
`lib/api.ts` is the single API client — all HTTP calls go through it. It handles auth headers, 401 token refresh, and typed responses. `lib/auth.ts` manages JWT in localStorage. `components/auth-guard.tsx` wraps protected routes.

## Environment Profiles

| Profile | DB | Boleto | Use Case |
|---------|-----|--------|----------|
| `local` | H2/embedded or env | Mock (instant, no creds needed) | Local dev |
| `dev` | PostgreSQL | Mock | Dev with real DB |
| `sandbox` | PostgreSQL | Banco Inter sandbox | Integration testing |
| `prod` | PostgreSQL | Banco Inter production | Production |

Enable mock boletos explicitly with `BANK_MOCK_ENABLED=true`.

## Key Environment Variables

```
# Database
SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME, SPRING_DATASOURCE_PASSWORD

# Auth
AUTH_USER_EMAIL, AUTH_USER_PASSWORD
JWT_SECRET, JWT_EXPIRATION_MS

# Frontend
NEXT_PUBLIC_API_URL

# Banco Inter (only for sandbox/prod)
INTER_CLIENT_ID, INTER_CLIENT_SECRET
INTER_CERTIFICATE_PATH, INTER_CERTIFICATE_PASSWORD
BANK_MOCK_ENABLED
```

## Important Files

- `frontend/lib/api.ts` — all frontend API calls with TypeScript types
- `backend/src/main/java/dev/gustavorosa/cpsystem/boleto/service/BoletoService.java` — boleto orchestration
- `backend/src/main/java/dev/gustavorosa/cpsystem/boleto/service/strategy/` — bank-specific implementations
- `backend/src/main/resources/db/migration/` — Flyway SQL migrations (never edit existing ones)
- `backend/src/main/resources/application-*.yaml` — profile-specific configs
- `scripts/` — utility scripts for DB backup, restore, docker management, boleto testing
