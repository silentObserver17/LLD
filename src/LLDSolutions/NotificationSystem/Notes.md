# LLD Session Notes — Notification System
> Java · Design Patterns · SOLID · Concurrency  
> Next session: convert to Go (sync.Once, iota, implicit interfaces)

---

## 1. System Design

### Core Entities

| Entity | Responsibility |
|---|---|
| `User` | Holds user data — email, phone, deviceToken |
| `Message` | Holds notification content — title, body, metadata |
| `NotificationType` | Enum label — TRANSACTIONAL or PROMOTIONAL |
| `NotificationChannel` | Interface — one method: `send(User, Message)` |
| `EmailNotification` | Implements NotificationChannel for email |
| `SMSNotification` | Implements NotificationChannel for SMS |
| `PushNotification` | Implements NotificationChannel for push |
| `Subscription` | Per-user list of channels + list of types |
| `NotificationOrchestrator` | Singleton — coordinates the entire flow |

---

### Notification Flow (the story)

```
1. Someone triggers send(User, NotificationType, Message)
2. Orchestrator looks up the User's Subscription from the HashMap
3. Checks if the notification type is in the user's subscribed types
4. Iterates over the user's subscribed channels
5. Calls channel.send(user, message) on each one
```

---

### Why NotificationType is an Enum, not a Class

`TRANSACTIONAL` and `PROMOTIONAL` have **no behaviour of their own**.  
They are labels — the Orchestrator uses them to decide whether to send.

**Rule:** if a thing has no methods of its own → enum. If it needs behaviour → class/interface.

---

### The Two-Dimension Problem

If you encode both channel AND type into class names:
```
TransactionalEmailNotification
PromotionalEmailNotification
TransactionalSMSNotification
PromotionalSMSNotification
...
```
With C channels and T types → **C × T classes**. Combinatorial explosion.

**Solution:** keep channel and type as separate, independent concerns.
- `NotificationChannel` handles the channel dimension
- `NotificationType` enum handles the type dimension
- `Subscription` links a user to both independently

---

## 2. Design Patterns

### Singleton — NotificationOrchestrator

**Why Singleton?**  
The orchestrator holds `HashMap<Integer, Subscription>` for ALL users.  
If two instances exist → two separate HashMaps → users registered in one won't be found in the other.

**Implementation — Double-Checked Locking:**

```java
public class NotificationOrchestrator {
    private static volatile NotificationOrchestrator instance;
    private HashMap<Integer, Subscription> subscriptions;

    private NotificationOrchestrator() {           // private = no one can call new
        this.subscriptions = new HashMap<>();
    }

    public static NotificationOrchestrator getInstance() {
        if (instance == null) {                    // 1st check — no lock (fast path)
            synchronized (NotificationOrchestrator.class) {
                if (instance == null) {            // 2nd check — with lock (safe path)
                    instance = new NotificationOrchestrator();
                }
            }
        }
        return instance;
    }
}
```

**Three things that make it Singleton:**
1. `private` constructor — nobody outside can call `new`
2. `static volatile` instance field — one instance for the JVM
3. `getInstance()` — the only access point

---

### Why `volatile` is Required

Without `volatile`, CPU instruction reordering can cause Thread B to see a non-null but **unconstructed** object:

```
Thread A steps:
  1. Allocate memory
  2. Assign reference  ← instance != null HERE (but object not constructed yet)
  3. Call constructor

Thread B sees instance != null at step 2 → uses broken object
```

`volatile` prevents this reordering. Always use it with double-checked locking.

---

### Observer Pattern — Channel + Subscription

| Observer Role | This System |
|---|---|
| Subject (publisher) | `NotificationOrchestrator` |
| Observers | `NotificationChannel` implementations |
| Observer registry | `Subscription` (per user) |

The orchestrator iterates registered channels and calls `send()` on each — classic Observer.

---

## 3. SOLID Principles Applied

### OCP — Open-Closed Principle

`NotificationChannel` interface enforces OCP.

- **Open for extension:** add `WhatsAppNotification` by implementing the interface
- **Closed for modification:** `NotificationOrchestrator.send()` doesn't change at all

