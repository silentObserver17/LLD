## What is a LRU Cache?

**LRU** stands for **Least Recently Used**. LRU Cache is a type of cache replacement policy that **evicts the least recently accessed item** when the cache reaches its capacity.

In performance-critical systems (like web servers, databases, or OS memory management), caching helps avoid expensive computations or repeated data fetching. But cache memory is limited so when it's full, we need a policy to decide **which item to remove**.

LRU chooses the **least recently accessed** item, based on the assumption that:

> “If you haven’t used something for a while, you probably won’t need it soon.”

The LRU strategy is both intuitive and effective. It reflects real-world usage patterns. We tend to access the same small subset of items frequently, and rarely go back to older, untouched entries.

This makes LRU a popular default caching policy in many systems where **speed and memory efficiency** are critical.

## 1.1 Functional Requirements

- Support `get(key)` operation: returns the value if the key exists, otherwise returns `null` or `-1`
- Support `put(key, value)` operation: inserts a new key-value pair or updates the value of an existing key
- If the cache exceeds its capacity, it should automatically **evict the least recently used item**.
- Both `get` and `put` operations should update the **recency** of the accessed or inserted item.
- Keys and values should be **generic** (e.g., `<K, V>`), provided the keys are hashable.

## 1.2 Non-Functional Requirements

1. **Time Complexity:** Both `get` and `put` operations must run in O(1) time on average.
2. **Thread Safety:** The implementation must be thread-safe for use in concurrent environments.
3. **Modularity:** The design should follow object-oriented principles with clean separation of responsibilities.
4. **Memory Efficiency:** The internal data structures should be optimized for speed and space within the defined constraints.

Now that we understand what we're building, let's identify the building blocks of our system.

### Understanding the Core Problem

LRU Cache needs to answer two questions in O(1):

**On `get(key)`:**

- Does this key exist? → O(1) lookup
- Move it to "most recently used" position → O(1) update

**On `put(key, value)`:**

- Does key exist? Update value + move to MRU → O(1)
- New key? Insert at MRU position → O(1)
- Over capacity? Remove LRU item → O(1)

The challenge is — what data structure gives us O(1) lookup **AND** O(1) ordered insertion/deletion simultaneously?

### The Data Structure Insight

Let's think through the options:

**HashMap alone:**

```
get → O(1) ✅
track order → ❌ HashMap has no order
```

**Array/LinkedList alone:**

```
track order → ✅
get → O(n) ❌ must scan to find key
```

**HashMap + Array:**

```
get → O(1) ✅
move to front → O(n) ❌ shifting elements is expensive
```

**HashMap + Doubly Linked List → O(1) everything ✅**

```
HashMap:
  key → pointer to Node in LinkedList

Doubly LinkedList:
  HEAD ◄──► Node1 ◄──► Node2 ◄──► Node3 ◄──► TAIL
  (MRU)                                       (LRU)

Why doubly linked?
  To remove a node you need prev and next pointers
  With only next pointer, removing a node is O(n)
  With both, removal is O(1) — just rewire neighbors
```

This combination is the **canonical LRU solution**. HashMap gives O(1) lookup. Doubly LinkedList gives O(1) insertion and deletion anywhere. Together they give O(1) for all operations.

### Visual — How it works
**Initial state — capacity 3, cache: A→1, B→2, C→3:**
```
HashMap:
  A → NodeA
  B → NodeB
  C → NodeC

List (MRU → LRU):
HEAD ◄──► [A:1] ◄──► [B:2] ◄──► [C:3] ◄──► TAIL
```

**get(A) — move A to front:**
```
HEAD ◄──► [A:1] ◄──► [B:2] ◄──► [C:3] ◄──► TAIL
                                              ↑ LRU
After:
HEAD ◄──► [A:1] ◄──► [B:2] ◄──► [C:3] ◄──► TAIL
          ↑ wait — A was already MRU

Let's say get(C):
HEAD ◄──► [C:3] ◄──► [A:1] ◄──► [B:2] ◄──► TAIL
```

**put(D→4) — over capacity, evict LRU (B):**
```
Before:
HEAD ◄──► [C:3] ◄──► [A:1] ◄──► [B:2] ◄──► TAIL
                                   ↑ LRU — evict this

After:
HEAD ◄──► [D:4] ◄──► [C:3] ◄──► [A:1] ◄──► TAIL
HashMap: remove B, add D
```

