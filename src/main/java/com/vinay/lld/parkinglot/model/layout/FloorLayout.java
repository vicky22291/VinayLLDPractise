package com.vinay.lld.parkinglot.model.layout;

import com.vinay.lld.parkinglot.model.Spot;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class FloorLayout {

    @Getter
    private final int number;

    @Getter
    private final List<Section> sections;

    public FloorLayout(final int floorNumber) {
        this.sections = new ArrayList<>();
        this.initialise();
        this.number = floorNumber;
    }

    private void initialise() {
        // Need to do something here
    }
}