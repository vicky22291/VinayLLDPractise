package com.vinay.lld.parkinglot.model.impls;

import com.vinay.lld.parkinglot.model.ElectricVehicle;

public class ElectricBike implements ElectricVehicle {

    @Override
    public int numberOfWheels() {
        return 2;
    }
}
