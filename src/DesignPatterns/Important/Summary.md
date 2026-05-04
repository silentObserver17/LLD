### Complete LLD Design Patterns — Full Recap

#### Creational Patterns

| Pattern              | One line                        | Key mechanism                         |
| -------------------- | ------------------------------- | ------------------------------------- |
| **Singleton**        | One instance globally           | Private constructor + static instance |
| **Factory Method**   | Subclass decides what to create | Override one creation method          |
| **Abstract Factory** | Families of related objects     | Interface with multiple creators      |
| **Builder**          | Complex object step by step     | Method chaining + `build()`           |

#### Structural Patterns

| Pattern       | One line                   | Key differentiator                                  |
| ------------- | -------------------------- | --------------------------------------------------- |
| **Adapter**   | Fix interface mismatch     | Translates interface                                |
| **Decorator** | Add behavior at runtime    | Same interface, wraps one object                    |
| **Proxy**     | Control access             | Same interface, controls when real object is called |
| **Facade**    | Simplify complex subsystem | Unifies many objects behind one interface           |

#### Behavioral Patterns

| Pattern                     | One line                    | Key differentiator                   |
| --------------------------- | --------------------------- | ------------------------------------ |
| **Observer**                | One-to-many notification    | Subject doesn't know who's listening |
| **Strategy**                | Swap algorithms at runtime  | Client picks the strategy            |
| **Command**                 | Request as an object        | Enables queue, undo, replay          |
| **Chain of Responsibility** | Pass request along handlers | Sender doesn't know who handles it   |
| **State**                   | Behavior changes with state | Object swaps itself internally       |
