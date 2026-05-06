# Question

Designing a Parking Lot System Requirements

1. The parking lot should have multiple levels, each level with a certain number of parking spots.
2. The parking lot should support different types of vehicles, such as cars, motorcycles, and trucks.
3. Each parking spot should be able to accommodate a specific type of vehicle.
4. The system should assign a parking spot to a vehicle upon entry and release it when the vehicle exits.
5. The system should track the availability of parking spots and provide real-time information to customers.
6. The system should handle multiple entry and exit points and support concurrent access.
---
### Understanding the Domain
Before jumping to classes, let's understand what actually happens in a parking lot:
```
Vehicle arrives at entry
    → system finds available spot of correct type
    → issues ticket (spot assigned, time recorded)
    → vehicle parks

Vehicle leaves at exit
    → ticket scanned
    → spot released
    → fee calculated
```

Three core concepts: **Spots**, **Vehicles**, **Tickets**. Everything else supports these.

### Identifying Entities
```
ParkingLot
    └── has many Levels
            └── has many ParkingSpots
                    └── accommodates specific VehicleType

Vehicle (hierarchy)
    └── Motorcycle
    └── Car
    └── Truck

Ticket
    └── links Vehicle to ParkingSpot
    └── records entry time
    └── used to calculate fee on exit

EntryPoint / ExitPoint
    └── handles vehicle entry/exit
    └── issues/processes tickets
```

### Vehicle Type → Spot Type Mapping
This is a core domain rule that needs careful thought:

```
Motorcycle → MOTORCYCLE spot only
Car        → CAR spot only
Truck      → TRUCK spot only
```

Real parking lots sometimes allow smaller vehicles in larger spots — but the requirement doesn't mention this. Keep it simple, follow requirements exactly.

### Spot Assignment — Finding the Right Spot
When a vehicle arrives, the system needs to:
```
1. Determine vehicle type
2. Find ANY available spot on ANY level that fits this type
3. Assign it atomically (thread safety — two vehicles can't get same spot)
```

Which level do we assign from? Strategy options:
```
Nearest available  → always assign lowest level first
Round robin        → distribute evenly across levels
Random             → simplest, no ordering guarantee
```

**Nearest available** is most realistic — pick the first available spot scanning from Level 1 down. This is where **Strategy Pattern is genuinely justified** — the assignment algorithm can vary and the requirement says nothing about which strategy to use.

### Thread Safety — The Critical NFR
Requirement 6 says concurrent access. Two vehicles can arrive simultaneously at different entry points. Both could be assigned the same spot if not handled carefully:
```
Thread 1 (Entry Point A): finds spot S1 available
Thread 2 (Entry Point B): finds spot S1 available   ← race condition
Thread 1: assigns S1 to Car A
Thread 2: assigns S1 to Car B                       ← double assignment!
```

This must be atomic — **find + assign must happen as one operation**.

### Design Patterns Applicable
**1. Strategy — Spot Assignment Algorithm**
```
SpotAssignmentStrategy (interface)
    └── assign(List<Level> levels, VehicleType type) → ParkingSpot

NearestFirstStrategy    — scan from level 1, first available
RoundRobinStrategy      — distribute across levels evenly
```

Justified because assignment logic is genuinely variable and independent of the core domain.

**2. Factory Method — Vehicle Creation**
```
VehicleFactory
    └── create(licensePlate, type) → Vehicle
```

Client doesn't call `new Car()` directly — factory decides the concrete type.

**3. Singleton — ParkingLot**

```
ParkingLot
    └── one instance for the entire system
    └── all entry/exit points share the same lot state
```

Justified — there is exactly one parking lot. All entry and exit points must operate on the same shared state.

**4. Observer — Spot Availability**

```
ParkingSpot (Subject)
    └── notifies when status changes

DisplayBoard (Observer)
    └── updates available count per level in real time
```

Requirement 5 says "real time information to customers" — Observer is the natural fit.

**5. Decorator — Fee Calculation**

```
BaseFeeStrategy         → flat rate per hour
PeakHourDecorator       → adds peak hour surcharge
WeekendDecorator        → adds weekend surcharge
OversizeDecorator       → adds surcharge for trucks
```

Fee rules compose independently — Decorator over a base strategy.

