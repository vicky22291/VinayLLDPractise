package com.vinay.lld.parkinglot.model;

import com.vinay.lld.parkinglot.model.enums.VehicleType;

import javax.annotation.Nonnull;
import java.util.List;

public interface Spot {
    default boolean isElectric() {
        return Boolean.FALSE;
    }
    List<VehicleType> getSupportedTypes();
    Location getLocation();
    boolean isHandicappedReserved();
    boolean isEmpty();
    void release();
    void allocate(@Nonnull final Vehicle vehicle, final boolean isElectricRequired, final boolean isHandicapped);
}
