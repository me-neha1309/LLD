package service;

import model.*;
import strategy.PricingStrategy;
import strategy.*;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public final class ParkingLotService {
    public static final class PaymentReceipt {
        private final String ticketId;
        private final long amountCharged;
        private final boolean success;
        private final String message;

        public PaymentReceipt(String ticketId, long amountCharged, boolean success, String message){
            this.ticketId = ticketId;
            this.amountCharged = amountCharged;
            this.success = success;
            this.message = message;
        }

        public String getTicketId() {
            return ticketId;
        }

        public boolean isSuccess() {
            return success;
        }

        public long getAmountCharged() {
            return amountCharged;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString(){
            return "PaymentReceipt{ticketId=" + ticketId + ", amountCharged = " + amountCharged + ", success = " + success + ", message = " + message + "}";
        }
    }

    private final ParkingLot lot;
    private final PricingStrategy pricingstrategy;
    private final PaymentService paymentService;

    private final ReentrantLock lock = new ReentrantLock(true);
    private final AtomicLong ticketSeq = new AtomicLong(0);

    //Active tickets only (PAID tickets are removed)
    private final Map<String, ParkingTicket> activeTicketsById = new HashMap<>();
    private final Map<String, String> activeTicketIdByVehicleLicense = new HashMap<>();

    //Availability structures (all muted under lock)
    private final Map<String, Map<SpotType, NavigableSet<ParkingSpot>>> availableByGateAndSpotType = new HashMap<>();
    private final Map<Integer, EnumMap<SpotType, Integer>> availableCountByFloorType = new TreeMap<>();

    public ParkingLotService(ParkingLot lot, PricingStrategy pricingStrategy, PaymentService paymentService){
        this.lot = Objects.requireNonNull(lot, "lot");
        this.pricingstrategy = Objects.requireNonNull(pricingStrategy, "pricingStrategy");
        this.paymentService = Objects.requireNonNull(paymentService, "paymentService");
        initAvailabilityIndex();
    }

    public ParkingLot getLot(){
        return lot;
    }

    public ParkingTicket parkVehicle(String gateId, Vehicle vehicle){
        Objects.requireNonNull(vehicle, "vehicle");
        String normalizedGate = normalizeGateId(gateId);

        lock.lock();
        try {
            if(activeTicketIdByVehicleLicense.containsKey(vehicle.getLicenseNumber())){
                throw new IllegalStateException("Repeated entry not allowed for vehicle: " + vehicle.getLicenseNumber());
            }
            List<SpotType> candidates = candidateSpotTypes(vehicle.getType());
            ParkingSpot chosen = null;
            SpotType chosenSpotType = null;

            for(SpotType st : candidates){
                NavigableSet<ParkingSpot> set = availableByGateAndSpotType.get(normalizedGate).get(st);
                if(set!=null && !set.isEmpty()){
                    chosen = set.first();
                    chosenSpotType = st;
                    break;
                }
            }

            if(chosen==null){
                throw new IllegalStateException("Parking full for vehicle type : " + vehicle.getType());
            }

            //allocate
            chosen.assignTo(vehicle);
            removeFromAllGateIndexes(chosenSpotType, chosen);
            decrementAvailabilityCount(chosen.getFloorNumber(), chosenSpotType);

            String ticketId = "T-" + ticketSeq.incrementAndGet();
            ParkingTicket ticket = new ParkingTicket(ticketId, vehicle, chosen, Instant.now());
            activeTicketsById.put(ticketId, ticket);
            activeTicketIdByVehicleLicense.put(vehicle.getLicenseNumber(), ticketId);
            return ticket;
        } finally {
            lock.unlock();
        }
    }

    public void markTicketLost(String ticketId){
        Objects.requireNonNull(ticketId, "ticketId");
        lock.lock();
        try {
            ParkingTicket ticket = getActiveTicketOrThrow(ticketId);
            ticket.markLost();
        } finally {
            lock.unlock();
        }
    }

    public PaymentReceipt unpark(String ticketId){
        Objects.requireNonNull(ticketId, "ticketId");
        lock.lock();
        try {
            ParkingTicket ticket = getActiveTicketOrThrow(ticketId);
            boolean lost = ticket.getStatus()==TicketStatus.LOST;
            Duration duration = Duration.between(ticket.getEntryTime(), Instant.now());
            long fee = pricingstrategy.calculateFee(ticket.getVehicle().getType(), duration, lost);

            ticket.incrementPaymentAttempts();
            boolean paid = paymentService.pay(ticket.getTicketId(), fee);
            if(!paid){
                return new PaymentReceipt(ticket.getTicketId(), fee, false, "Payment Failed. Spot not freed. Retry allowed");
            }

            //Mark paid, free spot, and close ticket
            ticket.markPaid(Instant.now(), fee);
            freeSpotAndCloseTicket(ticket);
            return new PaymentReceipt(ticket.getTicketId(), fee, true, "Payment Successful. Exit Completed");
        } finally {
            lock.unlock();
        }
    }

    public String formatAvailability() {
        lock.lock();
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("Availability for lot ").append(lot.getLotId()).append("\n");
            for (Map.Entry<Integer, EnumMap<SpotType, Integer>> e : availableCountByFloorType.entrySet()) {
                int floor = e.getKey();
                EnumMap<SpotType, Integer> m = e.getValue();
                sb.append("  Floor ").append(floor).append(": ");
                sb.append("BIKE=").append(m.getOrDefault(SpotType.BIKE, 0)).append(", ");
                sb.append("CAR=").append(m.getOrDefault(SpotType.CAR, 0)).append(", ");
                sb.append("TRUCK=").append(m.getOrDefault(SpotType.TRUCK, 0));
                sb.append("\n");
            }

            EnumMap<SpotType, Integer> totals = new EnumMap<>(SpotType.class);
            for (SpotType t : SpotType.values()) totals.put(t, 0);
            for (EnumMap<SpotType, Integer> m : availableCountByFloorType.values()) {
                for (SpotType t : SpotType.values()) {
                    totals.put(t, totals.get(t) + m.getOrDefault(t, 0));
                }
            }
            sb.append("  Total: ");
            sb.append("BIKE=").append(totals.get(SpotType.BIKE)).append(", ");
            sb.append("CAR=").append(totals.get(SpotType.CAR)).append(", ");
            sb.append("TRUCK=").append(totals.get(SpotType.TRUCK)).append("\n");

            return sb.toString();
        } finally {
            lock.unlock();
        }
    }

    private void initAvailabilityIndex() {
        // init sets for each gate + spot type with gate-aware comparator
        for (String gateId : lot.getGateIds()) {
            Map<SpotType, NavigableSet<ParkingSpot>> byType = new EnumMap<>(SpotType.class);
            for (SpotType st : SpotType.values()) {
                byType.put(st, new TreeSet<>(spotComparatorForGate(gateId)));
            }
            availableByGateAndSpotType.put(gateId, byType);
        }

        // load all spots as available
        for (ParkingFloor floor : lot.getFloorsByNumber().values()) {
            EnumMap<SpotType, Integer> counts = new EnumMap<>(SpotType.class);
            for (SpotType st : SpotType.values()) counts.put(st, 0);

            for (ParkingSpot spot : floor.getSpotsById().values()) {
                SpotType st = spot.getSpotType();
                counts.put(st, counts.get(st) + 1);
                for (String gateId : lot.getGateIds()) {
                    availableByGateAndSpotType.get(gateId).get(st).add(spot);
                }
            }
            availableCountByFloorType.put(floor.getFloorNumber(), counts);
        }
    }

    private Comparator<ParkingSpot> spotComparatorForGate(String gateId) {
        return (a, b) -> {
            int c1 = Integer.compare(a.getFloorNumber(), b.getFloorNumber());
            if (c1 != 0) return c1;

            int c2 = Integer.compare(a.distanceToGate(gateId), b.distanceToGate(gateId));
            if (c2 != 0) return c2;

            int c3 = Integer.compare(a.getSpotNumber(), b.getSpotNumber());
            if (c3 != 0) return c3;

            return a.getSpotId().compareTo(b.getSpotId());
        };
    }

    private String normalizeGateId(String gateId) {
        if (gateId == null) return lot.getGateIds().get(0);
        String g = gateId.trim();
        if (availableByGateAndSpotType.containsKey(g)) return g;
        return lot.getGateIds().get(0);
    }

    private List<SpotType> candidateSpotTypes(VehicleType vehicleType) {
        List<SpotType> list = new ArrayList<>();
        if (vehicleType == VehicleType.BIKE) {
            list.add(SpotType.BIKE);
            list.add(SpotType.CAR);
        } else if (vehicleType == VehicleType.CAR) {
            list.add(SpotType.CAR);
        } else if (vehicleType == VehicleType.TRUCK) {
            list.add(SpotType.TRUCK);
        } else {
            throw new IllegalArgumentException("Unknown vehicleType: " + vehicleType);
        }
        return list;
    }

    private void removeFromAllGateIndexes(SpotType spotType, ParkingSpot spot) {
        for (String gateId : lot.getGateIds()) {
            availableByGateAndSpotType.get(gateId).get(spotType).remove(spot);
        }
    }

    private void addToAllGateIndexes(SpotType spotType, ParkingSpot spot) {
        for (String gateId : lot.getGateIds()) {
            availableByGateAndSpotType.get(gateId).get(spotType).add(spot);
        }
    }

    private void decrementAvailabilityCount(int floorNumber, SpotType spotType) {
        EnumMap<SpotType, Integer> m = availableCountByFloorType.get(floorNumber);
        m.put(spotType, m.getOrDefault(spotType, 0) - 1);
    }

    private void incrementAvailabilityCount(int floorNumber, SpotType spotType) {
        EnumMap<SpotType, Integer> m = availableCountByFloorType.get(floorNumber);
        m.put(spotType, m.getOrDefault(spotType, 0) + 1);
    }

    private ParkingTicket getActiveTicketOrThrow(String ticketId) {
        ParkingTicket t = activeTicketsById.get(ticketId);
        if (t == null) {
            throw new IllegalArgumentException("Ticket not found or already closed: " + ticketId);
        }
        return t;
    }

    private void freeSpotAndCloseTicket(ParkingTicket ticket) {
        ParkingSpot spot = ticket.getAllocatedSpot();
        SpotType spotType = spot.getSpotType();

        spot.free();
        addToAllGateIndexes(spotType, spot);
        incrementAvailabilityCount(spot.getFloorNumber(), spotType);

        activeTicketsById.remove(ticket.getTicketId());
        activeTicketIdByVehicleLicense.remove(ticket.getVehicle().getLicenseNumber());
    }
}