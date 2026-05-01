# Factory Method Design Pattern
#### Intent

Define an **interface for creating an object**, but let **subclasses decide which class to instantiate**. The creator doesn't need to know the concrete class — it delegates that decision.

#### The Core Problem

Imagine you're building a notification system. You write:

```java
Notification n = new EmailNotification(); // tightly coupled
```

Now if you need SMS or Push, you're modifying existing code everywhere — violating the **Open/Closed Principle** (open for extension, closed for modification).

Factory Method solves this by saying: _"Don't call `new` directly. Ask a factory to give you the object."_

#### Structure
```
Creator (abstract)
    └── factoryMethod()        ← subclasses override this
    └── sendNotification()     ← uses the product, doesn't care which one

ConcreteCreatorA  ──► factoryMethod() returns EmailNotification
ConcreteCreatorB  ──► factoryMethod() returns SMSNotification

Product (interface)
    └── EmailNotification
    └── SMSNotification
    └── PushNotification
```

There are two roles:

- **Product** — the interface/abstract type being created
- **Creator** — the class with the factory method that subclasses override

To add WhatsApp notifications tomorrow — you create `WhatsAppNotification` and `WhatsAppSender`. **Zero existing code changes.**

#### Implementation in Go

Go uses interfaces + functions/structs instead of abstract classes.
**Product interface + implementations:**
```go
type Notification interface {
    Send(message string)
}

type EmailNotification struct{}
func (e *EmailNotification) Send(msg string) { fmt.Println("Email:", msg) }

type SMSNotification struct{}
func (s *SMSNotification) Send(msg string) { fmt.Println("SMS:", msg) }

type PushNotification struct{}
func (p *PushNotification) Send(msg string) { fmt.Println("Push:", msg) }
```

**Creator — factory method as a function type:**
```go
// Factory function signature
type NotificationFactory func() Notification

// Creator struct that uses the factory
type NotificationSender struct {
    factory NotificationFactory
}

func NewNotificationSender(factory NotificationFactory) *NotificationSender {
    return &NotificationSender{factory: factory}
}

func (s *NotificationSender) Send(message string) {
    notification := s.factory() // delegate creation
    notification.Send(message)
}
```

**Concrete factories:**

```go
func EmailFactory() Notification { return &EmailNotification{} }
func SMSFactory() Notification   { return &SMSNotification{} }
func PushFactory() Notification  { return &PushNotification{} }
```

**Client**
```go
sender := NewNotificationSender(EmailFactory)
sender.Send("Your OTP is 4291")
// Output: Email: Your OTP is 4291

// Switch to SMS — one line change
sender = NewNotificationSender(SMSFactory)
sender.Send("Your OTP is 4291")
// Output: SMS: Your OTP is 4291
```

In Go, the "factory method" is just a **function that returns an interface** — no inheritance needed.

#### Real-world variant: Parameterized Factory

Often in interviews you'll see a simpler flat version — a static factory that takes a type parameter:
``` java
public class NotificationFactory {
    public static Notification create(String type) {
        return switch (type) {
            case "EMAIL" -> new EmailNotification();
            case "SMS"   -> new SMSNotification();
            case "PUSH"  -> new PushNotification();
            default      -> throw new IllegalArgumentException("Unknown type: " + type);
        };
    }
}

// Usage
Notification n = NotificationFactory.create("EMAIL");
n.send("Hello!");
```

This is technically called a **Simple Factory** (not a true GoF pattern) but it's extremely common in real codebases and interviews. Know the distinction:

| |Simple Factory|Factory Method|
|---|---|---|
|Structure|Single class, switch/if|Abstract creator + subclasses|
|Adding new types|Modify the factory class|Add a new subclass|
|OCP compliant?|❌ No|✅ Yes|
|Interview frequency|Very high|Very high|

#### Key Principles it satisfies

- **Open/Closed Principle** — add new products without touching existing code
- **Single Responsibility** — creation logic is separated from business logic
- **Dependency Inversion** — high-level code depends on the `Notification` interface, not concrete classes