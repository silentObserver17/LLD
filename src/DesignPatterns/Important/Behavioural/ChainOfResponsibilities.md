# Chain of Responsibility Design Pattern

#### Intent

Pass a request along a **chain of handlers**. Each handler decides either to **process the request** or **pass it to the next handler** in the chain. The sender doesn't know which handler will process it.

#### The Core Problem

Your expense approval workflow. A submitted expense needs to go through multiple approval levels — but the rules are:

- Amount < ₹5,000 → Coordinator can approve
- Amount < ₹20,000 → HOD can approve
- Amount < ₹1,00,000 → Finance can approve
- Amount ≥ ₹1,00,000 → CEO must approve

Without CoR:

```java
public void approve(Expense expense) {
    if (expense.getAmount() < 5000) {
        coordinator.approve(expense);
    } else if (expense.getAmount() < 20000) {
        hod.approve(expense);
    } else if (expense.getAmount() < 100000) {
        finance.approve(expense);
    } else {
        ceo.approve(expense);
    }
}
```

Problems:

- Adding a new approval level means modifying this method
- The logic of **who can approve what** is scattered here instead of owned by the approver
- Can't reorder or reconfigure the chain without touching this code
- No clean way to add cross-cutting logic like logging or escalation timeouts per level

CoR says — **each handler knows its own limit and who's next**. The request travels the chain until someone handles it.

#### Structure

```
Client
    └── sends request to first handler only

Handler (abstract)
    └── nextHandler reference
    └── setNext(Handler)
    └── handle(request) — process or pass forward

ConcreteHandlers
    └── CoordinatorHandler
    └── HODHandler
    └── FinanceHandler
    └── CEOHandler

Chain:
Request ──► Coordinator ──► HOD ──► Finance ──► CEO ──► (unhandled)
```

```
code in java file
```
The client only ever talks to `coordinator` — the first node. The chain handles the rest internally.

#### Implementation in Go
```go
// Handler interface
type ApprovalHandler interface {
    Handle(expense Expense)
    SetNext(handler ApprovalHandler) ApprovalHandler
}

// Base handler — holds next reference
type BaseHandler struct {
    next ApprovalHandler
}

func (b *BaseHandler) SetNext(next ApprovalHandler) ApprovalHandler {
    b.next = next
    return next
}

func (b *BaseHandler) PassToNext(expense Expense) {
    if b.next != nil {
        b.next.Handle(expense)
    } else {
        fmt.Printf("No handler could approve expense #%d. Rejected.\n", expense.ID)
    }
}

// Concrete Handlers
type CoordinatorHandler struct{ BaseHandler }

func (c *CoordinatorHandler) Handle(expense Expense) {
    if expense.Amount < 5000 {
        fmt.Printf("✓ Coordinator approved expense #%d of ₹%.0f\n",
            expense.ID, expense.Amount)
    } else {
        fmt.Println("Coordinator: Escalating...")
        c.PassToNext(expense)
    }
}

type HODHandler struct{ BaseHandler }

func (h *HODHandler) Handle(expense Expense) {
    if expense.Amount < 20000 {
        fmt.Printf("✓ HOD approved expense #%d of ₹%.0f\n",
            expense.ID, expense.Amount)
    } else {
        fmt.Println("HOD: Escalating...")
        h.PassToNext(expense)
    }
}

type FinanceHandler struct{ BaseHandler }

func (f *FinanceHandler) Handle(expense Expense) {
    if expense.Amount < 100000 {
        fmt.Printf("✓ Finance approved expense #%d of ₹%.0f\n",
            expense.ID, expense.Amount)
    } else {
        fmt.Println("Finance: Escalating...")
        f.PassToNext(expense)
    }
}

type CEOHandler struct{ BaseHandler }

func (c *CEOHandler) Handle(expense Expense) {
    fmt.Printf("✓ CEO approved expense #%d of ₹%.0f\n",
        expense.ID, expense.Amount)
}

// Build chain
func BuildApprovalChain() ApprovalHandler {
    coordinator := &CoordinatorHandler{}
    hod         := &HODHandler{}
    finance     := &FinanceHandler{}
    ceo         := &CEOHandler{}

    coordinator.SetNext(hod).SetNext(finance).SetNext(ceo)
    return coordinator
}

// Client
func main() {
    chain := BuildApprovalChain()
    chain.Handle(Expense{ID: 1, Amount: 3500})
    chain.Handle(Expense{ID: 2, Amount: 15000})
    chain.Handle(Expense{ID: 3, Amount: 75000})
    chain.Handle(Expense{ID: 4, Amount: 150000})
}
```

#### Variant — All handlers process, not just one

The classic CoR stops at the first handler that can process. But sometimes **every handler in the chain must process** the request — like middleware:

```java
// Middleware style — every handler runs, then passes forward
public abstract class MiddlewareHandler {

    private MiddlewareHandler next;

    public MiddlewareHandler setNext(MiddlewareHandler next) {
        this.next = next;
        return next;
    }

    public void handle(HttpRequest request) {
        process(request);          // always runs own logic
        if (next != null) {
            next.handle(request);  // always passes forward
        }
    }

    protected abstract void process(HttpRequest request);
}

public class LoggingMiddleware extends MiddlewareHandler {
    @Override
    protected void process(HttpRequest request) {
        System.out.println("LOG: " + request.getMethod() + " " + request.getPath());
    }
}

public class AuthMiddleware extends MiddlewareHandler {
    @Override
    protected void process(HttpRequest request) {
        System.out.println("AUTH: Validating token...");
        // throw UnauthorizedException to break chain early
    }
}

public class RateLimitMiddleware extends MiddlewareHandler {
    @Override
    protected void process(HttpRequest request) {
        System.out.println("RATE: Checking rate limit for " + request.getIp());
    }
}

// Build and use
MiddlewareHandler pipeline = new LoggingMiddleware();
pipeline.setNext(new AuthMiddleware())
        .setNext(new RateLimitMiddleware());

pipeline.handle(incomingRequest);

// Output:
// LOG: POST /api/expenses
// AUTH: Validating token...
// RATE: Checking rate limit for 192.168.1.1
```

#### CoR vs Strategy vs Command

|                           | Strategy               | Command                  | CoR                             |
| ------------------------- | ---------------------- | ------------------------ | ------------------------------- |
| **How many handle?**      | One (selected upfront) | One (explicit invoker)   | One or all (decided at runtime) |
| **Sender knows handler?** | Yes                    | Yes (via invoker)        | ❌ No                            |
| **Runtime routing?**      | ❌ No                   | ❌ No                     | ✅ Yes                           |
| **Use when**              | Swapping algorithms    | Queuing/undoing requests | Dynamic handler selection       |

The key CoR differentiator — the **sender is completely decoupled** from which handler processes it.

#### Real-world Java examples

- **Java's exception handling** — `catch` blocks form a chain. If the first `catch` doesn't match the exception type, it propagates to the next
- **`javax.servlet.Filter`** — every servlet filter calls `filterChain.doFilter()` to pass to the next filter — textbook CoR
- **Spring Security filter chain** — `SecurityFilterChain` is CoR with 15+ built-in filters
- **Java logging** — `Logger` passes log records up to parent loggers until one handles it

---

#### Key Principles it satisfies

- **Open/Closed** — add new handlers without touching existing ones or the client
- **Single Responsibility** — each handler owns exactly one approval level's logic
- **Loose Coupling** — sender only knows the first handler, never the full chain
