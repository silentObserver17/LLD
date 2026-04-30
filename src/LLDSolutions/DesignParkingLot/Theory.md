### 🧠 Step 0: How to Approach ANY LLD Question

Before jumping into Parking Lot, lock this mental model:

### 1. Clarify requirements (ALWAYS)

Ask:

- What types of vehicles?
- Multiple floors?
- Entry/exit gates?
- Payment system?
- Real-time availability?

👉 LLD is NOT about guessing—it’s about **scoping correctly**

### 2. Identify core entities (noun extraction)

From “Parking Lot”, you immediately get:

- ParkingLot
- Floor
- ParkingSpot
- Vehicle
- Ticket
- Gate

### 3. Define relationships

- ParkingLot → has Floors
- Floor → has Spots
- Spot → assigned to Vehicle
- Ticket → maps Vehicle ↔ Spot

### 4. Identify behaviors (verbs)

- parkVehicle()
- unparkVehicle()
- assignSpot()
- calculateFee()

### 5. Apply patterns (only where needed)

Don’t force patterns. Use them when:

- Behavior varies → Strategy
- Object creation complex → Factory
- Shared global state → Singleton

# 🚗 Parking Lot

Let’s define a realistic version:
#### Step 1 — Clarifying Requirements (Parking Lot)
Let's ask the right questions first. For a parking lot:

**Functional requirements we'll cover:**

- Multiple floors, multiple spots per floor
- Different vehicle types: Motorcycle, Car, Truck
- Different spot sizes: Small, Medium, Large
- A vehicle can only park in a spot that fits it (or larger)
- Issue a ticket on entry, calculate fee on exit
- Know if the lot is full

**Things we'll keep simple (out of scope for now):**

- Payment processing (just fee calculation)
- Reservations
- Multiple entry/exit gates (we'll have one of each)

#### Step 2 — Identify Entities (The Nouns)
Reading the problem, the nouns are:

> ParkingLot → Floor → ParkingSpot → Vehicle → Ticket

Let's also think about _supporting_ entities:
- `EntranceGate` / `ExitGate` — manage entry/exit
- `FeeCalculator` — strategy for pricing


### Step 3 — Relationships

```
ParkingLot
  └── has many → ParkingFloor
        └── has many → ParkingSpot
              └── can hold one → Vehicle

Vehicle (abstract)
  ├── Motorcycle
  ├── Car
  └── Truck

Ticket
  ├── links to → Vehicle
  └── links to → ParkingSpot
```

