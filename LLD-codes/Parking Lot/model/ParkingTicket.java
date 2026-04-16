package model;

import java.time.Instant;
import java.util.Objects;

public final class ParkingTicket {
    private final String ticketId;
    private final Vehicle vehicle;
    private final ParkingSpot allocatedSpot;
    private final Instant entryTime;

    private TicketStatus status;
    private Instant paidAt;
    private long amountPaid;
    private int paymentAttempts;

    public ParkingTicket(String ticketId, Vehicle vehicle, ParkingSpot allocatedSpot, Instant entryTime){
        if(ticketId==null || ticketId.isBlank()){
            throw new IllegalArgumentException("Ticket Id cannot be blank");
        }
        this.ticketId = ticketId.trim();
        this.vehicle = Objects.requireNonNull(vehicle, "vehicle");
        this.allocatedSpot = Objects.requireNonNull(allocatedSpot, "allocatedSpot");
        this.entryTime = Objects.requireNonNull(entryTime, "entryTime");
        this.status = TicketStatus.ACTIVE;
        this.paidAt = null;
        this.amountPaid = 0;
        this.paymentAttempts = 0;
    }

    public String getTicketId() {
        return ticketId;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public ParkingSpot getAllocatedSpot() {
        return allocatedSpot;
    }

    public Instant getEntryTime() {
        return entryTime;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public long getAmountPaid() {
        return amountPaid;
    }

    public int getPaymentAttempts() {
        return paymentAttempts;
    }

    public void markLost() {
        if(status == TicketStatus.PAID) {
            throw new IllegalArgumentException("Cannot mark lost after payment");
        }
        status = TicketStatus.LOST;
    }

    public void incrementPaymentAttempts() {
        paymentAttempts++;
    }

    public void markPaid(Instant paidAt, long amountPaid) {
        if(amountPaid < 0){
            throw new IllegalArgumentException("Amount paid cannot be negative");
        }
        this.paidAt = Objects.requireNonNull(paidAt, "paidAt");
        this.amountPaid = amountPaid;
        this.status = TicketStatus.PAID;
    }

    @Override
    public String toString() {
        return "ParkingTicket{ticketId = " + ticketId + ", vehicle = " + vehicle + " spot = " + allocatedSpot + " entryTime=" + entryTime + ", status = " + status+"}";
    }

}