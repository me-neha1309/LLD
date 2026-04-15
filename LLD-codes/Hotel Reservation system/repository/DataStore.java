/*
 * In-memory data store (simulates database)
 * THREAD-SAFE: Uses ConcurrentHashMap
 */
package repository;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import model.*;
public class DataStore {
    private static final Map<Long, Hotel> hotels = new ConcurrentHashMap<>();
    private static final Map<Long, Room> rooms = new ConcurrentHashMap<>();
    private static final Map<String, Inventory> inventory = new ConcurrentHashMap<>();
    private static final Map<Long, Customer> customers = new ConcurrentHashMap<>();
    private static final Map<Long, Reservation> reservations = new ConcurrentHashMap<>();
    private static final Map<Long, AddOnService> services = new ConcurrentHashMap<>();

    //Hotel operations
    public static void saveHotel(Hotel hotel){
        hotels.put(hotel.getId(), hotel);
    }

    public static Hotel getHotel(Long id){
        return hotels.get(id);
    }

    public static Collection<Hotel> getAllHotels(){
        return hotels.values();
    }

    //Room operations
    public static void saveRoom(Room room){
        rooms.put(room.getHotelId(), room);
    }

    public static Room findRoom(Long hotelId, String roomType){
        return rooms.values().stream()
                .filter(r -> r.getHotelId().equals(hotelId) && r.getType().equals(roomType))
                .findFirst()
                .orElse(null);
    }

    //Inventory Operations
    public static void saveInventory(Inventory inv){
        inventory.put(inv.key(), inv);
    }

    public static Inventory getInventory(Long hotelId, String roomType, LocalDate date){
        String key = hotelId + "-" + roomType + "-" + date;
        return inventory.get(key);
    }

    //Customer Operations
    public static void saveCustomer(Customer customer){
        customers.put(customer.getId(), customer);
    }

    public static Customer getCustomer(Long id){
        return customers.get(id);
    }

    //Reservation operations
    public static void saveReservation(Reservation reservation){
        reservations.put(reservation.getId(), reservation);
    }

    public static Reservation getReservation(Long id){
        return reservations.get(id);
    }

    public static Collection<Reservation> getAllReservations() {
        return reservations.values();
    }

    //Add-ons
    public static void saveService(AddOnService addOnService){
        services.put(addOnService.getId(), addOnService);
    }

    public static AddOnService getService(Long id){
        return services.get(id);
    }

    public static Collection<AddOnService> getAllServices() {
        return services.values();
    }


}