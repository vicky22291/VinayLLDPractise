package com.vinay.lld.parkinglot.model.layout;

import java.util.ArrayList;
import java.util.List;

public class Section {
    private final List<Row> rows;

    public Section(final int rows, final int spotsEachRow) {
        this.rows = new ArrayList<>();
        this.initialise(rows, spotsEachRow);
    }

    private void initialise(final int rows, final int spotsEachRow) {
        // new to do something
    }
}