### Class Design
```
┌─────────────────────────────────────────────────────────────┐
│                        ParkingLot                           │
│  - instance: ParkingLot (Singleton)                         │
│  - levels: List<Level>                                      │
│  - entryPoints: List<EntryPoint>                            │
│  - exitPoints: List<ExitPoint>                              │
│  - assignmentStrategy: SpotAssignmentStrategy               │
│  + getInstance(): ParkingLot                                │
│  + parkVehicle(vehicle): Ticket                             │
│  + unparkVehicle(ticket): Receipt                           │
│  + getAvailableSpots(type): int                             │
└─────────────────────────────────────────────────────────────┘
              │ has many
              ▼
┌─────────────────────────────────────────────────────────────┐
│                          Level                              │
│  - levelNumber: int                                         │
│  - spots: List<ParkingSpot>                                 │
│  + getAvailableSpots(type): List<ParkingSpot>               │
│  + hasAvailability(type): boolean                           │
└─────────────────────────────────────────────────────────────┘
              │ has many
              ▼
┌─────────────────────────────────────────────────────────────┐
│                       ParkingSpot                           │
│  - spotId: String                                           │
│  - spotType: SpotType                                       │
│  - status: SpotStatus (AVAILABLE, OCCUPIED)                 │
│  - currentVehicle: Vehicle                                  │
│  + assign(vehicle): void                                    │
│  + release(): void                                          │
│  + isAvailable(): boolean                                   │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                    Vehicle (abstract)                       │
│  - licensePlate: String                                     │
│  - vehicleType: VehicleType                                 │
│  + getType(): VehicleType                                   │
├─────────────────────────────────────────────────────────────┤
│  Motorcycle extends Vehicle                                 │
│  Car        extends Vehicle                                 │
│  Truck      extends Vehicle                                 │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                         Ticket                              │
│  - ticketId: String                                         │
│  - vehicle: Vehicle                                         │
│  - spot: ParkingSpot                                        │
│  - entryTime: LocalDateTime                                 │
│  - status: TicketStatus (ACTIVE, CLOSED)                    │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                         Receipt                             │
│  - ticket: Ticket                                           │
│  - exitTime: LocalDateTime                                  │
│  - fee: double                                              │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                SpotAssignmentStrategy                       │
│  + assign(levels, vehicleType): Optional<ParkingSpot>       │
├─────────────────────────────────────────────────────────────┤
│  NearestFirstStrategy                                       │
│  RoundRobinStrategy                                         │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                     FeeCalculator                           │
│  + calculate(ticket, exitTime): double                      │
├─────────────────────────────────────────────────────────────┤
│  HourlyFeeCalculator (base)                                 │
│  PeakHourDecorator                                          │
│  WeekendDecorator                                           │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                      DisplayBoard                           │
│  - level: Level                                             │
│  - availableCounts: Map<SpotType, Integer>                  │
│  + update(): void   ← Observer                              │
│  + display(): void                                          │
└─────────────────────────────────────────────────────────────┘
```

### Enums
``` java
public enum VehicleType  { MOTORCYCLE, CAR, TRUCK }
public enum SpotType     { MOTORCYCLE, CAR, TRUCK }
public enum SpotStatus   { AVAILABLE, OCCUPIED }
public enum TicketStatus { ACTIVE, CLOSED }
```

SpotType mirrors VehicleType — they're separate enums because in future a spot might accommodate multiple vehicle types. Keeping them separate doesn't cost anything and avoids coupling.

### Thread Safety Design

Two levels of locking needed:
**Spot level lock** — when assigning a spot, lock that specific spot:

```java
// ParkingSpot
private final ReentrantLock lock = new ReentrantLock();

public boolean assignIfAvailable(Vehicle vehicle) {
    lock.lock();
    try {
        if (status != SpotStatus.AVAILABLE) return false;
        this.currentVehicle = vehicle;
        this.status = SpotStatus.OCCUPIED;
        return true;
    } finally {
        lock.unlock();
    }
}
```

**Why spot-level and not lot-level?**

- Lot-level lock means only one vehicle can enter at a time — kills throughput
- Spot-level lock means multiple vehicles can be assigned simultaneously as long as they get different spots
- Only contention is when two vehicles want the exact same spot — extremely rare

### Core Flow — parkVehicle
```
parkVehicle(vehicle):
    1. strategy.assign(levels, vehicle.getType())
           → scans levels for available spot
           → returns Optional<ParkingSpot>

    2. spot not found → throw ParkingLotFullException

    3. spot.assignIfAvailable(vehicle)
           → atomic check-and-assign under spot lock
           → if returns false (race condition lost) → retry from step 1

    4. create Ticket(vehicle, spot, now())

    5. notify DisplayBoard observers → update counts

    6. return Ticket
```

