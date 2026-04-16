package model;

import java.util.Objects;

public final class Vehicle {
    private final String licenseNumber;
    private final VehicleType type;

    public Vehicle(String licenseNumber, VehicleType vehicleType){
        if(licenseNumber==null || licenseNumber.isBlank()){
            throw new IllegalArgumentException("License Number cannot be blank");
        }
        this.licenseNumber = licenseNumber.trim();
        this.type = Objects.requireNonNull(vehicleType, "vehicleType");
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public VehicleType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "Vehicle{licenseNumber = " + licenseNumber + ", type = " + type + "}";
    }
}