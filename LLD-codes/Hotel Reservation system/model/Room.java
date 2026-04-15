/*
 * Room Entity representing a room type in a hotel
 */
package model;

public class Room {
    private final Long id;
    private final Long hotelId;
    private final String type;
    private final double basePrice;

    public Room(Long id, Long hotelId, String type, double basePrice) {
        this.id = id;
        this.hotelId = hotelId;
        this.type = type;
        this.basePrice = basePrice;
    }

    public Long getHotelId() {
        return hotelId;
    }

    public Long getId() {
        return id;
    }

    public double getBasePrice() {
        return basePrice;
    }

    public String getType() {
        return type;
    }
}