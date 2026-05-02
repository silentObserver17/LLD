# Strategy Design Pattern
#### Intent

Define a **family of algorithms**, encapsulate each one, and make them **interchangeable at runtime**. Strategy lets the algorithm vary independently from the clients that use it.

#### The Core Problem

You're building the **expense approval workflow**. Different expense types have different approval rules:

```java
public class ExpenseService {

    public void processApproval(Expense expense) {
        // Hardcoded logic — grows forever
        if (expense.getType().equals("TRAVEL")) {
            // travel approval logic
            if (expense.getAmount() < 5000) approveByCoordinator(expense);
            else if (expense.getAmount() < 20000) approveByHOD(expense);
            else approveByFinance(expense);

        } else if (expense.getType().equals("FOOD")) {
            // food approval logic
            if (expense.getAmount() < 1000) autoApprove(expense);
            else approveByCoordinator(expense);

        } else if (expense.getType().equals("EQUIPMENT")) {
            // equipment approval logic — different rules entirely
            approveByHOD(expense);
            approveByFinance(expense);
        }
        // adding CLIENT_ENTERTAINMENT means modifying this entire method
    }
}
```

Problems:

- Every new expense type means modifying `ExpenseService`
- All approval logic is tangled in one place — impossible to test in isolation
- Violates OCP — class is never closed for modification

Strategy says — **extract each algorithm into its own class** and swap them in at runtime.

#### The Core Idea
```
Context (ExpenseService)
    └── holds a reference to Strategy interface
    └── delegates algorithm execution to it
    └── can swap strategy at runtime

Strategy (interface)
    └── execute()

ConcreteStrategies
    └── TravelApprovalStrategy
    └── FoodApprovalStrategy
    └── EquipmentApprovalStrategy
```

The Context doesn't know or care which strategy is running — it just calls `execute()`.

#### Structure
```
Client
    └── picks the right strategy
    └── injects it into Context

Context
    └── setStrategy(Strategy)
    └── executeStrategy()  ──► delegates to Strategy.execute()

Strategy (interface)
    └── execute(data)

ConcreteStrategy A    ConcreteStrategy B    ConcreteStrategy C
    └── algorithm A       └── algorithm B       └── algorithm C
```

Adding `CLIENT_ENTERTAINMENT` approval rules tomorrow — create one new class, register it in the resolver. **Zero existing code changes.**

#### Implementation in Go

Go makes Strategy even leaner — strategies are just functions:

```go
// Strategy as a function type
type ApprovalStrategy func(expense Expense)

// Concrete strategies — plain functions
func TravelApproval(expense Expense) {
    fmt.Printf("Travel: Processing expense #%d\n", expense.ID)
    if expense.Amount < 5000 {
        fmt.Println("  → Approved by Coordinator")
    } else if expense.Amount < 20000 {
        fmt.Println("  → Escalated to HOD")
    } else {
        fmt.Println("  → Escalated to Finance")
    }
}

func FoodApproval(expense Expense) {
    fmt.Printf("Food: Processing expense #%d\n", expense.ID)
    if expense.Amount < 1000 {
        fmt.Println("  → Auto-approved")
    } else {
        fmt.Println("  → Sent to Coordinator")
    }
}

func EquipmentApproval(expense Expense) {
    fmt.Printf("Equipment: Processing expense #%d\n", expense.ID)
    fmt.Println("  → HOD + Finance approval required")
}

// Context
type ExpenseService struct {
    strategy ApprovalStrategy
}

func (s *ExpenseService) SetStrategy(strategy ApprovalStrategy) {
    s.strategy = strategy
}

func (s *ExpenseService) ProcessApproval(expense Expense) {
    s.strategy(expense)
}

// Strategy map — resolver equivalent
var strategyMap = map[string]ApprovalStrategy{
    "TRAVEL":    TravelApproval,
    "FOOD":      FoodApproval,
    "EQUIPMENT": EquipmentApproval,
}

// Client
func main() {
    service := &ExpenseService{}

    expenses := []Expense{
        {ID: 1, Type: "TRAVEL", Amount: 3500},
        {ID: 2, Type: "FOOD", Amount: 800},
        {ID: 3, Type: "EQUIPMENT", Amount: 45000},
    }

    for _, expense := range expenses {
        strategy, ok := strategyMap[expense.Type]
        if !ok {
            log.Fatalf("No strategy for type: %s", expense.Type)
        }
        service.SetStrategy(strategy)
        service.ProcessApproval(expense)
    }
}
```

In Go, the strategy is just a **first-class function** — no interface needed unless the strategies need to carry state.

#### Strategy + Spring — production Java pattern

In Spring, strategies are beans and the resolver uses injection:
```java
// Each strategy is a Spring bean
@Component
public class TravelApprovalStrategy implements ApprovalStrategy { ... }

@Component
public class FoodApprovalStrategy implements ApprovalStrategy { ... }

// Resolver auto-collects all ApprovalStrategy beans
@Service
public class ApprovalStrategyResolver {

    private final List<ApprovalStrategy> strategies;

    // Spring injects ALL ApprovalStrategy beans automatically
    public ApprovalStrategyResolver(List<ApprovalStrategy> strategies) {
        this.strategies = strategies;
    }

    public ApprovalStrategy resolve(Expense expense) {
        return strategies.stream()
            .filter(s -> s.canHandle(expense))
            .findFirst()
            .orElseThrow();
    }
}
```

Add a new strategy — just create the class with `@Component`. Spring picks it up automatically. The resolver never changes. This is the cleanest production-grade Strategy implementation in Java.

#### Strategy vs if-else vs Factory Method

Gets confused in interviews — clear distinction:

| |if-else|Factory Method|Strategy|
|---|---|---|---|
|**Creates objects?**|❌|✅ Yes|❌|
|**Swappable at runtime?**|❌|❌|✅ Yes|
|**Encapsulates algorithm?**|❌|❌|✅ Yes|
|**OCP compliant?**|❌|✅|✅|
|**Use when**|Simple branching|Object creation varies|Behavior varies|

Factory Method decides **what to create**. Strategy decides **how to behave**. They often work together — a Factory creates the right Strategy.

#### Real-world Java examples

- **`java.util.Comparator`** — the classic Strategy. `Collections.sort(list, comparator)` — the sorting algorithm is fixed, the comparison strategy is swapped:

```java
list.sort(Comparator.comparing(User::getName));          // sort by name
list.sort(Comparator.comparing(User::getAge).reversed()); // sort by age desc
```

- **Spring Security's `AuthenticationStrategy`** — different auth mechanisms (JWT, session, OAuth) are strategies plugged into the same security filter chain
- **`javax.xml.parsers.SAXParser` vs `DOMParser`** — two parsing strategies behind the same `parse()` interface

---

#### Key Principles it satisfies

- **Open/Closed** — new strategies added without touching context
- **Single Responsibility** — each strategy owns exactly one algorithm
- **Composition over Inheritance** — behavior injected, not inherited

