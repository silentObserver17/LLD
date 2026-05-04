```
┌─────────────────────────────────────────────────────────────┐
│                    SYSTEM DESIGN PATTERNS                   │
├─────────────────────┬───────────────────────────────────────┤
│   ARCHITECTURE      │  Microservices, Event-Driven, CQRS    │
│   (how you build)   │  Saga, API Gateway                    │
├─────────────────────┼───────────────────────────────────────┤
│   RESILIENCE        │  Circuit Breaker, Retry,              │
│   (how you survive) │  Rate Limiting                        │
├─────────────────────┼───────────────────────────────────────┤
│   SCALABILITY       │  Caching, Load Balancer,              │
│   (how you scale)   │  Sharding, Replication                │
└─────────────────────┴───────────────────────────────────────┘
```

### 1. Microservices Architecture
#### Intent

Break a large application into **small, independently deployable services**, each owning its own data and business capability.

#### The Monolith Problem

Imagine your expense tracking app as a monolith:

```
┌─────────────────────────────────────────────┐
│                  MONOLITH                   │
│                                             │
│  UserModule  ExpenseModule  ApprovalModule  │
│  NotifModule PaymentModule  ReportModule    │
│                                             │
│         Single Database                     │
│         Single Deployment                   │
└─────────────────────────────────────────────┘
```

Works fine early. Then:

- Approval logic bug → **redeploy entire app** including unrelated modules
- Report generation is slow → **entire app slows down**
- Want to scale only notifications → **must scale everything**
- Team A and Team B both touching `UserModule` → **merge conflicts, coordination overhead**
- One memory leak → **entire system down**
#### Microservices Solution
```
┌──────────────┐   ┌──────────────┐   ┌──────────────┐
│  User        │   │  Expense     │   │  Approval    │
│  Service     │   │  Service     │   │  Service     │
│  [DB]        │   │  [DB]        │   │  [DB]        │
└──────────────┘   └──────────────┘   └──────────────┘

┌──────────────┐   ┌──────────────┐   ┌──────────────┐
│ Notification │   │  Payment     │   │  Report      │
│  Service     │   │  Service     │   │  Service     │
│  [DB]        │   │  [DB]        │   │  [DB]        │
└──────────────┘   └──────────────┘   └──────────────┘
```

Each service:

- Has its **own database** — no shared DB
- Deploys **independently**
- Scales **independently**
- Owned by **one team**
- Communicates via **API or events**

#### How services communicate
**Synchronous — REST or gRPC:**
```
ExpenseService ──HTTP/gRPC──► UserService
                              "Is user 101 active?"
                              ← yes/no response
```

Used when you need an **immediate answer** to continue.

**Asynchronous — Message Queue:**
```
ExpenseService ──► [Queue] ──► NotificationService
  "expense.approved"              sends WhatsApp/email
                   └──────────► AnalyticsService
                                   records event
```

Used when you **don't need an immediate response** and want loose coupling.

#### The core tradeoffs

| |Monolith|Microservices|
|---|---|---|
|**Simplicity**|✅ Simple to develop|❌ Complex distributed system|
|**Deployment**|❌ All or nothing|✅ Independent per service|
|**Scaling**|❌ Scale everything|✅ Scale only what needs it|
|**Failure isolation**|❌ One bug can kill all|✅ Failure contained|
|**Data consistency**|✅ ACID transactions|❌ Eventual consistency|
|**Network overhead**|✅ In-process calls|❌ Network calls between services|
|**Testing**|✅ Easier end-to-end|❌ Integration testing is hard|
|**Team autonomy**|❌ Coordination needed|✅ Teams own their service|
#### The new problems microservices introduce
don't just say "use microservices". Know the problems it creates:

**1. Distributed transactions** — how do you maintain consistency when an operation spans multiple services? → Solved by **Saga Pattern** (coming up)

**2. Service discovery** — how does Service A find Service B's address? → Solved by service registries like Consul, Kubernetes DNS

**3. Cross-cutting concerns** — auth, logging, rate limiting repeated in every service → Solved by **API Gateway** (coming up)

**4. Cascading failures** — Service A calls B calls C — C goes down, takes B and A with it → Solved by **Circuit Breaker** (coming up)

**5. Data consistency** — each service has its own DB, queries spanning services are hard → Solved by **CQRS + Event-Driven Architecture** (coming up)

This is why you learn these 12 patterns together — microservices **creates** the problems that the other patterns **solve**.

---
#### When to use microservices

Use it when:

