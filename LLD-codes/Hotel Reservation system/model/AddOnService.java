package model;

public class AddOnService {
    private final Long id;
    private final String name;
    private final ServiceType type;
    private final ChargeType chargeType;
    private final double price;

    public AddOnService(Long id, String name, ServiceType type, ChargeType chargeType, double price){
        this.id = id;
        this.name = name;
        this.type = type;
        this.chargeType = chargeType;
        this.price = price;
    }

    @Override
    public String toString(){
        return "AddOnService[" + id + ", " + name +", "+ chargeType +", $" + price + "]";
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ServiceType getType() {
        return type;
    }

    public ChargeType getChargeType() {
        return chargeType;
    }

    public double getPrice() {
        return price;
    }
}