##### Sentinel Nodes — why they matter
Real implementation uses **dummy HEAD and TAIL nodes** that never hold actual data:

```
Without sentinels — edge cases everywhere:
  Empty list? → null checks
  Inserting first node? → special case
  Removing last node? → special case

With sentinels — zero edge cases:
HEAD(dummy) ◄──► [real nodes] ◄──► TAIL(dummy)

Insert after HEAD → always valid (HEAD.next always exists)
Remove before TAIL → always valid (TAIL.prev always exists)
Empty list = HEAD ◄──► TAIL — still valid structure
```

### Class Design
```
┌─────────────────────────────────┐
│         LRUCache<K, V>          │  ← Context / Public API
│─────────────────────────────────│
│ - capacity: int                 │
│ - map: HashMap<K, Node<K,V>>    │
│ - head: Node<K,V> (sentinel)    │
│ - tail: Node<K,V> (sentinel)    │
│─────────────────────────────────│
│ + get(key: K): V                │
│ + put(key: K, value: V): void   │
│─────────────────────────────────│
│ - addToFront(node)              │  ← private internals
│ - removeNode(node)              │
│ - evictLRU()                    │
└─────────────────────────────────┘
           │ uses
           ▼
┌─────────────────────────────────┐
│         Node<K, V>              │  ← Data container
│─────────────────────────────────│
│ - key: K                        │
│ - value: V                      │
│ - prev: Node<K,V>               │
│ - next: Node<K,V>               │
└─────────────────────────────────┘
```

Node stores the **key** too — critical for eviction. When you evict the LRU node (tail.prev), we need its key to remove it from the HashMap. Without storing key in Node, we can't do this in O(1).

### Design Patterns Applicable

**1. Template Method** — `get` and `put` both follow a skeleton:

```
get → [lookup] → [validate] → [update recency] → [return]
put → [lookup] → [update or insert] → [update recency] → [evict if needed]
```

The skeleton is fixed, steps are well-defined.

**2. Null Object Pattern** — instead of returning `null` on cache miss, you can return a Null Object. Keeps client code clean — no null checks.

**3. Strategy Pattern** — the eviction policy is a Strategy. Today it's LRU. Tomorrow it could be LFU or FIFO. Extracting `EvictionPolicy` as an interface makes the cache extensible:

```
EvictionPolicy<K> (interface)
    └── onAccess(key)        ← called on get/put hit
    └── onInsert(key)        ← called on new put
    └── evict() → K          ← returns key to evict

LRUEvictionPolicy   implements EvictionPolicy
LFUEvictionPolicy   implements EvictionPolicy
FIFOEvictionPolicy  implements EvictionPolicy
```

This is the cleanest OOP design — separates **what to cache** from **how to evict**.

**4. Decorator Pattern** — thread safety added as a decorator:

```
LRUCache (core, not thread-safe)
    └── wrapped by ThreadSafeLRUCache (adds synchronization)
```

Core logic stays clean. Concurrency concern is separate.

### Thread Safety Design

For the thread safety NFR, two approaches:
**Approach 1 — Coarse-grained lock (`synchronized` / `sync.Mutex`):**

```java
public synchronized V get(K key) { ... }
public synchronized void put(K key, V value) { ... }
```

Simple. Correct. But only **one thread at a time** for everything — reads block reads.

**Approach 2 — `ReadWriteLock`:**
```java
private final ReadWriteLock lock = new ReentrantReadWriteLock();

public V get(K key) {
    lock.readLock().lock();    // multiple readers allowed simultaneously
    try { ... }
    finally { lock.readLock().unlock(); }
}

public void put(K key, V value) {
    lock.writeLock().lock();   // exclusive — blocks all readers + writers
    try { ... }
    finally { lock.writeLock().unlock(); }
}
```

Better throughput for read-heavy workloads.

**BUT — LRU `get` is actually a write operation internally** (moves node to front). So ReadWriteLock doesn't give much benefit here — `get` also needs a write lock. Coarse-grained `synchronized` is actually the right call for LRU specifically.