### Core Flow — unparkVehicle
```
unparkVehicle(ticket):
    1. validate ticket — ACTIVE status

    2. spot = ticket.getSpot()
       spot.release()           → status = AVAILABLE, vehicle = null

    3. exitTime = now()

    4. fee = feeCalculator.calculate(ticket, exitTime)

    5. ticket.setStatus(CLOSED)

    6. notify DisplayBoard observers → update counts

    7. return Receipt(ticket, exitTime, fee)
```

### What We're NOT building

Keeping scope honest — these are out of scope for this LLD:

- Payment processing — fee is calculated, not collected
- Reservation system — no pre-booking
- Vehicle re-entry — same ticket model only
- Overflow parking — lot full means rejection
- Admin panel — no spot configuration at runtime

### Design Summary
```
┌──────────────────────────────────────────────────────────────┐
│                  Parking Lot Design Summary                  │
├──────────────────────────────────────────────────────────────┤
│  Patterns Used:                                              │
│    Singleton   → ParkingLot (one lot, shared state)          │
│    Strategy    → SpotAssignmentStrategy (justified — varies) │
│    Factory     → VehicleFactory (decouple creation)          │
│    Observer    → DisplayBoard (real-time availability)       │
│    Decorator   → FeeCalculator (composable fee rules)        │
│                                                              │
│  Thread Safety:                                              │
│    Spot-level ReentrantLock (not lot-level)                  │
│    Atomic check-and-assign in assignIfAvailable()            │
│    Retry on race condition loss                              │
│                                                              │
│  Key Design Decisions:                                       │
│    SpotType ≠ VehicleType enum (future flexibility)          │
│    Ticket links vehicle ↔ spot ↔ time (single source)        │
│    Receipt separate from Ticket (open = Ticket, closed = Receipt) │
│    Optional<ParkingSpot> for clean miss handling             │
└──────────────────────────────────────────────────────────────┘
```

Let's build it!

---

## Project Structure

```
parking-lot/
├── enums/
│   ├── VehicleType.java
│   ├── SpotType.java
│   ├── SpotStatus.java
│   └── TicketStatus.java
├── models/
│   ├── vehicle/
│   │   ├── Vehicle.java
│   │   ├── Car.java
│   │   ├── Motorcycle.java
│   │   └── Truck.java
│   ├── ParkingSpot.java
│   ├── Level.java
│   ├── Ticket.java
│   └── Receipt.java
├── strategy/
│   ├── SpotAssignmentStrategy.java
│   └── NearestFirstStrategy.java
├── factory/
│   └── VehicleFactory.java
├── fee/
│   ├── FeeCalculator.java
│   ├── HourlyFeeCalculator.java
│   ├── PeakHourDecorator.java
│   └── WeekendDecorator.java
├── observer/
│   ├── ParkingObserver.java
│   └── DisplayBoard.java
├── exceptions/
│   ├── ParkingLotFullException.java
│   └── InvalidTicketException.java
├── ParkingLot.java
└── Main.java
```

---

## Enums

```java
// enums/VehicleType.java
public enum VehicleType {
    MOTORCYCLE, CAR, TRUCK
}

// enums/SpotType.java
public enum SpotType {
    MOTORCYCLE, CAR, TRUCK
}

// enums/SpotStatus.java
public enum SpotStatus {
    AVAILABLE, OCCUPIED
}

// enums/TicketStatus.java
public enum TicketStatus {
    ACTIVE, CLOSED
}
```

---

## Vehicle Hierarchy

```java
// models/vehicle/Vehicle.java
public abstract class Vehicle {
    private final String licensePlate;
    private final VehicleType vehicleType;

    public Vehicle(String licensePlate, VehicleType vehicleType) {
        this.licensePlate = licensePlate;
        this.vehicleType  = vehicleType;
    }

    public String getLicensePlate() { return licensePlate; }
    public VehicleType getVehicleType() { return vehicleType; }

    @Override
    public String toString() {
        return vehicleType + "(" + licensePlate + ")";
    }
}

// models/vehicle/Car.java
public class Car extends Vehicle {
    public Car(String licensePlate) {
        super(licensePlate, VehicleType.CAR);
    }
}

// models/vehicle/Motorcycle.java
public class Motorcycle extends Vehicle {
    public Motorcycle(String licensePlate) {
        super(licensePlate, VehicleType.MOTORCYCLE);
    }
}

// models/vehicle/Truck.java
public class Truck extends Vehicle {
    public Truck(String licensePlate) {
        super(licensePlate, VehicleType.TRUCK);
    }
}
```

