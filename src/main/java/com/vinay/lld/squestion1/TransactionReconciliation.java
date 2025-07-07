package com.vinay.lld.squestion1;

import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
class Transaction{
    private final String accountID;
    private final int timestamp;
    private final float amount;
    private final String currency;
    private final String stringFormat;

    public Transaction(final String transactionCsv) {
        final String[] data = transactionCsv.split(",");
        this.accountID = data[0];
        this.timestamp = Integer.valueOf(data[1]);
        this.amount = Float.valueOf(data[2]);
        this.currency = data[3];
        this.stringFormat = transactionCsv;
    }

    @Override
    public String toString() {
        return this.stringFormat;
    }
}

class RejectedException extends Exception {

}

public class TransactionReconciliation {
    private static Float INITIAL_BALANCE = Float.valueOf(0);
    private final Map<String, Float> accounts;
    private final List<Transaction> rejectedTransactions;
    private final String platformAccountID;
    private final Map<String, Float> adjustments;

    public TransactionReconciliation(final String platformAccount) {
        this.accounts = new HashMap<>();
        this.rejectedTransactions = new ArrayList<>();
        this.platformAccountID = platformAccount;
        this.adjustments = new HashMap<>();
    }

    private float getBalance(final String accountID) {
        return this.accounts.getOrDefault(accountID, INITIAL_BALANCE);
    }

    private void addAndSet(final String accountID, float amount) {
        this.accounts.put(accountID, this.getBalance(accountID) + amount);
    }

    private float getAdjustmentBalance(final String accountID) {
        return this.adjustments.getOrDefault(accountID, INITIAL_BALANCE);
    }

    private void addAndSetAdjustments(final String accountID, float amount) {
        this.adjustments.put(accountID, this.getBalance(accountID) + amount);
    }

    private float[] getSplitForCredit(final String accountID, final float amount) {
        if (this.getAdjustmentBalance(accountID) > 0) {
            final float creditBack = Math.min(amount, this.getAdjustmentBalance(accountID));
            final float creditAmount = amount -  creditBack;
            return new float[]{creditAmount, creditBack};
        }
        return new float[]{amount, 0};
    }

    private float[] getSplitForDebit(final String accountID, final float amount) throws RejectedException {
        final float absAmount = Math.abs(amount);
        if (accountID == this.platformAccountID && absAmount > this.getBalance(accountID)) {
            throw new RejectedException();
        }
        else if (accountID != this.platformAccountID && absAmount > this.getBalance(accountID) + this.getBalance(this.platformAccountID)) {
            throw new RejectedException();
        }
        final float adjustment = absAmount - Math.min(this.getBalance(accountID), absAmount);
        return new float[] {-Math.min(this.getBalance(accountID), absAmount), -adjustment};
    }

    private void performCredit(final Transaction transaction) {
        final float[] split = this.getSplitForCredit(transaction.getAccountID(), transaction.getAmount());
        final float creditAmount = split[0];
        final float creditBack = split[1];
        this.addAndSet(transaction.getAccountID(), creditAmount);
        this.addAndSet(this.platformAccountID, creditBack);
        this.addAndSetAdjustments(transaction.getAccountID(), -creditBack);
    }

    private void performDebit(final Transaction transaction) throws RejectedException {
        final float[] split = this.getSplitForDebit(transaction.getAccountID(), transaction.getAmount());
        final float accountDeduction = split[0];
        final float adjustment = split[1];
        this.addAndSet(transaction.getAccountID(), accountDeduction);
        if (Math.abs(adjustment) > 0) {
            this.addAndSet(this.platformAccountID, adjustment);
            this.addAndSetAdjustments(transaction.getAccountID(), Math.abs(adjustment));
        }
    }

    public void performTransaction(final Transaction transaction) {
        try {
            if (transaction.getAmount() >= 0) {
                this.performCredit(transaction);
            } else {
                this.performDebit(transaction);
            }
        } catch (RejectedException e) {
            this.rejectedTransactions.add(transaction);
        }
    }

    public void printAccounts() {
        this.accounts.forEach((accountID, balance) -> {
            if (balance > 0) {
                System.out.println(String.format("Account with '%s' ID has balance of %f", accountID, balance));
            }
        });
    }

    public void printRejectedTransactions() {
        if (this.rejectedTransactions.isEmpty()) {
            return;
        }
        System.out.println("Below are rejected Transactions:");
        this.rejectedTransactions.forEach(transaction -> {
            System.out.println(transaction);
        });
    }
}