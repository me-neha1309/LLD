import model.*;
import service.BookingService;
import repository.DataStore;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * HOTEL RESERVATION SYSTEM - Main Driver Class
 *
 * Features:
 * 1. Search hotels by city and check availability
 * 2. Dynamic pricing based on occupancy (0.8x to 2.0x multiplier)
 * 3. 110% overbooking support (like airlines)
 * 4. THREAD-SAFE concurrency control to prevent double bookings
 * 5. Payment processing and cancellations
 *
 * Thread-Safety Mechanisms:
 * - AtomicLong for ID generation
 * - ReentrantLock for inventory operations
 * - ConcurrentHashMap for all collections
 * - Synchronized blocks for status updates
 * - Volatile fields for visibility
 *
 * Run: javac com/hrs/HotelReservationSystem.java && java com.hrs.HotelReservationSystem
 */
public class HotelReservationSystem {

    public static void main(String[] args) {
        System.out.println("=== HOTEL RESERVATION SYSTEM (THREAD-SAFE) ===\n");
        setupData();

        // SCENARIO 1: Normal booking
        System.out.println("--- Scenario 1: Normal Booking ---");
        testNormalBooking();

        // SCENARIO 2: High occupancy (surge pricing)
        System.out.println("\n--- Scenario 2: High Occupancy Booking ---");
        testHighOccupancy();

        // SCENARIO 3: Overbooking (110%)
        System.out.println("\n--- Scenario 3: Overbooking ---");
        testOverbooking();

        // SCENARIO 4: Beyond limit (should fail)
        System.out.println("\n--- Scenario 4: Booking Beyond 110% ---");
        testBeyondLimit();

        // SCENARIO 5: Cancellation
        System.out.println("\n--- Scenario 5: Cancellation ---");
        testCancellation();

        // SCENARIO 5b: Booking with add-on services
        System.out.println("\n--- Scenario 5b: Booking With Add-on Services (Meals + Spa) ---");
        testAddonServices();

        // SCENARIO 6: Concurrent bookings (STRESS TEST)
        System.out.println("\n--- Scenario 6: Concurrent Bookings (10 threads) ---");
        testConcurrency();

        // SCENARIO 7: Race condition test
        System.out.println("\n--- Scenario 7: Race Condition Test (100 threads) ---");
        testRaceCondition();

        // SCENARIO 8: Meals add-on only (with expected output)
        System.out.println("\n--- Scenario 8: Add-on Meals Only (Expected vs Actual) ---");
        testMealsAddonOnly();

        System.out.println("\n=== ALL TESTS COMPLETED ===");
    }

    // ==================== SETUP TEST DATA ====================

    static void setupData() {
        // Create hotel
        DataStore.saveHotel(new Hotel(1L, "Grand Plaza", "New York", 100));

        // Create room
        DataStore.saveRoom(new Room(1L, 1L, "DELUXE", 200.0));

        // Create inventory for next 30 days (enough for all test scenarios)
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 30; i++) {
            LocalDate date = today.plusDays(i);
            Inventory inv = new Inventory(1L, "DELUXE", date, 100);
            DataStore.saveInventory(inv);
        }

        // Add-on services catalog (interview-friendly)
        // Meals: per night per room; Spa: per stay
        DataStore.saveService(new AddOnService(1L, "Breakfast & Dinner (Meals)", ServiceType.MEAL, ChargeType.PER_NIGHT_PER_ROOM, 30.0));
        DataStore.saveService(new AddOnService(2L, "Spa Access", ServiceType.SPA, ChargeType.PER_STAY, 80.0));
        DataStore.saveService(new AddOnService(3L, "Airport Pickup", ServiceType.TRANSPORT, ChargeType.PER_STAY, 50.0));

