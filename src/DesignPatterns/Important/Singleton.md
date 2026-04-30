# Singleton Design Pattern
---
#### Intent

Ensure a class has **only one instance** throughout the application's lifetime, and provide a **global access point** to it.

#### The Core Problem

Some resources should logically exist only once:
- A database connection pool
- A logger
- A configuration manager
- A thread pool

If you allow multiple instances, you get inconsistent state, wasted resources, or race conditions.

#### Structure
```
Client ──► Singleton::getInstance() ──► returns same instance every time
                   │
                   ▼
           private static instance
           private constructor()
```

#### Implementation in Go

Go doesn't have classes, but Singleton maps cleanly using a package-level variable + `sync.Once`.

``` go
package logger

import (
    "fmt"
    "sync"
)

type Logger struct {
    prefix string
}

var (
    instance *Logger
    once     sync.Once
)

func GetInstance() *Logger {
    once.Do(func() {
        instance = &Logger{prefix: "[APP]"}
        fmt.Println("Logger initialized") // runs only once
    })
    return instance
}

func (l *Logger) Log(msg string) {
    fmt.Printf("%s %s\n", l.prefix, msg)
}
```

```go
// Usage
l1 := logger.GetInstance()
l2 := logger.GetInstance()

fmt.Println(l1 == l2) // true — same pointer
```

`sync.Once` guarantees the initializer runs **exactly once**, even under concurrent goroutines. This is the idiomatic Go approach.

#### Implementation in Java
Java has more nuance here — several flavors exist:

**1. Eager Initialization** (simplest, not lazy)
```java
public class Config {
    private static final Config INSTANCE = new Config(); // created at class load

    private Config() {}

    public static Config getInstance() {
        return INSTANCE;
    }
}
```

✅ Thread-safe by JVM class loading guarantees. ❌ Instance created even if never used.

**2. Lazy + Double-Checked Locking**
```java
public class Config {
    private static volatile Config instance; // volatile is critical

    private Config() {}

    public static Config getInstance() {
        if (instance == null) {                    // first check (no lock)
            synchronized (Config.class) {
                if (instance == null) {            // second check (with lock)
                    instance = new Config();
                }
            }
        }
        return instance;
    }
}
```
###### Why `volatile`?(IMPORTANT)

Prevents:
- prevents the JVM from reordering the write to `instance` before the constructor completes
- without it, another thread could see a partially constructed object.

**3. Bill Pugh / Initialization-on-demand Holder** (elegant, lazy, thread-safe)
```JAVA
public class Config {
    private Config() {}

    private static class Holder {
        static final Config INSTANCE = new Config();
    }

    public static Config getInstance() {
        return Holder.INSTANCE;
    }
}
```

The inner `Holder` class is only loaded when `getInstance()` is first called. JVM class loading is inherently thread-safe, so no `synchronized` needed. This is the cleanest Java approach.

#### Breaking Singleton (and defenses)

| Attack                                             | Defense                                                   |
| -------------------------------------------------- | --------------------------------------------------------- |
| Reflection (`setAccessible(true)` on constructor)  | Throw exception in constructor if instance exists         |
| Serialization (creates new object on `readObject`) | Implement `readResolve()` returning `instance`            |
| Cloning (`clone()`)                                | Override `clone()` and throw `CloneNotSupportedException` |


---

## Attack 1: Reflection

### The Attack

Java's Reflection API lets you access **private members** at runtime — including private constructors.

```java
Config c1 = Config.getInstance();

Constructor<Config> constructor = Config.class.getDeclaredConstructor();
constructor.setAccessible(true); // bypass private!
Config c2 = constructor.newInstance(); // creates a SECOND instance

System.out.println(c1 == c2); // false — Singleton broken!
```

The JVM normally blocks access to private constructors, but `setAccessible(true)` overrides that access check.

### The Defense

Throw an exception inside the constructor if an instance already exists:

```java
public class Config {
    private static volatile Config instance;

    private Config() {
        if (instance != null) {
            throw new IllegalStateException("Use getInstance() — direct instantiation not allowed");
        }
    }

    public static Config getInstance() {
        if (instance == null) {
            synchronized (Config.class) {
                if (instance == null) {
                    instance = new Config();
                }
            }
        }
        return instance;
    }
}
```

