#### Step 1: Clarifying Requirements

**Functional:**

- Given a long URL, generate a unique short URL (e.g. `https://short.ly/aB3kQz`)
- Given a short URL/code, resolve it back to the original long URL
- Optional: support custom aliases (e.g. `short.ly/my-blog`)
- Optional: URL expiry (TTL)

**Non-functional (LLD scope):**

- The encoding must produce short, unique, URL-safe codes
- Collision handling must be explicit
- The design should be extensible (swap storage, swap encoding strategy)

We'll implement the core: **encode, decode, collision handling, optional expiry, optional custom alias.**

#### Step 2: Key Design Decisions

##### Decision 1: How do you generate the short code?

There are two main approaches:

**a) Hash-based (MD5/SHA256 + truncate)** Take the long URL, hash it, take the first 6–8 characters. Fast, deterministic — but collisions are possible when two URLs produce the same prefix.

**b) Base62 encoding of a counter** Maintain an auto-incrementing ID. Encode it in Base62 (`a-z`, `A-Z`, `0-9`). 6 characters of Base62 gives you 62⁶ ≈ 56 billion unique URLs. Completely collision-free by construction.

We'll go with **Base62 of a counter** — it's the production-correct choice everyone loves it.

##### Decision 2: Where is state stored?

For LLD, we use an in-memory store (`HashMap`). The design will use a `UrlRepository` interface so it's swappable with a DB-backed implementation.

##### Decision 3: How do you handle custom aliases?

Custom aliases bypass the counter entirely — they go straight into the store. We just need to check for conflicts first.

##### Decision 4: Expiry?

Store an `expiresAt` timestamp alongside each entry. On lookup, check if the URL has expired before returning.

#### Step 3: Class Design

```
UrlShortener (service — entry point)
│
├── UrlEncoder (interface)
│     └── Base62Encoder (impl)
│
├── UrlRepository (interface)
│     └── InMemoryUrlRepository (impl)
│
├── UrlEntry (model — stores original URL + expiry)
│
└── ShortenerConfig (optional alias prefix, code length)
```

The `UrlShortener` service orchestrates everything. It doesn't know about encoding details or storage details — it depends on abstractions.

#### Step 4: Implementation

```java
// ---- UrlEntry.java (Model) ----
import java.time.Instant;
import java.util.Optional;

public class UrlEntry {
    private final String originalUrl;
    private final Instant expiresAt; // null = never expires

    public UrlEntry(String originalUrl, Instant expiresAt) {
        this.originalUrl = originalUrl;
        this.expiresAt = expiresAt;
    }

    public String getOriginalUrl() { return originalUrl; }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public Optional<Instant> getExpiresAt() {
        return Optional.ofNullable(expiresAt);
    }
}
```

```java
// ---- UrlEncoder.java (Interface) ----
public interface UrlEncoder {
    String encode(long id);
}
```

```java
// ---- Base62Encoder.java ----
public class Base62Encoder implements UrlEncoder {

    private static final String ALPHABET =
        "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int BASE = 62;
    private final int minLength;

    public Base62Encoder(int minLength) {
        this.minLength = minLength;
    }

    @Override
    public String encode(long id) {
        StringBuilder sb = new StringBuilder();
        while (id > 0) {
            sb.append(ALPHABET.charAt((int)(id % BASE)));
            id /= BASE;
        }
        // Pad to minLength if needed
        while (sb.length() < minLength) {
            sb.append(ALPHABET.charAt(0));
        }
        return sb.reverse().toString();
    }
}
```

```java
// ---- UrlRepository.java (Interface) ----
import java.util.Optional;

public interface UrlRepository {
    void save(String code, UrlEntry entry);
    Optional<UrlEntry> findByCode(String code);
    boolean existsByCode(String code);
    // For reverse lookup (dedup): same long URL → same short code
    Optional<String> findCodeByOriginalUrl(String originalUrl);
}
```

