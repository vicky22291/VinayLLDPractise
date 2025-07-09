package com.vinay.lld.parkinglot.model.impls;

import com.vinay.lld.parkinglot.model.Vehicle;

public class Bike implements Vehicle {

    @Override
    public int numberOfWheels() {
        return 2;
    }
}