- Multiple teams working on the same system
- Different parts of the system have vastly different scale requirements
- Parts of the system need independent deployment cycles
- System is large enough that monolith complexity is a real pain

Don't use it when:

- Small team, early stage product
- System is simple with low traffic
- You don't have DevOps maturity to manage distributed systems

A well-structured monolith beats a poorly implemented microservices setup every time. **Start monolith, extract services when you feel the pain.**

### 2. Event-Driven Architecture
#### Intent

Services communicate by **producing and consuming events** rather than calling each other directly. The producer doesn't know or care who's listening.

#### The Problem with Direct Calls

```
ExpenseService ──HTTP──► NotificationService
              ──HTTP──► AnalyticsService
              ──HTTP──► AuditService
              ──HTTP──► ReportService
```

Problems:

- ExpenseService **coupled** to every downstream service
- If NotificationService is down, expense approval **fails**
- Adding a new downstream means **modifying ExpenseService**
- All downstream services must be **available simultaneously**

This is the Observer Pattern problem — at distributed system scale.

#### Event-Driven Solution
```
ExpenseService
    │
    └──► [Event Bus / Message Broker]
              │   "expense.approved" event
              │
              ├──► NotificationService  (sends WhatsApp + email)
              ├──► AnalyticsService     (records conversion)
              ├──► AuditService         (logs approval trail)
              └──► ReportService        (updates dashboards)
```

ExpenseService publishes **one event**. Every interested service subscribes independently. ExpenseService has **zero knowledge** of who's listening.

#### Event anatomy

A well-designed event carries enough context to be processed independently:

json

```json
{
  "eventId":   "evt_9f3a2c",
  "eventType": "expense.approved",
  "version":   "1.0",
  "timestamp": "2026-05-04T10:30:00Z",
  "source":    "expense-service",
  "payload": {
    "expenseId":  7823,
    "userId":     101,
    "amount":     15000.00,
    "currency":   "INR",
    "approvedBy": "HOD",
    "approvedAt": "2026-05-04T10:29:58Z"
  }
}
```

Rule: **events describe what happened, not what to do**. `expense.approved` not `send-notification-for-expense`.

#### Two messaging models

**Queue (Point-to-Point):**

```
Producer ──► [Queue] ──► Consumer
```

- One message consumed by **exactly one** consumer
- Used for task distribution — BullMQ works this way
- Good for: job processing, work queues

**Topic / Pub-Sub:**

```
Producer ──► [Topic] ──► Consumer A
                    └──► Consumer B
                    └──► Consumer C
```

- One message consumed by **all subscribers**
- Used for event broadcasting — Kafka works this way
- Good for: notifications, audit logs, analytics

#### Kafka architecture — the industry standard
```
┌─────────────┐     ┌─────────────────────────────┐     ┌──────────────────┐
│  Producers  │     │          Kafka               │     │    Consumers     │
│             │     │                              │     │                  │
│ expense-svc ├────►│  Topic: expense-events       ├────►│ notification-svc │
│ payment-svc ├────►│  ┌──────────────────────┐   ├────►│ analytics-svc    │
│ auth-svc    ├────►│  │ Partition 0          │   ├────►│ audit-svc        │
│             │     │  │ Partition 1          │   │     │                  │
└─────────────┘     │  │ Partition 2          │   │     └──────────────────┘
                    │  └──────────────────────┘   │
                    │  Messages retained 7 days    │
                    └─────────────────────────────┘
```

Key Kafka properties:

- **Persistent** — messages stored on disk, not lost if consumer is down
- **Replayable** — consumers can rewind and reprocess old events
- **Ordered within partition** — ordering guaranteed per partition
- **Consumer groups** — multiple instances of same service share the load

#### Event-Driven tradeoffs

|Benefit|Cost|
|---|---|
|✅ Loose coupling between services|❌ Eventual consistency — consumers lag|
|✅ Producer doesn't wait for consumers|❌ Harder to debug — no single call trace|
|✅ Easy to add new consumers|❌ Message ordering complexity|
|✅ Resilient — consumers can be down temporarily|❌ Duplicate message handling needed|
|✅ Natural audit log — event history|❌ Schema evolution is tricky|
### 3. CQRS — Command Query Responsibility Segregation
#### Intent

**Separate the read model from the write model**. Commands change state. Queries read state. They use different models, often different databases optimized for each purpose.

#### The Problem

A single model serving both reads and writes gets painful fast:

