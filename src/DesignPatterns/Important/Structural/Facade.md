# Facade Design Pattern

#### Intent

Provide a **simplified interface to a complex subsystem**. The Facade hides the complexity of multiple interacting components behind a single, clean entry point.

#### The Core Problem

You're building an **order placement flow** in an e-commerce system. To place one order, your client code has to orchestrate multiple subsystems:

```
// What the client has to do WITHOUT a Facade
inventoryService.checkStock(productId);
paymentService.validateCard(cardDetails);
paymentService.charge(amount);
orderService.createOrder(userId, productId);
notificationService.sendEmail(userId, "Order confirmed");
notificationService.sendSMS(userId, "Your order is on the way");
shippingService.scheduleDelivery(orderId);
invoiceService.generateInvoice(orderId);
```

The client knows about **8 different subsystems** and their correct sequence. If the flow changes, every client that calls this breaks. This is tight coupling at its worst.

Facade wraps all of this behind one method:

```java
orderFacade.placeOrder(userId, productId, cardDetails);
```

#### Facade vs Other Patterns

This is a common interview question — Facade gets confused with Adapter and Proxy:

|Pattern|Simplifies?|Changes interface?|Wraps how many?|Intent|
|---|---|---|---|---|
|**Facade**|✅ Yes|✅ Yes — simpler|Multiple subsystems|Hide complexity|
|**Adapter**|❌ No|✅ Yes — translates|One adaptee|Fix mismatch|
|**Proxy**|❌ No|❌ No — same|One object|Control access|
|**Decorator**|❌ No|❌ No — same|One object|Add behavior|

The key differentiator for Facade — it **unifies multiple subsystems** behind a simpler interface. The others wrap a single object.

#### Structure
```
Client
    └── calls OrderFacade.placeOrder()

OrderFacade
    ├── uses InventoryService
    ├── uses PaymentService
    ├── uses OrderService
    ├── uses NotificationService
    ├── uses ShippingService
    └── uses InvoiceService

Subsystems (complex, unchanged)
    └── each has its own interface and logic
```

The subsystems don't know about the Facade — they're independent. The Facade just orchestrates them.

```go
type OrderFacade struct {
    inventory    *InventoryService
    payment      *PaymentService
    order        *OrderService
    notification *NotificationService
    shipping     *ShippingService
    invoice      *InvoiceService
}

func NewOrderFacade() *OrderFacade {
    return &OrderFacade{
        inventory:    &InventoryService{},
        payment:      &PaymentService{},
        order:        &OrderService{},
        notification: &NotificationService{},
        shipping:     &ShippingService{},
        invoice:      &InvoiceService{},
    }
}

func (f *OrderFacade) PlaceOrder(userId, productId int, card string, amount float64) (int, error) {

    if !f.inventory.CheckStock(productId) {
        return 0, errors.New("out of stock")
    }

    if !f.payment.ValidateCard(card) {
        return 0, errors.New("invalid card")
    }

    txnId := f.payment.Charge(amount)
    f.inventory.ReserveStock(productId)

    orderId := f.order.CreateOrder(userId, productId, txnId)

    f.notification.SendEmail(userId, fmt.Sprintf("Order #%d confirmed!", orderId))
    f.notification.SendSMS(userId, "Your order is on the way")

    f.shipping.ScheduleDelivery(orderId)
    f.invoice.GenerateInvoice(orderId)

    return orderId, nil
}

// Client
func main() {
    facade := NewOrderFacade()
    orderId, err := facade.PlaceOrder(1, 42, "4111-1111-1111-1111", 1499.0)
    if err != nil {
        log.Fatal(err)
    }
    fmt.Println("Order placed:", orderId)
}
```

#### Facade in your own codebase

Your **expense approval workflow** is a Facade situation. When a salesperson submits an expense, you're likely orchestrating:

```javascript
// Without Facade — scattered across controller
await expenseRepo.create(expenseData);
await approvalRepo.createWorkflow(expenseId);
await notificationService.sendWhatsApp(coordinatorId, message);
await notificationService.sendEmail(coordinatorId, emailData);
await auditLog.record(userId, 'EXPENSE_SUBMITTED', expenseId);
await socketService.emit(coordinatorId, 'NEW_EXPENSE', expenseData);
```

Wrapping this in an `ExpenseFacade.submitExpense()` would make your controller one line and centralize the orchestration logic — easier to maintain and test.

#### Important nuance — Facade doesn't lock you out

A Facade simplifies but **doesn't hide** the subsystems. Advanced clients can still access subsystems directly if needed:

```java
// Normal client — uses Facade
orderFacade.placeOrder(userId, productId, card, amount);

// Power client — bypasses Facade, talks to subsystem directly
invoiceService.generateInvoice(existingOrderId); // perfectly valid
```

This is different from Encapsulation — Facade is a **convenience layer**, not a wall.

#### Real-world Java examples

- **`javax.faces.context.FacesContext`** — single entry point to the entire JSF subsystem (request, response, session, application)
- **Spring's `JdbcTemplate`** — facades over raw JDBC (`Connection`, `PreparedStatement`, `ResultSet`, exception handling, resource cleanup) behind simple `query()` and `update()` calls
- **SLF4J's `LoggerFactory`** — one call hides the complexity of finding and initializing the underlying logging implementation
- **Your BullMQ setup** — the queue itself acts as a Facade. You call `queue.add('send-notification', data)` without caring about Redis connections, job serialization, retry logic, or worker assignment

#### All Structural Patterns — Complete Recap

|Pattern|Core idea|Wraps?|Interface change?|Real example|
|---|---|---|---|---|
|**Adapter**|Translate incompatible interfaces|✅ One object|✅ Yes|`InputStreamReader`, SLF4J|
|**Decorator**|Stack behavior at runtime|✅ One object|❌ No|`BufferedReader`, Spring Security filters|
|**Proxy**|Control access to an object|✅ One object|❌ No|Spring AOP, Redis cache layer|
|**Facade**|Simplify a complex subsystem|✅ Many objects|✅ Yes|`JdbcTemplate`, checkout flow|
