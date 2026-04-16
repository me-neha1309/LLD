import factory.ParkingLotFactory;
import factory.VehicleFactory;
import model.ParkingLot;
import model.ParkingTicket;
import model.SpotType;
import model.Vehicle;
import model.VehicleType;
import service.MockPaymentService;
import service.ParkingLotRegistry;
import service.ParkingLotService;
import strategy.HourlyPricingStrategy;
import strategy.PricingStrategy;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Main {
    public static void main(String[] args) {
        List<String> gates = List.of("G1", "G2");

        Map<SpotType, Integer> spotsPerFloor = new EnumMap<>(SpotType.class);
        spotsPerFloor.put(SpotType.BIKE, 1);
        spotsPerFloor.put(SpotType.CAR, 2);
        spotsPerFloor.put(SpotType.TRUCK, 1);

        ParkingLotFactory parkingLotFactory = new ParkingLotFactory();
        ParkingLot lot = parkingLotFactory.create("LOT-1", gates, 2, spotsPerFloor);

        Map<VehicleType, Long> rates = new EnumMap<>(VehicleType.class);
        rates.put(VehicleType.BIKE, 10L);
        rates.put(VehicleType.CAR, 20L);
        rates.put(VehicleType.TRUCK, 50L);
        PricingStrategy pricing = new HourlyPricingStrategy(rates, 100L, 24);

        // Make ticket T-3 fail payment once (retry should succeed)
        MockPaymentService paymentService = new MockPaymentService(Set.of("T-3"));

        ParkingLotService lotService = new ParkingLotService(lot, pricing, paymentService);
        ParkingLotRegistry registry = new ParkingLotRegistry();
        registry.register(lotService);

        VehicleFactory vehicleFactory = new VehicleFactory();

        System.out.println("=== Initial ===");
        System.out.println(lotService.formatAvailability());

        // BIKE -> BIKE spot
        ParkingTicket t1 = lotService.parkVehicle("G1", vehicleFactory.create("BIKE-111", VehicleType.BIKE));
        System.out.println("Parked: " + t1);

        // Fill BIKE spots, then next BIKE should take CAR spot (rule: Bike -> Bike/Car)
        ParkingTicket t2 = lotService.parkVehicle("G1", vehicleFactory.create("BIKE-222", VehicleType.BIKE));
        System.out.println("Parked: " + t2);

        ParkingTicket t3 = lotService.parkVehicle("G2", vehicleFactory.create("BIKE-333", VehicleType.BIKE));
        System.out.println("Parked (bike overflow to car spot): " + t3);
        System.out.println("Allocated spot type for BIKE-333: " + t3.getAllocatedSpot().getSpotType());

        // CAR -> CAR spot only
        ParkingTicket t4 = lotService.parkVehicle("G1", vehicleFactory.create("CAR-444", VehicleType.CAR));
        System.out.println("Parked: " + t4);

        // TRUCK -> TRUCK spot only
        ParkingTicket t5 = lotService.parkVehicle("G2", vehicleFactory.create("TRUCK-555", VehicleType.TRUCK));
        System.out.println("Parked: " + t5);

        System.out.println("\n=== After few parks ===");
        System.out.println(lotService.formatAvailability());

        // Repeated entry edge case
        System.out.println("=== Repeated entry attempt ===");
        try {
            Vehicle repeated = new Vehicle("CAR-444", VehicleType.CAR);
            lotService.parkVehicle("G1", repeated);
        } catch (Exception e) {
            System.out.println("Expected rejection: " + e.getMessage());
        }

        // Parking full edge case (try to overfill trucks: only 2 truck spots total in this setup)
        System.out.println("\n=== Parking full attempt (TRUCK) ===");
        try {
            lotService.parkVehicle("G1", vehicleFactory.create("TRUCK-666", VehicleType.TRUCK));
            lotService.parkVehicle("G1", vehicleFactory.create("TRUCK-777", VehicleType.TRUCK)); // should fail
        } catch (Exception e) {
            System.out.println("Expected rejection: " + e.getMessage());
        }

        System.out.println("\n=== Availability ===");
        System.out.println(lotService.formatAvailability());

        // Lost ticket flow
        System.out.println("=== Lost ticket flow (mark LOST, then pay+exit) ===");
        lotService.markTicketLost(t1.getTicketId());
        ParkingLotService.PaymentReceipt lostReceipt = lotService.unpark(t1.getTicketId());
        System.out.println(lostReceipt);

        // Payment failure flow: T-3 fails once, should not free spot, then retry succeeds
        System.out.println("\n=== Payment failure + retry (ticket " + t3.getTicketId() + ") ===");
        ParkingLotService.PaymentReceipt r1 = lotService.unpark(t3.getTicketId());
        System.out.println("Attempt 1: " + r1);
        System.out.println("Availability (spot should still be occupied):");
        System.out.println(lotService.formatAvailability());

        ParkingLotService.PaymentReceipt r2 = lotService.unpark(t3.getTicketId());
        System.out.println("Attempt 2: " + r2);

        // Normal exit
        System.out.println("\n=== Normal exit (ticket " + t4.getTicketId() + ") ===");
        System.out.println(lotService.unpark(t4.getTicketId()));

        System.out.println("\n=== Final availability ===");
        System.out.println(lotService.formatAvailability());

        // Example multi-lot scaling: create another lot service and register
        ParkingLot lot2 = parkingLotFactory.create("LOT-2", gates, 1, spotsPerFloor);
        ParkingLotService lotService2 = new ParkingLotService(lot2, pricing, new MockPaymentService(Set.of()));
        registry.register(lotService2);
        System.out.println("Registered multiple lots: LOT-1 and LOT-2");
    }
}