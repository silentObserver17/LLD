# State Design Pattern

#### Intent

Allow an object to **alter its behavior when its internal state changes**. The object will appear to **change its class**. Instead of giant if-else chains checking state, each state is its own class that knows what to do.

#### The Core Problem

Your expense approval workflow. An expense moves through states:

```
PENDING → COORDINATOR_REVIEW → HOD_REVIEW → FINANCE_REVIEW → APPROVED
                                                            → REJECTED
```

Without State pattern:

```java
public class Expense {

    private String status = "PENDING";

    public void submit() {
        if (status.equals("PENDING")) {
            status = "COORDINATOR_REVIEW";
            System.out.println("Submitted for coordinator review");
        } else {
            System.out.println("Can only submit a PENDING expense");
        }
    }

    public void approve() {
        if (status.equals("COORDINATOR_REVIEW")) {
            status = "HOD_REVIEW";
            System.out.println("Coordinator approved, escalated to HOD");
        } else if (status.equals("HOD_REVIEW")) {
            status = "FINANCE_REVIEW";
            System.out.println("HOD approved, escalated to Finance");
        } else if (status.equals("FINANCE_REVIEW")) {
            status = "APPROVED";
            System.out.println("Finance approved. Expense fully approved!");
        } else {
            System.out.println("Cannot approve in current state: " + status);
        }
    }

    public void reject() {
        if (status.equals("COORDINATOR_REVIEW")
                || status.equals("HOD_REVIEW")
                || status.equals("FINANCE_REVIEW")) {
            status = "REJECTED";
            System.out.println("Expense rejected");
        } else {
            System.out.println("Cannot reject in current state: " + status);
        }
    }

    public void resubmit() {
        if (status.equals("REJECTED")) {
            status = "PENDING";
            System.out.println("Expense resubmitted");
        } else {
            System.out.println("Can only resubmit a REJECTED expense");
        }
    }
}
```

Problems:

- Every method is a giant if-else on `status`
- Adding a new state means touching **every single method**
- State transition rules are scattered everywhere
- Impossible to read what's valid in each state at a glance
- Violates OCP badly — it never stops changing

State pattern says — **extract each state into its own class**. Each state class knows its own valid transitions and behavior.

#### The Core Idea

```
Context (Expense)
    └── holds current State object
    └── delegates all behavior to current State
    └── state can replace itself when transitioning

State (interface)
    └── submit()
    └── approve()
    └── reject()
    └── resubmit()

ConcreteStates
    └── PendingState
    └── CoordinatorReviewState
    └── HODReviewState
    └── FinanceReviewState
    └── ApprovedState
    └── RejectedState
```

When the context calls `approve()`, it delegates to the current state. The state handles it and **swaps the context's state** to the next one if valid. The context never contains a single if-else.

#### Structure

```
Client ──► Context.approve()
                │
                ▼
         currentState.approve(context)
                │
                ▼
         state validates, executes,
         then calls context.setState(nextState)
```

