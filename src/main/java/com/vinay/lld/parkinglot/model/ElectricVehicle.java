package com.vinay.lld.parkinglot.model;

public interface ElectricVehicle extends Vehicle {
    default boolean isElectric() {
        return Boolean.TRUE;
    }
}