In Go:
```go
type LRUCache[K comparable, V any] struct {
    mu       sync.Mutex   // protects everything
    capacity int
    // ...
}
```

### Operations walkthrough:

**`get(key)`:**
```
1. lock
2. key in map? → NO  → unlock, return -1
3.              → YES → get node from map
4.                      removeNode(node)     ← detach from current position
5.                      addToFront(node)     ← move to MRU
6.                      unlock, return node.value
```

**`put(key, value)`:**
```
1. lock
2. key in map? → YES → get node
                        update node.value
                        removeNode(node)
                        addToFront(node)
                        unlock, return
3.             → NO  → create new node(key, value)
4.                      map.put(key, node)
5.                      addToFront(node)
6.                      map.size > capacity?
7.                          → YES → lruNode = tail.prev
8.                                  removeNode(lruNode)
9.                                  map.remove(lruNode.key)  ← why Node stores key
10.                     unlock, return
```

**`addToFront(node)`:**

```
// Insert between HEAD and HEAD.next
node.prev = HEAD
node.next = HEAD.next
HEAD.next.prev = node
HEAD.next = node
```

**`removeNode(node)`:**

```
// Rewire neighbors — node disappears from list
node.prev.next = node.next
node.next.prev = node.prev
```

Both are pure pointer operations — O(1), no iteration.

### Complete Design Summary
```
┌─────────────────────────────────────────────────────┐
│                   LRU Cache Design                  │
├─────────────────────────────────────────────────────┤
│  Data Structures:                                   │
│    HashMap<K, Node>  → O(1) lookup                  │
│    Doubly LinkedList → O(1) ordered insert/delete   │
│    Sentinel HEAD + TAIL → eliminate edge cases      │
│                                                     │
│  Design Patterns:                                   │
│    Strategy  → pluggable eviction policy            │
│    Decorator → thread safety as a wrapper           │
│    Null Object → clean miss handling                │
│                                                     │
│  Thread Safety:                                     │
│    sync.Mutex (Go) / synchronized (Java)            │
│    Coarse-grained — get IS a write internally       │
│                                                     │
│  Key Insight:                                       │
│    Node stores key → enables O(1) eviction          │
│    Sentinel nodes → zero edge cases                 │
│    Eviction = removeNode(tail.prev) + map.remove()  │
└─────────────────────────────────────────────────────┘
```

---

## Implementation in Java

### Node.java

```java
public class Node<K, V> {
    K key;
    V value;
    Node<K, V> prev;
    Node<K, V> next;

    public Node(K key, V value) {
        this.key   = key;
        this.value = value;
    }
}
```

---

### EvictionPolicy.java

```java
public interface EvictionPolicy<K> {
    void onAccess(K key);   // called on get hit or put update
    void onInsert(K key);   // called on new put
    K evict();              // returns key to evict
}
```

---

### LRUEvictionPolicy.java

```java
public class LRUEvictionPolicy<K, V> implements EvictionPolicy<K> {

    // Sentinel nodes — never hold real data
    private final Node<K, V> head;
    private final Node<K, V> tail;

    public LRUEvictionPolicy() {
        head = new Node<>(null, null); // dummy HEAD
        tail = new Node<>(null, null); // dummy TAIL
        head.next = tail;
        tail.prev = head;
    }

    @Override
    public void onAccess(K key) {
        // Will be called with the actual node from cache
        // We'll handle this differently — see LRUCache
    }

    @Override
    public void onInsert(K key) {
        // Same — handled in cache
    }

    @Override
    public K evict() {
        // LRU node is just before TAIL
        Node<K, V> lruNode = tail.prev;
        if (lruNode == head) return null; // empty
        removeNode(lruNode);
        return lruNode.key;
    }

    // ── Internal linked list operations ──────────────────────

    public void addToFront(Node<K, V> node) {
        node.prev       = head;
        node.next       = head.next;
        head.next.prev  = node;
        head.next       = node;
    }

    public void removeNode(Node<K, V> node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
        // don't null out prev/next — node might be re-inserted
    }

    public void moveToFront(Node<K, V> node) {
        removeNode(node);
        addToFront(node);
    }
}
```

---

### LRUCache.java — Core (not thread-safe)

