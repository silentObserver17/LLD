# 🧠 Command Design Pattern (In Simple Terms)

👉 The **Command Pattern** turns a _request_ into an **object**.

Instead of calling methods directly like:

```java
light.turnOn();
```
You wrap that action into a **command object**:
```java
Command cmd = new TurnOnLightCommand(light);
cmd.execute();
```

Command says — **wrap each request in its own object**. Now you can pass requests around, store them, queue them, undo them — treat them as first-class citizens.
# 🔥 Why this even exists?

Because sometimes you want to:

- Delay execution ⏳
- Queue operations 📥
- Undo/redo actions ↩️
- Log actions 📜
- Decouple sender from receiver 🔌

# 🧱 Structure (Who is who?)

```
Command (interface)
    └── execute()
    └── undo()

ConcreteCommand
    └── holds reference to Receiver
    └── implements execute() by calling receiver methods
    └── stores state needed for undo

Receiver
    └── the actual business logic object
    └── knows how to perform the operation

Invoker
    └── holds and triggers commands
    └── doesn't know what the command does
    └── just calls execute()

Client
    └── creates ConcreteCommand
    └── sets its Receiver
    └── hands it to Invoker
```

**STRUCTURE**:
```
Client ──creates──► ConcreteCommand ──holds──► Receiver
           │
           └──gives to──► Invoker
                              │
                              └── calls execute() on Command
```

```
code in java file
```

#### Implementation in Go
``` go
// Command interface
type Command interface {
    Execute()
    Undo()
}

// Receiver
type InventoryService struct {
    stock int
}

func (s *InventoryService) ReserveStock(productId, qty int) {
    s.stock -= qty
    fmt.Printf("Stock reserved: %d units | Remaining: %d\n", qty, s.stock)
}

func (s *InventoryService) ReleaseStock(productId, qty int) {
    s.stock += qty
    fmt.Printf("Stock released: %d units | Remaining: %d\n", qty, s.stock)
}

// Concrete Command
type ReserveStockCommand struct {
    service   *InventoryService
    productId int
    quantity  int
}

func (c *ReserveStockCommand) Execute() {
    c.service.ReserveStock(c.productId, c.quantity)
}

func (c *ReserveStockCommand) Undo() {
    c.service.ReleaseStock(c.productId, c.quantity)
}

// Invoker
type CommandInvoker struct {
    history []Command
}

func (inv *CommandInvoker) Execute(cmd Command) {
    cmd.Execute()
    inv.history = append(inv.history, cmd)
}

func (inv *CommandInvoker) Undo() {
    n := len(inv.history)
    if n == 0 {
        fmt.Println("Nothing to undo")
        return
    }
    last := inv.history[n-1]
    inv.history = inv.history[:n-1]
    last.Undo()
}

// Client
func main() {
    svc     := &InventoryService{stock: 100}
    invoker := &CommandInvoker{}

    invoker.Execute(&ReserveStockCommand{svc, 42, 5})
    invoker.Execute(&ReserveStockCommand{svc, 42, 3})

    fmt.Println("── Undo ──")
    invoker.Undo()
    invoker.Undo()
}
```

# ⚡ When SHOULD you use it?

Use Command Pattern when:

### ✅ 1. You need Undo/Redo

- Text editors
- Drawing apps

👉 Each action = command → store in stack

---

### ✅ 2. You want to Queue or Schedule tasks

- Job queues
- Task schedulers
- Background workers

---

### ✅ 3. You want loose coupling

Invoker doesn’t know:

- What action is
- Who executes it

---

### ✅ 4. You want to log or replay operations

- Event sourcing systems
- Audit logs

#### Command + Event Sourcing

Command pattern is the foundation of **Event Sourcing** — instead of storing current state, you store the full history of commands and replay them:

```java
public class EventStore {

    // Every command ever executed — never deleted
    private final List<Command> eventLog = new ArrayList<>();

    public void execute(Command command) {
        command.execute();
        eventLog.add(command); // append only
    }

    // Replay all commands to rebuild state
    public void replay() {
        System.out.println("Replaying " + eventLog.size() + " events...");
        eventLog.forEach(Command::execute);
    }
}
```

Your current state is always derivable by replaying the log from the beginning. This is what databases like Kafka use — the log **is** the source of truth.

# 🚀 Real-world examples (VERY IMPORTANT)

You’ve already used this without realizing:

### 🧩 1. Runnable (Java)

```
Runnable r = () -> System.out.println("Hello");new Thread(r).start();
```

👉 Runnable = Command

---

### 🧩 2. Spring Boot / REST controllers

Each API request → wrapped as command-like object

---

### 🧩 3. Message Queues (Kafka, RabbitMQ)

- Producer sends command
- Consumer executes later

---

### 🧩 4. UI Buttons (Swing / JavaFX)

```
button.setOnClickListener(() -> doSomething());
```

#### Key Principles it satisfies

- **Open/Closed** — new commands added without touching Invoker
- **Single Responsibility** — each command owns one operation
- **Separation of Concerns** — who triggers (Invoker), what does it (Receiver), and how it's packaged (Command) are all separate

> “Why not just call method directly?”

Answer:

👉 Because Command Pattern:

- Decouples sender & receiver
- Supports undo/redo
- Enables queuing & logging
- Makes system extensible