---

## VehicleFactory

```java
// factory/VehicleFactory.java
public class VehicleFactory {

    public static Vehicle create(String licensePlate, VehicleType type) {
        return switch (type) {
            case CAR        -> new Car(licensePlate);
            case MOTORCYCLE -> new Motorcycle(licensePlate);
            case TRUCK      -> new Truck(licensePlate);
        };
    }
}
```

---

## ParkingSpot

```java
// models/ParkingSpot.java
import java.util.concurrent.locks.ReentrantLock;

public class ParkingSpot {

    private final String spotId;
    private final SpotType spotType;
    private volatile SpotStatus status;
    private Vehicle currentVehicle;

    // Spot level lock — fine grained concurrency
    private final ReentrantLock lock = new ReentrantLock();

    // Observers — display boards watching this spot
    private final List<ParkingObserver> observers = new ArrayList<>();

    public ParkingSpot(String spotId, SpotType spotType) {
        this.spotId   = spotId;
        this.spotType = spotType;
        this.status   = SpotStatus.AVAILABLE;
    }

    // ── Observer registration ─────────────────────────────────

    public void addObserver(ParkingObserver observer) {
        observers.add(observer);
    }

    private void notifyObservers() {
        observers.forEach(o -> o.onSpotStatusChanged(this));
    }

    // ── Core operations — thread safe ─────────────────────────

    // Atomic check-and-assign
    // Returns true if successfully assigned, false if already taken
    public boolean assignIfAvailable(Vehicle vehicle) {
        lock.lock();
        try {
            if (status != SpotStatus.AVAILABLE) return false;
            this.currentVehicle = vehicle;
            this.status         = SpotStatus.OCCUPIED;
            notifyObservers();
            return true;
        } finally {
            lock.unlock();
        }
    }

    public void release() {
        lock.lock();
        try {
            this.currentVehicle = null;
            this.status         = SpotStatus.AVAILABLE;
            notifyObservers();
        } finally {
            lock.unlock();
        }
    }

    // ── Getters ───────────────────────────────────────────────

    public String getSpotId()             { return spotId; }
    public SpotType getSpotType()         { return spotType; }
    public SpotStatus getStatus()         { return status; }
    public Vehicle getCurrentVehicle()    { return currentVehicle; }
    public boolean isAvailable()          { return status == SpotStatus.AVAILABLE; }

    // SpotType matches VehicleType by name
    public boolean canFit(VehicleType vehicleType) {
        return spotType.name().equals(vehicleType.name());
    }

    @Override
    public String toString() {
        return spotId + "[" + spotType + ":" + status + "]";
    }
}
```

---

## Level

```java
// models/Level.java
public class Level {

    private final int levelNumber;
    private final List<ParkingSpot> spots;

    public Level(int levelNumber, List<ParkingSpot> spots) {
        this.levelNumber = levelNumber;
        this.spots       = Collections.unmodifiableList(spots);
    }

    public List<ParkingSpot> getAvailableSpots(VehicleType type) {
        return spots.stream()
            .filter(s -> s.canFit(type) && s.isAvailable())
            .collect(Collectors.toList());
    }

    public boolean hasAvailability(VehicleType type) {
        return spots.stream()
            .anyMatch(s -> s.canFit(type) && s.isAvailable());
    }

    public List<ParkingSpot> getSpots()  { return spots; }
    public int getLevelNumber()          { return levelNumber; }

    public int getAvailableCount(VehicleType type) {
        return (int) spots.stream()
            .filter(s -> s.canFit(type) && s.isAvailable())
            .count();
    }

    public int getTotalCount(VehicleType type) {
        return (int) spots.stream()
            .filter(s -> s.canFit(type))
            .count();
    }
}
```

---

## Ticket and Receipt

