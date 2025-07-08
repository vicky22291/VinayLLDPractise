package com.vinay.lld.donation.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class Donation {
    private final String donorName;
    private final String email;
    private final String charity;
    private double amount;
    private Currency currency;
    private final LocalDateTime time;
}
