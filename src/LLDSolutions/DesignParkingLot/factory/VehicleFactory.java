package LLDSolutions.DesignParkingLot.factory;

import LLDSolutions.DesignParkingLot.enums.VehicleType;
import LLDSolutions.DesignParkingLot.models.vehicle.Car;
import LLDSolutions.DesignParkingLot.models.vehicle.MotorCycle;
import LLDSolutions.DesignParkingLot.models.vehicle.Truck;
import LLDSolutions.DesignParkingLot.models.vehicle.Vehicle;

public class VehicleFactory {
    public static Vehicle createVehicle(String licensePlate, VehicleType vehicleType) {
        return switch (vehicleType) {
            case CAR -> new Car(licensePlate);
            case TRUCK -> new Truck(licensePlate);
            case MOTORCYCLE ->  new MotorCycle(licensePlate);
        };
    }
}

