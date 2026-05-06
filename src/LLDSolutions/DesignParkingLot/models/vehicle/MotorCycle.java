package LLDSolutions.DesignParkingLot.models.vehicle;

import LLDSolutions.DesignParkingLot.enums.VehicleType;

public class MotorCycle extends Vehicle{
    public MotorCycle(String licensePlate){
        super(licensePlate, VehicleType.MOTORCYCLE);
    }
}
