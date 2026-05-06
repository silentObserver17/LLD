package LLDSolutions.DesignParkingLot.fee;

import LLDSolutions.DesignParkingLot.models.Ticket;

import java.time.LocalDateTime;

public interface FeeCalculator {
    double calculate(Ticket ticket, LocalDateTime exitTime);
}
