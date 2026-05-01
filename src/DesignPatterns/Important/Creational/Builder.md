# Builder Design Pattern
#### Intent

Construct a **complex object step by step**, separating the construction process from the final representation. Lets you produce different configurations of an object using the same building process.

#### The Core Problem

Consider a `User` object with many fields:

```java
public class User {
    String name;        // required
    String email;       // required
    int age;            // optional
    String phone;       // optional
    String address;     // optional
    boolean isVerified; // optional
}
```

**Option 1 — Telescoping Constructors** (the ugly way):

```java
public User(String name, String email) { ... }
public User(String name, String email, int age) { ... }
public User(String name, String email, int age, String phone) { ... }
public User(String name, String email, int age, String phone, String address) { ... }
// ... keeps growing
```

Unreadable, unmaintainable. Ever seen `new User("John", "j@j.com", 0, null, null, false)`? You have no idea what those nulls mean.

**Option 2 — Setters** (the dangerous way):

```java
User user = new User();
user.setName("John");
user.setEmail("john@example.com");
// Object is in INCOMPLETE state here — someone could use it now
user.setAge(25);
```

The object is mutable and can exist in an **inconsistent intermediate state**. Not thread-safe either.

**Builder solves both** — readable construction + immutable final object.

#### Structure
```
Director (optional)
    └── constructs using Builder interface

Builder (interface)
    └── setName(), setEmail(), setAge()...
    └── build() → Product

ConcreteBuilder
    └── implements Builder
    └── holds state during construction

Product (User)
    └── immutable final object
```

```
code in java file
```

Notice how each line tells you exactly what you're setting — no positional ambiguity, no mystery nulls.

#### Validation in Builder

The `build()` method is the perfect place for **cross-field validation** — rules that involve multiple fields together:

```java
public User build() {
    // Cross-field validation
    if (isVerified && phone == null) {
        throw new IllegalStateException("Verified users must have a phone number");
    }
    if (age > 0 && age < 18 && isVerified) {
        throw new IllegalStateException("Minors cannot be verified");
    }
    return new User(this);
}
```

This is something you simply can't do cleanly with constructors or setters.

#### Implementation in Go

Go doesn't have inner classes, so the pattern looks slightly different — a separate `UserBuilder` struct with a functional options variant being common:

**Classic approach:**

```go
type User struct {
    name      string
    email     string
    age       int
    phone     string
    address   string
    isVerified bool
}

type UserBuilder struct {
    user User
    err  error // capture errors during building
}

func NewUserBuilder(name, email string) *UserBuilder {
    if name == "" || email == "" {
        return &UserBuilder{err: errors.New("name and email are required")}
    }
    return &UserBuilder{user: User{name: name, email: email}}
}

func (b *UserBuilder) Age(age int) *UserBuilder {
    if b.err != nil { return b } // short-circuit on prior error
    if age < 0 {
        b.err = errors.New("age cannot be negative")
        return b
    }
    b.user.age = age
    return b
}

func (b *UserBuilder) Phone(phone string) *UserBuilder {
    if b.err != nil { return b }
    b.user.phone = phone
    return b
}

func (b *UserBuilder) Address(address string) *UserBuilder {
    if b.err != nil { return b }
    b.user.address = address
    return b
}

func (b *UserBuilder) Verified(v bool) *UserBuilder {
    if b.err != nil { return b }
    b.user.isVerified = v
    return b
}

func (b *UserBuilder) Build() (User, error) {
    if b.err != nil {
        return User{}, b.err
    }
    return b.user, nil
}
```

```go
// Client
user, err := NewUserBuilder("Jane", "jane@example.com").
    Age(28).
    Phone("+91-9876543210").
    Verified(true).
    Build()

if err != nil {
    log.Fatal(err)
}
fmt.Println(user.name) // Jane
```

The `b.err != nil { return b }` pattern is called the **error propagation shortcut** — once any step fails, all subsequent steps are no-ops and `Build()` returns the error. Very idiomatic Go.

#### Lombok's @Builder — what it generates

Since you'll see this constantly in Java codebases:

java

```java
@Builder
@Getter
public class User {
    private final String name;
    private final String email;
    private final int age;
    private final String phone;
}
```

Lombok generates the entire `Builder` inner class at compile time — all the setters, `build()`, and the private constructor. The usage looks identical:

java

```java
User user = User.builder()
    .name("John")
    .email("john@example.com")
    .age(25)
    .build();
```

The pattern is the same — Lombok just removes the boilerplate. Understanding the manual implementation means you understand exactly what `@Builder` is doing under the hood.

#### Builder vs Constructor vs Setters

| |Constructor|Setters|Builder|
|---|---|---|---|
|Immutability|✅|❌|✅|
|Readable for many fields|❌|✅|✅|
|Validation at creation|✅|❌|✅|
|Optional fields|❌ ugly|✅|✅|
|Thread safe|✅|❌|✅|

#### Key Principles it satisfies

- **Single Responsibility** — construction logic lives in Builder, not the object itself
- **Immutability** — the final `User` object has no setters, safe to share across threads
- **Open/Closed** — add new optional fields to Builder without breaking existing call sites