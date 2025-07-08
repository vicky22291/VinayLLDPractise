package com.vinay.lld.donation;

import com.vinay.lld.donation.charity.AccountManager;
import com.vinay.lld.donation.model.Donation;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class TransactionManager {
    private final ConcurrentHashMap<String, Donation> donations;
    private final AccountManager accountManager;

    public TransactionManager(@Nonnull final AccountManager accountManager) {
        this.donations = new ConcurrentHashMap<>();
        this.accountManager = accountManager;
    }

    public void recordDonation(@Nonnull final Donation donation) {
        final String donationKey = this.getDonationKey(donation);
        if (this.donations.containsKey(donationKey)) {
            return;
        }
        this.donations.put(donationKey, donation);
        this.accountManager.getAccount(donation.getCharity()).addDonation(donation);
    }

    private String getDonationKey(@Nonnull final Donation donation) {
        return String.format("%s-%s", donation.getDonorName(), donation.getCharity());
    }

    public List<Donation> getDonations() {
        return this.donations.values().stream().toList();
    }
}
