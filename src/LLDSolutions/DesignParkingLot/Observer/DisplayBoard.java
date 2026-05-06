package LLDSolutions.DesignParkingLot.Observer;

import LLDSolutions.DesignParkingLot.enums.SpotType;
import LLDSolutions.DesignParkingLot.enums.VehicleType;
import LLDSolutions.DesignParkingLot.models.Level;
import LLDSolutions.DesignParkingLot.models.ParkingSpot;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class DisplayBoard implements ParkingObserver {
    private final int levelNumber;

    // AtomicInteger - Thread safe counter per spot.
    private final ConcurrentHashMap<SpotType, AtomicInteger> availableCounts = new ConcurrentHashMap<>();

    public DisplayBoard(int levelNumber, Level level) {
        this.levelNumber = levelNumber;

        // Initialize Counts from Levels
        for(SpotType spot : SpotType.values()){
            VehicleType vehicleType = VehicleType.valueOf(spot.name());
            availableCounts.put(spot, new AtomicInteger(level.getAvailableCount(vehicleType)));
        }
    }

    @Override
    public void onSpotStatusChanged(ParkingSpot spot) {
        SpotType type = spot.getSpotType();
        AtomicInteger counter = availableCounts.get(type);

        if(spot.isAvailable()){
            counter.incrementAndGet(); // spot Released
        }else{
            counter.decrementAndGet(); // spot occupied
        }

        display();
    }

    public void display() {
        System.out.println("── Display Board — Level " + levelNumber + " ──");
        availableCounts.forEach((type, count) ->
                System.out.printf("  %-12s: %d available%n", type, count.get()));
    }

}
