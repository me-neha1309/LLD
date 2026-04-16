package strategy;

import model.VehicleType;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class HourlyPricingStrategy implements PricingStrategy {
    private final Map<VehicleType, Long> ratePerHour;
    private final long lostTicketPenalty;
    private final int lostTicketMaxHours;

    public HourlyPricingStrategy(Map<VehicleType, Long> ratePerHour, long lostTicketPenalty, int lostTicketMaxHours){
        Objects.requireNonNull(ratePerHour, "ratePerHour");
        if(lostTicketPenalty < 0){
            throw new IllegalArgumentException("lostTicket Penalty cannot be negative");
        }

        if(lostTicketMaxHours <= 0){
            throw new IllegalArgumentException("lostTicketMaxHours must be > 0");
        }
        EnumMap<VehicleType, Long> tmp = new EnumMap<>(VehicleType.class);
        tmp.putAll(ratePerHour);
        for(VehicleType t : VehicleType.values()){
            if(!tmp.containsKey(t)){
                throw new IllegalArgumentException("Missing hourly rate for: " + t);
            }
            if(tmp.get(t)==null || tmp.get(t)<0){
                throw new IllegalArgumentException("Invalid hourly rate for: " + t);
            }
        }
        this.ratePerHour = ratePerHour;
        this.lostTicketPenalty = lostTicketPenalty;
        this.lostTicketMaxHours = lostTicketMaxHours;
    }

    public long calculateFee(VehicleType vehicleType, Duration parkedDuration, boolean isLostTicket){
        Objects.requireNonNull(vehicleType, "vehicleType");
        Objects.requireNonNull(parkedDuration, "parkedDuration");

        long rate = ratePerHour.get(vehicleType);
        if(isLostTicket){
            return (rate * lostTicketMaxHours) + lostTicketPenalty;
        }

        long min = Math.max(0, parkedDuration.toMinutes());
        long hours = Math.max(1, (min + 59) /60);
        return rate * hours;
    }
}