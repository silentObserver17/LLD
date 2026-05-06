package LLDSolutions.DesignParkingLot.Strategy;

import LLDSolutions.DesignParkingLot.enums.VehicleType;
import LLDSolutions.DesignParkingLot.models.Level;
import LLDSolutions.DesignParkingLot.models.ParkingSpot;

import java.util.List;
import java.util.Optional;

public class NearestFirstStrategy implements SpotAssignmentStrategy {

    @Override
    public Optional<ParkingSpot> assignSpot(List<Level> levels, VehicleType vehicleType) {
        for(Level level : levels) {
            List<ParkingSpot> available = level.getAvailableSpots(vehicleType);
            if(!available.isEmpty()) {
                return Optional.of(available.get(0));
            }
        }
        return Optional.empty();
    }
}