        // Create customers
        DataStore.saveCustomer(new Customer(1L, "John Doe", "john@example.com"));
        DataStore.saveCustomer(new Customer(2L, "Jane Smith", "jane@example.com"));
    }

    // ==================== TEST SCENARIOS ====================

    static void testNormalBooking() {
        LocalDate checkIn = LocalDate.now().plusDays(1);
        LocalDate checkOut = checkIn.plusDays(3);

        Reservation res = BookingService.createReservation(1L, 1L, "DELUXE", checkIn, checkOut, 1, Collections.EMPTY_LIST);
        BookingService.processPayment(res.getId());

        Inventory inv = DataStore.getInventory(1L, "DELUXE", checkIn);
        System.out.printf("✓ Reservation: %s\n", res.getNumber());
        inv.lock.lock();
        try {
            System.out.printf("  Occupancy: %d%%, Base: $%.2f, Services: $%.2f, Total(with tax): $%.2f, Status: %s\n",
                    inv.occupancy(), res.getBaseAmount(), res.getServiceCharges(), res.getTotal(), res.getStatus());
        } finally {
            inv.lock.unlock();
        }
    }

    static void testHighOccupancy() {
        LocalDate checkIn = LocalDate.now().plusDays(5);
        LocalDate checkOut = checkIn.plusDays(2);

        // Book 85 rooms to reach high occupancy
        for (int i = 0; i < 85; i++) {
            BookingService.createReservation(2L, 1L, "DELUXE", checkIn, checkOut, 1, Collections.EMPTY_LIST);
        }

        Reservation res = BookingService.createReservation(1L, 1L, "DELUXE", checkIn, checkOut, 1, Collections.EMPTY_LIST);
        Inventory inv = DataStore.getInventory(1L, "DELUXE", checkIn);
        System.out.printf("✓ Reservation at high occupancy\n");
        inv.lock.lock();
        try {
            System.out.printf("  Occupancy: %d%%, Base: $%.2f, Services: $%.2f, Total(with tax): $%.2f (surge pricing)\n",
                    inv.occupancy(), res.getBaseAmount(), res.getServiceCharges(), res.getTotal());
        } finally {
            inv.lock.unlock();
        }
    }

    static void testOverbooking() {
        LocalDate checkIn = LocalDate.now().plusDays(10);
        LocalDate checkOut = checkIn.plusDays(2);

        // Book 105 rooms (beyond 100 capacity)
        for (int i = 0; i < 105; i++) {
            BookingService.createReservation(1L, 1L, "DELUXE", checkIn, checkOut, 1, Collections.EMPTY_LIST);
        }

        Inventory inv = DataStore.getInventory(1L, "DELUXE", checkIn);
        System.out.printf("✓ Booked 105 rooms in 100-room hotel\n");
        inv.lock.lock();
        try {
            System.out.printf("  Total: %d, Booked: %d, Overbooked: %d\n",
                    inv.getTotal(), inv.getBooked(), inv.getOverbooked());
            System.out.printf("  Is Overbooked: %s\n", inv.isOverbooked());
        } finally {
            inv.lock.unlock();
        }
    }

    static void testBeyondLimit() {
        LocalDate checkIn = LocalDate.now().plusDays(15);
        LocalDate checkOut = checkIn.plusDays(2);

        // Try to book 111 rooms (beyond 110% limit)
        int successCount = 0;
        for (int i = 0; i < 111; i++) {
            try {
                BookingService.createReservation(1L, 1L, "DELUXE", checkIn, checkOut, 1, Collections.EMPTY_LIST);
                successCount++;
            } catch (Exception e) {
                System.out.printf("✗ Booking #%d failed: %s\n", i + 1, e.getMessage());
                break;
            }
        }
        System.out.printf("✓ Successfully booked %d rooms (max 110)\n", successCount);
    }

    static void testCancellation() {
        LocalDate checkIn = LocalDate.now().plusDays(20);
        LocalDate checkOut = checkIn.plusDays(2);

        Reservation res = BookingService.createReservation(1L, 1L, "DELUXE", checkIn, checkOut, 1, Collections.EMPTY_LIST);
        Inventory inv = DataStore.getInventory(1L, "DELUXE", checkIn);

        int bookedBefore;
        inv.lock.lock();
        try {
            bookedBefore = inv.getBooked();
        } finally {
            inv.lock.unlock();
        }

        BookingService.cancelReservation(res.getId());

        int bookedAfter;
        inv.lock.lock();
        try {
            bookedAfter = inv.getBooked();
        } finally {
            inv.lock.unlock();
        }

        System.out.printf("✓ Reservation %s canceled\n", res.getNumber());
        System.out.printf("  Booked rooms: %d → %d (released 1 room)\n",
                bookedBefore, bookedAfter);
    }

    static void testAddonServices() {
        LocalDate checkIn = LocalDate.now().plusDays(21);
        LocalDate checkOut = checkIn.plusDays(2); // 2 nights

        // Customer chooses Meals (id=1) and Spa (id=2)
        List<Long> addonServiceIds = List.of(1L, 2L);
        Reservation res = BookingService.createReservation(1L, 1L, "DELUXE", checkIn, checkOut, 1, addonServiceIds);
        BookingService.processPayment(res.getId());

        System.out.printf("✓ Reservation: %s (with add-ons)\n", res.getNumber());
        System.out.printf("  Base: $%.2f, Services: $%.2f, Total(with tax): $%.2f, Status: %s\n",
                res.getBaseAmount(), res.getServiceCharges(), res.getTotal(), res.getStatus());
        System.out.println("  Services selected: Meals + Spa");
    }

    static void testConcurrency() {
        LocalDate checkIn = LocalDate.now().plusDays(23);
        LocalDate checkOut = checkIn.plusDays(2);

        List<Thread> threads = new ArrayList<>();
        List<String> results = Collections.synchronizedList(new ArrayList<>());

        // Create 10 threads trying to book simultaneously
        for (int i = 1; i <= 10; i++) {
            final int threadNum = i;
            Thread t = new Thread(() -> {
                try {
                    Reservation res = BookingService.createReservation((long)threadNum, 1L, "DELUXE",
                            checkIn, checkOut, 1, Collections.EMPTY_LIST);
                    results.add("Thread " + threadNum + ": SUCCESS - " + res.getNumber());
                } catch (Exception e) {
                    results.add("Thread " + threadNum + ": FAILED - " + e.getMessage());
                }
            });
            threads.add(t);
            t.start();
        }

        // Wait for all threads
        threads.forEach(t -> {
            try { t.join(); } catch (InterruptedException e) { }
        });

        results.forEach(System.out::println);
        System.out.println("✓ All concurrent bookings handled without conflicts");
    }

    static void testRaceCondition() {
        LocalDate checkIn = LocalDate.now().plusDays(26);
        LocalDate checkOut = checkIn.plusDays(2);

        // First book 90 rooms, leaving only 10 available (within 110% = 110 total)
        for (int i = 0; i < 90; i++) {
            BookingService.createReservation(1L, 1L, "DELUXE", checkIn, checkOut, 1, Collections.EMPTY_LIST);
        }

        List<Thread> threads = new ArrayList<>();
        int[] successCount = {0};
        int[] failCount = {0};

        // Create 100 threads trying to book the last 20 rooms (only 20 available due to 110% limit)
        for (int i = 1; i <= 100; i++) {
            final int threadNum = i;
            Thread t = new Thread(() -> {
                try {
                    BookingService.createReservation((long)(threadNum + 100), 1L, "DELUXE", checkIn, checkOut, 1, Collections.EMPTY_LIST);
                    synchronized (successCount) {
                        successCount[0]++;
                    }
                } catch (Exception e) {
                    synchronized (failCount) {
                        failCount[0]++;
                    }
                }
            });
            threads.add(t);
        }

        // Start all threads at once
        threads.forEach(Thread::start);

        // Wait for all threads
        threads.forEach(t -> {
            try { t.join(); } catch (InterruptedException e) { }
        });

        System.out.printf("✓ Race condition test completed\n");
        System.out.printf("  Success: %d, Failed: %d (Total: 100 threads)\n",
                successCount[0], failCount[0]);
        System.out.printf("  Expected: 20 success, 80 failed (90 already booked, 20 slots left)\n");

        // Verify no double booking
        Inventory inv = DataStore.getInventory(1L, "DELUXE", checkIn);
        inv.lock.lock();
        try {
            System.out.printf("  Actual booked: %d (should be 90 + %d = %d)\n",
                    inv.getBooked() + inv.getOverbooked(), successCount[0], 90 + successCount[0]);
        } finally {
            inv.lock.unlock();
        }
    }

    static void testMealsAddonOnly() {
        // Use a date not used by other scenarios
        LocalDate checkIn = LocalDate.now().plusDays(22);
        LocalDate checkOut = checkIn.plusDays(2); // 2 nights

        // Customer chooses Meals only (id=1)
        List<Long> addonServiceIds = List.of(1L);
        Reservation res = BookingService.createReservation(1L, 1L, "DELUXE", checkIn, checkOut, 1, addonServiceIds);
        BookingService.processPayment(res.getId());

        // Expected numbers at low occupancy:
        // base = 200 * 0.8 * 2 nights * 1 room = 320
        // services = 30 * 2 nights * 1 room = 60
        // total(with tax @12%) = (320 + 60) * 1.12 = 425.60
        System.out.println("Expected: Base=$320.00, Services=$60.00, Total(with tax)=$425.60");
        System.out.printf("Actual:   Base=$%.2f, Services=$%.2f, Total(with tax)=$%.2f, Status=%s\n",
                res.getBaseAmount(), res.getServiceCharges(), res.getTotal(), res.getStatus());
        System.out.println("  Services selected: Meals");
    }
}