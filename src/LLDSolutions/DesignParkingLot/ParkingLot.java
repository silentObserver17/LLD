package LLDSolutions.DesignParkingLot;

import LLDSolutions.DesignParkingLot.Exceptions.InvalidTicketException;
import LLDSolutions.DesignParkingLot.Exceptions.ParkingLotFullException;
import LLDSolutions.DesignParkingLot.Observer.DisplayBoard;
import LLDSolutions.DesignParkingLot.Strategy.SpotAssignmentStrategy;
import LLDSolutions.DesignParkingLot.enums.VehicleType;
import LLDSolutions.DesignParkingLot.fee.FeeCalculator;
import LLDSolutions.DesignParkingLot.models.Level;
import LLDSolutions.DesignParkingLot.models.ParkingSpot;
import LLDSolutions.DesignParkingLot.models.Receipt;
import LLDSolutions.DesignParkingLot.models.Ticket;
import LLDSolutions.DesignParkingLot.models.vehicle.Vehicle;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class ParkingLot {
    // -- SINGLETON --------------------------------------------------------
    public static volatile ParkingLot instance;

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

    // -- STATE ----------------------------------------------------------------
    private List<Level> levels;
    private List<DisplayBoard> displayBoards;
    private SpotAssignmentStrategy assignmentStrategy;
    private FeeCalculator feeCalculator;

    private ParkingLot() {}

    // -- Initialization --------------------------------------------------------
    public void initialize(
            List<Level> levels,
            List<DisplayBoard> displayBoards,
            SpotAssignmentStrategy strategy,
            FeeCalculator feeCalculator
    ){
        this.levels = levels;
        this.displayBoards = displayBoards;
        this.assignmentStrategy = strategy;
        this.feeCalculator = feeCalculator;
    }

    // -- CORE OPERATIONS --------------------------------------------------------------
    public Ticket parkVehicle(Vehicle vehicle) {
        Optional<ParkingSpot> spotOpt = assignmentStrategy.assignSpot(levels, vehicle.getVehicleType());

        if(spotOpt.isEmpty()) {
            throw new ParkingLotFullException(vehicle.getVehicleType());
        }

        ParkingSpot spot = spotOpt.get();

        // Atomic check-and-assign — handles race condition
        // If another thread grabbed this spot first, retry
        boolean assigned = spot.AssignIfAvailable(vehicle);
        if(!assigned) {
            return parkVehicle(vehicle);   // retry — tail recursion
        }

        Ticket ticket = new Ticket(vehicle, spot);
        System.out.println("✓ Parked: " + vehicle + " at spot " + spot.getSpotId());
        return ticket;
    }

    public Receipt unparkVehicle(Ticket ticket) {
        if(!ticket.isActive()){
            throw new InvalidTicketException(ticket.getTicketId());
        }

        ParkingSpot spot = ticket.getSpot();
        LocalDateTime exitTime = LocalDateTime.now();
        double fee = feeCalculator.calculate(ticket, exitTime);

        spot.release();
        ticket.close();

        Receipt receipt = new Receipt(ticket, exitTime, fee);
        System.out.println("✓ Unparked: " + ticket.getVehicle() + " from spot " + spot.getSpotId());

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
