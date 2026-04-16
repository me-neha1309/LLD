package model;

import java.util.*;

public final class ParkingLot {
    private final String lotId;
    private final List<String> gateIds;
    private final Map<Integer, ParkingFloor> floorsByNumber;

    public ParkingLot(String lotId, List<String> gateIds, List<ParkingFloor> floors){
        if(lotId==null || lotId.isBlank()){
            throw new IllegalArgumentException("lotId cannot be blank");
        }
        Objects.requireNonNull(gateIds, "gateIds");
        if(gateIds.isEmpty()){
            throw new IllegalArgumentException("At least one gate is required");
        }
        Objects.requireNonNull(floors, "floors");
        if(floors.isEmpty()){
            throw new IllegalArgumentException("At least one floor is required");
        }
        this.lotId = lotId;
        this.gateIds = gateIds;

        Map<Integer, ParkingFloor> tmp = new LinkedHashMap<>();
        for(ParkingFloor pf : floors){
            if(tmp.putIfAbsent(pf.getFloorNumber(), pf)!=null){
                throw new IllegalArgumentException("Duplicate floor: " + pf.getFloorNumber());
            }
        }
        this.floorsByNumber = Collections.unmodifiableMap(tmp);
    }

    public String getLotId() {
        return lotId;
    }

    public List<String> getGateIds() {
        return gateIds;
    }

    public Map<Integer, ParkingFloor> getFloorsByNumber() {
        return floorsByNumber;
    }

    @Override
    public String toString() {
        return "ParkingLot{lotId = " + lotId + ", gates = " + gateIds + " floors = " + floorsByNumber.size() + "}";
    }
}