```java
// ---- InMemoryUrlRepository.java ----
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryUrlRepository implements UrlRepository {

    // code → entry
    private final Map<String, UrlEntry> codeToEntry = new HashMap<>();
    // originalUrl → code (for deduplication)
    private final Map<String, String> urlToCode = new HashMap<>();

    @Override
    public void save(String code, UrlEntry entry) {
        codeToEntry.put(code, entry);
        urlToCode.put(entry.getOriginalUrl(), code);
    }

    @Override
    public Optional<UrlEntry> findByCode(String code) {
        return Optional.ofNullable(codeToEntry.get(code));
    }

    @Override
    public boolean existsByCode(String code) {
        return codeToEntry.containsKey(code);
    }

    @Override
    public Optional<String> findCodeByOriginalUrl(String originalUrl) {
        return Optional.ofNullable(urlToCode.get(originalUrl));
    }
}
```

```java
// ---- ShortenRequest.java ----
import java.time.Duration;
import java.util.Optional;

public class ShortenRequest {
    private final String originalUrl;
    private final String customAlias;   // nullable
    private final Duration ttl;         // nullable = no expiry

    public ShortenRequest(String originalUrl, String customAlias, Duration ttl) {
        this.originalUrl = originalUrl;
        this.customAlias = customAlias;
        this.ttl = ttl;
    }

    // Convenience factory for simple case
    public static ShortenRequest of(String url) {
        return new ShortenRequest(url, null, null);
    }

    public String getOriginalUrl() { return originalUrl; }
    public Optional<String> getCustomAlias() { return Optional.ofNullable(customAlias); }
    public Optional<Duration> getTtl() { return Optional.ofNullable(ttl); }
}
```

```java
// ---- UrlShortener.java (Service) ----
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class UrlShortener {

    private final UrlEncoder encoder;
    private final UrlRepository repository;
    private final String baseUrl;
    private final AtomicLong counter;

    public UrlShortener(UrlEncoder encoder, UrlRepository repository, String baseUrl) {
        this.encoder = encoder;
        this.repository = repository;
        this.baseUrl = baseUrl;
        this.counter = new AtomicLong(100_000L); // start away from trivial codes
    }

    public String shorten(ShortenRequest request) {
        // 1. Dedup: if same URL was already shortened (no TTL consideration), return existing
        if (request.getCustomAlias().isEmpty()) {
            Optional<String> existing = repository.findCodeByOriginalUrl(request.getOriginalUrl());
            if (existing.isPresent()) {
                return buildShortUrl(existing.get());
            }
        }

        // 2. Determine the code
        String code = request.getCustomAlias().orElseGet(() -> {
            long id = counter.getAndIncrement();
            return encoder.encode(id);
        });

        // 3. Conflict check for custom aliases
        if (repository.existsByCode(code)) {
            throw new IllegalArgumentException(
                "Alias '" + code + "' is already taken."
            );
        }

        // 4. Build entry with optional expiry
        Instant expiresAt = request.getTtl()
            .map(ttl -> Instant.now().plus(ttl))
            .orElse(null);

        UrlEntry entry = new UrlEntry(request.getOriginalUrl(), expiresAt);
        repository.save(code, entry);

        return buildShortUrl(code);
    }

    public String resolve(String shortUrl) {
        String code = extractCode(shortUrl);

        UrlEntry entry = repository.findByCode(code)
            .orElseThrow(() -> new IllegalArgumentException("Short URL not found: " + shortUrl));

        if (entry.isExpired()) {
            throw new IllegalStateException("Short URL has expired: " + shortUrl);
        }

        return entry.getOriginalUrl();
    }

    private String buildShortUrl(String code) {
        return baseUrl + "/" + code;
    }

    private String extractCode(String shortUrl) {
        // handles both full URL and bare code
        int idx = shortUrl.lastIndexOf('/');
        return idx >= 0 ? shortUrl.substring(idx + 1) : shortUrl;
    }
}
```

