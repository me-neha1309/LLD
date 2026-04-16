package model;

import java.util.*;

public final class ParkingFloor {
    private final int floorNumber;
    private final Map<String, ParkingSpot> spotsById;

    public ParkingFloor(int floorNumber, Collection<ParkingSpot> spots){
        if(floorNumber < 0){
            throw new IllegalArgumentException("floorNumber cannot be negative");
        }

        this.floorNumber = floorNumber;
        Objects.requireNonNull(spots, "spots");

        Map<String, ParkingSpot> tmp = new LinkedHashMap<>();
        for(ParkingSpot s : spots){
            if(s.getFloorNumber() != floorNumber){
                throw new IllegalArgumentException("Spots floor mismatch: " + s);
            }
            if(tmp.putIfAbsent(s.getSpotId(), s) != null){
                throw new IllegalArgumentException("Duplicate Spot Id " + s.getSpotId());
            }
        }
        //gives on the read view (unmodifiable) not an immutable copy
        this.spotsById = Collections.unmodifiableMap(tmp);
    }

    public int getFloorNumber(){
        return floorNumber;
    }

    public Map<String, ParkingSpot> getSpotsById() {
        return spotsById;
    }

    @Override
    public String toString() {
        return "ParkingFloor{floorNumber = " + floorNumber + ", spots = " + spotsById.size() + "}";
    }
}