```
// Your expense table serves BOTH:

// Write — normalized, relational, ACID
INSERT INTO expenses (user_id, amount, type, status) VALUES (...)
UPDATE expenses SET status = 'APPROVED' WHERE id = 7823

// Read — denormalized, joined, needs aggregation
SELECT e.*, u.name, u.department, a.approver_name,
       SUM(e.amount) OVER (PARTITION BY u.department) as dept_total
FROM expenses e
JOIN users u ON e.user_id = u.id
JOIN approvals a ON e.id = a.expense_id
WHERE e.status = 'APPROVED'
AND e.created_at > NOW() - INTERVAL '30 days'
```

The same schema can't be optimally designed for both. Writes want normalized, indexed-for-writes tables. Reads want denormalized, pre-joined, aggregated views.

At scale — write load and read load require **different scaling strategies**.

#### CQRS Solution
```
                    ┌─────────────────────────────────┐
                    │         Your Application        │
                    └──────────────┬──────────────────┘
                                   │
                    ┌──────────────┴──────────────┐
                    │                             │
             COMMANDS (writes)            QUERIES (reads)
                    │                             │
                    ▼                             ▼
          ┌──────────────────┐         ┌──────────────────┐
          │  Write Model     │         │   Read Model     │
          │  (normalized)    │         │  (denormalized)  │
          │                  │         │                  │
          │  PostgreSQL       │         │  Read Replica    │
          │  ACID compliant  │         │  Elasticsearch   │
          │  Append-only     │         │  Redis Cache     │
          └────────┬─────────┘         └──────────────────┘
                   │                             ▲
                   │    event: expense.approved  │
                   └────────────────────────────►│
                        (sync read model)        │
```

#### Implementation concept
```java
// ── COMMAND SIDE ──────────────────────────────────────────

// Commands — intent to change state
public record ApproveExpenseCommand(int expenseId, String approverRole) {}
public record SubmitExpenseCommand(int userId, double amount, String type) {}

// Command Handler — write to normalized DB
@Service
public class ExpenseCommandHandler {

    private final ExpenseWriteRepository writeRepo;
    private final EventPublisher eventPublisher;

    public void handle(ApproveExpenseCommand cmd) {
        Expense expense = writeRepo.findById(cmd.expenseId());
        expense.approve(cmd.approverRole()); // domain logic
        writeRepo.save(expense);

        // Publish event to sync read model
        eventPublisher.publish(new ExpenseApprovedEvent(
            expense.getId(), expense.getAmount(), expense.getUserId()
        ));
    }
}

// ── QUERY SIDE ────────────────────────────────────────────

// Query — no business logic, just data fetching
public record GetDepartmentExpenseSummaryQuery(String department, YearMonth month) {}

// Query Handler — reads from denormalized read model
@Service
public class ExpenseQueryHandler {

    private final ExpenseReadRepository readRepo; // read replica or separate DB

    public DepartmentSummaryDTO handle(GetDepartmentExpenseSummaryQuery query) {
        // Read model is pre-computed, no joins needed
        return readRepo.getDepartmentSummary(query.department(), query.month());
    }
}

// ── READ MODEL UPDATER ────────────────────────────────────
// Listens to events, keeps read model in sync

@EventListener
public class ExpenseReadModelUpdater {

    private final ExpenseReadRepository readRepo;

    public void on(ExpenseApprovedEvent event) {
        // Denormalize and store in read-optimized format
        readRepo.updateApprovalStatus(event.expenseId(), "APPROVED");
        readRepo.incrementDepartmentTotal(event.department(), event.amount());
    }
}
```

#### CQRS tradeoffs

| Benefit                                | Cost                                    |
| -------------------------------------- | --------------------------------------- |
| ✅ Read and write scale independently   | ❌ Eventual consistency between models   |
| ✅ Read model optimized for queries     | ❌ More complex — two models to maintain |
| ✅ Write model clean, focused on domain | ❌ Read model can lag behind writes      |
| ✅ Natural audit trail via commands     | ❌ Overkill for simple CRUD apps         |

**Use CQRS when:** read and write loads are very different, complex reporting needs, high scalability requirements. **Don't use when:** simple app, small team, CRUD with no complex queries.

### 4. Saga Pattern
#### Intent

Manage **distributed transactions** across multiple services by breaking them into a sequence of local transactions, each publishing events or messages to trigger the next step — with **compensating transactions** to undo completed steps on failure.

#### The Problem

In a monolith, placing an order is one ACID transaction:

```sql
BEGIN;
  UPDATE inventory SET stock = stock - 1 WHERE product_id = 42;
  INSERT INTO orders (user_id, product_id, amount) VALUES (101, 42, 1499);
  UPDATE payments SET balance = balance - 1499 WHERE user_id = 101;
COMMIT; -- all or nothing
```