**Violation example:** if `send()` had `if-else` blocks per channel type, every new channel would require modifying `send()`.

---

### SRP — Single Responsibility Principle

Each class has one reason to change:

| Class | Changes only if... |
|---|---|
| `User` | User data model changes |
| `Message` | Message structure changes |
| `Subscription` | Subscription rules change |
| Channel impls | That channel's sending logic changes |
| `NotificationOrchestrator` | Orchestration flow changes |

---

### DIP — Dependency Inversion Principle

`NotificationOrchestrator` depends on the `NotificationChannel` **interface**, not on `EmailNotification` or `SMSNotification` directly.

`Subscription` holds `List<NotificationChannel>`, not `List<EmailNotification>`.

High-level policy (orchestration) doesn't depend on low-level details (email sending).

---

## 4. Java Concepts

### HashMap Key — Why Integer not User

HashMap uses:
- `hashCode()` → finds the **bucket**
- `equals()` → finds the **exact key** within the bucket

Using `User` as a key without overriding both:
- Two `User` objects with the same `userid` could have different `hashCode()`s
- HashMap treats them as different keys → silent bug

**Fix:** use `Integer` (userid) as the key — it already has correct `hashCode()` and `equals()`.

---

### The hashCode/equals Contract

```
If a.equals(b) == true  →  a.hashCode() == b.hashCode()  MUST be true
```

**Danger:** override `equals()` but not `hashCode()` → two equal objects land in different buckets → HashMap never finds the key even though it exists.

**Rule:** always override both together. Java warns you if you don't.

---

### Subscription — Full Implementation

```java
public class Subscription {
    private User user;
    private List<NotificationChannel> channels;
    private List<NotificationType> type;

    public Subscription(User user) {
        this.user = user;
        this.channels = new ArrayList<>();
        this.type = new ArrayList<>();
    }

    public void subscribe(NotificationChannel channel) {
        for (NotificationChannel ch : this.channels) {
            if (ch.equals(channel)) {
                throw new ChannelAlreadySubscribedException("Channel already subscribed");
            }
        }
        this.channels.add(channel);
    }

    public void unsubscribe(NotificationChannel channel) {
        for (NotificationChannel ch : this.channels) {
            if (ch.equals(channel)) {
                this.channels.remove(channel);
                return;                           // found it, removed, done
            }
        }
        throw new ChannelNotSubscribedException("Channel not subscribed");
    }

    public void addType(NotificationType t)    { this.type.add(t); }
    public void removeType(NotificationType t) { this.type.remove(t); }
    public List<NotificationChannel> getChannels() { return this.channels; }
    public List<NotificationType> getType()        { return this.type; }
}
```

---

### NotificationOrchestrator — Full send() Logic

```java
public void send(User user, NotificationType type, Message message) {
    if (!this.subscriptions.containsKey(user.getUserid())) {
        throw new IllegalArgumentException("User subscription does not exist");
    }

    Subscription subs = this.subscriptions.get(user.getUserid());
    List<NotificationChannel> channels = subs.getChannels();
    List<NotificationType> notiType = subs.getType();

    for (NotificationChannel channel : channels) {
        if (notiType.contains(type)) {           // only fire if type is subscribed
            channel.send(user, message);
        }
    }
}
```

---

## 5. Gotchas & Mistakes Made

### 1. Public fields in Subscription
**Bug:** all fields were `public`  
**Fix:** make all fields `private`, initialise lists in constructor, expose via getters  
**Rule:** always `private` by default — public fields let anyone mutate the list directly, bypassing your subscribe/unsubscribe guards

---

### 2. Copied subscribe() logic into unsubscribe()
**Bug:** threw exception when channel WAS found (same logic as subscribe)  
**Fix:** invert — throw when channel is NOT found

```
subscribe()   → throws if FOUND    (prevents duplicates)
unsubscribe() → throws if NOT FOUND (can't remove what isn't there)
```

---