Now even if reflection bypasses `private`, the constructor itself guards against a second creation.

---

## Attack 2: Serialization

### The Attack

When you serialize an object to bytes and deserialize it back, Java calls `readObject()` internally — which **creates a brand new object**, bypassing your constructor entirely.

```java
Config c1 = Config.getInstance();

// Serialize to bytes
ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("config.ser"));
out.writeObject(c1);
out.close();

// Deserialize — creates a NEW object!
ObjectInputStream in = new ObjectInputStream(new FileInputStream("config.ser"));
Config c2 = (Config) in.readObject();
in.close();

System.out.println(c1 == c2); // false — Singleton broken!
```

The deserialization mechanism completely bypasses your `getInstance()` and the constructor — it allocates memory and populates fields directly.

### The Defense

Implement `readResolve()` — Java's serialization framework calls this method after deserialization and uses its **return value** as the final object:

```java
public class Config implements Serializable {
    private static volatile Config instance;

    private Config() {}

    public static Config getInstance() {
        if (instance == null) {
            synchronized (Config.class) {
                if (instance == null) {
                    instance = new Config();
                }
            }
        }
        return instance;
    }

    // Called by JVM after deserializing — return the existing instance
    protected Object readResolve() {
        return getInstance(); // discard the newly created object, return the real one
    }
}
```

The newly deserialized object gets **thrown away** and your singleton instance is returned instead.

---

## Attack 3: Cloning

### The Attack

If your Singleton class implements `Cloneable` (or inherits from a class that does), someone can call `clone()` to get a **shallow copy** — a completely separate object:

```java
Config c1 = Config.getInstance();
Config c2 = (Config) c1.clone(); // new object in memory!

System.out.println(c1 == c2); // false — Singleton broken!
```

`clone()` allocates a new object and copies field values — again completely bypassing your constructor and `getInstance()`.

### The Defense

Override `clone()` and throw an exception:

```java
public class Config implements Cloneable {
    private static volatile Config instance;

    private Config() {}

    public static Config getInstance() { ... }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException("Singleton cannot be cloned");
    }
}
```

---

## Why Enum is immune to all three

```java
public enum Config {
    INSTANCE;
}
```

| Attack        | Why it fails on Enum                                                                                                                                               |
| ------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Reflection    | JVM **explicitly prohibits** calling `newInstance()` on enum constructors — throws `IllegalArgumentException`                                                      |
| Serialization | Enum serialization only writes the **name** (`"INSTANCE"`), and deserialization calls `Enum.valueOf()` which returns the existing constant — no new object created |
| Cloning       | `Enum` base class has `clone()` declared as `final` and it **always throws** `CloneNotSupportedException`                                                          |

The JVM spec itself provides these guarantees for enums — it's not something you have to implement. That's why enum Singleton is considered bulletproof in Java.

---

### The Go angle

In Go, **none of these attacks exist** because:

- No reflection-based constructor invocation (Go reflection can't call unexported functions from outside the package)
- No serialization framework that bypasses constructors
- No `clone()` mechanism

`sync.Once` is sufficient and you don't need to think about any of this. This whole attack surface is a Java-specific concern due to how deep the JVM's runtime capabilities go.

---

For Java, using an **enum** is the bulletproof approach:
```JAVA
public enum Config {
    INSTANCE;

    public String get(String key) { ... }
}
// Usage: Config.INSTANCE.get("db.url")
```

Enums are immune to reflection, serialization, and cloning attacks by the JVM spec.

#### When NOT to use Singleton

- When you need **testability** — Singleton is hard to mock (tight coupling). Prefer **Dependency Injection** instead.
- When the "single instance" assumption might change (e.g., multi-tenant, multi-DB).
- It's essentially a **global variable** — overuse leads to hidden dependencies.

#### Quick Recap

| Aspect        | Go                                 | Java                                 |
| ------------- | ---------------------------------- | ------------------------------------ |
| Thread safety | `sync.Once`                        | `volatile` + DCL, or enum, or Holder |
| Laziness      | Yes (Once only runs on first call) | Depends on approach                  |
| Best approach | `sync.Once` + package var          | Enum (if no state) or Holder pattern |