```java
import java.util.HashMap;
import java.util.Map;

public class LRUCache<K, V> {

    private final int capacity;
    private final Map<K, Node<K, V>> map;
    private final LRUEvictionPolicy<K, V> policy;

    public LRUCache(int capacity) {
        if (capacity <= 0)
            throw new IllegalArgumentException("Capacity must be positive");

        this.capacity = capacity;
        this.map      = new HashMap<>();
        this.policy   = new LRUEvictionPolicy<>();
    }

    // ── Public API ────────────────────────────────────────────

    public V get(K key) {
        Node<K, V> node = map.get(key);
        if (node == null) return null; // cache miss

        policy.moveToFront(node); // update recency
        return node.value;
    }

    public void put(K key, V value) {
        Node<K, V> existing = map.get(key);

        if (existing != null) {
            // Key exists — update value and move to front
            existing.value = value;
            policy.moveToFront(existing);
            return;
        }

        // New key — create node and insert
        Node<K, V> newNode = new Node<>(key, value);
        map.put(key, newNode);
        policy.addToFront(newNode);

        // Over capacity — evict LRU
        if (map.size() > capacity) {
            K evictedKey = policy.evict();
            if (evictedKey != null) {
                map.remove(evictedKey);
            }
        }
    }

    public int size()     { return map.size(); }
    public boolean containsKey(K key) { return map.containsKey(key); }

    // ── Debug helper ─────────────────────────────────────────

    public void printCache() {
        StringBuilder sb = new StringBuilder("MRU → ");
        Node<K, V> curr = policy.head.next;
        while (curr != policy.tail) {
            sb.append("[").append(curr.key).append(":").append(curr.value).append("] ");
            curr = curr.next;
        }
        sb.append("← LRU");
        System.out.println(sb);
    }
}
```

---

### ThreadSafeLRUCache.java — Decorator

```java
public class ThreadSafeLRUCache<K, V> {

    private final LRUCache<K, V> cache;
    private final Object lock = new Object();

    public ThreadSafeLRUCache(int capacity) {
        this.cache = new LRUCache<>(capacity);
    }

    public V get(K key) {
        synchronized (lock) {
            return cache.get(key);
        }
    }

    public void put(K key, V value) {
        synchronized (lock) {
            cache.put(key, value);
        }
    }

    public int size() {
        synchronized (lock) {
            return cache.size();
        }
    }

    public void printCache() {
        synchronized (lock) {
            cache.printCache();
        }
    }
}
```

---

### Main.java — Driver

```java
public class Main {
    public static void main(String[] args) {

        System.out.println("═══ Basic Operations ═══");
        LRUCache<Integer, String> cache = new LRUCache<>(3);

        cache.put(1, "A");
        cache.put(2, "B");
        cache.put(3, "C");
        cache.printCache();
        // MRU → [3:C] [2:B] [1:A] ← LRU

        System.out.println("get(1): " + cache.get(1)); // A — moves 1 to front
        cache.printCache();
        // MRU → [1:A] [3:C] [2:B] ← LRU

        cache.put(4, "D"); // over capacity — evict LRU (2:B)
        cache.printCache();
        // MRU → [4:D] [1:A] [3:C] ← LRU

        System.out.println("get(2): " + cache.get(2)); // null — evicted

        System.out.println("\n═══ Update Existing Key ═══");
        cache.put(1, "A_updated"); // update existing
        cache.printCache();
        // MRU → [1:A_updated] [4:D] [3:C] ← LRU

        System.out.println("\n═══ Generic Types ═══");
        LRUCache<String, Integer> wordCache = new LRUCache<>(2);
        wordCache.put("hello", 1);
        wordCache.put("world", 2);
        wordCache.printCache();
        // MRU → [world:2] [hello:1] ← LRU

        wordCache.get("hello"); // move hello to front
        wordCache.put("java", 3); // evict world
        wordCache.printCache();
        // MRU → [java:3] [hello:1] ← LRU

        System.out.println("\n═══ Thread Safety Test ═══");
        ThreadSafeLRUCache<Integer, Integer> safeCache =
            new ThreadSafeLRUCache<>(100);

        // Spin up 10 threads hammering the cache simultaneously
        Thread[] threads = new Thread[10];
        for (int i = 0; i < 10; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 100; j++) {
                    safeCache.put(threadId * 100 + j, threadId * 100 + j);
                    safeCache.get(threadId * 100 + j);
                }
            });
        }

        for (Thread t : threads) t.start();
        for (Thread t : threads) {
            try { t.join(); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }

        System.out.println("Thread-safe cache size: " + safeCache.size());
        // Always ≤ 100 — no corruption despite concurrent access
    }
}
```