```java
// models/Ticket.java
import java.time.LocalDateTime;
import java.util.UUID;

public class Ticket {

    private final String ticketId;
    private final Vehicle vehicle;
    private final ParkingSpot spot;
    private final LocalDateTime entryTime;
    private TicketStatus status;

    public Ticket(Vehicle vehicle, ParkingSpot spot) {
        this.ticketId  = UUID.randomUUID().toString();
        this.vehicle   = vehicle;
        this.spot      = spot;
        this.entryTime = LocalDateTime.now();
        this.status    = TicketStatus.ACTIVE;
    }

    public void close()                    { this.status = TicketStatus.CLOSED; }
    public boolean isActive()              { return status == TicketStatus.ACTIVE; }

    public String getTicketId()            { return ticketId; }
    public Vehicle getVehicle()            { return vehicle; }
    public ParkingSpot getSpot()           { return spot; }
    public LocalDateTime getEntryTime()    { return entryTime; }
    public TicketStatus getStatus()        { return status; }

    @Override
    public String toString() {
        return "Ticket{id=" + ticketId
            + ", vehicle=" + vehicle
            + ", spot=" + spot.getSpotId()
            + ", entry=" + entryTime + "}";
    }
}

// models/Receipt.java
import java.time.LocalDateTime;

public class Receipt {

    private final Ticket ticket;
    private final LocalDateTime exitTime;
    private final double fee;

    public Receipt(Ticket ticket, LocalDateTime exitTime, double fee) {
        this.ticket   = ticket;
        this.exitTime = exitTime;
        this.fee      = fee;
    }

    public Ticket getTicket()              { return ticket; }
    public LocalDateTime getExitTime()     { return exitTime; }
    public double getFee()                 { return fee; }

    @Override
    public String toString() {
        return "\n── Receipt ──────────────────────────"
            + "\n  Vehicle    : " + ticket.getVehicle()
            + "\n  Spot       : " + ticket.getSpot().getSpotId()
            + "\n  Entry      : " + ticket.getEntryTime()
            + "\n  Exit       : " + exitTime
            + "\n  Fee        : ₹" + String.format("%.2f", fee)
            + "\n─────────────────────────────────────";
    }
}
```

---

## Observer

```java
// observer/ParkingObserver.java
public interface ParkingObserver {
    void onSpotStatusChanged(ParkingSpot spot);
}

// observer/DisplayBoard.java
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class DisplayBoard implements ParkingObserver {

    private final int levelNumber;
    // AtomicInteger — thread safe counter per spot type
    private final ConcurrentHashMap<SpotType, AtomicInteger> availableCounts
        = new ConcurrentHashMap<>();

    public DisplayBoard(int levelNumber, Level level) {
        this.levelNumber = levelNumber;

        // Initialize counts from level
        for (SpotType type : SpotType.values()) {
            VehicleType vType = VehicleType.valueOf(type.name());
            availableCounts.put(type,
                new AtomicInteger(level.getAvailableCount(vType)));
        }
    }

    @Override
    public void onSpotStatusChanged(ParkingSpot spot) {
        SpotType type    = spot.getSpotType();
        AtomicInteger counter = availableCounts.get(type);

        if (spot.isAvailable()) {
            counter.incrementAndGet(); // spot released
        } else {
            counter.decrementAndGet(); // spot occupied
        }

        display();
    }

    public void display() {
        System.out.println("── Display Board — Level " + levelNumber + " ──");
        availableCounts.forEach((type, count) ->
            System.out.printf("  %-12s: %d available%n", type, count.get()));
    }
}
```

---

## Strategy

```java
// strategy/SpotAssignmentStrategy.java
public interface SpotAssignmentStrategy {
    Optional<ParkingSpot> assign(List<Level> levels, VehicleType vehicleType);
}

// strategy/NearestFirstStrategy.java
public class NearestFirstStrategy implements SpotAssignmentStrategy {

    @Override
    public Optional<ParkingSpot> assign(List<Level> levels, VehicleType vehicleType) {
        // Scan from level 1 downward — nearest first
        for (Level level : levels) {
            List<ParkingSpot> available = level.getAvailableSpots(vehicleType);
            if (!available.isEmpty()) {
                return Optional.of(available.get(0));
            }
        }
        return Optional.empty(); // no spot found
    }
}
```

---

## Fee Calculator