In microservices, these are **three separate databases**:

```
InventoryService [its own DB]
OrderService     [its own DB]
PaymentService   [its own DB]
```

You **cannot** do a single ACID transaction across them. If payment fails after inventory was reserved and order was created — you have inconsistent state. This is the **distributed transaction problem**.

#### Saga Solution — two flavors

1. **Choreography Saga** (event-driven, decentralized):
```
OrderService          InventoryService       PaymentService
     │                      │                     │
     │── order.created ────►│                     │
     │                      │ reserve stock        │
     │◄── stock.reserved ───│                     │
     │                                            │
     │── payment.requested ──────────────────────►│
     │                                            │ charge card
     │◄── payment.completed ─────────────────────│
     │
     └── order.confirmed (done!)
```

Each service reacts to events and publishes its own. No central coordinator. Simple but hard to track overall flow.

**Orchestration Saga** (centralized coordinator):
```
                  ┌─────────────────┐
                  │  Saga           │
                  │  Orchestrator   │
                  └────────┬────────┘
                           │
          ┌────────────────┼────────────────┐
          │                │                │
          ▼                ▼                ▼
   InventoryService   OrderService   PaymentService
   "reserve stock"   "create order"  "charge card"
```

Orchestrator directs each step explicitly. Easier to track, debug, and handle failures.

#### Compensating Transactions — the undo mechanism

If payment fails after inventory was reserved:

```
Step 1: InventoryService.reserveStock()     ✅ SUCCESS
Step 2: OrderService.createOrder()          ✅ SUCCESS
Step 3: PaymentService.chargeCard()         ❌ FAILURE

// Compensations run in reverse:
Step 2 undo: OrderService.cancelOrder()     ✅
Step 1 undo: InventoryService.releaseStock() ✅
```

Compensation is **not rollback** — it's a new forward transaction that logically reverses the effect. Each service must implement its own compensation

### 5. API Gateway Pattern

#### Intent

Provide a **single entry point** for all client requests into a microservices system. The gateway handles cross-cutting concerns so individual services don't have to.

#### Without API Gateway

```
Mobile App ──► UserService:3001
           ──► ExpenseService:3002
           ──► ApprovalService:3003
           ──► NotificationService:3004
```

Problems:

- Client knows internal service addresses — tight coupling
- Auth logic duplicated in every service
- CORS, rate limiting, SSL termination repeated everywhere
- Want to aggregate two service responses? Client makes two calls

#### With API Gateway

```
Mobile App ──► API Gateway :443
                    │
                    ├── /api/users     ──► UserService:3001
                    ├── /api/expenses  ──► ExpenseService:3002
                    ├── /api/approvals ──► ApprovalService:3003
                    └── /api/notify    ──► NotificationService:3004
```

Gateway handles:

- **Routing** — forward to correct service
- **Authentication** — validate JWT once, not in every service
- **Rate limiting** — centralized traffic control
- **SSL termination** — HTTPS outside, HTTP inside
- **Request aggregation** — combine multiple service responses
- **Logging** — single place to log all incoming requests
- **Load balancing** — distribute across service instances

#### Request aggregation — powerful feature

```
// Client needs dashboard data — without gateway: 3 separate calls
GET /users/101
GET /expenses?userId=101&month=2026-05
GET /approvals/pending?userId=101

// With gateway aggregation — one call
GET /api/dashboard/101

// Gateway internally calls all three and merges:
{
  "user":     { ...from UserService },
  "expenses": { ...from ExpenseService },
  "pending":  { ...from ApprovalService }
}
```

---

#### Your Nginx is an API Gateway

nginx

```nginx
# Your current nginx.conf is a basic API gateway
location /api/auth/ {
    proxy_pass http://auth-service:8080/;
}

location /api/expenses/ {
    proxy_pass http://expense-service:3000/;
}
```

Adding auth validation, rate limiting, and SSL termination to Nginx makes it a full API gateway. Kong, AWS API Gateway, and Traefik are dedicated tools that do this with more features.

### 6. Circuit Breaker

#### Intent

**Stop calling a failing service** and return a fallback immediately, instead of letting failures cascade and exhaust resources waiting for timeouts.

Named after electrical circuit breakers — when current is too high, the breaker trips and stops the flow to prevent damage.

#### The Cascading Failure Problem

```
User Request
    │
    ▼
Service A (response time: 30s timeout)
    │
    ▼
Service B ──► Service C ──► [Database DOWN]
                              timeout after 30s
```

