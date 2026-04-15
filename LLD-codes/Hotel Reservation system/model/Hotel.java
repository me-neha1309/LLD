/*
 * Hotel entity representing a hotel property
 */
package model;
public class Hotel {
    private final Long id;
    private final String name;
    private final String city;
    private final int totalRooms;

    public Hotel(Long id, String name, String city, int totalRooms){
        this.id = id;
        this.name = name;
        this.city = city;
        this.totalRooms = totalRooms;
    }

    @Override
    public String toString() {
        return String.format("Hotel[%s, %s, %d rooms]", name, city, totalRooms);
    }

    public String getName() {
        return name;
    }

    public Long getId() {
        return id;
    }

    public int getTotalRooms() {
        return totalRooms;
    }

    public String getCity() {
        return city;
    }
}