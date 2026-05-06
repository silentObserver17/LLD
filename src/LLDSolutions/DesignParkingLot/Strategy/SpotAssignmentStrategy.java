package LLDSolutions.DesignParkingLot.Strategy;

import LLDSolutions.DesignParkingLot.enums.VehicleType;
import LLDSolutions.DesignParkingLot.models.Level;
import LLDSolutions.DesignParkingLot.models.ParkingSpot;

import java.util.List;
import java.util.Optional;

public interface SpotAssignmentStrategy {
    Optional<ParkingSpot> assignSpot(List<Level> levels, VehicleType vehicleType);
}
