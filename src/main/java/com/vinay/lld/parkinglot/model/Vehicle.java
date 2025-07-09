package com.vinay.lld.parkinglot.model;

public interface Vehicle {
    default boolean isElectric() {
        return Boolean.FALSE;
    }
    int numberOfWheels();
}