Every request to A waits 30 seconds before timing out. Threads pile up. Service B's thread pool exhausts. Service A's thread pool exhausts. **Entire system collapses** because of one downstream failure.

#### Circuit Breaker — three states
```
failure threshold exceeded
CLOSED ─────────────────────────────────────► OPEN
(normal operation)                           (fail fast)
        ▲                                        │
        │                                        │ timeout elapsed
        │     probe succeeds                     ▼
        └──────────────────────────── HALF-OPEN
                                     (testing recovery)
```

**CLOSED** — normal operation, requests flow through, failures counted.

**OPEN** — failure threshold hit, all requests **immediately return fallback** without calling the service. No waiting. No thread exhaustion.

**HALF-OPEN** — after a timeout, let **one probe request** through. If it succeeds → back to CLOSED. If it fails → back to OPEN.

#### Implementation concept in Go
```go
type CircuitBreaker struct {
    state            string    // "CLOSED", "OPEN", "HALF_OPEN"
    failureCount     int
    failureThreshold int       // e.g. 5 failures
    lastFailureTime  time.Time
    timeout          time.Duration // e.g. 30 seconds
    mu               sync.Mutex
}

func (cb *CircuitBreaker) Call(fn func() (interface{}, error)) (interface{}, error) {
    cb.mu.Lock()
    defer cb.mu.Unlock()

    switch cb.state {
    case "OPEN":
        // Check if timeout has elapsed — try half-open
        if time.Since(cb.lastFailureTime) > cb.timeout {
            cb.state = "HALF_OPEN"
            fmt.Println("Circuit HALF-OPEN: probing...")
        } else {
            // Fail fast — don't even try
            return nil, errors.New("circuit breaker is OPEN — service unavailable")
        }

    case "CLOSED", "HALF_OPEN":
        result, err := fn() // attempt the actual call

        if err != nil {
            cb.failureCount++
            cb.lastFailureTime = time.Now()

            if cb.failureCount >= cb.failureThreshold || cb.state == "HALF_OPEN" {
                cb.state = "OPEN"
                fmt.Printf("Circuit OPEN after %d failures\n", cb.failureCount)
            }
            return nil, err
        }

        // Success — reset
        cb.failureCount = 0
        cb.state = "CLOSED"
        return result, nil
    }

    return nil, errors.New("unexpected circuit breaker state")
}

// Usage
cb := &CircuitBreaker{
    failureThreshold: 5,
    timeout:          30 * time.Second,
    state:            "CLOSED",
}

result, err := cb.Call(func() (interface{}, error) {
    return paymentService.charge(amount) // potentially failing call
})

if err != nil {
    // Return fallback — don't let failure propagate
    return fallbackResponse()
}
```

#### Resilience4j — Java standard

```java
CircuitBreakerConfig config = CircuitBreakerConfig.custom()
    .failureRateThreshold(50)           // open if 50% of calls fail
    .waitDurationInOpenState(Duration.ofSeconds(30))
    .slidingWindowSize(10)              // evaluate last 10 calls
    .build();

CircuitBreaker cb = CircuitBreaker.of("paymentService", config);

// Wrap your call
Supplier<String> decorated = CircuitBreaker
    .decorateSupplier(cb, () -> paymentService.charge(amount));

String result = Try.ofSupplier(decorated)
    .recover(CallNotPermittedException.class, ex -> "fallback-response")
    .get();
```

### 7. Retry Pattern

#### Intent

**Automatically retry** a failed operation, with configurable delay and backoff, before giving up. Handles **transient failures** — temporary network blips, brief service unavailability.
#### Transient vs Permanent Failures

Critical distinction — retry only makes sense for transient failures:

|Failure type|Example|Retry?|
|---|---|---|
|**Transient**|Network timeout, 503 Service Unavailable|✅ Yes|
|**Permanent**|404 Not Found, 400 Bad Request, auth failure|❌ No — retrying won't help|

Retrying a 400 Bad Request is wasted work — the request is malformed, it'll fail every time.

#### Retry with exponential backoff + jitter

Naive retry — retry immediately, all clients retry at the same time → **thundering herd** hammers the recovering service.
```
Attempt 1: fails → wait 1s
Attempt 2: fails → wait 2s   (exponential backoff)
Attempt 3: fails → wait 4s
Attempt 4: fails → wait 8s
Attempt 5: fails → give up
```

