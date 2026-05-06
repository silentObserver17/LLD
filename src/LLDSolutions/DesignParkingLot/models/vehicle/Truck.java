package LLDSolutions.DesignParkingLot.models.vehicle;

import LLDSolutions.DesignParkingLot.enums.VehicleType;

public class Truck extends Vehicle {
    public Truck(String licensePlate) {
        super(licensePlate, VehicleType.TRUCK);
    }
}
