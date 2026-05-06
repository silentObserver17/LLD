package LLDSolutions.DesignParkingLot.models.vehicle;

import LLDSolutions.DesignParkingLot.enums.VehicleType;

public class Car extends Vehicle {
    public Car(String licensePlate){
        super(licensePlate, VehicleType.CAR);
    }
}