Add **jitter** — randomize the wait to spread load:
```go
func retryWithBackoff(fn func() error, maxRetries int) error {
    for attempt := 0; attempt < maxRetries; attempt++ {
        err := fn()
        if err == nil {
            return nil // success
        }

        // Don't retry permanent errors
        if isPermanentError(err) {
            return err
        }

        if attempt == maxRetries-1 {
            return fmt.Errorf("max retries exceeded: %w", err)
        }

        // Exponential backoff with jitter
        base    := time.Duration(1<<attempt) * time.Second // 1s, 2s, 4s, 8s
        jitter  := time.Duration(rand.Intn(1000)) * time.Millisecond
        waitFor := base + jitter

        fmt.Printf("Attempt %d failed, retrying in %v\n", attempt+1, waitFor)
        time.Sleep(waitFor)
    }
    return nil
}

func isPermanentError(err error) bool {
    var httpErr *HttpError
    if errors.As(err, &httpErr) {
        // 4xx errors are permanent — don't retry
        return httpErr.StatusCode >= 400 && httpErr.StatusCode < 500
    }
    return false
}

// Usage
err := retryWithBackoff(func() error {
    return paymentService.charge(amount)
}, 5)
```

#### Circuit Breaker + Retry together

They're complementary — not competing:

```
Request
    │
    ▼
Circuit Breaker ──OPEN──► fail fast (no retry)
    │
  CLOSED
    │
    ▼
  Retry (up to N times with backoff)
    │
    ▼
Actual Service Call
```

Retry handles transient failures. Circuit Breaker handles sustained failures. Together they give you resilient service calls.

### 8. Rate Limiting

#### Intent

**Control the rate of requests** a client or service can make, to protect downstream services from being overwhelmed and ensure fair usage.

#### Why it matters

Without rate limiting:

- One misbehaving client can exhaust your server's resources
- DDoS attacks can take down your API
- A bug in a client causing infinite retries can cascade

#### Four algorithms

**1. Token Bucket** — most common, allows bursting:

```
Bucket capacity: 10 tokens
Refill rate: 2 tokens/second

Request arrives → consume 1 token → allowed
Request arrives → consume 1 token → allowed
...
Bucket empty → request denied (429 Too Many Requests)
Tokens refill over time
```

Allows burst traffic up to bucket size, then throttles.

**2. Fixed Window Counter:**

```
Window: 1 minute
Limit: 100 requests per window

00:00-01:00 → 100 requests allowed
01:00-02:00 → counter resets → 100 requests allowed
```

Simple but edge case: 100 requests at 00:59 + 100 at 01:01 = 200 in 2 seconds.

**3. Sliding Window** — fixes fixed window edge case:

```
At any point in time, count requests in last 60 seconds
Always rolling — no hard boundary edge case
```

More accurate but more memory intensive.

**4. Leaky Bucket** — smooths traffic:

```
Requests enter bucket at any rate
Bucket "leaks" (processes) at fixed rate
Overflow is dropped
```

Useful when you need consistent output rate regardless of burst input.

### 9. Caching Patterns

#### Intent

Store frequently accessed data in a **fast, temporary store** (Redis, Memcached) to reduce latency and database load.
#### Three main patterns

**Cache-Aside (most common):**

```
Read:
  App checks cache → HIT → return data
                  → MISS → read DB → store in cache → return data

Write:
  App writes to DB → invalidate cache entry
```

Application owns cache logic. Simple and flexible. Cache can be out of sync briefly.

```go
func (s *UserService) GetUser(id int) (User, error) {
    // 1. Check cache
    cacheKey := fmt.Sprintf("user:%d", id)
    cached, err := s.redis.Get(ctx, cacheKey).Result()
    if err == nil {
        var user User
        json.Unmarshal([]byte(cached), &user)
        return user, nil // cache hit
    }

    // 2. Cache miss — fetch from DB
    user, err := s.db.QueryUser(id)
    if err != nil { return User{}, err }

    // 3. Store in cache with TTL
    data, _ := json.Marshal(user)
    s.redis.SetEX(ctx, cacheKey, data, 5*time.Minute)

    return user, nil
}
```

**Read-Through:**

```
App ──► Cache ──► DB (cache handles DB fetch automatically)
```

Cache sits transparently between app and DB. On miss, cache fetches from DB itself. App always talks to cache only.

**Write-Through:**

```
App writes ──► Cache ──► DB (synchronously)
```

Every write goes through cache to DB. Cache always consistent with DB. Write latency slightly higher.

**Write-Back (Write-Behind):**

```
App writes ──► Cache (acknowledge immediately)
                  │
                  └──► DB (asynchronous, batched)
```