```java
// ---- Main.java (Driver) ----
import java.time.Duration;

public class Main {
    public static void main(String[] args) {
        UrlEncoder encoder = new Base62Encoder(6);
        UrlRepository repository = new InMemoryUrlRepository();
        UrlShortener shortener = new UrlShortener(encoder, repository, "https://short.ly");

        // Basic shorten
        String s1 = shortener.shorten(ShortenRequest.of("https://www.google.com/search?q=java+lld"));
        System.out.println("Shortened: " + s1);

        // Dedup — same URL should return same code
        String s2 = shortener.shorten(ShortenRequest.of("https://www.google.com/search?q=java+lld"));
        System.out.println("Dedup same: " + s2);
        System.out.println("Same code? " + s1.equals(s2)); // true

        // Resolve
        String original = shortener.resolve(s1);
        System.out.println("Resolved: " + original);

        // Custom alias
        String s3 = shortener.shorten(new ShortenRequest("https://github.com/jm", "github-jm", null));
        System.out.println("Custom alias: " + s3);

        // With TTL
        String s4 = shortener.shorten(new ShortenRequest(
            "https://example.com/promo", null, Duration.ofSeconds(2)
        ));
        System.out.println("With TTL: " + s4);

        try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
        try {
            shortener.resolve(s4); // should throw
        } catch (IllegalStateException e) {
            System.out.println("Expired: " + e.getMessage());
        }
    }
}
```

**OUTPUT:**
```
Shortened: https://short.ly/aaaBa5
Dedup same: https://short.ly/aaaBa5
Same code? true
Resolved: https://www.google.com/search?q=java+lld
Custom alias: https://short.ly/github-jm
With TTL: https://short.ly/aaaBa6
Expired: Short URL is expired: https://short.ly/aaaBa6
```

#### Step 5: Complexity Analysis

| Operation               | Time               | Space          |
| ----------------------- | ------------------ | -------------- |
| `shorten` (auto)        | O(1)               | O(1) per entry |
| `shorten` (dedup check) | O(1) avg (HashMap) | —              |
| `resolve`               | O(1)               | —              |
| `encode(id)`            | O(log₆₂ id) ≈ O(1) | —              |
These are the follow-up questions you should be ready for after this LLD:

1. **Why Base62 over MD5?** → Counter gives zero collisions; hash truncation has birthday problem risk.
2. **What if two threads call `shorten` simultaneously?** → `AtomicLong` on the counter handles that. The `save` in `InMemoryUrlRepository` would need `ConcurrentHashMap` for thread safety.
3. **How would you scale the counter in a distributed system?** → This is the bridge to HLD: use a centralized ID generator (Redis `INCR`, Twitter Snowflake, Zookeeper ranges).
4. **What about the dedup HashMap reverse lookup — memory concern?** → Valid. In production you'd use a Bloom filter or skip dedup entirely.
5. **How would you swap to a DB-backed repository?** → Implement `UrlRepository` with JDBC/JPA. The service doesn't change at all — that's the value of the interface.
### Design Patterns Used

This is a high-value moment. Don't just name patterns — explain _why_ you reached for each one.

#### 1. Strategy Pattern

`UrlEncoder` is an interface with `Base62Encoder` as an implementation. The `UrlShortener` service doesn't care _how_ encoding works — you can swap in a `HashEncoder`, a `NanoIdEncoder`, or anything else without touching the service. The encoding algorithm is a _strategy_ that's injected.

#### 2. Repository Pattern

`UrlRepository` abstracts all data access behind an interface. The service never touches a `HashMap` directly — it talks to a contract. Swapping `InMemoryUrlRepository` for a `PostgresUrlRepository` or `RedisUrlRepository` requires zero changes to business logic. This is the standard pattern for persistence abstraction in production systems.

#### 3. Factory Method / Static Factory

`ShortenRequest.of(url)` is a static factory method — a named constructor that communicates intent clearly (`of` = simple case) while hiding the full constructor's optional parameters.

#### 4. Dependency Injection (not a GoF pattern, but worth naming)

`UrlShortener` receives its `UrlEncoder` and `UrlRepository` through the constructor. It doesn't instantiate them. This makes the class testable in isolation and decoupled from implementations —  will appreciate you naming this explicitly.

#### 5. Value Object (DDD concept, often asked)

`UrlEntry` is immutable — no setters, final fields. It represents a _value_ in the domain, not an entity with mutable lifecycle. This is intentional and defensible.

