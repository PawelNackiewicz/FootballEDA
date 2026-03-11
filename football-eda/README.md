# FootballEDA - Real-Time Football Match Event Streaming Platform

![Java 21](https://img.shields.io/badge/Java-21-orange)
![Spring Boot 3.2](https://img.shields.io/badge/Spring%20Boot-3.2-green)
![React 18](https://img.shields.io/badge/React-18-blue)
![RabbitMQ 3.12](https://img.shields.io/badge/RabbitMQ-3.12-orange)
![PostgreSQL 15](https://img.shields.io/badge/PostgreSQL-15-blue)
![Docker](https://img.shields.io/badge/Docker-Compose-blue)

**FootballEDA** is an educational platform that teaches **Event-Driven Architecture (EDA)** through a real-world football match simulation. Watch live match events flow through a message broker to independent consumers — all visualized in real-time on a stunning dashboard.

> **Quick Start** (3 commands):
> ```bash
> cd docker && docker-compose -f docker-compose.dev.yml up -d
> cd ../backend && ./mvnw spring-boot:run
> cd ../frontend && npm install && npm run dev
> ```

---

## Architecture Overview

```
[Match Simulator] ──publish──> [RabbitMQ Topic Exchange: football.events]
                                         |
                  +----------------------+----------------------+
                  |                      |                      |                       |
           [standings.queue]    [stats.queue]    [notifications.queue]    [audit.queue]
                  |                      |                      |                       |
           [StandingsConsumer] [StatsConsumer] [NotificationConsumer] [AuditConsumer]
                  |                      |                      |                       |
                  +----------------------+----------+-----------+                       |
                                                    |                                   |
                                            [WebSocket Broker]                          v
                                                    |                          [Event Store DB]
                                                    v                                   |
                                            [React Dashboard] <---- replay -------------+
```

**How it works:**
1. The **Match Simulator** generates domain events (goals, cards, fouls, substitutions, status changes)
2. Events are published to a **RabbitMQ topic exchange** with routing keys (e.g., `match.goal`)
3. RabbitMQ **fans out** events to multiple queues based on binding patterns
4. **4 independent consumers** process events asynchronously from their own queues
5. Each consumer broadcasts updates via **WebSocket** to the React dashboard
6. The **Audit Consumer** persists all events to an **Event Store** for replay capability

---

## EDA Patterns Demonstrated

### 1. Fan-Out
**What:** One event is delivered to multiple independent consumers simultaneously.
**Where:** `RabbitMQConfig.java` — the `football.events` topic exchange routes a single `GoalScored` event to `standings.queue`, `stats.queue`, `notifications.queue`, and `audit.queue`.
**Observe:** Start a match at 10x speed. In the EDA Flow Visualizer, watch colored dots fan out from the exchange to all 4 queues simultaneously.

### 2. Event Sourcing
**What:** The complete history of state changes is stored as a sequence of immutable events.
**Where:** `AuditConsumer` writes every event to the `event_store` table. `MatchReplayService` rebuilds state by replaying events in order.
**Observe:** After a match ends, call `POST /api/matches/{id}/replay` — the system wipes derived state and reconstructs it perfectly from the event store.

### 3. Idempotency
**What:** Processing the same event multiple times produces the same result as processing it once.
**Where:** Every consumer checks `processed_events` table before processing. The `eventId` (UUID) uniquely identifies each event.
**Observe:** Run `IdempotencyIT` — publishes the same GoalScored event 3 times, but standings show only 1 goal.

### 4. Dead Letter Queue
**What:** Failed messages are routed to a special queue for inspection and retry.
**Where:** All queues have `x-dead-letter-exchange: football.dlx` configured. On consumer failure after retries, `basicNack(requeue=false)` sends the message to `football.dlq`.
**Observe:** In RabbitMQ Management UI (http://localhost:15672), check the `football.dlq` queue after forcing a consumer error.

### 5. Eventual Consistency
**What:** All consumers eventually reach a consistent state, but not necessarily at the same instant.
**Where:** After a GoalScored event, the standings, stats, notifications, and event store are updated independently — each at their own pace.
**Observe:** At high simulation speeds, you can see slight timing differences between the score update, stats update, and notification appearing.

### 6. Event Schema / Contract
**What:** Events follow a strict schema contract between producers and consumers.
**Where:** `MatchEvent` sealed interface with `@JsonTypeInfo` / `@JsonSubTypes` for polymorphic serialization. `EventSchemaContractTest` validates JSON structure.
**Observe:** Run `EventSchemaContractTest` — it verifies all required fields are present and types serialize/deserialize correctly.

### 7. Replay
**What:** Rebuilding complete application state from the event store.
**Where:** `MatchReplayService.replay()` — loads events from `event_store`, resets all derived state, then replays each event in chronological order.
**Observe:** After a match, click "Replay" in the dashboard — watch the score, stats, and standings rebuild from scratch.

---

## Data Flow - Step by Step

When a **GoalScored** event occurs, here is the complete journey:

```
Step 1: MatchSimulator generates GoalScored event (UUID assigned)
Step 2: EventStoreService persists event BEFORE publishing (outbox pattern)
Step 3: MatchEventPublisher sends to exchange with routing key 'match.goal'
Step 4: RabbitMQ routes to: standings.queue + stats.queue + notifications.queue + audit.queue
Step 5a: StandingsConsumer -> updates goals in DB -> publishes to WebSocket /topic/standings
Step 5b: StatsConsumer -> recalculates xG, shots -> publishes to WebSocket /topic/stats
Step 5c: NotificationConsumer -> creates "GOAL! Lewandowski 67'" -> /topic/notifications
Step 5d: AuditConsumer -> confirms event in event_store (idempotent check on eventId)
Step 6: React dashboard receives all 4 WebSocket updates independently
Step 7: UI animates: event feed + score update + standings update + notification toast
```

---

## Running Locally

### Prerequisites
- **Java 21** (JDK)
- **Node.js 20+** (with npm)
- **Docker** and **Docker Compose**
- **Maven** (or use the included `./mvnw` wrapper)

### Step-by-step

```bash
# 1. Clone the project
cd football-eda

# 2. Start infrastructure (PostgreSQL + RabbitMQ)
cd docker && docker-compose -f docker-compose.dev.yml up -d

# 3. Wait for services to be healthy
docker-compose -f docker-compose.dev.yml ps

# 4. Start the backend
cd ../backend
./mvnw spring-boot:run

# 5. Start the frontend (in a new terminal)
cd ../frontend
npm install
npm run dev

# 6. Open the dashboard
# http://localhost:3000

# 7. Open RabbitMQ Management UI
# http://localhost:15672 (login: football / football123)

# 8. Click "Start" on the dashboard to begin a match simulation!
```

### Full Docker deployment

```bash
cd docker
docker-compose up --build
# Dashboard: http://localhost:3000
# Backend API: http://localhost:8080
# RabbitMQ UI: http://localhost:15672
```

---

## Running Tests

```bash
cd backend

# Unit tests only (no Docker required)
./mvnw test -Dtest="*Test"

# Integration tests (requires Docker for Testcontainers)
./mvnw test -Dtest="*IT"

# Contract tests
./mvnw test -Dtest="*ContractTest"

# All tests
./mvnw verify
```

---

## Key API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/simulator/start` | Start match simulation (random teams) |
| `POST` | `/api/simulator/start?matchId={id}` | Start with specific match |
| `POST` | `/api/simulator/pause` | Pause simulation |
| `POST` | `/api/simulator/resume` | Resume simulation |
| `POST` | `/api/simulator/stop` | Stop and reset |
| `PUT` | `/api/simulator/speed/{multiplier}` | Change speed (1-20x) |
| `GET` | `/api/simulator/status` | Get current simulator state |
| `POST` | `/api/matches/{id}/replay` | Replay match from event store |
| `GET` | `/api/matches` | List all matches |
| `GET` | `/api/matches/{id}` | Get match details |
| `GET` | `/api/matches/standings` | Current league table |
| `GET` | `/api/events/{matchId}` | Get all events for match |
| `GET` | `/api/events/{matchId}/count` | Event count for match |
| `GET` | `/api/events/types?matchId={id}` | Event type breakdown |

---

## WebSocket Topics

| Topic | Payload | Source Consumer |
|-------|---------|----------------|
| `/topic/events` | `MatchEvent` (polymorphic JSON) | AuditConsumer |
| `/topic/standings` | `List<Standing>` | StandingsConsumer |
| `/topic/stats` | `List<MatchStats>` | StatsConsumer |
| `/topic/notifications` | `Notification` (message + type + minute) | NotificationConsumer |
| `/topic/consumers` | `ConsumerHealthStatus` | All consumers |

**WebSocket endpoint:** `ws://localhost:8080/ws` (STOMP over SockJS)

---

## What to Explore / Learning Exercises

### 1. Kill a Consumer Mid-Match
Stop the backend, then restart it during a match. Observe:
- What happens to unprocessed messages? (They wait in the queue)
- Does the consumer pick up where it left off?
- Check RabbitMQ Management for queue depth during downtime

### 2. Verify Idempotency
Use `curl` to publish the same event to RabbitMQ twice:
```bash
# Run the IdempotencyIT test
./mvnw test -Dtest="IdempotencyIT"
```
Verify that duplicate events don't corrupt standings or stats.

### 3. Run a Replay and Compare States
1. Start a match and let it complete
2. Note the final score and standings
3. Call `POST /api/matches/{id}/replay`
4. Verify the rebuilt state matches exactly

### 4. Add a New Consumer
Create a `BettingOddsConsumer` from scratch:
1. Create `consumer/BettingOddsConsumer.java` following the existing pattern
2. Add a new queue `betting.queue` in `RabbitMQConfig.java`
3. Bind it to `match.goal` and `match.card` routing keys
4. Calculate simple odds based on events
5. Broadcast to a new `/topic/betting` WebSocket topic

### 5. Observe RabbitMQ Under Load
1. Set simulation speed to 20x
2. Open RabbitMQ Management (http://localhost:15672)
3. Watch: message rates, queue depths, consumer acknowledgment rates
4. Compare throughput across different queues

---

## Tech Stack

### Backend
| Technology | Purpose |
|-----------|---------|
| Java 21 | Language (sealed interfaces, records, pattern matching) |
| Spring Boot 3.2 | Application framework |
| Spring AMQP | RabbitMQ integration |
| Spring Data JPA | Database access |
| Spring WebSocket | Real-time communication (STOMP) |
| PostgreSQL 15 | Primary database |
| Flyway | Database migrations |
| Lombok | Boilerplate reduction |
| MapStruct | Object mapping |
| Jackson | JSON serialization |

### Frontend
| Technology | Purpose |
|-----------|---------|
| React 18 | UI framework |
| TypeScript | Type safety |
| Vite | Build tool |
| Tailwind CSS | Styling (dark theme) |
| Zustand | State management |
| STOMP.js | WebSocket client |
| Recharts | Charts and statistics |
| Lucide React | Icons |

### Infrastructure
| Technology | Purpose |
|-----------|---------|
| Docker Compose | Container orchestration |
| RabbitMQ 3.12 | Message broker |
| PostgreSQL 15 | Database |
| Nginx | Frontend reverse proxy |

### Testing
| Technology | Purpose |
|-----------|---------|
| JUnit 5 | Unit testing |
| Mockito | Mocking |
| Testcontainers | Integration testing (real DB + broker) |
| Awaitility | Async assertion |
| Pact JVM | Consumer-driven contracts |

---

## Project Structure

```
football-eda/
+-- docker/
|   +-- docker-compose.yml          # Full deployment
|   +-- docker-compose.dev.yml      # Dev (infra only)
|   +-- postgres/init.sql           # DB initialization
|   +-- rabbitmq/
|       +-- definitions.json        # Pre-configured topology
|       +-- rabbitmq.conf
+-- backend/
|   +-- src/main/java/com/footballeda/
|   |   +-- config/                 # RabbitMQ, WebSocket, JPA config
|   |   +-- domain/event/           # Sealed event interfaces (GoalScored, etc.)
|   |   +-- domain/model/           # JPA entities (Match, Team, Player, etc.)
|   |   +-- domain/enums/           # CardType, MatchStatus, EventType
|   |   +-- producer/               # MatchSimulator + MatchEventPublisher
|   |   +-- consumer/               # 4 independent consumers
|   |   +-- service/                # Business logic + replay
|   |   +-- websocket/              # WebSocket broadcaster
|   |   +-- repository/             # Spring Data JPA repositories
|   |   +-- api/                    # REST controllers
|   +-- src/main/resources/
|   |   +-- application.yml
|   |   +-- db/migration/           # Flyway SQL migrations
|   +-- src/test/java/              # Unit, integration, contract tests
|   +-- pom.xml
+-- frontend/
|   +-- src/
|   |   +-- components/dashboard/   # MatchHeader, EventFeed, Stats, Standings
|   |   +-- components/simulator/   # Controls, SpeedControl
|   |   +-- components/eda/         # EventFlowVisualizer, ConsumerStatus
|   |   +-- hooks/                  # useWebSocket, useMatchEvents
|   |   +-- services/               # API client, WebSocket client
|   |   +-- store/                  # Zustand state management
|   |   +-- types/                  # TypeScript event definitions
|   +-- package.json
+-- README.md
```

---

*Built to teach Event-Driven Architecture through the beautiful game.*
