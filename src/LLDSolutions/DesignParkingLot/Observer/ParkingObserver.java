package LLDSolutions.DesignParkingLot.Observer;

import LLDSolutions.DesignParkingLot.models.ParkingSpot;

public interface ParkingObserver {
    void onSpotStatusChanged(ParkingSpot spot);
}