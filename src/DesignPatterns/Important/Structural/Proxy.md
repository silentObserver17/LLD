# Proxy Design Pattern
#### Intent

Provide a **surrogate or placeholder** for another object to **control access** to it. The proxy sits between the client and the real object — intercepting calls to add logic like caching, access control, lazy initialization, or logging — all without the client knowing.

#### The Core Problem

You have a `DatabaseService` that runs expensive queries. Every call hits the DB directly:

java

```java
UserService service = new DatabaseUserService();
service.getUser(101); // hits DB
service.getUser(101); // hits DB again — same data, wasted round trip
service.getUser(101); // hits DB again
```

You want to add caching — but you don't want to touch `DatabaseUserService` (it's working fine, maybe it's third-party, maybe it violates SRP to add caching there).

The Proxy sits in front, intercepts the call, checks the cache first, and only hits the real service on a miss. The client never changes — it still just calls `getUser()`.

#### The Key Insight

Proxy and Decorator are **structurally identical** — both wrap an object and implement the same interface. The difference is purely **intent**:

```
Decorator → "I want to ADD behavior to this object"
Proxy     → "I want to CONTROL ACCESS to this object"
```

A caching proxy isn't adding a feature to the service — it's controlling _when and whether_ the real service gets called.

#### Types of Proxy

| Type                 | What it controls                             | Real-world example         |
| -------------------- | -------------------------------------------- | -------------------------- |
| **Cache Proxy**      | Avoids redundant calls by storing results    | Redis in front of DB       |
| **Protection Proxy** | Controls who can call what (auth/RBAC)       | Auth middleware            |
| **Virtual Proxy**    | Delays expensive initialization until needed | Lazy-loading ORM relations |
| **Remote Proxy**     | Represents object in another process/server  | gRPC stub, RMI             |
| **Logging Proxy**    | Transparently logs all calls                 | AOP logging in Spring      |
All five follow the exact same structure — only the logic inside differs.

#### Structure
```
Subject (interface)
    └── getUser(id)

RealSubject
    └── DatabaseUserService — the actual implementation

Proxy
    └── CachingUserServiceProxy
    └── holds reference to RealSubject
    └── implements Subject — client can't tell the difference

Client
    └── talks to Subject interface only
    └── never knows if it's hitting Proxy or Real
```

```
code in java file
```

This is **exactly** how your Redis layer in the auth system works — proxy sitting in front of Postgres, cache-aside pattern, invalidation on write.

#### Protection Proxy — RBAC enforcement

This maps directly to your auth middleware:
```java
public class ProtectedUserServiceProxy implements UserService {

    private final UserService realService;
    private final AuthContext authContext; // current user's session

    public ProtectedUserServiceProxy(UserService realService, AuthContext authContext) {
        this.realService = realService;
        this.authContext = authContext;
    }

    @Override
    public User getUser(int id) {
        // Any authenticated user can read
        if (!authContext.isAuthenticated()) {
            throw new UnauthorizedException("Login required");
        }
        return realService.getUser(id);
    }

    @Override
    public void saveUser(User user) {
        // Only admins can write
        if (!authContext.hasRole("ADMIN")) {
            throw new ForbiddenException("Admin role required to save users");
        }
        realService.saveUser(user);
    }

    @Override
    public void deleteUser(int id) {
        // Only super admins can delete
        if (!authContext.hasRole("SUPER_ADMIN")) {
            throw new ForbiddenException("Super admin role required to delete users");
        }
        realService.deleteUser(id);
    }
}
```

#### Virtual Proxy — Lazy Initialization

Expensive object that shouldn't be created until actually needed:

```java
public class LazyReportServiceProxy implements ReportService {

    private ReportService realService; // null until first use

    @Override
    public Report generateReport(String type) {
        if (realService == null) {
            System.out.println("Initializing heavy ReportService for the first time...");
            realService = new HeavyReportService(); // expensive — DB connections, config loading
        }
        return realService.generateReport(type);
    }
}
```

This is what Hibernate does with lazy-loaded relations — `user.getOrders()` returns a proxy that hits the DB only when you actually iterate the collection.

#### Implementation in Go

go

