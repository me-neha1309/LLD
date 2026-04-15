/*
* Inventory entity tracking daily room availability
* THREAD-SAFE: Uses ReentrantLock for concurrency control
 */
package model;
import java.time.LocalDate;
import java.util.concurrent.locks.ReentrantLock;

public class Inventory {
    private final Long hotelId;
    private final String roomType;
    private final LocalDate date;
    private final int total;

    //THREAD-SAFE: Modified under lock
    private int booked;
    private int overbooked;

    //THREAD-SAFE: Reentrant Lock for exclusive access
    public final ReentrantLock lock = new ReentrantLock();

    public Inventory(Long hotelId, String roomType, LocalDate date, int total){
        this.hotelId = hotelId;
        this.roomType = roomType;
        this.date = date;
        this.total = total;
        this.booked = 0;
        this.overbooked = 0;
    }

    public String key(){
        return hotelId + "-" + roomType + "-" + date;
    }

    private void ensureLocked(){
        if(!lock.isHeldByCurrentThread()) {
            throw new IllegalStateException("Inventory.lock must be held by the current thread");
        }
    }

    public boolean canBook(int requested){
        ensureLocked();
        int maxAllowed = (int)(total * 1.10);
        return (booked + overbooked + requested) <= maxAllowed;
    }

    public void book(int requested) {
        ensureLocked();
        int current = booked + overbooked;
        if(current + requested <= total){
            booked += requested;
        } else {
            int regular = Math.max(0, total - current);
            booked += regular;
            overbooked += (requested - regular);
        }
    }

    public void release(int count, boolean wasOverbooked) {
        ensureLocked();
        if(wasOverbooked){
            overbooked = Math.max(0, overbooked-count);
        } else {
            booked = Math.max(0, booked - count);
        }
    }

    public int occupancy(){
        ensureLocked();
        return total==0 ? 0 : ((booked + overbooked)*100)/total;
    }

    public boolean isOverbooked(){
        ensureLocked();
        return (booked + overbooked) > total;
    }

    public int getBooked(){
        return booked;
    }

    public int getOverbooked() {
        return overbooked;
    }

    public int getTotal() {
        return total;
    }

    public LocalDate getDate() {
        return date;
    }

    public Long getHotelId() {
        return hotelId;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setBooked(int booked) {
        this.booked = booked;
    }

    public void setOverbooked(int overbooked) {
        this.overbooked = overbooked;
    }
}