### 3. Missing type check in send()
**Bug:** iterated all channels and fired them regardless of NotificationType  
**Fix:** `if (notiType.contains(type))` before `channel.send()`  
**Impact:** without this, a TRANSACTIONAL-only user would receive PROMOTIONAL notifications

---

### 4. Single Subscription in Orchestrator
**Bug:** `NotificationOrchestrator(Subscription subscription)` — hardcoded one user  
**Fix:** `HashMap<Integer, Subscription>` — one map for all users, keyed by userid

---

### 5. Missing private constructor on Singleton
**Bug:** public constructor — anyone could call `new NotificationOrchestrator()`  
**Fix:** `private NotificationOrchestrator()` — getInstance() is the only entry point

---

## 6. Quick Revision Checklist

Before the Go session, make sure you can answer these from memory:

- [ ] Why is `NotificationType` an enum and not a class?
- [ ] Why is the Singleton constructor `private`?
- [ ] Why `volatile` on the instance field?
- [ ] Why `Integer` and not `User` as the HashMap key?
- [ ] What two methods does HashMap need on a key object?
- [ ] What does `subscribe()` throw vs what does `unsubscribe()` throw?
- [ ] Where exactly does the OCP apply in this system?
- [ ] What is the two-dimension class explosion problem?

---

# IMPLEMENTATION IN GO: 


**User.go**
```go
package main

type User struct {
	UserId int64
	Name string
	Email string
	Phone string
	DeviceToken string
}

func NewUser(userId int64, Name, Email, Phone, DeviceToken string) *User {
	return &User{
		UserId: userId,
		Name: Name,
		Email: Email,
		Phone: Phone,
		DeviceToken: DeviceToken,
	}
}
```

**Subscription.go**
```go
package main

import "fmt"

type Subscription struct {
	User *User
	Channels []NotificationChannel
	Types []NotificationType
}

func NewSubscription(user *User) *Subscription {
	return &Subscription{
		User:  user,
		Channels: []NotificationChannel{},
		Types: []NotificationType{}, 
	}
}

func (s *Subscription) subscribe(channel NotificationChannel) error {
	for _, val := range s.Channels {
		if val == channel {
			return fmt.Errorf("channel already subscribed.")
		}
	}

	s.Channels = append(s.Channels, channel)
	return nil
} 

func (s *Subscription) unsubscribe(channel NotificationChannel) error {
	for i, val := range s.Channels {
		if val == channel {
			s.Channels = append(s.Channels[:i], s.Channels[i + 1:]...)
			return nil
		}
	}

	return fmt.Errorf("channel is not subscribed.")
}

func (s *Subscription) addType(notiType NotificationType) error {
	for _, val := range s.Types {
		if val == notiType {
			return fmt.Errorf("notification type already subscribed.")
		}
	}

	s.Types = append(s.Types, notiType)
	return nil

}

func (s *Subscription) removeType(notiType NotificationType) error {
	for i, val := range s.Types {
		if val == notiType {
			s.Types = append(s.Types[:i], s.Types[i + 1:]...)
			return nil
		}
	}

	return fmt.Errorf("notification type is not subscribed.")

}
```

**Message.go**
```go
package main

type Message struct {
	Title string
	Body string
	Metadata *string
}

func NewMessage(title, body string, metadata *string) *Message {
	return &Message{
		Title: title,
		Body: body,
		Metadata: metadata,
	}
}
```

**NotificationType.go**
```go
package main

type NotificationType uint8

const (
	TypeUnknown NotificationType = iota // 0
	TypeTransactional                   // 1
	TypePromotional                     // 2
)

func(t NotificationType) String() string {
	switch t {
		case TypeTransactional:
			return "TRANSACTIONAL"
		case TypePromotional:
			return "PROMOTIONAL"
		default:
			return "UNKNOWN"
	}
}
```
**NotificationChannel.go**
```go
package main

import "fmt"

type NotificationChannel interface {
	Send(*User, *Message)
}

type EmailNotification struct{}

func (e *EmailNotification) Send(user *User, message *Message) {
	fmt.Printf("[STUB] Email logic triggered for %s\n", user.Email)
	fmt.Printf("Message Title: %s and Message Body: %s\n", message.Title, message.Body)
}

type SMSNotification struct{}

func (e *SMSNotification) Send(user *User, message *Message) {
	fmt.Printf("[STUB] SMS logic triggered for %s\n", user.Phone)
	fmt.Printf("Message Title: %s and Message Body: %s\n", message.Title, message.Body)
}

type PushNotification struct{}

func (e *PushNotification) Send(user *User, message *Message) {
	fmt.Printf("[STUB] Push Notification logic triggered for %s\n", user.DeviceToken)
	fmt.Printf("Message Title: %s and Message Body: %s\n", message.Title, message.Body)
}

```

