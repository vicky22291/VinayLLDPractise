package com.vinay.lld.parkinglot.model.layout;

import com.vinay.lld.parkinglot.model.Spot;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class Row {
    @Getter
    private final List<Spot> spots;
    public Row(final int count, final Spot spotInstance) {
        this.spots = new ArrayList<>();
        this.initialise(spotInstance);
    }

    private void initialise(final Spot spotInstance) {
        // Will have a way to initialise spot
    }
}