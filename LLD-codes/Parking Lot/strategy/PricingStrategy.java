package strategy;

import model.VehicleType;

import java.time.Duration;

public interface PricingStrategy {
    long calculateFee(VehicleType vehicleType, Duration parkedDuration, boolean isLostTicket);
}