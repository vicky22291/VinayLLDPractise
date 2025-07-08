package com.vinay.lld.donation.charity;

import com.google.common.base.Preconditions;
import com.vinay.lld.donation.model.Donation;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.UUID;

public class CharityAccount {
    @Getter
    private final String name;
    @Getter
    private double balance;
    private final String id;

    public CharityAccount(@Nonnull final String name) {
        this.name = name;
        this.balance = 0;
        this.id = UUID.randomUUID().toString();
    }

    public void addDonation(@Nonnull final Donation donation) {
        Preconditions.checkArgument(donation.getCharity().equals(this.name), String.format("%s is not expected Charity name for %s", donation.getCharity(), this.id));
        Preconditions.checkArgument(donation.getAmount() >= 0, String.format("%f donation amount cannot be negative.", donation.getAmount()));
        this.balance += donation.getAmount();
    }
}
