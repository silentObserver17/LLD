package LLDSolutions.DesignParkingLot.fee;

import LLDSolutions.DesignParkingLot.models.Ticket;

import java.time.DayOfWeek;
import java.time.LocalDateTime;

public class WeekendDecorator implements FeeCalculator{

    private static final double WEEKEND_MULTIPLIER = 1.25;
    private final FeeCalculator wrapped;

    public WeekendDecorator(FeeCalculator wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public double calculate(Ticket ticket, LocalDateTime exitTime) {
        double baseFee = wrapped.calculate(ticket, exitTime);
        DayOfWeek day = ticket.getEntryTime().getDayOfWeek();

        if(day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY){
            System.out.println(" WWeekend surcharge applied (1.25x)");
            return baseFee * WEEKEND_MULTIPLIER;
        }

        return baseFee;
    }
}
