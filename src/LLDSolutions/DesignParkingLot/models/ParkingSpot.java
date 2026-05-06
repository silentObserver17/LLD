package LLDSolutions.DesignParkingLot.models;

import LLDSolutions.DesignParkingLot.Observer.ParkingObserver;
import LLDSolutions.DesignParkingLot.enums.SpotStatus;
import LLDSolutions.DesignParkingLot.enums.SpotType;
import LLDSolutions.DesignParkingLot.enums.VehicleType;
import LLDSolutions.DesignParkingLot.models.vehicle.Vehicle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class ParkingSpot {
    private final String spotId;
    private final SpotType spotType;
    private volatile SpotStatus status;
    private Vehicle currentVehicle;

    // spot leve lock - fined grained concurrency
    private final ReentrantLock lock = new ReentrantLock();

    // observers - display boards watching this.
    private final List<ParkingObserver> observers = new ArrayList<>();

    public ParkingSpot(String spotId, SpotType spotType) {
        this.spotId = spotId;
        this.spotType = spotType;
        this.status = SpotStatus.AVAILABLE;
    }

    // observer registration ====================================
    public void addObserver(ParkingObserver observer) {
        observers.add(observer);
    }

    public void notifyObservers() {
        for (ParkingObserver observer : observers) {
            observer.onSpotStatusChanged(this);
        }
    }
    // core operations - thread safe. ===========================

    // Atomic check-and-assign
    // Returns true if successfully assigned, false if already taken
    public boolean AssignIfAvailable(Vehicle vehicle) {
        lock.lock();
        try{
            if(status !=  SpotStatus.AVAILABLE) return false;
            this.currentVehicle = vehicle;
            this.status = SpotStatus.OCCUPIED;
             notifyObservers();
            return true;
        }finally {
            lock.unlock();
        }
    }

    public void release(){
        lock.lock();
        try{
            this.currentVehicle = null;
            this.status = SpotStatus.AVAILABLE;
            notifyObservers();
        }finally {
            lock.unlock();
        }
    }

    public String getSpotId() {
        return spotId;
    }

    public SpotType getSpotType() {
        return spotType;
    }

    public SpotStatus getStatus() {
        return status;
    }

    public Vehicle getCurrentVehicle() {
        return currentVehicle;
    }

    public boolean isAvailable() {
        return status == SpotStatus.AVAILABLE;
    }

    public boolean canFit(VehicleType vehicleType) {
        return spotType.name().equals(vehicleType.name());
    }

    @Override
    public String toString() {
        return spotId + "[" + spotType + ":" + status + "]";
    }

}