```go
type UserService interface {
    GetUser(id int) User
    SaveUser(user User)
    DeleteUser(id int)
}

// Real Subject
type DatabaseUserService struct{}

func (d *DatabaseUserService) GetUser(id int) User {
    fmt.Printf("DB HIT: Fetching user %d\n", id)
    return User{ID: id, Name: fmt.Sprintf("User_%d", id)}
}
func (d *DatabaseUserService) SaveUser(user User) {
    fmt.Printf("DB: Saving user %d\n", user.ID)
}
func (d *DatabaseUserService) DeleteUser(id int) {
    fmt.Printf("DB: Deleting user %d\n", id)
}

// Cache Proxy
type CachingUserServiceProxy struct {
    real  UserService
    cache map[int]User
}

func NewCachingProxy(real UserService) *CachingUserServiceProxy {
    return &CachingUserServiceProxy{
        real:  real,
        cache: make(map[int]User),
    }
}

func (p *CachingUserServiceProxy) GetUser(id int) User {
    if user, ok := p.cache[id]; ok {
        fmt.Printf("CACHE HIT: user %d\n", id)
        return user
    }
    fmt.Printf("CACHE MISS: user %d\n", id)
    user := p.real.GetUser(id)
    p.cache[id] = user
    return user
}

func (p *CachingUserServiceProxy) SaveUser(user User) {
    p.real.SaveUser(user)
    p.cache[user.ID] = user
}

func (p *CachingUserServiceProxy) DeleteUser(id int) {
    p.real.DeleteUser(id)
    delete(p.cache, id)
}

// Client
func main() {
    service := NewCachingProxy(&DatabaseUserService{})

    service.GetUser(101) // CACHE MISS → DB
    service.GetUser(101) // CACHE HIT
    service.GetUser(101) // CACHE HIT
}
```

---

#### Stacking Proxies

Since all proxies implement the same interface, you can **chain them**:

java

```java
UserService db       = new DatabaseUserService();
UserService cached   = new CachingUserServiceProxy(db);
UserService protected = new ProtectedUserServiceProxy(cached, authContext);
UserService logged   = new LoggingUserServiceProxy(protected);

// Request flows:
// Client → LoggingProxy → ProtectionProxy → CachingProxy → DB
```

This is exactly what Spring does — every `@Transactional`, `@Cacheable`, `@PreAuthorize` annotation wraps your bean in a proxy layer at startup. When you call a `@Transactional` method, you're going through a proxy that opens and commits the transaction around your real method call.

#### Dynamic Proxy in Java

Java supports generating proxies **at runtime** without writing a class — using `java.lang.reflect.Proxy`:

java

```java
UserService realService = new DatabaseUserService();

UserService loggingProxy = (UserService) Proxy.newProxyInstance(
    realService.getClass().getClassLoader(),
    new Class[]{ UserService.class },
    (proxy, method, args) -> {
        System.out.println("Calling: " + method.getName());
        long start = System.currentTimeMillis();

        Object result = method.invoke(realService, args); // delegate

        long elapsed = System.currentTimeMillis() - start;
        System.out.println(method.getName() + " took " + elapsed + "ms");
        return result;
    }
);

loggingProxy.getUser(101);
// Output:
// Calling: getUser
// DB HIT: Fetching user 101 from database...
// getUser took 12ms
```

This is the foundation of **Spring AOP** — every `@Transactional`, `@Cacheable`, `@Async` bean is a dynamic proxy generated at startup. You never write a proxy class — Spring generates one for each annotated bean at runtime.

---

#### All Structural Patterns so far — comparison

|Pattern|Wraps object?|Changes interface?|Primary intent|
|---|---|---|---|
|**Adapter**|✅ Yes|✅ Yes|Fix interface mismatch|
|**Decorator**|✅ Yes|❌ No|Add behavior|
|**Proxy**|✅ Yes|❌ No|Control access|

The three are structurally similar — always ask _why_ the wrapping is happening to identify which pattern it is.

---

#### Key Principles it satisfies

- **Open/Closed** — add caching, logging, auth without touching the real service
- **Single Responsibility** — caching lives in the proxy, business logic in the real service
- **Dependency Inversion** — client depends on `UserService` interface, never on the concrete proxy or real implementation