---

## Implementation in Go

### node.go

```go
package lrucache

// Node holds a key-value pair with doubly linked pointers
type Node[K comparable, V any] struct {
    key   K
    value V
    prev  *Node[K, V]
    next  *Node[K, V]
}
```

---

### eviction_policy.go

```go
package lrucache

// EvictionPolicy defines the strategy for cache eviction
type EvictionPolicy[K comparable, V any] interface {
    AddToFront(node *Node[K, V])
    RemoveNode(node *Node[K, V])
    MoveToFront(node *Node[K, V])
    Evict() *Node[K, V] // returns evicted node (caller removes from map)
}
```

---

### lru_eviction_policy.go

```go
package lrucache

// LRUEvictionPolicy implements doubly linked list
// with sentinel HEAD and TAIL nodes
type LRUEvictionPolicy[K comparable, V any] struct {
    head *Node[K, V] // sentinel — MRU side
    tail *Node[K, V] // sentinel — LRU side
}

func NewLRUEvictionPolicy[K comparable, V any]() *LRUEvictionPolicy[K, V] {
    head := &Node[K, V]{} // dummy head
    tail := &Node[K, V]{} // dummy tail
    head.next = tail
    tail.prev = head
    return &LRUEvictionPolicy[K, V]{head: head, tail: tail}
}

// AddToFront inserts node right after HEAD (MRU position)
func (p *LRUEvictionPolicy[K, V]) AddToFront(node *Node[K, V]) {
    node.prev      = p.head
    node.next      = p.head.next
    p.head.next.prev = node
    p.head.next    = node
}

// RemoveNode detaches node from its current position
func (p *LRUEvictionPolicy[K, V]) RemoveNode(node *Node[K, V]) {
    node.prev.next = node.next
    node.next.prev = node.prev
}

// MoveToFront removes then re-inserts at front
func (p *LRUEvictionPolicy[K, V]) MoveToFront(node *Node[K, V]) {
    p.RemoveNode(node)
    p.AddToFront(node)
}

// Evict removes and returns the LRU node (tail.prev)
// Returns nil if cache is empty
func (p *LRUEvictionPolicy[K, V]) Evict() *Node[K, V] {
    lruNode := p.tail.prev
    if lruNode == p.head {
        return nil // empty list
    }
    p.RemoveNode(lruNode)
    return lruNode
}
```

---

### lru_cache.go — Core (not thread-safe)

```go
package lrucache

import "fmt"

// LRUCache is the core cache — not thread-safe
// Use ThreadSafeLRUCache for concurrent environments
type LRUCache[K comparable, V any] struct {
    capacity int
    cache    map[K]*Node[K, V]
    policy   *LRUEvictionPolicy[K, V]
}

func NewLRUCache[K comparable, V any](capacity int) *LRUCache[K, V] {
    if capacity <= 0 {
        panic("capacity must be positive")
    }
    return &LRUCache[K, V]{
        capacity: capacity,
        cache:    make(map[K]*Node[K, V]),
        policy:   NewLRUEvictionPolicy[K, V](),
    }
}

// Get returns value and true on hit, zero value and false on miss
func (c *LRUCache[K, V]) Get(key K) (V, bool) {
    node, ok := c.cache[key]
    if !ok {
        var zero V
        return zero, false // cache miss
    }

    c.policy.MoveToFront(node) // update recency
    return node.value, true
}

// Put inserts or updates a key-value pair
// Evicts LRU entry if over capacity
func (c *LRUCache[K, V]) Put(key K, value V) {
    if existing, ok := c.cache[key]; ok {
        // Key exists — update and move to front
        existing.value = value
        c.policy.MoveToFront(existing)
        return
    }

    // New key — create and insert
    node := &Node[K, V]{key: key, value: value}
    c.cache[key] = node
    c.policy.AddToFront(node)

    // Over capacity — evict LRU
    if len(c.cache) > c.capacity {
        evicted := c.policy.Evict()
        if evicted != nil {
            delete(c.cache, evicted.key)
        }
    }
}

func (c *LRUCache[K, V]) Size() int { return len(c.cache) }

func (c *LRUCache[K, V]) Contains(key K) bool {
    _, ok := c.cache[key]
    return ok
}

// PrintCache prints from MRU to LRU — for debugging
func (c *LRUCache[K, V]) PrintCache() {
    curr := c.policy.head.next
    fmt.Print("MRU → ")
    for curr != c.policy.tail {
        fmt.Printf("[%v:%v] ", curr.key, curr.value)
        curr = curr.next
    }
    fmt.Println("← LRU")
}
```

