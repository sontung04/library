# Library Management System

A microservices-based library management system built with Spring Boot and React/TypeScript. The system handles user authentication, book catalog management, and loan tracking through independent services communicating via an API gateway and Kafka event streaming.

## Architecture

```
Browser
  └─► React/TypeScript SPA (Vite + Nginx)
        └─► API Gateway :8080  (JWT auth, routing, rate limiting)
              ├─► User Service  :8081  → PostgreSQL (user_db)
              ├─► Book Service  :8082  → PostgreSQL (book_db)
              └─► Loan Service  :8083  → PostgreSQL (loan_db)

Shared:  Eureka Discovery :8761 | Redis :6379 | Kafka | Prometheus :9090 | Grafana :3000
```

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 4.0.3, Spring Cloud 2025.1.0 |
| API Gateway | Spring Cloud Gateway, JWT (JJWT 0.12.6) |
| Service Discovery | Netflix Eureka |
| Database | PostgreSQL 16, Liquibase migrations |
| Cache | Redis 7 |
| Messaging | Apache Kafka |
| Frontend | React, TypeScript, Vite |
| Observability | Micrometer, Prometheus, Grafana |

## Services

| Service | Port | Responsibility |
|---|---|---|
| Frontend | 80 (prod) / 5173 (dev) | Web UI |
| API Gateway | 8080 | Routing, JWT validation, rate limiting |
| User Service | 8081 | Registration, authentication, user management |
| Book Service | 8082 | Book catalog CRUD |
| Loan Service | 8083 | Borrow/return tracking |
| Discovery Server | 8761 | Eureka service registry |
| Grafana | 3000 | Metrics dashboards |
| RedisInsight | 5540 | Redis management UI |

---

## Setup Guide

### Prerequisites

- [Docker](https://www.docker.com/) and Docker Compose
- Java 21 and Maven 3.9+ *(only for local development)*
- Node.js 22+ *(only for local development)*

### Option 1 — Docker Compose (Recommended)

This starts all services, databases, and the monitoring stack in one command.

**1. Clone the repository**
```bash
git clone <repo-url>
cd library
```

**2. Configure environment variables**

Create a `.env` file at the project root (or export variables directly):
```env
JWT_SECRET=your-secret-key-at-least-32-characters-long
INTERNAL_API_KEY=library-internal-key
```

**3. Start everything**
```bash
docker compose -f compose.yaml up --build
```

**4. Access the application**

| URL | Description |
|---|---|
| `http://localhost` | Web UI |
| `http://localhost:8080` | API Gateway |
| `http://localhost:8761` | Eureka dashboard |
| `http://localhost:3000` | Grafana (admin / admin) |
| `http://localhost:5540` | RedisInsight |
| `http://localhost:9090` | Prometheus |

---

### Option 2 — Local Development

Run each service individually for faster iteration.

**1. Start infrastructure dependencies**
```bash
# PostgreSQL, Redis, and Kafka must be running locally or via Docker
docker compose -f compose.yaml up postgres-user postgres-book postgres-loan redis kafka --build
```

**2. Start backend services** (run each in a separate terminal)
```bash
# Service discovery — start this first
mvn -pl infrastructure/discovery-server spring-boot:run

# API Gateway
mvn -pl infrastructure/gateway spring-boot:run

# Business services (order does not matter after the two above)
mvn -pl services/user-service spring-boot:run
mvn -pl services/book-service spring-boot:run
mvn -pl services/loan-service spring-boot:run
```

**3. Start the frontend**
```bash
cd frontend/fe
npm install
npm run dev   # http://localhost:5173
```

The Vite dev server proxies `/api` and `/auth` requests to the gateway at `http://localhost:8080`.

---

## Project Structure

```
library/
├── compose.yaml                  # Docker Compose orchestration
├── pom.xml                       # Maven parent POM
├── services/
│   ├── user-service/             # Authentication & user management
│   ├── book-service/             # Book catalog
│   └── loan-service/             # Loan tracking
├── infrastructure/
│   ├── gateway/                  # Spring Cloud Gateway
│   ├── discovery-server/         # Eureka server
│   └── monitoring/               # Prometheus & Grafana config
├── frontend/
│   └── fe/                       # React/TypeScript SPA
└── docs/
    └── documents/                # Data model, use-case diagrams, guidelines
```

## Documentation

Additional design documents are in [`docs/documents/`](docs/documents/):

- `Data_modeling.docx` — Database schema
- `guideline.docx` — Development guidelines
- `UseCase Diagram0.png` — Use-case diagram