Fast writes — app gets acknowledgment immediately. DB written asynchronously. Risk: data loss if cache fails before DB write.

#### Cache eviction policies

|Policy|Description|Use when|
|---|---|---|
|**LRU** (Least Recently Used)|Evict least recently accessed|General purpose|
|**LFU** (Least Frequently Used)|Evict least accessed overall|Frequency matters|
|**TTL** (Time To Live)|Expire after fixed duration|Time-sensitive data|
|**Write-through invalidation**|Invalidate on write|Strong consistency needed|

#### Cache stampede problem

All cache entries for a popular key expire simultaneously → all requests hit DB at once:

```go
// Solution: probabilistic early expiration
func GetWithStampedeProtection(key string, ttl time.Duration) (string, error) {
    val, remainingTTL, err := getWithTTL(key)
    if err != nil { // cache miss
        return fetchFromDB(key)
    }

    // Probabilistically refresh before expiry
    // More likely to refresh as TTL gets closer to 0
    if rand.Float64() > float64(remainingTTL)/float64(ttl) {
        go refreshCache(key) // background refresh
    }

    return val, nil
}
```

### 10. Load Balancer

#### Intent

**Distribute incoming traffic** across multiple service instances to maximize throughput, minimize latency, and ensure no single instance is overwhelmed.

---

#### Why you need it

```
Without load balancer:
Client ──► Service Instance 1 (100% of traffic, overwhelmed)
           Service Instance 2 (idle)
           Service Instance 3 (idle)

With load balancer:
Client ──► Load Balancer ──► Service Instance 1 (~33%)
                         ──► Service Instance 2 (~33%)
                         ──► Service Instance 3 (~33%)
```

#### Algorithms

**Round Robin** — requests distributed sequentially:

```
Request 1 → Instance 1
Request 2 → Instance 2
Request 3 → Instance 3
Request 4 → Instance 1 (cycle repeats)
```

Simple. Good when all instances have equal capacity.

**Weighted Round Robin:**

```
Instance 1 (weight 3): gets 3 out of every 5 requests
Instance 2 (weight 2): gets 2 out of every 5 requests
```

Good when instances have different capacities.

**Least Connections:**

```
Route to instance with fewest active connections
```

Good for long-lived connections (WebSockets, file uploads).

**IP Hash:**

```
hash(clientIP) % instanceCount → always same instance
```

Ensures same client always hits same instance — useful for session affinity.

**Least Response Time:**

```
Route to instance with lowest average response time
```

Most intelligent — accounts for both load and instance health.

#### Layer 4 vs Layer 7
```
Layer 4 (Transport):
  Routes based on IP + port only
  Faster — no content inspection
  Can't route based on URL path or headers

Layer 7 (Application):
  Routes based on HTTP headers, URL, cookies
  Slower — must parse HTTP
  Can do path-based routing, SSL termination, sticky sessions
```

Your Nginx is a **Layer 7 load balancer**:

```nginx
upstream expense_service {
    least_conn; # algorithm
    server expense-1:3000 weight=3;
    server expense-2:3000 weight=2;
    server expense-3:3000;
    keepalive 32;
}

server {
    location /api/expenses {
        proxy_pass http://expense_service;
    }
}
```

### 11. Sharding

#### Intent

**Horizontally partition data** across multiple database instances, where each instance (shard) holds a subset of the total data.
#### The Problem

```
Single DB:
┌─────────────────────────────────────────┐
│  expenses table: 500 million rows        │
│  All reads + writes hit this one server  │
│  → CPU bottleneck, storage limit         │
└─────────────────────────────────────────┘
```

Vertical scaling (bigger server) has limits. You need to split the data.

#### Sharding strategies

**Range-based sharding:**

```
Shard 1: userId 1 – 1,000,000
Shard 2: userId 1,000,001 – 2,000,000
Shard 3: userId 2,000,001 – 3,000,000
```

Simple. Risk: **hotspots** if new users concentrate on last shard.

**Hash-based sharding:**

```
shardId = hash(userId) % numberOfShards

userId 101  → hash → shard 2
userId 202  → hash → shard 0
userId 303  → hash → shard 1
```

Even distribution. Problem: **resharding is expensive** when you add shards.

**Directory-based sharding:**

```
Lookup Service:
  userId 101 → Shard 2
  userId 202 → Shard 0

App queries lookup service → gets shard → queries correct DB
```

Most flexible — can move data between shards without rehashing. But lookup service is a single point of failure.