---

### thread_safe_lru_cache.go — Decorator

```go
package lrucache

import "sync"

// ThreadSafeLRUCache wraps LRUCache with a mutex — Decorator pattern
type ThreadSafeLRUCache[K comparable, V any] struct {
    mu    sync.Mutex
    cache *LRUCache[K, V]
}

func NewThreadSafeLRUCache[K comparable, V any](capacity int) *ThreadSafeLRUCache[K, V] {
    return &ThreadSafeLRUCache[K, V]{
        cache: NewLRUCache[K, V](capacity),
    }
}

func (c *ThreadSafeLRUCache[K, V]) Get(key K) (V, bool) {
    c.mu.Lock()
    defer c.mu.Unlock()
    return c.cache.Get(key)
}

func (c *ThreadSafeLRUCache[K, V]) Put(key K, value V) {
    c.mu.Lock()
    defer c.mu.Unlock()
    c.cache.Put(key, value)
}

func (c *ThreadSafeLRUCache[K, V]) Size() int {
    c.mu.Lock()
    defer c.mu.Unlock()
    return c.cache.Size()
}

func (c *ThreadSafeLRUCache[K, V]) PrintCache() {
    c.mu.Lock()
    defer c.mu.Unlock()
    c.cache.PrintCache()
}
```

---

### main.go — Driver

```go
package main

import (
    "fmt"
    "sync"
    lrucache "lru/cache"
)

func main() {

    fmt.Println("═══ Basic Operations ═══")
    cache := lrucache.NewLRUCache[int, string](3)

    cache.Put(1, "A")
    cache.Put(2, "B")
    cache.Put(3, "C")
    cache.PrintCache()
    // MRU → [3:C] [2:B] [1:A] ← LRU

    val, ok := cache.Get(1)
    fmt.Printf("get(1): %v, found: %v\n", val, ok) // A, true
    cache.PrintCache()
    // MRU → [1:A] [3:C] [2:B] ← LRU

    cache.Put(4, "D") // evicts 2:B (LRU)
    cache.PrintCache()
    // MRU → [4:D] [1:A] [3:C] ← LRU

    _, ok = cache.Get(2)
    fmt.Println("get(2) found:", ok) // false — evicted

    fmt.Println("\n═══ Update Existing Key ═══")
    cache.Put(1, "A_updated")
    cache.PrintCache()
    // MRU → [1:A_updated] [4:D] [3:C] ← LRU

    fmt.Println("\n═══ Generic Types — string keys ═══")
    wordCache := lrucache.NewLRUCache[string, int](2)
    wordCache.Put("hello", 1)
    wordCache.Put("world", 2)
    wordCache.PrintCache()
    // MRU → [world:2] [hello:1] ← LRU

    wordCache.Get("hello")    // move to front
    wordCache.Put("go", 3)    // evict world
    wordCache.PrintCache()
    // MRU → [go:3] [hello:1] ← LRU

    fmt.Println("\n═══ Edge Cases ═══")

    // Capacity 1
    tiny := lrucache.NewLRUCache[int, string](1)
    tiny.Put(1, "A")
    tiny.Put(2, "B") // evicts 1
    tiny.PrintCache()
    // MRU → [2:B] ← LRU
    _, ok = tiny.Get(1)
    fmt.Println("get(1) after eviction:", ok) // false

    // Same key put twice
    tiny.Put(2, "B_updated")
    tiny.PrintCache()
    // MRU → [2:B_updated] ← LRU

    fmt.Println("\n═══ Thread Safety Test ═══")
    safeCache := lrucache.NewThreadSafeLRUCache[int, int](100)

    var wg sync.WaitGroup
    for i := 0; i < 10; i++ {
        wg.Add(1)
        go func(threadId int) {
            defer wg.Done()
            for j := 0; j < 100; j++ {
                key := threadId*100 + j
                safeCache.Put(key, key*2)
                safeCache.Get(key)
            }
        }(i)
    }
    wg.Wait()

    fmt.Printf("Thread-safe cache size: %d (max 100)\n", safeCache.Size())
    // Always ≤ 100 — no data races
}
```