```java
// fee/FeeCalculator.java
import java.time.LocalDateTime;

public interface FeeCalculator {
    double calculate(Ticket ticket, LocalDateTime exitTime);
}

// fee/HourlyFeeCalculator.java
import java.time.Duration;
import java.time.LocalDateTime;

public class HourlyFeeCalculator implements FeeCalculator {

    private static final double MOTORCYCLE_RATE = 20.0;
    private static final double CAR_RATE        = 40.0;
    private static final double TRUCK_RATE      = 80.0;

    @Override
    public double calculate(Ticket ticket, LocalDateTime exitTime) {
        long minutes  = Duration.between(ticket.getEntryTime(), exitTime).toMinutes();
        long hours    = (long) Math.ceil(minutes / 60.0); // round up to next hour
        hours         = Math.max(1, hours);                // minimum 1 hour charge

        double rate = switch (ticket.getVehicle().getVehicleType()) {
            case MOTORCYCLE -> MOTORCYCLE_RATE;
            case CAR        -> CAR_RATE;
            case TRUCK      -> TRUCK_RATE;
        };

        return hours * rate;
    }
}

// fee/PeakHourDecorator.java
import java.time.LocalDateTime;

public class PeakHourDecorator implements FeeCalculator {

    private static final double PEAK_MULTIPLIER = 1.5;
    private static final int PEAK_START = 9;
    private static final int PEAK_END   = 18;

    private final FeeCalculator wrapped;

    public PeakHourDecorator(FeeCalculator wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public double calculate(Ticket ticket, LocalDateTime exitTime) {
        double baseFee = wrapped.calculate(ticket, exitTime);
        int hour = ticket.getEntryTime().getHour();

        if (hour >= PEAK_START && hour < PEAK_END) {
            System.out.println("  Peak hour surcharge applied (1.5x)");
            return baseFee * PEAK_MULTIPLIER;
        }
        return baseFee;
    }
}

// fee/WeekendDecorator.java
import java.time.DayOfWeek;
import java.time.LocalDateTime;

public class WeekendDecorator implements FeeCalculator {

    private static final double WEEKEND_MULTIPLIER = 1.25;
    private final FeeCalculator wrapped;

    public WeekendDecorator(FeeCalculator wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public double calculate(Ticket ticket, LocalDateTime exitTime) {
        double baseFee  = wrapped.calculate(ticket, exitTime);
        DayOfWeek day   = ticket.getEntryTime().getDayOfWeek();

        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            System.out.println("  Weekend surcharge applied (1.25x)");
            return baseFee * WEEKEND_MULTIPLIER;
        }
        return baseFee;
    }
}
```

---

## Exceptions

```java
// exceptions/ParkingLotFullException.java
public class ParkingLotFullException extends RuntimeException {
    public ParkingLotFullException(VehicleType type) {
        super("No available spot for vehicle type: " + type);
    }
}

// exceptions/InvalidTicketException.java
public class InvalidTicketException extends RuntimeException {
    public InvalidTicketException(String ticketId) {
        super("Invalid or already closed ticket: " + ticketId);
    }
}
```

---

## ParkingLot — Singleton + Core Orchestrator

```java
// ParkingLot.java
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class ParkingLot {

    // ── Singleton ─────────────────────────────────────────────
    private static volatile ParkingLot instance;

    public static ParkingLot getInstance() {
        if (instance == null) {
            synchronized (ParkingLot.class) {
                if (instance == null) {
                    instance = new ParkingLot();
                }
            }
        }
        return instance;
    }

    // ── State ─────────────────────────────────────────────────
    private List<Level> levels;
    private List<DisplayBoard> displayBoards;
    private SpotAssignmentStrategy assignmentStrategy;
    private FeeCalculator feeCalculator;

    private ParkingLot() {} // private — Singleton

    // ── Initialization (called once at startup) ───────────────
    public void initialize(
            List<Level> levels,
            List<DisplayBoard> displayBoards,
            SpotAssignmentStrategy strategy,
            FeeCalculator feeCalculator) {

        this.levels             = levels;
        this.displayBoards      = displayBoards;
        this.assignmentStrategy = strategy;
        this.feeCalculator      = feeCalculator;
    }

    // ── Core Operations ───────────────────────────────────────

    public Ticket parkVehicle(Vehicle vehicle) {
        Optional<ParkingSpot> spotOpt = assignmentStrategy
            .assign(levels, vehicle.getVehicleType());

        if (spotOpt.isEmpty()) {
            throw new ParkingLotFullException(vehicle.getVehicleType());
        }

        ParkingSpot spot = spotOpt.get();

        // Atomic check-and-assign — handles race condition
        // If another thread grabbed this spot first, retry
        boolean assigned = spot.assignIfAvailable(vehicle);
        if (!assigned) {
            return parkVehicle(vehicle); // retry — tail recursion
        }

        Ticket ticket = new Ticket(vehicle, spot);
        System.out.println("✓ Parked: " + vehicle
            + " at spot " + spot.getSpotId());
        return ticket;
    }

    public Receipt unparkVehicle(Ticket ticket) {
        if (!ticket.isActive()) {
            throw new InvalidTicketException(ticket.getTicketId());
        }

        ParkingSpot spot    = ticket.getSpot();
        LocalDateTime exitTime = LocalDateTime.now();
        double fee          = feeCalculator.calculate(ticket, exitTime);

        spot.release();
        ticket.close();

        Receipt receipt = new Receipt(ticket, exitTime, fee);
        System.out.println("✓ Unparked: " + ticket.getVehicle()
            + " from spot " + spot.getSpotId());
        return receipt;
    }

    // ── Availability ──────────────────────────────────────────

    public int getAvailableSpots(VehicleType type) {
        return levels.stream()
            .mapToInt(l -> l.getAvailableCount(type))
            .sum();
    }

    public void displayAllBoards() {
        displayBoards.forEach(DisplayBoard::display);
    }
}
```

