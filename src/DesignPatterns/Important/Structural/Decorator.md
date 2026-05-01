# Decorator Design Pattern
---
#### Intent

Attach **additional responsibilities to an object dynamically** without modifying its class or using inheritance. Decorators provide a flexible alternative to subclassing for extending behavior.

#### The Core Problem

You have a `Coffee` ordering system. You start with:

```java
class Espresso { cost = 50 }
```

Now customers want add-ons — milk, sugar, whipped cream. The naive approach is subclassing:

```java
class EspressoWithMilk extends Espresso { ... }
class EspressoWithMilkAndSugar extends EspressoWithMilk { ... }
class EspressoWithMilkAndSugarAndWhip extends EspressoWithMilkAndSugar { ... }
```

With just 4 add-ons, you'd need **2⁴ = 16 subclasses**. This is called the **class explosion problem**. It gets worse every time you add a new add-on.

Decorator solves this by **wrapping objects inside other objects** — stacking behavior at runtime instead of baking it into class hierarchies.

#### The Key Insight

A Decorator:
1. **Implements the same interface** as the object it wraps — so the client can't tell the difference
2. **Holds a reference** to the wrapped object — composition
3. **Delegates** to the wrapped object, then **adds its own behavior** before or after

This means decorators are **stackable** — you can wrap a decorator inside another decorator indefinitely.

#### Structure
```
Component (interface)
    └── cost(): double
    └── description(): String

ConcreteComponent
    └── Espresso — base object

BaseDecorator (abstract)
    └── wraps a Component
    └── delegates all calls to it

ConcreteDecorators
    └── MilkDecorator     — adds milk behavior
    └── SugarDecorator    — adds sugar behavior
    └── WhipDecorator     — adds whip behavior

Client
    Coffee c = new WhipDecorator(new MilkDecorator(new Espresso()));
```

```
java code in java file
```
Notice — the client never changed. You compose behavior entirely through wrapping.

#### How the call chain works

For `new WhipDecorator(new SugarDecorator(new MilkDecorator(new Espresso())))`:
```
getCost() called on WhipDecorator
    → delegates to SugarDecorator.getCost()
        → delegates to MilkDecorator.getCost()
            → delegates to Espresso.getCost() → 50.0
        ← MilkDecorator adds 10 → 60.0
    ← SugarDecorator adds 5 → 65.0
← WhipDecorator adds 20 → 85.0
```

Each layer in the stack adds its behavior and passes the call inward. This is the **chain of responsibility** nature of Decorator.

#### Implementation in Go

Go makes this even cleaner since interfaces are implicit:
```go
// Component interface
type Coffee interface {
    GetCost() float64
    GetDescription() string
}

// Concrete Component
type Espresso struct{}
func (e *Espresso) GetCost() float64        { return 50.0 }
func (e *Espresso) GetDescription() string  { return "Espresso" }

// Base Decorator — embeds Coffee interface
type BaseDecorator struct {
    coffee Coffee
}
func (b *BaseDecorator) GetCost() float64       { return b.coffee.GetCost() }
func (b *BaseDecorator) GetDescription() string { return b.coffee.GetDescription() }

// Concrete Decorators
type MilkDecorator struct{ BaseDecorator }
func (m *MilkDecorator) GetCost() float64 {
    return m.coffee.GetCost() + 10.0
}
func (m *MilkDecorator) GetDescription() string {
    return m.coffee.GetDescription() + ", Milk"
}

type SugarDecorator struct{ BaseDecorator }
func (s *SugarDecorator) GetCost() float64 {
    return s.coffee.GetCost() + 5.0
}
func (s *SugarDecorator) GetDescription() string {
    return s.coffee.GetDescription() + ", Sugar"
}

type WhipDecorator struct{ BaseDecorator }
func (w *WhipDecorator) GetCost() float64 {
    return w.coffee.GetCost() + 20.0
}
func (w *WhipDecorator) GetDescription() string {
    return w.coffee.GetDescription() + ", Whipped Cream"
}

// Constructor helpers
func NewMilkDecorator(c Coffee) *MilkDecorator {
    return &MilkDecorator{BaseDecorator{coffee: c}}
}
func NewSugarDecorator(c Coffee) *SugarDecorator {
    return &SugarDecorator{BaseDecorator{coffee: c}}
}
func NewWhipDecorator(c Coffee) *WhipDecorator {
    return &WhipDecorator{BaseDecorator{coffee: c}}
}

// Client
func main() {
    order := NewWhipDecorator(NewSugarDecorator(NewMilkDecorator(&Espresso{})))
    fmt.Printf("%s → ₹%.1f\n", order.GetDescription(), order.GetCost())
    // Espresso, Milk, Sugar, Whipped Cream → ₹85.0
}
```

#### Real-world Java examples

**`java.io` streams — the most classic Decorator usage in Java:**

```java
// Layers of decorators stacked on a base FileInputStream
InputStream base        = new FileInputStream("data.txt");      // base component
InputStream buffered    = new BufferedInputStream(base);         // adds buffering
InputStream compressed  = new GZIPInputStream(buffered);         // adds decompression
InputStreamReader chars = new InputStreamReader(compressed);     // adapts bytes→chars
BufferedReader reader   = new BufferedReader(chars);             // adds line reading

// Or the classic one-liner you write every day:
BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
```

Every wrapper here is a Decorator — same `InputStream`/`Reader` interface, new behavior added at each layer.

**Spring Security filter chain:**

```
Request → LoggingFilter → AuthFilter → CorsFilter → RateLimitFilter → Controller
```

Each filter wraps the next — pure Decorator. Each adds one concern and delegates the rest down the chain.

**Your own codebase** — your Express/Node.js middleware chain is Decorator in spirit:

```javascript
app.use(morgan(...))       // logging decorator
app.use(cors(...))         // cors decorator
app.use(authenticate(...)) // auth decorator
app.use(rateLimiter(...))  // rate limit decorator
router.post('/pay', handler)
```


#### Decorator vs Inheritance

| |Inheritance|Decorator|
|---|---|---|
|Behavior added|Compile time|Runtime|
|Combination of behaviors|Class explosion|Wrap and stack|
|Modifies original class?|No|No|
|Same interface?|Yes (via extends)|Yes (via implements)|
|Flexibility|Low|High|

The rule of thumb — if you find yourself creating subclasses just to add combinations of behavior, **Decorator is the signal**.

#### Decorator vs Adapter vs Proxy

These three all wrap an object — the intent is what separates them:

| Pattern       | Wraps? | Changes interface?           | Intent              |
| ------------- | ------ | ---------------------------- | ------------------- |
| **Decorator** | ✅ Yes  | ❌ No — same interface        | Add behavior        |
| **Adapter**   | ✅ Yes  | ✅ Yes — translates interface | Fix incompatibility |
| **Proxy**     | ✅ Yes  | ❌ No — same interface        | Control access      |
Decorator and Proxy look structurally identical — the difference is **why** you're wrapping. Decorator adds features, Proxy controls access (lazy loading, caching, auth checks).

#### Key Principles it satisfies

- **Open/Closed Principle** — add new behaviors (decorators) without modifying existing classes
- **Single Responsibility** — each decorator handles exactly one concern
- **Composition over Inheritance** — behavior built by combining small focused objects, not deep class trees