---

## File Structure

```
Java:
src/
├── Node.java
├── EvictionPolicy.java
├── LRUEvictionPolicy.java
├── LRUCache.java
├── ThreadSafeLRUCache.java
└── Main.java

Go:
lru/
├── cache/
│   ├── node.go
│   ├── eviction_policy.go
│   ├── lru_eviction_policy.go
│   ├── lru_cache.go
│   └── thread_safe_lru_cache.go
└── main.go
```

---

## Design Decisions

|Decision|Reasoning|
|---|---|
|Sentinel HEAD + TAIL|Eliminates all edge cases in list operations|
|Node stores key|Enables O(1) map deletion during eviction|
|EvictionPolicy interface|Strategy pattern — swap LRU for LFU without touching cache|
|ThreadSafe as Decorator|Core logic stays clean, concurrency concern is separate|
|Generics `<K,V>`|Type-safe, reusable across any key-value types|
|Coarse-grained lock|`get` modifies list order — it's internally a write|
|Return `(V, bool)` in Go|Idiomatic — distinguishes miss from zero-value hit|

---

When the question says **"Design LRU Cache"** — the eviction policy is fixed. It's LRU. Period. Adding a Strategy pattern here is **over-engineering** — we're solving a problem that doesn't exist in the requirements.

---

## What Over-Engineering Costs us

People notice this and it actually **hurts** us:

- Shows you're adding complexity without a reason
- Wastes time explaining patterns that don't serve the problem
- Can signal you're pattern-matching ("I know Strategy, let me use it") rather than thinking about the actual problem
- The interface abstraction introduced a real design flaw — we ended up going back and forth fixing it, which wouldn't have happened if we just built LRU directly

---

## The Right Approach

```java
// This is all you need — clean, direct, correct
public class LRUCache<K, V> {

    private final int capacity;
    private final Map<K, Node<K, V>> map;
    private final Node<K, V> head; // sentinel
    private final Node<K, V> tail; // sentinel

    public LRUCache(int capacity) {
        this.capacity = capacity;
        this.map      = new HashMap<>();
        this.head     = new Node<>(null, null);
        this.tail     = new Node<>(null, null);
        head.next     = tail;
        tail.prev     = head;
    }

    public V get(K key) { ... }
    public void put(K key, V value) { ... }

    private void addToFront(Node<K, V> node) { ... }
    private void removeNode(Node<K, V> node) { ... }
}
```

No EvictionPolicy interface. No CacheFactory. No strategy resolver. Just the data structures and operations the problem demands.

---

## The Rule of Thumb

| Situation                                                       | Right call                       |
| --------------------------------------------------------------- | -------------------------------- |
| Question says "Design LRU Cache"                                | Build LRU directly — no Strategy |
| Question says "Design a cache that supports pluggable eviction" | Strategy Pattern justified       |
| Question says "Design a cache system that supports LRU and LFU" | Strategy Pattern justified       |

**Add patterns when requirements demand flexibility. Not before.**

The patterns worth keeping for this specific question are:

- ✅ **Decorator** — ThreadSafeLRUCache wrapping LRUCache — justified by the thread safety NFR
- ✅ **Null Object / Optional** — it is optional to implement.
- ❌ **Strategy** — not justified, requirement is fixed to LRU

Good challenge — this kind of pushback on your own design is exactly what strong engineers do.

### Final Clean Structure
```
Node<K, V>
    └── key, value, prev, next

LRUEvictionPolicy<K, V>
    └── owns: head, tail sentinels
    └── addToFront(node)
    └── removeNode(node)
    └── moveToFront(node)
    └── evictLRU() → Node

LRUCache<K, V>
    └── owns: map, capacity, policy
    └── get(key) → delegates ordering to policy
    └── put(key, value) → delegates ordering to policy

ThreadSafeLRUCache<K, V>        ← Decorator
    └── wraps LRUCache
    └── adds sync.Mutex / synchronized
```

