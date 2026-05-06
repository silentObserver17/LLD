package LLDSolutions.DesignParkingLot.Exceptions;

import LLDSolutions.DesignParkingLot.enums.VehicleType;

public class ParkingLotFullException extends  RuntimeException{
    public ParkingLotFullException(VehicleType vehicleType) {
        super("No available spot for vehicle type: " + vehicleType);
    }
}
