# Observer Design Pattern

#### Intent
Define a **one-to-many dependency** between objects so that when one object changes state, all its dependents are **notified and updated automatically**. The subject doesn't need to know who is listening — it just broadcasts.

#### The Core Problem

You have an `Order` object. When an order's status changes to `CONFIRMED`, multiple things need to happen:

```java
// Without Observer — the Order class knows too much
public void confirmOrder(int orderId) {
    order.setStatus("CONFIRMED");

    // Tightly coupled to every downstream system
    emailService.sendConfirmation(order);
    smsService.sendConfirmation(order);
    inventoryService.reserveStock(order);
    analyticsService.trackConversion(order);
    invoiceService.generate(order);
}
```

Problems:

- `Order` is coupled to 5 unrelated systems
- Adding a new downstream (WhatsApp notification) means modifying `Order`
- Testing `Order` requires mocking all 5 services
- Violates SRP — Order shouldn't know about analytics or invoicing

Observer decouples the **source of the event** from the **reactions to it**.

#### The Core Idea
```
Subject (Observable)          Observers
    │                         ┌─ EmailObserver
    ├── attach(observer)      ├─ SMSObserver
    ├── detach(observer)      ├─ InventoryObserver
    └── notify()  ──────────► └─ AnalyticsObserver
```

The Subject maintains a list of observers and fires `notify()` when its state changes. Observers register themselves — the Subject never hardcodes who's listening.

---
#### Structure

```
Subject (interface)
    └── attach(Observer)
    └── detach(Observer)
    └── notifyObservers()

ConcreteSubject
    └── holds state
    └── calls notifyObservers() on state change

Observer (interface)
    └── update(event)

ConcreteObservers
    └── EmailObserver
    └── SMSObserver
    └── InventoryObserver
```

#### Implementation in Go

Go's channel system makes Observer feel very natural:

```go
// Observer interface
type OrderObserver interface {
    Update(order Order, event string)
}

// Subject
type Order struct {
    OrderID   int
    UserID    int
    Amount    float64
    Status    string
    observers []OrderObserver
}

func (o *Order) Attach(observer OrderObserver) {
    o.observers = append(o.observers, observer)
}

func (o *Order) Detach(observer OrderObserver) {
    for i, obs := range o.observers {
        if obs == observer {
            o.observers = append(o.observers[:i], o.observers[i+1:]...)
            return
        }
    }
}

func (o *Order) notifyObservers(event string) {
    for _, obs := range o.observers {
        obs.Update(*o, event)
    }
}

func (o *Order) Confirm() {
    o.Status = "CONFIRMED"
    o.notifyObservers("ORDER_CONFIRMED")
}

func (o *Order) Ship() {
    o.Status = "SHIPPED"
    o.notifyObservers("ORDER_SHIPPED")
}

// Concrete Observers
type EmailObserver struct{}
func (e *EmailObserver) Update(order Order, event string) {
    fmt.Printf("Email: Order #%d event [%s]\n", order.OrderID, event)
}

type InventoryObserver struct{}
func (i *InventoryObserver) Update(order Order, event string) {
    if event == "ORDER_CONFIRMED" {
        fmt.Printf("Inventory: Reserving stock for order #%d\n", order.OrderID)
    }
}

// Client
func main() {
    order := &Order{OrderID: 7823, UserID: 101, Amount: 1499.0}
    order.Attach(&EmailObserver{})
    order.Attach(&InventoryObserver{})

    order.Confirm()
    order.Ship()
}
```

#### Push vs Pull model

Two variants of how observers receive data:
**Push model** — Subject sends the data directly (what we built above):

```java
void update(Order order, String event); // subject pushes full order
```

✅ Observer gets everything immediately ❌ Observer receives data it might not need

**Pull model** — Subject sends minimal signal, observer fetches what it needs:

```java
// Observer only receives a reference
void update(OrderSubject subject, String event);

// Observer pulls only what it needs
public void update(OrderSubject subject, String event) {
    Order order = (Order) subject;
    double amount = order.getAmount(); // pull only what's needed
}
```

✅ Observers are decoupled from data shape ❌ Observers need a reference back to subject

Most real systems use a **hybrid** — send a lightweight event object with just enough context.

#### Event object pattern — more realistic

Rather than passing raw strings, real systems use typed event objects:
```java
// Typed event
public class OrderEvent {
    private final String type;
    private final int orderId;
    private final int userId;
    private final double amount;
    private final Instant timestamp;
    // constructor, getters...
}

// Observer receives typed event
public interface OrderObserver {
    void onEvent(OrderEvent event);
}

// Subject fires typed events
public void confirm() {
    this.status = "CONFIRMED";
    OrderEvent event = new OrderEvent("ORDER_CONFIRMED", orderId, userId, amount, Instant.now());
    observers.forEach(o -> o.onEvent(event));
}
```

This is exactly how your **BullMQ jobs** work — you put a typed payload on the queue and each worker (observer) processes it independently. BullMQ is essentially a persistent, async Observer implementation.

#### Observer in the wild — your stack

You're already using Observer everywhere:

|Your code|Observer pattern|
|---|---|
|`socket.on('event', handler)`|`attach(observer)`|
|`socket.emit('event', data)`|`notifyObservers(event)`|
|`socket.off('event', handler)`|`detach(observer)`|
|BullMQ `queue.add(job)`|`notifyObservers()` async|
|BullMQ `worker.on('completed')`|observer reacting to event|
|Node.js `EventEmitter.on()`|`attach(observer)`|
|Node.js `EventEmitter.emit()`|`notifyObservers()`|

`EventEmitter` **is** the Observer pattern — Node.js just gave it a different name.

#### Java's built-in Observer support

Java had `java.util.Observable` and `java.util.Observer` — but they were **deprecated in Java 9** because:

- `Observable` was a class, not an interface — forced inheritance
- Not thread-safe out of the box
- Too rigid for real-world use

Modern Java uses:

- **`PropertyChangeListener`** — for JavaBeans property change events
- **Reactor / RxJava** — reactive streams are Observer at scale
- **Spring ApplicationEvent** — Spring's built-in Observer for application-level events:

```java
// Spring Observer — publish
applicationEventPublisher.publishEvent(new OrderConfirmedEvent(this, order));

// Spring Observer — listen
@EventListener
public void handleOrderConfirmed(OrderConfirmedEvent event) {
    emailService.sendConfirmation(event.getOrder());
}
```

This is the cleanest Java Observer implementation for production systems.

#### Key Principles it satisfies

- **Open/Closed** — add new observers without touching the subject
- **Single Responsibility** — each observer handles exactly one concern
- **Loose Coupling** — subject knows only the `Observer` interface, never concrete types

#### All Behavioral Patterns — what's ahead

|Pattern|Core idea|
|---|---|
|**Observer** ✅|One-to-many event notification|
|**Strategy**|Swap algorithms at runtime|
|**Command**|Encapsulate a request as an object|
|**Chain of Responsibility**|Pass request along a handler chain|
|**Template Method**|Define skeleton, subclasses fill in steps|
|**State**|Object changes behavior when state changes|
