package LLDSolutions.DesignParkingLot.fee;

import LLDSolutions.DesignParkingLot.models.Ticket;

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

        if(hour >= PEAK_START && hour <= PEAK_END) {
            System.out.println("  Peak hour surcharge applied (1.5x)");
            return baseFee * PEAK_MULTIPLIER;
        }

        return baseFee;
    }
}
