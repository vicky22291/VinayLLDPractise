package com.vinay.lld.parkinglot.model.impls;

import com.google.common.base.Preconditions;
import com.vinay.lld.parkinglot.model.Location;
import com.vinay.lld.parkinglot.model.Spot;
import com.vinay.lld.parkinglot.model.Vehicle;
import com.vinay.lld.parkinglot.model.enums.VehicleType;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;

public class BikeSpot implements Spot {
    @Getter
    @Setter
    private boolean handicappedReserved = Boolean.FALSE;

    private final Location location;
    private Vehicle vehicleParked;

    @Getter
    private boolean empty;

    public BikeSpot(final Location location) {
        this.location = location;
        this.empty = Boolean.TRUE;
        this.vehicleParked = null;
    }

    @Override
    public List<VehicleType> getSupportedTypes() {
        return List.of(VehicleType.BIKE, VehicleType.ELECTRIC_BIKE);
    }

    @Override
    public Location getLocation() {
        return null;
    }

    @Override
    public synchronized void release() {
        this.empty = Boolean.TRUE;
        this.vehicleParked = null;
    }

    @Override
    public synchronized void allocate(@Nonnull final Vehicle vehicle, final boolean isElectricRequired, final boolean isHandicapped) {
        this.validateVehicle(vehicle, isElectricRequired, isHandicapped);
        this.vehicleParked = vehicle;
        this.empty = Boolean.FALSE;
    }

    private void validateVehicle(@Nonnull final Vehicle vehicle, final boolean isElectricRequired, final boolean isHandicapped) {
        Preconditions.checkArgument(this.getSupportedTypes()
                        .stream().anyMatch(
                                vehicleType -> vehicleType.getKlass().equals(vehicle.getClass())
                        ),
                String.format("%s is not supported for this spot.", vehicle.getClass()));
        Preconditions.checkArgument(this.isEmpty(), "Spot not empty");
        Preconditions.checkArgument(isElectricRequired && this.isElectric(), "There is no electric station here.");
        Preconditions.checkArgument(!this.isHandicappedReserved() || (this.isHandicappedReserved() && isHandicapped), "Is reserved for Handicapped.");
    }
}
