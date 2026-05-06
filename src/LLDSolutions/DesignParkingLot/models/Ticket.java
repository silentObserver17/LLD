package LLDSolutions.DesignParkingLot.models;

import LLDSolutions.DesignParkingLot.enums.TicketStatus;
import LLDSolutions.DesignParkingLot.models.vehicle.Vehicle;

import java.time.LocalDateTime;
import java.util.UUID;

public class Ticket {
    private final String ticketId;
    private final Vehicle vehicle;
    private final ParkingSpot spot;
    private final LocalDateTime entryTime;
    private TicketStatus status;

    public Ticket(Vehicle vehicle, ParkingSpot spot) {
        this.ticketId = UUID.randomUUID().toString();
        this.vehicle = vehicle;
        this.entryTime = LocalDateTime.now();
        this.spot = spot;
        this.status = TicketStatus.ACTIVE;
    }

    public String getTicketId() {
        return ticketId;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public ParkingSpot getSpot() {
        return spot;
    }

    public LocalDateTime getEntryTime() {
        return entryTime;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void close(){
        this.status = TicketStatus.CLOSED;
    }

    public boolean isActive(){
        return status == TicketStatus.ACTIVE;
    }

    @Override
    public String toString() {
        return "Ticket{id=" + ticketId
                + ", vehicle=" + vehicle
                + ", spot=" + spot.getSpotId()
                + ", entry=" + entryTime + "}";
    }
}