**NotificationOrchestrator.go**
```go
package main

import (
	"fmt"
	"sync"
)

type NotificationOrchestrator struct {
	subscriptions map[int64]Subscription
}

var (
	instance *NotificationOrchestrator
	once     sync.Once
)

func GetInstance() *NotificationOrchestrator {
	once.Do(func() {
		instance = &NotificationOrchestrator{
			subscriptions: make(map[int64]Subscription),
		}
	})

	return instance
}

func (o *NotificationOrchestrator) AddUserSubscription(user *User, subs *Subscription) {
	o.subscriptions[user.UserId] = *subs
}

func (o *NotificationOrchestrator) Send(user *User, notiType NotificationType, message *Message) error {
	subs, ok := o.subscriptions[user.UserId]
	if !ok {
		return fmt.Errorf("user subscription does not exist")
	}

	allowed := false

	for _, t := range subs.Types {
		if t == notiType {
			allowed = true
			break
		}
	}

	if !allowed {
		return nil
	}

	for _, channel := range subs.Channels {
		channel.Send(user, message)
	}

	return nil
}

```

**Main.go**

```go
package main

import "fmt"

func main() {
	user := NewUser(1, "John Doe", "john.doe@mail.com", "9632587410", "deviceToken001")

	channel1 := &EmailNotification{}

	sub1 := NewSubscription(user)
	if err := sub1.subscribe(channel1); err != nil {
		fmt.Printf("Error subscribing to channel: %v\n", err.Error())
	}

	if err := sub1.addType(TypeTransactional); err != nil {
		fmt.Printf("Error adding subscription type: %v\n", err.Error())
	}

	message := NewMessage("Transactional Message", "This is a new Transactional message for subscribers", nil)

	orchestrator := GetInstance()

	orchestrator.AddUserSubscription(user, sub1)
	orchestrator.Send(user, TypeTransactional, message)

	user2 := NewUser(2, "Jane Doe", "jane.doe@mail.com", "9632587411", "deviceToken002")
	channel2 := &SMSNotification{}

	sub2 := NewSubscription(user2)
	if err := sub2.subscribe(channel2); err != nil {
		fmt.Printf("Error subscribing to channel: %v\n", err.Error())
	}
	if err := sub2.subscribe(&PushNotification{}); err != nil {
		fmt.Printf("Error subscribing to channel: %v\n", err.Error())
	}

	if err := sub2.addType(TypeTransactional); err != nil {
		fmt.Printf("Error adding subscription type: %v\n", err.Error())
	}
	if err := sub2.addType(TypePromotional); err != nil {
		fmt.Printf("Error adding subscription type: %v\n", err.Error())
	}

	message2 := NewMessage("Promotional Message", "This is a new Promotional message for subscribers", nil)

	orchestrator.AddUserSubscription(user2, sub2)

	orchestrator.Send(user, TypePromotional, message)
	orchestrator.Send(user2, TypeTransactional, message2)

}

```

**OUTPUT**
```
go run .
[STUB] Email logic triggered for john.doe@mail.com
Message Title: This is a new Transactional message for subscribers and Message Body: Transactional Message
[STUB] SMS logic triggered for 9632587411
Message Title: This is a new Promotional message for subscribers and Message Body: Promotional Message
[STUB] Push Notification logic triggered for deviceToken002
Message Title: This is a new Promotional message for subscribers and Message Body: Promotional Message
```