package LLDSolutions.DesignParkingLot;

import LLDSolutions.DesignParkingLot.Exceptions.InvalidTicketException;
import LLDSolutions.DesignParkingLot.Exceptions.ParkingLotFullException;
import LLDSolutions.DesignParkingLot.Observer.DisplayBoard;
import LLDSolutions.DesignParkingLot.Strategy.NearestFirstStrategy;
import LLDSolutions.DesignParkingLot.enums.SpotType;
import LLDSolutions.DesignParkingLot.enums.VehicleType;
import LLDSolutions.DesignParkingLot.factory.VehicleFactory;
import LLDSolutions.DesignParkingLot.fee.FeeCalculator;
import LLDSolutions.DesignParkingLot.fee.HourlyFeeCalculator;
import LLDSolutions.DesignParkingLot.fee.PeakHourDecorator;
import LLDSolutions.DesignParkingLot.fee.WeekendDecorator;
import LLDSolutions.DesignParkingLot.models.Level;
import LLDSolutions.DesignParkingLot.models.ParkingSpot;
import LLDSolutions.DesignParkingLot.models.Receipt;
import LLDSolutions.DesignParkingLot.models.Ticket;
import LLDSolutions.DesignParkingLot.models.vehicle.Vehicle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Main {
   // -- Helper ------------------------------------------------------------
   public static Level buildLevel(
           int levelNum,
           int motorcycleCount,
           int carCount,
           int truckCount) {
       List<ParkingSpot> spots =  new ArrayList<>();
       String prefix = "L" + levelNum;

       for(int i = 0; i < motorcycleCount; i++){
           spots.add(new ParkingSpot(prefix + "-M" + (i+1), SpotType.MOTORCYCLE));
       }

       for (int i = 0; i < carCount; i++)
           spots.add(new ParkingSpot(prefix + "-C" + (i+1), SpotType.CAR));

       for (int i = 0; i < truckCount; i++)
           spots.add(new ParkingSpot(prefix + "-T" + (i+1), SpotType.TRUCK));

       return new Level(levelNum, spots);
   }

    public static void main(String[] args) throws InterruptedException {
        // ── Build Levels ──────────────────────────────────────
        Level level1 = buildLevel(1, 2, 3, 1); // 2 motorcycle, 3 car, 1 truck
        Level level2 = buildLevel(2, 2, 3, 1);

        List<Level> levels = List.of(level1, level2);

        // ── Build Display Boards ──────────────────────────────
        DisplayBoard board1 = new DisplayBoard(1, level1);
        DisplayBoard board2 = new DisplayBoard(2, level2);

        // Wire display boards as observers to each spot
        level1.getParkingSpots().forEach(s -> s.addObserver(board1));
        level2.getParkingSpots().forEach(s -> s.addObserver(board2));

        // ── Fee Calculator — Decorator chain ──────────────────
        FeeCalculator feeCalculator = new WeekendDecorator(
                new PeakHourDecorator(
                        new HourlyFeeCalculator()));

        // ── Initialize ParkingLot ─────────────────────────────
        ParkingLot lot = ParkingLot.getInstance();
        lot.initialize(
                levels,
                List.of(board1, board2),
                new NearestFirstStrategy(),
                feeCalculator
        );

        // ── Scenario 1: Basic park and unpark ─────────────────
        System.out.println("\n═══ Scenario 1: Basic Operations ═══");
        lot.displayAllBoards();

        Vehicle car1  = VehicleFactory.createVehicle("GJ01AB1234", VehicleType.CAR);
        Vehicle car2  = VehicleFactory.createVehicle("GJ01CD5678", VehicleType.CAR);
        Vehicle bike1 = VehicleFactory.createVehicle("GJ02EF9012", VehicleType.MOTORCYCLE);
        Vehicle truck = VehicleFactory.createVehicle("GJ03GH3456", VehicleType.TRUCK);

        Ticket t1 = lot.parkVehicle(car1);
        Ticket t2 = lot.parkVehicle(car2);
        Ticket t3 = lot.parkVehicle(bike1);
        Ticket t4 = lot.parkVehicle(truck);

        System.out.println("\nAfter parking 4 vehicles:");
        lot.displayAllBoards();

        Receipt r1 = lot.unparkVehicle(t1);
        System.out.println(r1);

        System.out.println("\nAfter unparking car1:");
        lot.displayAllBoards();

        // ── Scenario 2: Lot full for a type ───────────────────
        System.out.println("\n═══ Scenario 2: Lot Full ═══");
        // Fill all truck spots
        Ticket truckT2 = lot.parkVehicle(
                VehicleFactory.createVehicle("GJ04IJ7890", VehicleType.TRUCK));

        try {
            // No more truck spots
            lot.parkVehicle(VehicleFactory.createVehicle("GJ05KL1234", VehicleType.TRUCK));
        } catch (ParkingLotFullException e) {
            System.out.println("Expected: " + e.getMessage());
        }

        // ── Scenario 3: Invalid ticket ─────────────────────────
        System.out.println("\n═══ Scenario 3: Invalid Ticket ═══");
        Receipt r4 = lot.unparkVehicle(t4);
        System.out.println(r4);
        try {
            lot.unparkVehicle(t4); // already closed
        } catch (InvalidTicketException e) {
            System.out.println("Expected: " + e.getMessage());
        }

        // ── Scenario 4: Concurrent access ─────────────────────
        System.out.println("\n═══ Scenario 4: Concurrent Access ═══");
        int threadCount = 6;
        Thread[] threads = new Thread[threadCount];
        List<Ticket> tickets = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            final int id = i;
            threads[i] = new Thread(() -> {
                try {
                    Vehicle car = VehicleFactory.createVehicle(
                            "THREAD-" + id, VehicleType.CAR);
                    Ticket ticket = lot.parkVehicle(car);
                    tickets.add(ticket);
                    System.out.println("Thread " + id
                            + " parked at " + ticket.getSpot().getSpotId());
                } catch (ParkingLotFullException e) {
                    System.out.println("Thread " + id + ": " + e.getMessage());
                }
            });
        }

        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        System.out.println("\nFinal available CAR spots: "
                + lot.getAvailableSpots(VehicleType.CAR));
    }
}