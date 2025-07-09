package com.vinay.lld.parkinglot.model.enums;

import com.vinay.lld.parkinglot.model.impls.Bike;
import com.vinay.lld.parkinglot.model.impls.ElectricBike;
import lombok.Getter;

public enum VehicleType {
    BIKE(Bike.class),
    ELECTRIC_BIKE(ElectricBike.class);

    @Getter
    private final Class klass;

    VehicleType(final Class klass) {
        this.klass = klass;
    }
}