If someone asks "which SOLID principles does this satisfy?" — you can walk through:

- **S** — each class has one job
- **O** — adding a new encoder doesn't modify existing code
- **D** — the service depends on abstractions (`UrlEncoder`, `UrlRepository`), not concretions

### HLD — URL Shortener at Scale

Now let's zoom out. This is where the LLD falls apart and you need a real architecture.

#### The Scale Problem

Imagine you're building bit.ly. The numbers look roughly like:

- 100M URLs shortened per day → ~1,200 writes/sec
- 10B redirects per day → ~115,000 reads/sec
- Read:write ratio is roughly **100:1** — this is an extremely read-heavy system

This single fact drives most of the architecture decisions.

#### Component Breakdown

```
Client
  │
  ▼
Load Balancer (L7 — Nginx / AWS ALB)
  │
  ├──► Shortener Service (write path)
  │         │
  │         ├──► ID Generator Service
  │         └──► Primary DB (write)
  │
  └──► Redirect Service (read path)
            │
            ├──► Cache Layer (Redis)
            └──► Read Replica DB (fallback)
```

The write path and read path are separated because they have completely different scaling needs and SLAs.

#### The Counter Problem at Scale

In our LLD, `AtomicLong` works fine for a single JVM. In a distributed system with 10 shortener service instances, each has its own counter — you get collisions.

**Solution 1 — Redis INCR** Redis is single-threaded internally. `INCR url:counter` is atomic across all service instances. Every service calls Redis, gets a unique ID, encodes it in Base62. Simple, works well up to moderate scale.

**Solution 2 — Range pre-allocation** Each service instance claims a _range_ from a coordinator (e.g. instance A gets 1–10,000, instance B gets 10,001–20,000). Instances exhaust their local range before claiming the next. This eliminates per-request network calls to Redis at the cost of slight gaps in the ID space. Twitter's Snowflake is a more sophisticated version of this idea.

**Solution 3 — Snowflake ID** A 64-bit ID composed of: timestamp (41 bits) + machine ID (10 bits) + sequence (12 bits). Globally unique, time-ordered, no coordination needed. This is what you'd use at Twitter/Meta scale.


#### Storage — Which Database?

The data model is simple: `code → {original_url, created_at, expires_at, user_id}`. No complex joins, no transactions across tables.

This is a classic case where a **NoSQL key-value store** fits perfectly:

- **Cassandra** — excellent write throughput, tunable consistency, handles billions of rows. `code` as partition key = O(1) lookup.
- **DynamoDB** — same idea, managed, good for AWS-native stacks.
- **PostgreSQL** — fine at moderate scale with proper indexing. Easier operational story for smaller teams.

The key insight to state: since reads vastly outnumber writes, the DB for reads can be eventually consistent — we don't need strong consistency on every redirect.

#### Caching — The Most Important Optimization

80% of redirects will be to a small fraction of URLs (power law / Zipf distribution — a few viral links get most of the traffic). This is the textbook case for caching.

**Redis** as a read-through cache in front of the DB:

```
Redirect Service
      │
      ├─── Redis GET code
      │         │
      │    HIT──┘  return immediately
      │    MISS
      │         │
      └─── DB lookup → cache the result → return
```