#### Implementation in Go
```go
// State interface
type ExpenseState interface {
    Submit(ctx *ExpenseContext)
    Approve(ctx *ExpenseContext, role string)
    Reject(ctx *ExpenseContext, reason string)
    Resubmit(ctx *ExpenseContext)
    Name() string
}

// Context
type ExpenseContext struct {
    ID     int
    Amount float64
    state  ExpenseState
}

func NewExpenseContext(id int, amount float64) *ExpenseContext {
    return &ExpenseContext{ID: id, Amount: amount, state: &PendingState{}}
}

func (c *ExpenseContext) SetState(state ExpenseState) {
    fmt.Printf("  State: %s → %s\n", c.state.Name(), state.Name())
    c.state = state
}

func (c *ExpenseContext) Submit()                    { c.state.Submit(c) }
func (c *ExpenseContext) Approve(role string)        { c.state.Approve(c, role) }
func (c *ExpenseContext) Reject(reason string)       { c.state.Reject(c, reason) }
func (c *ExpenseContext) Resubmit()                  { c.state.Resubmit(c) }

// Concrete States
type PendingState struct{}

func (s *PendingState) Submit(ctx *ExpenseContext) {
    fmt.Printf("Expense #%d submitted for coordinator review\n", ctx.ID)
    ctx.SetState(&CoordinatorReviewState{})
}
func (s *PendingState) Approve(ctx *ExpenseContext, role string) {
    fmt.Println("✗ Cannot approve — not yet submitted")
}
func (s *PendingState) Reject(ctx *ExpenseContext, reason string) {
    fmt.Println("✗ Cannot reject — not yet submitted")
}
func (s *PendingState) Resubmit(ctx *ExpenseContext) {
    fmt.Println("✗ Was never rejected")
}
func (s *PendingState) Name() string { return "PENDING" }

type CoordinatorReviewState struct{}

func (s *CoordinatorReviewState) Submit(ctx *ExpenseContext) {
    fmt.Println("✗ Already submitted")
}
func (s *CoordinatorReviewState) Approve(ctx *ExpenseContext, role string) {
    if role != "COORDINATOR" {
        fmt.Println("✗ Only Coordinator can approve here")
        return
    }
    fmt.Printf("✓ Coordinator approved expense #%d\n", ctx.ID)
    if ctx.Amount < 20000 {
        ctx.SetState(&ApprovedState{})
    } else {
        ctx.SetState(&HODReviewState{})
    }
}
func (s *CoordinatorReviewState) Reject(ctx *ExpenseContext, reason string) {
    fmt.Printf("✗ Rejected: %s\n", reason)
    ctx.SetState(&RejectedState{})
}
func (s *CoordinatorReviewState) Resubmit(ctx *ExpenseContext) {
    fmt.Println("✗ Under review — cannot resubmit")
}
func (s *CoordinatorReviewState) Name() string { return "COORDINATOR_REVIEW" }

// ApprovedState, RejectedState, HODReviewState follow same pattern...
```

#### State Transition Diagram
```
				    ┌─────────┐
               ┌───►  PENDING │◄──────────────────┐
               │    └────┬────┘                   │
               │         │ submit()               │ resubmit()
               │         ▼                        │
               │  ┌──────────────┐                │
               │  │ COORDINATOR  │─── reject() ──►┤
               │  │   REVIEW     │                │
               │  └──────┬───────┘           ┌────┴──────┐
               │         │ approve()         │  REJECTED │
               │    ┌────┴────┐              └───────────┘
               │    │amount   │
               │  <20k     >=20k
               │    │         │
               │    ▼         ▼
               │ APPROVED  ┌──────────┐
               │           │   HOD    │─── reject() ──► REJECTED
               │           │  REVIEW  │
               │           └────┬─────┘
               │                │ approve()
               │           ┌────┴────┐
               │           │ amount  │
               │         <1L       >=1L
               │           │         │
               │           ▼         ▼
               └────── APPROVED  ┌─────────────┐
                                 │   FINANCE   │─── reject() ──► REJECTED
                                 │   REVIEW    │
                                 └──────┬──────┘
                                        │ approve()
                                        ▼
                                    APPROVED
```

#### State vs Strategy — the most common interview confusion

They look structurally identical — both have an interface with a context holding a reference to a concrete implementation. The difference is intent and who controls switching:

| |Strategy|State|
|---|---|---|
|**Who swaps?**|Client explicitly sets strategy|State swaps itself internally|
|**Aware of other states?**|❌ Strategies don't know each other|✅ States know what comes next|
|**Transitions?**|No concept of transition|Core concept|
|**Use when**|Interchangeable algorithms|Object lifecycle with valid transitions|

Strategy — _"I want to use a different algorithm"_ — client decides. State — _"My behavior changes because my state changed"_ — object decides internally.

#### Real-world examples

- **TCP Connection** — `CLOSED → LISTEN → SYN_RECEIVED → ESTABLISHED → FIN_WAIT` — each state handles packets differently
- **Order lifecycle** — `PLACED → CONFIRMED → SHIPPED → DELIVERED → RETURNED`
- **Vending machine** — `IDLE → HAS_MONEY → DISPENSING → OUT_OF_STOCK` — each state handles button presses differently
- **Your auth token** — `ACTIVE → EXPIRED → REVOKED` — each state validates differently in your middleware
- **Traffic light** — `RED → GREEN → YELLOW → RED` — classic textbook example

---

#### Key Principles it satisfies

- **Open/Closed** — add new states without touching context or other states
- **Single Responsibility** — each state owns its own behavior and transition logic
- **Eliminates conditionals** — no if-else on status anywhere in the codebase

