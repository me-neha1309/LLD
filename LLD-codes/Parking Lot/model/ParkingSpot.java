package model;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public final class ParkingSpot {
    private final String spotId;
    private final int floorNumber;
    private final int spotNumber;
    private final SpotType spotType;
    private final Map<String, Integer> distanceByGate;

    private boolean occupied;
    private Vehicle currentVehicle;

    public ParkingSpot(String spotId, int floorNumber, int spotNumber,
                       SpotType spotType, Map<String, Integer> distanceByGate) {
        if(spotId == null || spotId.isBlank()){
            throw new IllegalArgumentException("SpotId cannot be blank");
        }
        if(floorNumber < 0){
            throw new IllegalArgumentException("floorNumber cannot be negative");
        }
        if(spotNumber < 0){
            throw new IllegalArgumentException("SpotNumber cannot be negative");
        }
        this.spotId = spotId.trim();
        this.floorNumber = floorNumber;
        this.spotNumber = spotNumber;
        this.spotType = Objects.requireNonNull(spotType, "spotType");
        this.distanceByGate = Collections.unmodifiableMap(
                Objects.requireNonNull(distanceByGate, "distanceByGate"));
        this.occupied = false;
        this.currentVehicle = null;
    }

    public String getSpotId() {
        return spotId;
    }

    public int getFloorNumber() {
        return floorNumber;
    }

    public int getSpotNumber() {
        return spotNumber;
    }

    public SpotType getSpotType() {
        return spotType;
    }

    public int distanceToGate(String gateId) {
        Integer d = distanceByGate.get(gateId);
        return d==null ? Integer.MAX_VALUE : d;
    }

    public boolean isOccupied() {
        return occupied;
    }

    public Vehicle getCurrentVehicle() {
        return currentVehicle;
    }

    public void assignTo(Vehicle vehicle) {
        if(occupied){
            throw new IllegalStateException("Spot already occupied: " + spotId);
        }
        this.occupied = true;
        this.currentVehicle = vehicle;
    }

    public void free() {
        if(!occupied){
            System.out.println("Spot already free : " + spotId);
        }
        this.occupied = false;
        this.currentVehicle = null;
    }

    @Override
    public String toString() {
        return "ParkingSpot{spotId = " + spotId + ", floorNumber = " + floorNumber +  " type=" + spotType + "}";
    }
}