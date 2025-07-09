package com.vinay.lld.donation.model;

import lombok.Getter;

public enum Currency {
    DOLLAR("$");

    @Getter
    private final String symbol;

    Currency(final String symbol) {
        this.symbol = symbol;
    }
}
