package service;
import model.*;
import repository.DataStore;

import javax.xml.crypto.Data;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/*
 * Service for handling reservations
 * THREAD-SAFE: Uses locks for critical sections
 */
public class BookingService {
    /*
     * Create a new reservation with full concurrency control
     * THREAD SAFE: Uses locks for critical sections
     */

    public static Reservation createReservation(Long customerId, Long hotelId, String roomType,
                                                LocalDate checkIn, LocalDate checkOut, int roomsNeeded,
                                                List<Long> addOnServiceIds){
        //Lock all inventory for the date range (atomic across dates)
        List<Inventory> invList = new ArrayList<>();
        try {
            LocalDate current = checkIn;
            while(current.isBefore(checkOut)){
                Inventory inv = DataStore.getInventory(hotelId, roomType, current);
                if(inv==null){
                    throw new RuntimeException("Inventory not found for date: " + current);
                }
                inv.lock.lock();
                invList.add(inv);
                current = current.plusDays(1);
            }

            for(Inventory inv : invList){
                if(!inv.canBook(roomsNeeded)){
                    throw new RuntimeException("No availability for requested dates");
                }
            }

            Room room = DataStore.findRoom(hotelId, roomType);
            if(room==null){
                throw new RuntimeException("Room not found");
            }

            Inventory firstInv = invList.get(0);
            int nights = (int)(checkOut.toEpochDay() - checkIn.toEpochDay());
            double baseAmount = PricingService.calculatePrice(room, firstInv, nights, roomsNeeded);

            double serviceCharges = calculateServiceCharges(addOnServiceIds, nights, roomsNeeded);

            double total = PricingService.calculateTotal(baseAmount, serviceCharges, 0.0);

            for(Inventory inv : invList){
                inv.book(roomsNeeded);
            }

            Reservation res = new Reservation(customerId, hotelId, roomType, checkIn,
                                              checkOut, roomsNeeded, addOnServiceIds,
                                              baseAmount, serviceCharges, total);

            res.setOverbooked(firstInv.isOverbooked());

            DataStore.saveReservation(res);
            return res;
        } finally {
            for(Inventory inv : invList){
                inv.lock.unlock();
            }
        }
    }

    public static double calculateServiceCharges(List<Long> addOnServiceIds, int nights, int rooms){
        if(addOnServiceIds==null || addOnServiceIds.isEmpty()) return 0.0;

        double total = 0.0;
        for(Long id : addOnServiceIds){
            AddOnService svc = DataStore.getService(id);
            if(svc==null){
                throw new RuntimeException("Unknown add-on service id : " + id);
            }

            switch(svc.getChargeType()) {
                case PER_STAY -> total += svc.getPrice();
                case PER_NIGHT_PER_ROOM -> total += svc.getPrice() * nights * rooms;
            }
        }

        return total;
    }

    /*
     * Cancel a reservation and release inventory
     * THREAD-SAFE: Locks inventory during release
     */
    public static void cancelReservation(Long reservationId){
        Reservation res = DataStore.getReservation(reservationId);
        if(res==null){
            throw new RuntimeException("Reservation not found");
        }

        LocalDate current = res.getCheckIn();
        while(current.isBefore(res.getCheckOut())) {
            Inventory inv = DataStore.getInventory(res.getHotelId(), res.getRoomType(), current);
            if(inv!=null){
                inv.lock.lock();
                try {
                    inv.release(res.getRooms(), res.overbooked);
                } finally {
                    inv.lock.unlock();
                }
            }
            current = current.plusDays(1);
        }
        res.updateStatus("CANCELLED");
    }

    public static void processPayment(Long reservationId){
        Reservation res = DataStore.getReservation(reservationId);
        if(res==null){
            throw new RuntimeException("Reservation not found");
        }
        res.updateStatus("PAID");
    }

    public static List<Hotel> searchHotels(String city, LocalDate checkIn, LocalDate checkOut, String roomType, int roomsNeeded){
        List<Hotel> results = new ArrayList<>();
        for(Hotel hotel : DataStore.getAllHotels()){
            if(hotel.getCity().equalsIgnoreCase(city)){
                if(checkAvailability(hotel.getId(), roomType, checkIn, checkOut, roomsNeeded)){
                    results.add(hotel);
                }
            }
        }
        return results;
    }

    private static boolean checkAvailability(Long hotelId, String roomType, LocalDate start,
                                             LocalDate end, int roomsNeeded) {
        LocalDate current = start;
        List<Inventory> lockedInventory = new ArrayList<>();
        try{
            while(current.isBefore(end)){
                Inventory inv = DataStore.getInventory(hotelId, roomType, current);
                if(inv==null) return false;

                inv.lock.lock();
                lockedInventory.add(inv);
                current = current.plusDays(1);
            }

            for(Inventory inv : lockedInventory){
                if(!inv.canBook(roomsNeeded)){
                    return false;
                }
            }

            return true;
        } finally {
            for(Inventory inv : lockedInventory){
                inv.lock.unlock();
            }
        }
    }
}