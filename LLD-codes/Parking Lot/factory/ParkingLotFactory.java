package factory;

import model.ParkingFloor;
import model.ParkingLot;

import model.ParkingSpot;
import model.SpotType;

import java.util.*;

public final class ParkingLotFactory {
    public ParkingLot create(String lotId, List<String> gateIds, int numberOfFloors, Map<SpotType, Integer> spotsPerFloorByType){
        Objects.requireNonNull(gateIds, "gateIds");
        Objects.requireNonNull(spotsPerFloorByType, "spotsPerFloorByType");
        if(numberOfFloors < 0){
            throw new IllegalArgumentException("numberOfFloors must be > 0");
        }

        EnumMap<SpotType, Integer> counts = new EnumMap<>(SpotType.class);
        counts.putAll(spotsPerFloorByType);
        for(SpotType t : SpotType.values()){
            if(!counts.containsKey(t)){
                counts.put(t, 0);
            }
            Integer c = counts.get(t);
            if(c==null || c<0){
                throw new IllegalArgumentException("Invalid Count for Spot Type : " + t);
            }
        }

        List<ParkingFloor> floors = new ArrayList<>();
        for(int floor=0; floor<numberOfFloors; floor++){
            List<ParkingSpot> spots = new ArrayList<>();

            int spotNumber = 1;
            for(SpotType type : SpotType.values()){
                int count = counts.get(type);
                for(int i=0; i<count; i++){
                    String spotId = "F" + floor + "-" + type + "-" + spotNumber;
                    Map<String, Integer> distanceByGate = new HashMap<>();
                    for(int g=0; g<gateIds.size(); g++){
                        String gateId = gateIds.get(g);
                        int gateAnchor = (g + 1)*50;
                        int distance = Math.abs(spotNumber - gateAnchor) + (floor * 5);
                        distanceByGate.put(gateId, distance);
                    }
                    spots.add(new ParkingSpot(spotId, floor, spotNumber, type, distanceByGate));
                    spotNumber++;
                }
            }
            floors.add(new ParkingFloor(floor, spots));
        }
        return new ParkingLot(lotId, gateIds, floors);
    }
}