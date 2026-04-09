# QueueSmart – Backend (A3)

Spring Boot REST API for the QueueSmart smart queue management application.

---

## Tech Stack

| Layer        | Technology                      |
|--------------|---------------------------------|
| Language     | Java 17                         |
| Framework    | Spring Boot 3.2                 |
| Security     | Spring Security + JWT (JJWT)    |
| Validation   | Jakarta Bean Validation         |
| Data storage | In-memory (ConcurrentHashMap)   |
| Testing      | JUnit 5 + Mockito               |
| Coverage     | JaCoCo Maven Plugin             |
| Build        | Maven                           |

---

## Project Structure

```
src/
├── main/java/com/queuesmart/
│   ├── QueueSmartApplication.java
│   ├── config/
│   │   ├── JwtUtil.java            # Token generation & validation
│   │   ├── JwtAuthFilter.java      # Request filter – reads Bearer token
│   │   └── SecurityConfig.java     # CORS, role-based access rules
│   ├── controller/
│   │   ├── AuthController.java     # POST /api/auth/register, /login
│   │   ├── ServiceController.java  # CRUD /api/services
│   │   ├── QueueController.java    # Join/leave/status/serve /api/queue
│   │   ├── NotificationController.java
│   │   └── HistoryController.java
│   ├── service/
│   │   ├── AuthService.java
│   │   ├── ServiceManagementService.java
│   │   ├── QueueService.java
│   │   ├── NotificationService.java
│   │   ├── HistoryService.java
│   │   └── WaitTimeEstimator.java  # Rule-based wait time logic
│   ├── model/          # User, Service, QueueEntry, Notification, HistoryRecord
│   ├── dto/            # AuthDto, ServiceDto, QueueDto, ApiResponse
│   ├── repository/     # Five in-memory ConcurrentHashMap repositories
│   └── exception/      # GlobalExceptionHandler
└── test/java/com/queuesmart/
    ├── controller/
    │   └── AuthControllerTest.java
    └── service/
        ├── AuthServiceTest.java
        ├── QueueServiceTest.java
        ├── ServiceManagementServiceTest.java
        ├── NotificationServiceTest.java
        ├── WaitTimeEstimatorTest.java
        └── RepositoryTest.java
```

---

## How to Run

### Prerequisites
- Java 17+
- Maven 3.8+

### Steps

```bash
# 1. Clone the repo
git clone <your-github-repo-url>
cd queuesmart

# 2. Build and run
mvn spring-boot:run

# 3. Server starts on
http://localhost:8080
```

### Run Tests

```bash
mvn test
```

### Generate Coverage Report

```bash
mvn test
# Report is generated at:
target/site/jacoco/index.html
```

---

## API Reference

All responses follow this structure:
```json
{
  "success": true,
  "message": "...",
  "data": { ... }
}
```

### Authentication (Public — no token needed)

| Method | Endpoint              | Body                                          |
|--------|-----------------------|-----------------------------------------------|
| POST   | /api/auth/register    | { username, email, password, role? }          |
| POST   | /api/auth/login       | { email, password }                           |

**Register response:**
```json
{
  "data": {
    "token": "eyJ...",
    "userId": "uuid",
    "username": "alice",
    "email": "alice@example.com",
    "role": "USER"
  }
}
```

> All subsequent requests require the header:
> `Authorization: Bearer <token>`

---

### Services

| Method | Endpoint              | Role      | Description                   |
|--------|-----------------------|-----------|-------------------------------|
| GET    | /api/services         | Any       | List active services          |
| GET    | /api/services/all     | ADMIN     | List all services             |
| GET    | /api/services/{id}    | Any       | Get a single service          |
| POST   | /api/services         | ADMIN     | Create a service              |
| PUT    | /api/services/{id}    | ADMIN     | Update a service              |
| DELETE | /api/services/{id}    | ADMIN     | Delete a service              |

**Create service body:**
```json
{
  "name": "Academic Advising",
  "description": "One-on-one advising sessions",
  "expectedDurationMinutes": 15,
  "priorityLevel": "MEDIUM"
}
```

---

### Queue

| Method | Endpoint                    | Role   | Description                        |
|--------|-----------------------------|--------|------------------------------------|
| POST   | /api/queue/join             | USER   | Join a queue                       |
| DELETE | /api/queue/leave/{serviceId}| USER   | Leave a queue                      |
| GET    | /api/queue/status/{serviceId}| Any   | Get full queue status              |
| GET    | /api/queue/my/{serviceId}   | USER   | Get your own queue entry           |
| POST   | /api/queue/serve/{serviceId}| ADMIN  | Serve the next user                |

**Join queue body:**
```json
{
  "serviceId": "uuid",
  "priorityLevel": "HIGH"
}
```

**Queue status response (data field):**
```json
{
  "serviceId": "...",
  "serviceName": "Advising",
  "totalWaiting": 4,
  "estimatedWaitForNew": 60,
  "entries": [
    {
      "id": "...",
      "username": "alice",
      "position": 1,
      "estimatedWaitMinutes": 0,
      "status": "WAITING",
      "priorityLevel": "HIGH"
    }
  ]
}
```

---

### Notifications

| Method | Endpoint                       | Description                  |
|--------|--------------------------------|------------------------------|
| GET    | /api/notifications             | All notifications for user   |
| GET    | /api/notifications/unread      | Only unread                  |
| GET    | /api/notifications/count       | Unread count                 |
| PATCH  | /api/notifications/{id}/read   | Mark one as read             |

---

### History

| Method | Endpoint            | Role   | Description                   |
|--------|---------------------|--------|-------------------------------|
| GET    | /api/history        | USER   | Your own history              |
| GET    | /api/history/all    | ADMIN  | All records                   |
| GET    | /api/history/stats  | ADMIN  | Usage stats per service       |

---

## Wait Time Estimation Logic

Formula used (rule-based, no advanced algorithms):

```
baseWait = (position - 1) × expectedDurationMinutes
wait     = ceil(baseWait × priorityMultiplier)

priorityMultiplier:
  HIGH   → 0.6   (HIGH priority users experience ~40% shorter waits)
  MEDIUM → 0.8
  LOW    → 1.0
```

---

## Queue Ordering

Queue entries are sorted by:
1. **Priority level** — HIGH first, then MEDIUM, then LOW
2. **Arrival time** — FIFO within the same priority level

---

## Validation Rules

| Field                     | Rule                                         |
|---------------------------|----------------------------------------------|
| username                  | Required, 3–50 characters                    |
| email                     | Required, valid email format, max 100 chars  |
| password                  | Required, 6–100 characters                   |
| service name              | Required, 2–100 characters                   |
| service description       | Required, max 500 characters                 |
| expectedDurationMinutes   | Required, 1–480                              |
| priorityLevel             | Required, one of: LOW, MEDIUM, HIGH          |

Invalid requests return HTTP 400 with field-level error details.

---

## Frontend Integration

Open `queuesmart_frontend_integrated.html` directly in a browser.
The frontend calls `http://localhost:8080/api` — make sure the backend is running first.

**Flow:**
1. Register or log in → JWT token stored in memory
2. Token sent as `Authorization: Bearer <token>` on every request
3. USER role → User Dashboard (join queue, view status, history, notifications)
4. ADMIN role → Admin Dashboard (manage services, view queues, serve next)

---

## Notes

- No database is used — all data is stored in `ConcurrentHashMap` instances (as per A3 requirements)
- Data resets on server restart
- Notifications are in-app only (no real email/SMS sent)
- Email verification is designed but not enforced (as per A1 design)
