package com.vinay.lld.donation.charity;

import javax.annotation.Nonnull;
import java.util.concurrent.ConcurrentHashMap;

public class AccountManager {
    private final ConcurrentHashMap<String, CharityAccount> accounts;

    public AccountManager() {
        this.accounts = new ConcurrentHashMap<>();
    }

    public CharityAccount getAccount(@Nonnull final String charity) {
        if (!this.accounts.containsKey(charity)) {
            this.accounts.put(charity, new CharityAccount(charity));
        }
        return this.accounts.get(charity);
    }
}
