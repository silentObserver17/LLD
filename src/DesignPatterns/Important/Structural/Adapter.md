# Adapter Design Pattern

#### Intent

Convert the interface of a class into another interface that clients expect. Adapter lets classes work together that **couldn't otherwise because of incompatible interfaces**.

#### The Core Problem

You're building a payment system that works with `RazorpayClient`. Six months later, the business wants to add PayPal. But PayPal's SDK has a completely different interface:

```java
// Your system expects this
public interface PaymentProcessor {
    void processPayment(double amount, String currency);
    void refund(String transactionId);
}

// RazorpayClient already implements this — works fine

// PayPal SDK — you can't modify this (third-party library)
public class PayPalSDK {
    public void makePayment(PayPalRequest request) { ... }
    public void initiateRefund(PayPalRefundRequest request) { ... }
}
```

`PayPalSDK` is what you **have**. `PaymentProcessor` is what your system **expects**. They don't match — enter the Adapter.

#### The Real-world Analogy

A power socket adapter. Your Indian charger (3-pin) needs to work in a European socket (2-pin). You don't redesign your charger or the wall socket — you plug in an **adapter** that sits between them and translates.
```
Your Code → PaymentProcessor interface
                    ↓
              PayPalAdapter        ← the adapter
                    ↓
              PayPalSDK            ← the adaptee (third-party, unchanged)
```

#### Two flavors of Adapter

|              | Object Adapter                     | Class Adapter                       |
| ------------ | ---------------------------------- | ----------------------------------- |
| Mechanism    | Wraps adaptee via **composition**  | Extends adaptee via **inheritance** |
| Java support | ✅ Preferred                        | ⚠️ Limited (single inheritance)     |
| Go support   | ✅ Yes                              | ❌ No inheritance in Go              |
| Flexibility  | High — can adapt multiple adaptees | Low — locked to one class           |

Object Adapter (composition) is almost always the right choice.

#### Implementation in Go
```go
// Target interface
type PaymentProcessor interface {
    ProcessPayment(amount float64, currency string)
    Refund(transactionId string)
}

// Adaptee — third party SDK
type PayPalSDK struct{}

type PayPalRequest struct {
    Amount   float64
    Currency string
}

type PayPalRefundRequest struct {
    OrderId string
}

func (p *PayPalSDK) MakePayment(req PayPalRequest) {
    fmt.Printf("PayPal SDK: Payment of %.2f %s\n", req.Amount, req.Currency)
}

func (p *PayPalSDK) InitiateRefund(req PayPalRefundRequest) {
    fmt.Printf("PayPal SDK: Refund for order %s\n", req.OrderId)
}

// Adapter — wraps PayPalSDK, implements PaymentProcessor
type PayPalAdapter struct {
    sdk *PayPalSDK // composition
}

func NewPayPalAdapter(sdk *PayPalSDK) *PayPalAdapter {
    return &PayPalAdapter{sdk: sdk}
}

func (a *PayPalAdapter) ProcessPayment(amount float64, currency string) {
    req := PayPalRequest{Amount: amount, Currency: currency}
    a.sdk.MakePayment(req)
}

func (a *PayPalAdapter) Refund(transactionId string) {
    req := PayPalRefundRequest{OrderId: transactionId}
    a.sdk.InitiateRefund(req)
}

// Client
type PaymentService struct {
    processor PaymentProcessor
}

func (s *PaymentService) Checkout(amount float64, currency string) {
    s.processor.ProcessPayment(amount, currency)
}

// Bootstrap
func main() {
    adapter := NewPayPalAdapter(&PayPalSDK{})
    service := &PaymentService{processor: adapter}
    service.Checkout(1500.00, "INR")
}
```

#### Two-way Adapter

Sometimes you need translation in **both directions** — your system needs to call the third-party SDK, and the SDK also needs to call back into your system. The adapter implements both interfaces:

```java
public class BidirectionalAdapter implements PaymentProcessor, PayPalListener {

    private final PayPalSDK sdk;

    // Implements PaymentProcessor — your system → PayPal
    @Override
    public void processPayment(double amount, String currency) {
        sdk.makePayment(new PayPalRequest(amount, currency));
    }

    // Implements PayPalListener — PayPal → your system
    @Override
    public void onPaymentSuccess(String paypalEventId) {
        System.out.println("Handling PayPal success event: " + paypalEventId);
    }
}
```

Less common but worth knowing exists.

#### Real-world Java examples you've used

- **`InputStreamReader`** — adapts `InputStream` (bytes) to `Reader` (characters). `InputStream` and `Reader` are incompatible — `InputStreamReader` bridges them:

```java
BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
```

- **`Arrays.asList()`** — adapts a plain array to the `List` interface
- **Spring's `HandlerAdapter`** — adapts different types of controllers (`@Controller`, `HttpRequestHandler`, `Servlet`) to a uniform handler interface that `DispatcherServlet` expects
- **SLF4J** — the entire library is an adapter layer. Your code calls SLF4J's `Logger` interface, which adapts to whatever underlying logging framework (Log4j, Logback, JUL) is on the classpath

#### Adapter vs Facade vs Decorator

These three get confused in interviews — clear distinction:

|Pattern|Intent|Changes interface?|Wraps how many?|
|---|---|---|---|
|**Adapter**|Make incompatible interfaces work together|✅ Yes — translates interface|One adaptee|
|**Facade**|Simplify a complex subsystem|✅ Yes — provides simpler interface|Multiple subsystems|
|**Decorator**|Add behavior without changing interface|❌ No — same interface|One component|

The key differentiator for Adapter: **the interface mismatch is the whole reason it exists**.

#### Key Principles it satisfies

- **Open/Closed Principle** — add new payment providers without touching `PaymentService`
- **Single Responsibility** — translation logic lives in the adapter, not scattered across client code
- **Dependency Inversion** — `PaymentService` depends on `PaymentProcessor` abstraction, never on `PayPalSDK` directly