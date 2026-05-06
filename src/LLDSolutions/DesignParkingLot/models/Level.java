package LLDSolutions.DesignParkingLot.models;

import LLDSolutions.DesignParkingLot.enums.VehicleType;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Level {
    private final int levelNumber;
    private final List<ParkingSpot> parkingSpots;

    public Level(int levelNumber, List<ParkingSpot> parkingSpots) {
        this.levelNumber = levelNumber;
        this.parkingSpots = Collections.unmodifiableList(parkingSpots);
    }

    public List<ParkingSpot> getAvailableSpots(VehicleType vehicleType) {
        return parkingSpots.stream()
                .filter(s -> s.canFit(vehicleType) && s.isAvailable())
                .collect(Collectors.toList());
    }

    public boolean hasAvailability(VehicleType vehicleType) {
        return parkingSpots.stream().anyMatch(s -> s.canFit(vehicleType) && s.isAvailable());
    }

    public int getLevelNumber() {
        return levelNumber;
    }

    public List<ParkingSpot> getParkingSpots() {
        return parkingSpots;
    }

    public int getAvailableCount(VehicleType vehicleType) {
        return (int)parkingSpots.stream()
                .filter(s -> s.canFit(vehicleType) && s.isAvailable())
                .count();
    }

    public int getTotalCount(VehicleType vehicleType) {
        return (int)parkingSpots.stream()
                .filter(s -> s.canFit(vehicleType))
                .count();
    }
}