#### Consistent Hashing — solves resharding
```
Hash ring: 0 ────────────────────────── 360°

Shards placed on ring:
  Shard A at 60°
  Shard B at 180°
  Shard C at 300°

Data key hashed to position:
  Key X at 90°  → goes to Shard B (next clockwise)
  Key Y at 200° → goes to Shard C
  Key Z at 320° → goes to Shard A (wraps around)

Adding Shard D at 240°:
  Only keys between 180°-240° move from C to D
  Everything else unchanged
```
Adding/removing shards only affects **neighboring keys**, not the entire dataset. This is how Redis Cluster, Cassandra, and DynamoDB work.

#### Sharding challenges

- **Cross-shard queries** — `SELECT * FROM expenses WHERE department = 'Sales'` must query all shards and merge
- **Cross-shard transactions** — need Saga pattern
- **Resharding** — moving data when adding shards is expensive
- **Hot shards** — uneven data distribution causes some shards to be overloaded

### 12. Replication

#### Intent

**Maintain copies of data** on multiple nodes for high availability, fault tolerance, and read scalability.
#### Primary-Replica (Master-Slave)
```
			  ┌─────────────┐
Writes ──────►│   Primary   │
              │   (Master)  │
              └──────┬──────┘
                     │ replication
          ┌──────────┼──────────┐
          ▼          ▼          ▼
    ┌──────────┐ ┌──────────┐ ┌──────────┐
    │ Replica 1│ │ Replica 2│ │ Replica 3│
    │ (Slave)  │ │          │ │          │
    └──────────┘ └──────────┘ └──────────┘
         │            │            │
         └────────────┴────────────┘
                   Reads
```

- All **writes go to primary** only
- **Reads distributed** across replicas — scales read throughput
- Replicas **lag behind** primary slightly — eventual consistency
- If primary fails → promote a replica to primary

**Your PostgreSQL setup should use this.** Reads (expense reports, dashboards) hit replicas. Writes (submitting expenses, approvals) hit primary.

#### Synchronous vs Asynchronous Replication

**Synchronous:**

```
Write to primary → wait for replica to confirm → acknowledge client
```

✅ No data loss — replica always up to date ❌ Higher write latency — must wait for replica

**Asynchronous:**

```
Write to primary → acknowledge client immediately
                → replicate to replica in background
```

✅ Low write latency ❌ Replica can lag — risk of data loss if primary fails before replication

Most systems use **asynchronous** with at least one **synchronous** replica for the best balance.

#### Multi-Primary (Multi-Master)

```
Primary 1 ◄──► Primary 2
(writes)        (writes)
```

Both nodes accept writes. Conflict resolution needed when same record updated on both simultaneously. Used in geographically distributed systems (active-active across regions).

#### Replication vs Sharding — key distinction

|                    | Replication                             | Sharding                    |
| ------------------ | --------------------------------------- | --------------------------- |
| **Purpose**        | Availability + read scale               | Write scale + storage scale |
| **Data**           | Full copy on each node                  | Subset on each node         |
| **Reads**          | Any replica can serve any read          | Must route to correct shard |
| **Writes**         | Only primary                            | Distributed across shards   |
| **Used together?** | ✅ Yes — each shard has its own replicas |                             |

In production: **shard your data, replicate each shard**.

```
Shard 1 Primary + Shard 1 Replica 1 + Shard 1 Replica 2
Shard 2 Primary + Shard 2 Replica 1 + Shard 2 Replica 2
```

### Complete System Design Patterns — Full Map
```
┌─────────────────────────────────────────────────────────────────┐
│                    ARCHITECTURE                                 │
│                                                                 │
│  Microservices ──creates problems──► solved by all others       │
│  Event-Driven  ──enables──► loose coupling, async processing    │
│  CQRS          ──enables──► separate read/write optimization    │
│  Saga          ──enables──► distributed transactions            │
│  API Gateway   ──enables──► single entry point, cross-cutting   │
├─────────────────────────────────────────────────────────────────┤
│                    RESILIENCE                                   │
│                                                                 │
│  Circuit Breaker ──prevents──► cascading failures               │
│  Retry           ──handles──► transient failures                │
│  Rate Limiting   ──prevents──► overload + abuse                 │
├─────────────────────────────────────────────────────────────────┤
│                    SCALABILITY                                  │
│                                                                 │
│  Caching       ──reduces──► latency + DB load                   │
│  Load Balancer ──distributes──► traffic across instances        │
│  Sharding      ──splits──► data horizontally                    │
│  Replication   ──copies──► data for availability + reads        │
└─────────────────────────────────────────────────────────────────┘
```