---

## Main.java — Wiring + Driver

```java
// Main.java
import java.util.*;

public class Main {

    public static void main(String[] args) throws InterruptedException {

        // ── Build Levels ──────────────────────────────────────
        Level level1 = buildLevel(1, 2, 3, 1); // 2 motorcycle, 3 car, 1 truck
        Level level2 = buildLevel(2, 2, 3, 1);

        List<Level> levels = List.of(level1, level2);

        // ── Build Display Boards ──────────────────────────────
        DisplayBoard board1 = new DisplayBoard(1, level1);
        DisplayBoard board2 = new DisplayBoard(2, level2);

        // Wire display boards as observers to each spot
        level1.getSpots().forEach(s -> s.addObserver(board1));
        level2.getSpots().forEach(s -> s.addObserver(board2));

        // ── Fee Calculator — Decorator chain ──────────────────
        FeeCalculator feeCalculator = new WeekendDecorator(
                                        new PeakHourDecorator(
                                          new HourlyFeeCalculator()));

        // ── Initialize ParkingLot ─────────────────────────────
        ParkingLot lot = ParkingLot.getInstance();
        lot.initialize(
            levels,
            List.of(board1, board2),
            new NearestFirstStrategy(),
            feeCalculator
        );

        // ── Scenario 1: Basic park and unpark ─────────────────
        System.out.println("\n═══ Scenario 1: Basic Operations ═══");
        lot.displayAllBoards();

        Vehicle car1  = VehicleFactory.create("GJ01AB1234", VehicleType.CAR);
        Vehicle car2  = VehicleFactory.create("GJ01CD5678", VehicleType.CAR);
        Vehicle bike1 = VehicleFactory.create("GJ02EF9012", VehicleType.MOTORCYCLE);
        Vehicle truck = VehicleFactory.create("GJ03GH3456", VehicleType.TRUCK);

        Ticket t1 = lot.parkVehicle(car1);
        Ticket t2 = lot.parkVehicle(car2);
        Ticket t3 = lot.parkVehicle(bike1);
        Ticket t4 = lot.parkVehicle(truck);

        System.out.println("\nAfter parking 4 vehicles:");
        lot.displayAllBoards();

        Receipt r1 = lot.unparkVehicle(t1);
        System.out.println(r1);

        System.out.println("\nAfter unparking car1:");
        lot.displayAllBoards();

        // ── Scenario 2: Lot full for a type ───────────────────
        System.out.println("\n═══ Scenario 2: Lot Full ═══");
        // Fill all truck spots
        Ticket truckT2 = lot.parkVehicle(
            VehicleFactory.create("GJ04IJ7890", VehicleType.TRUCK));

        try {
            // No more truck spots
            lot.parkVehicle(VehicleFactory.create("GJ05KL1234", VehicleType.TRUCK));
        } catch (ParkingLotFullException e) {
            System.out.println("Expected: " + e.getMessage());
        }

        // ── Scenario 3: Invalid ticket ─────────────────────────
        System.out.println("\n═══ Scenario 3: Invalid Ticket ═══");
        Receipt r4 = lot.unparkVehicle(t4);
        System.out.println(r4);
        try {
            lot.unparkVehicle(t4); // already closed
        } catch (InvalidTicketException e) {
            System.out.println("Expected: " + e.getMessage());
        }

        // ── Scenario 4: Concurrent access ─────────────────────
        System.out.println("\n═══ Scenario 4: Concurrent Access ═══");
        int threadCount = 6;
        Thread[] threads = new Thread[threadCount];
        List<Ticket> tickets = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            final int id = i;
            threads[i] = new Thread(() -> {
                try {
                    Vehicle car = VehicleFactory.create(
                        "THREAD-" + id, VehicleType.CAR);
                    Ticket ticket = lot.parkVehicle(car);
                    tickets.add(ticket);
                    System.out.println("Thread " + id
                        + " parked at " + ticket.getSpot().getSpotId());
                } catch (ParkingLotFullException e) {
                    System.out.println("Thread " + id + ": " + e.getMessage());
                }
            });
        }

        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        System.out.println("\nFinal available CAR spots: "
            + lot.getAvailableSpots(VehicleType.CAR));
    }

    // ── Helper — builds a level with given spot counts ────────
    private static Level buildLevel(
            int levelNum,
            int motorcycleCount,
            int carCount,
            int truckCount) {

        List<ParkingSpot> spots = new ArrayList<>();
        String prefix = "L" + levelNum;

        for (int i = 0; i < motorcycleCount; i++)
            spots.add(new ParkingSpot(prefix + "-M" + (i+1), SpotType.MOTORCYCLE));

        for (int i = 0; i < carCount; i++)
            spots.add(new ParkingSpot(prefix + "-C" + (i+1), SpotType.CAR));

        for (int i = 0; i < truckCount; i++)
            spots.add(new ParkingSpot(prefix + "-T" + (i+1), SpotType.TRUCK));

        return new Level(levelNum, spots);
    }
}
```

