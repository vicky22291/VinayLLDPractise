//URL: https://leetcode.com/problems/building-h2o/description/?envType=problem-list-v2&envId=concurrency
package com.vinay.concurrency;

import java.util.concurrent.Semaphore;

public class BuildingH2O {

    private final Semaphore hydrogenSemaphore;
    private final Semaphore oxygenSemaphore;

    public BuildingH2O() {
        hydrogenSemaphore = new Semaphore(2);
        oxygenSemaphore = new Semaphore(0);
    }

    public void hydrogen(Runnable releaseHydrogen) throws InterruptedException {
        hydrogenSemaphore.acquire();

        // releaseHydrogen.run() outputs "H". Do not change or remove this line.
        releaseHydrogen.run();

        oxygenSemaphore.release();
    }

    public void oxygen(Runnable releaseOxygen) throws InterruptedException {
        oxygenSemaphore.acquire(2);

        // releaseOxygen.run() outputs "O". Do not change or remove this line.
        releaseOxygen.run();

        hydrogenSemaphore.release(2);
    }
}
