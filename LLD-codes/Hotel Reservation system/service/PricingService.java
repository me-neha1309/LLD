package service;

import model.Inventory;
import model.Room;

public class PricingService{
    public static double calculatePrice(Room room, Inventory inv, int nights, int rooms) {
        int occupancy = inv.occupancy();
        double multiplier = getPriceMultiplier(occupancy);
        return room.getBasePrice() * multiplier * nights * rooms;
    }

    private static double getPriceMultiplier(int occupancy){
        if(occupancy <= 30) return 0.80;
        else if(occupancy <= 60) return 1.00;
        else if(occupancy <= 80) return 1.30;
        else if(occupancy <= 100) return 1.80;
        else return 2.00;
    }

    public static double calculateTotal(double base, double services, double discount) {
        double subtotal = base + services - discount;
        double tax = subtotal * 0.12;
        return subtotal + tax;
    }
}