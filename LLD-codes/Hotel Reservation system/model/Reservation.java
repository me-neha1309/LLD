/*
Reservation entity representing a booking
Use volatile fields and synchronised updates
 */
package model;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class Reservation {
    private static  final AtomicLong idGenerator = new AtomicLong(1);

    private final Long id;
    private final String number;
    private final Long customerId;
    private final Long hotelId;
    private final String roomType;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final int rooms;
    private final List<Long> addOnServiceIds;
    private final double baseAmount;
    private final double serviceCharges;
    private final double total;

    public volatile String status; //PENDING, PAID, CANCELED
    public volatile boolean overbooked;

    private final Object statusLock = new Object();

    public Reservation(Long customerId, Long hotelId, String roomType, LocalDate checkIn,
                       LocalDate checkOut, int rooms, List<Long> addOnServiceIds,
                       double baseAmount, double serviceCharges, double total) {
        this.id = idGenerator.getAndIncrement();
        this.number = "RES-" + id;
        this.customerId = customerId;
        this.hotelId = hotelId;
        this.roomType = roomType;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.rooms = rooms;
        this.addOnServiceIds = (addOnServiceIds==null) ? List.of() : List.copyOf(addOnServiceIds);
        this.baseAmount = baseAmount;
        this.serviceCharges = serviceCharges;
        this.total = total;
        this.status = "PENDING";
        this.overbooked = false;
    }

    public int nights() {
        return (int)(checkOut.toEpochDay() - checkIn.toEpochDay());
    }

    public void updateStatus(String newStatus){
        synchronized (statusLock){
            this.status = newStatus;
        }
    }

    public void setOverbooked(boolean overbooked) {
        synchronized (statusLock){
            this.overbooked = overbooked;
        }
    }

    public Long getId() {
        return id;
    }

    public String getRoomType() {
        return roomType;
    }

    public Long getHotelId() {
        return hotelId;
    }

    public static AtomicLong getIdGenerator() {
        return idGenerator;
    }

    public boolean isOverbooked() {
        return overbooked;
    }

    public double getTotal() {
        return total;
    }

    public int getRooms() {
        return rooms;
    }

    public LocalDate getCheckIn() {
        return checkIn;
    }

    public LocalDate getCheckOut() {
        return checkOut;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public Object getStatusLock() {
        return statusLock;
    }

    public String getNumber() {
        return number;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        return super.toString();
    }

    public double getBaseAmount() {
        return baseAmount;
    }

    public double getServiceCharges() {
        return serviceCharges;
    }
}