package model;

//Simple enum constants are thread safe
//Enum with immutable fields are thread safe
//But Enum with mutable state are not thread safe
public enum SpotType {
    BIKE,
    CAR,
    TRUCK
}