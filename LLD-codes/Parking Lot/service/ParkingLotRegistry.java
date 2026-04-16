package service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

//Simple in-memory registry to scale to multiple parking lots
public final class ParkingLotRegistry {
    private final Map<String, ParkingLotService> byLotId = new HashMap<>();

    public synchronized void register(ParkingLotService service){
        Objects.requireNonNull(service, "service");
        String lotId = service.getLot().getLotId();
        if(byLotId.putIfAbsent(lotId, service)!=null){
            throw new IllegalStateException("Parking lot already registered");
        }
    }

    public synchronized ParkingLotService get(String lotId){
        Objects.requireNonNull(lotId, "lotId");
        ParkingLotService svc = byLotId.get(lotId);
        if(svc==null){
            throw new IllegalArgumentException("Unknown LotId: " + lotId);
        }
        return svc;
    }

}