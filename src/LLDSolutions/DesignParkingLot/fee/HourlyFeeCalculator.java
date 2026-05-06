package LLDSolutions.DesignParkingLot.fee;

import LLDSolutions.DesignParkingLot.models.Ticket;

import java.time.Duration;
import java.time.LocalDateTime;

public class HourlyFeeCalculator implements FeeCalculator {
    private static final double MOTORCYCLE_RATE = 20.0;
    private static final double CAR_RATE        = 40.0;
    private static final double TRUCK_RATE      = 80.0;


    @Override
    public double calculate(Ticket ticket, LocalDateTime exitTime) {
        long minutes = Duration.between(LocalDateTime.now(), exitTime).toMinutes();
        long hours = (long) Math.ceil(minutes / 60.0); // round up to next hour
        hours = Math.max(1, hours);  // minimum 1 hour charge

        double rate = switch (ticket.getVehicle().getVehicleType()) {
            case MOTORCYCLE ->  MOTORCYCLE_RATE;
            case TRUCK -> TRUCK_RATE;
            case CAR -> CAR_RATE;
        };

        return hours * rate;
    }
}