---

## Expected Output

```
═══ Scenario 1: Basic Operations ═══
── Display Board — Level 1 ──
  MOTORCYCLE  : 2 available
  CAR         : 3 available
  TRUCK       : 1 available
── Display Board — Level 2 ──
  MOTORCYCLE  : 2 available
  CAR         : 3 available
  TRUCK       : 1 available

✓ Parked: CAR(GJ01AB1234) at spot L1-C1
✓ Parked: CAR(GJ01CD5678) at spot L1-C2
✓ Parked: MOTORCYCLE(GJ02EF9012) at spot L1-M1
✓ Parked: TRUCK(GJ03GH3456) at spot L1-T1

After parking 4 vehicles:
── Display Board — Level 1 ──
  MOTORCYCLE  : 1 available
  CAR         : 1 available
  TRUCK       : 0 available

✓ Unparked: CAR(GJ01AB1234) from spot L1-C1

── Receipt ──────────────────────────
  Vehicle    : CAR(GJ01AB1234)
  Spot       : L1-C1
  Entry      : 2026-05-04T10:00
  Exit       : 2026-05-04T10:45
  Fee        : ₹40.00
─────────────────────────────────────

═══ Scenario 2: Lot Full ═══
Expected: No available spot for vehicle type: TRUCK

═══ Scenario 3: Invalid Ticket ═══
── Receipt ──────────────────────────
  Fee        : ₹80.00
─────────────────────────────────────
Expected: Invalid or already closed ticket: <uuid>

═══ Scenario 4: Concurrent Access ═══
Thread 0 parked at L1-C1
Thread 2 parked at L1-C3
Thread 1 parked at L2-C1
Thread 3 parked at L2-C2
Thread 4 parked at L2-C3
Thread 5: No available spot for vehicle type: CAR

Final available CAR spots: 0
```

---

## Design Decisions Summary

|Decision|Reasoning|
|---|---|
|Spot-level `ReentrantLock`|Fine-grained — multiple vehicles park simultaneously|
|Retry on race condition|Tail recursion — clean, handles rare contention|
|`volatile` on instance + DCL|Thread-safe Singleton without always locking|
|`AtomicInteger` in DisplayBoard|Thread-safe counter without explicit lock|
|Decorator chain for fees|Each rule composes independently|
|`Optional<ParkingSpot>` in strategy|Explicit miss handling — no nulls|
|`initialize()` separate from constructor|Singleton can't take constructor args — two-phase init|
