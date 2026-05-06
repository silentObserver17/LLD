package LLDSolutions.DesignParkingLot.models;

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