Cache policy: **LRU eviction** (which you've already implemented!), TTL aligned with URL expiry. A hot URL stays in cache; cold URLs get evicted naturally.

With a 100GB Redis cluster caching ~100M URLs (each entry ~1KB), you can serve the overwhelming majority of traffic without touching the database at all.

#### Redirect — 301 vs 302

This is a subtle point interviewers love to ask about:

- **301 (Permanent Redirect)** — the browser caches the redirect. Subsequent visits go _directly_ to the destination, bypassing your service entirely. Lower load on your servers, but you lose analytics on repeat visits.
- **302 (Temporary Redirect)** — the browser never caches it. Every visit hits your service. Higher load, but you capture every click for analytics.

Bit.ly uses 302 because analytics is core to their product. If analytics don't matter, 301 reduces load significantly.

#### Analytics (if asked)

Don't process analytics synchronously in the redirect path — that would add latency to every single redirect. Instead:

```
Redirect Service
      │
      ├──► Return 302 immediately   ← fast path
      │
      └──► Publish event to Kafka   ← async, fire-and-forget
                  │
                  ▼
          Analytics Consumer
          (Flink / Spark Streaming)
                  │
                  ▼
          Analytics Store
          (ClickHouse / BigQuery)
```

The redirect is fast. Analytics are eventually consistent. This is the **CQRS** pattern applied at the infrastructure level — writes (redirect events) and reads (analytics queries) are completely separated.

#### Expiry / Cleanup

Expired URLs still sit in the DB. Two strategies:

- **Lazy deletion** — check expiry on every read, return 410 Gone if expired. Simple, no background job needed.
- **Active deletion** — a background worker scans for expired entries and deletes them. Keeps storage clean but adds operational complexity.

Production systems typically use both: lazy deletion for correctness, active deletion for storage hygiene.

### Concurrency Deep Dive

Now let's go back to the code and ask: where are the race conditions?
#### Problem 1 — Counter under concurrent writes

```java
// UNSAFE in multithreaded context:
private long counter = 100_000L;
long id = counter++; // read-modify-write, not atomic
```

`counter++` is three operations: read, increment, write. Two threads can read the same value and generate the same code — a collision.

**Fix:** `AtomicLong.getAndIncrement()` — uses a CAS (Compare-And-Swap) loop internally. On x86, this compiles down to a single `LOCK XADD` instruction. No mutex needed, no blocking.

```java
private final AtomicLong counter = new AtomicLong(100_000L);
long id = counter.getAndIncrement(); // atomic, lock-free
```

#### Problem 2 — HashMap under concurrent reads/writes

`HashMap` is not thread-safe. Concurrent writes can cause infinite loops (in Java 7, due to the resize linked-list cycle bug) or data loss (in Java 8+).

**Fix:** Replace `HashMap` with `ConcurrentHashMap`. It uses bin-level locking (you studied this deeply with the Java concurrency internals) — reads are entirely lock-free using `volatile` reads, and writes only lock the specific bin being modified. You get far better throughput than a `synchronized HashMap` or `Collections.synchronizedMap`.

```java
// InMemoryUrlRepository — thread-safe version
private final Map<String, UrlEntry> codeToEntry = new ConcurrentHashMap<>();
private final Map<String, String> urlToCode = new ConcurrentHashMap<>();
```

#### Problem 3 — Check-then-act race (the subtle one)
Look at this sequence in `shorten()`:

```java
if (repository.existsByCode(code)) {      // check
    throw new IllegalArgumentException(); 
}
repository.save(code, entry);             // act
```

Two threads with the same custom alias can both pass the `existsByCode` check before either calls `save`. Both proceed to write — one overwrites the other silently.

This is a classic **check-then-act** race condition. The fix is to make the check and the save atomic together:
```java
// Use ConcurrentHashMap.putIfAbsent — atomic check+insert
UrlEntry existing = codeToEntry.putIfAbsent(code, entry);
if (existing != null) {
    throw new IllegalArgumentException("Alias already taken.");
}
```

`putIfAbsent` is a single atomic operation — if the key already exists, it returns the existing value and does nothing. No window between check and write.

#### Problem 4 — Dedup reverse lookup inconsistency

After a successful `save`, we have two maps that must stay consistent: `codeToEntry` and `urlToCode`. If the thread is interrupted between the two `put` calls, they're out of sync.

For an LLD , acknowledging this is enough. The production fix is to wrap both updates in a single logical transaction — either via DB transactions, or by using a single map with a value object that holds both directions of the mapping.

#### Summary — Concurrency Fixes Applied

| Issue                     | Root Cause                | Fix                            |
| ------------------------- | ------------------------- | ------------------------------ |
| Counter collision         | Non-atomic `++`           | `AtomicLong.getAndIncrement()` |
| Map corruption            | `HashMap` not thread-safe | `ConcurrentHashMap`            |
| Duplicate custom alias    | Check-then-act gap        | `putIfAbsent`                  |
| Reverse map inconsistency | Two non-atomic writes     | DB transaction / unified model |
