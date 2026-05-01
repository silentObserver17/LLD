# Abstract Factory Design Pattern
---
#### Intent

Provide an interface for creating **families of related objects** without specifying their concrete classes. Where Factory Method creates **one product**, Abstract Factory creates **a suite of related products** that are meant to work together.

#### The Core Problem

Imagine you're building a cross-platform UI toolkit. You need buttons, checkboxes, and text fields — but the look and behavior differs per OS:

```
Windows → WindowsButton + WindowsCheckbox + WindowsTextField
macOS   → MacButton    + MacCheckbox    + MacTextField
Linux   → LinuxButton  + LinuxCheckbox  + LinuxTextField
```

The constraint is: **you must never mix families**. A `MacButton` with a `WindowsCheckbox` would look broken.

Factory Method would only help you create one of these. Abstract Factory lets you create the **entire consistent family** through one interface.

#### Factory Method vs Abstract Factory

| |Factory Method|Abstract Factory|
|---|---|---|
|Creates|One product|A family of related products|
|Mechanism|Subclass overrides one method|Separate factory class per family|
|Use when|You need one type of object|You need multiple objects that must match|
|Example|`createNotification()`|`createButton() + createCheckbox()` together|
Think of it this way — **Abstract Factory is a factory of factories**.

### Structure
```
AbstractFactory (interface)
    ├── createButton()
    └── createCheckbox()

WindowsFactory ──implements──► AbstractFactory
    ├── createButton()   → WindowsButton
    └── createCheckbox() → WindowsCheckbox

MacFactory ──implements──► AbstractFactory
    ├── createButton()   → MacButton
    └── createCheckbox() → MacCheckbox

AbstractButton (interface)     AbstractCheckbox (interface)
    └── WindowsButton               └── WindowsCheckbox
    └── MacButton                   └── MacCheckbox

Client
    └── talks only to AbstractFactory + abstract products
    └── never touches concrete classes
```

```
code in java file
```

The `Application` class has **zero knowledge** of Windows or Mac. You swap the entire UI family by changing one line in `main`.

#### Implementation in Go
``` go
// Abstract products
type Button interface {
    Render()
    OnClick()
}

type Checkbox interface {
    Render()
    OnCheck()
}

// Windows family
type WindowsButton struct{}
func (w *WindowsButton) Render()   { fmt.Println("Rendering Windows Button [ OK ]") }
func (w *WindowsButton) OnClick()  { fmt.Println("Windows Button clicked") }

type WindowsCheckbox struct{}
func (w *WindowsCheckbox) Render()  { fmt.Println("Rendering Windows Checkbox [x]") }
func (w *WindowsCheckbox) OnCheck() { fmt.Println("Windows Checkbox checked") }

// Mac family
type MacButton struct{}
func (m *MacButton) Render()   { fmt.Println("Rendering Mac Button (rounded)") }
func (m *MacButton) OnClick()  { fmt.Println("Mac Button clicked") }

type MacCheckbox struct{}
func (m *MacCheckbox) Render()  { fmt.Println("Rendering Mac Checkbox ◉") }
func (m *MacCheckbox) OnCheck() { fmt.Println("Mac Checkbox checked") }

// Abstract Factory interface
type UIFactory interface {
    CreateButton()   Button
    CreateCheckbox() Checkbox
}

// Concrete Factories
type WindowsFactory struct{}
func (w *WindowsFactory) CreateButton()   Button   { return &WindowsButton{} }
func (w *WindowsFactory) CreateCheckbox() Checkbox { return &WindowsCheckbox{} }

type MacFactory struct{}
func (m *MacFactory) CreateButton()   Button   { return &MacButton{} }
func (m *MacFactory) CreateCheckbox() Checkbox { return &MacCheckbox{} }

// Application — only knows interfaces
type Application struct {
    button   Button
    checkbox Checkbox
}

func NewApplication(factory UIFactory) *Application {
    return &Application{
        button:   factory.CreateButton(),
        checkbox: factory.CreateCheckbox(),
    }
}

func (a *Application) RenderUI() {
    a.button.Render()
    a.checkbox.Render()
}

// Bootstrap
func main() {
    var factory UIFactory = &WindowsFactory{} // swap to MacFactory anytime
    app := NewApplication(factory)
    app.RenderUI()
}
```

#### Adding a new family — Linux

This is where Abstract Factory pays off. To add Linux support:
```java
// 1. New concrete products
public class LinuxButton implements Button {
    @Override public void render()  { System.out.println("Rendering Linux Button"); }
    @Override public void onClick() { System.out.println("Linux Button clicked"); }
}

public class LinuxCheckbox implements Checkbox {
    @Override public void render()  { System.out.println("Rendering Linux Checkbox"); }
    @Override public void onCheck() { System.out.println("Linux Checkbox checked"); }
}

// 2. New concrete factory
public class LinuxFactory implements UIFactory {
    @Override public Button createButton()     { return new LinuxButton(); }
    @Override public Checkbox createCheckbox() { return new LinuxCheckbox(); }
}
```

That's it. `Application`, `UIFactory`, `Button`, `Checkbox` — **nothing changes**. Pure OCP.

#### Adding a new product type — TextField

This is the **weakness** of Abstract Factory. If you need to add `TextField` to the family:

```java
public interface UIFactory {
    Button createButton();
    Checkbox createCheckbox();
    TextField createTextField(); // ← new method
}
```

Now **every existing concrete factory** (`WindowsFactory`, `MacFactory`, `LinuxFactory`) must be updated. This violates OCP in the other direction — adding a product type is expensive.

| Change                             | Impact                              |
| ---------------------------------- | ----------------------------------- |
| Add a new family (Linux)           | ✅ Easy — new factory class only     |
| Add a new product type (TextField) | ❌ Hard — all factory classes change |

This tradeoff is worth knowing for interviews.

#### Real-world Java examples

- **`javax.xml.parsers.DocumentBuilderFactory`** — creates parsers for different XML implementations
- **`java.sql.Connection`** — creates `Statement`, `PreparedStatement`, `CallableStatement` — a family of related DB objects
- **Spring's `ApplicationContext`** — acts as an abstract factory for beans, with different implementations (`ClassPathXmlApplicationContext`, `AnnotationConfigApplicationContext`) producing the same bean family

#### All Creational Patterns — Quick Recap

| Pattern              | Problem it solves                        | Key mechanism                               |
| -------------------- | ---------------------------------------- | ------------------------------------------- |
| **Singleton**        | Only one instance needed                 | Private constructor + static instance       |
| **Factory Method**   | Subclass decides which object to create  | Override one creation method                |
| **Abstract Factory** | Families of related objects              | Interface with multiple creation methods    |
| **Builder**          | Complex object with many optional fields | Step-by-step construction + method chaining |
