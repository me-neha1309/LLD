package factory;

import model.Vehicle;
import model.VehicleType;

import java.util.Locale;
import java.util.Objects;

public final class VehicleFactory {
    public Vehicle create(String licenseNumber, VehicleType type){
        return new Vehicle(licenseNumber, type);
    }

    public Vehicle create(String licenseNumber, String type){
        Objects.requireNonNull(type, "type");
        VehicleType vt = VehicleType.valueOf(type.trim().toUpperCase(Locale.ROOT));
        return new Vehicle(licenseNumber, vt);
    }
}