Three files. Clean boundaries. No over-engineering. Each class has one reason to change:

- `Node` changes if the data shape changes
- `LRUEvictionPolicy` changes if the ordering logic changes
- `LRUCache` changes if the cache API or capacity logic changes
- `ThreadSafeLRUCache` changes if the concurrency strategy changes

---

## Functional Requirements ✅

| Requirement                          | How it's met                     |
| ------------------------------------ | -------------------------------- |
| `get(key)` returns value or null     | ✅ `LRUCache.get()`               |
| `put(key, value)` inserts or updates | ✅ `LRUCache.put()`               |
| Evict LRU on exceeding capacity      | ✅ `LRUEvictionPolicy.evictLRU()` |
| `get` and `put` both update recency  | ✅ `moveToFront()` called in both |
| Generic keys and values `<K, V>`     | ✅ Full generics throughout       |

## Non-Functional Requirements ✅

| Requirement       | How it's met                                                                    |
| ----------------- | ------------------------------------------------------------------------------- |
| O(1) get and put  | ✅ HashMap + Doubly LinkedList                                                   |
| Thread safety     | ✅ `ThreadSafeLRUCache` decorator with mutex                                     |
| Modularity / SRP  | ✅ Node, LRUEvictionPolicy, LRUCache, ThreadSafeLRUCache — each owns one concern |
| Memory efficiency | ✅ No redundant storage — single HashMap, single LinkedList                      |

## Design Patterns used — all justified ✅

| Pattern       | Justification                                   |
| ------------- | ----------------------------------------------- |
| **Decorator** | Thread safety added without touching core logic |


---

## One thing worth noting

- Why HashMap + DoublyLinkedList → O(1)
- Why sentinel nodes → no edge cases

- Why coarse lock not ReadWriteLock → get is internally a write
- Why Strategy was considered but rejected → over-engineering for fixed requirement
- Why SRP justifies keeping LRUEvictionPolicy separate


## Why Node stores key → O(1) eviction
#### The Scenario — Eviction

When cache exceeds capacity, we evict the LRU node which is always `tail.prev`:

```java
private void evict() {
    Node<K, V> lruNode = tail.prev; // O(1) — we have the node directly
    removeNode(lruNode);            // O(1) — pointer rewiring

    // NOW — we need to remove it from the HashMap too
    map.remove(???);                // what key do we pass here?
}
```

We have the **node** in hand. But `map.remove()` needs a **key**.

---

## Your Argument

You're saying — we already have `map` which is `HashMap<K, Node>`. So can't we just get the key from the map somehow?

The problem is — **HashMap is not bidirectional**.

```
HashMap<K, Node>:
  key → node   ✅ you can do this (that's what HashMap does)
  node → key   ❌ you cannot do this without scanning everything
```

To go from node → key using only the HashMap, you'd have to iterate all entries and find which key maps to this node:

```java
// Without key stored in node — this is what you'd have to do
K evictedKey = null;
for (Map.Entry<K, Node<K,V>> entry : map.entrySet()) {
    if (entry.getValue() == lruNode) {  // reference equality
        evictedKey = entry.getKey();
        break;
    }
}
map.remove(evictedKey); // O(n) — unacceptable
```

That's O(n) — breaks the core requirement.

---

## Why storing K in Node solves it

```java
Node<K, V> lruNode = tail.prev;  // O(1) — LRU node
removeNode(lruNode);              // O(1) — rewire pointers
map.remove(lruNode.key);          // O(1) — key is right there on the node
```

The node itself carries its own key. No HashMap scan needed. No extra lookup. Pure O(1).

---

## Direct answer to your question

> Will it work without storing K in node?

**No** — not in O(1). You'd need O(n) to find which key maps to the LRU node.

The node stores key precisely because eviction starts from the **node side** (tail.prev), not the **key side**. You arrive at the node first, and then need to find its key to clean up the map. Without the key on the node, there's no O(1) path back to the map entry.

It's a classic **reverse lookup problem** — and storing the key on the node is the O(1) solution to it.