package com.vinay.lld.squestion1;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class TransactionReconciliationTest {
    @Test
    public void test1() {
        final TransactionReconciliation recon = new TransactionReconciliation("account_123");
        Arrays.stream(new String[] {
                "account_123,1,800,usd",
                "account_321,2,100,usd",
                "account_123,3,-300,usd",
                "account_321,4,-300,usd",
                "account_321,5,-500,usd",
                "account_321,6,1000,usd"
        }).forEach(tran -> recon.performTransaction(new Transaction(tran)));
        recon.printAccounts();
        recon.printRejectedTransactions();
    }
}