// URL: https://leetcode.com/problems/the-dining-philosophers/description/?envType=problem-list-v2&envId=concurrency
package com.vinay.concurrency;

import java.util.concurrent.locks.ReentrantLock;

public class TheDiningPhilosophers {
    private ReentrantLock[] forks = new ReentrantLock[5];
    public TheDiningPhilosophers() {
        for (int i = 0; i < 5; i++) {
            forks[i] = new ReentrantLock();
        }
    }

    // call the run() method of any runnable to execute its code
    public void wantsToEat(int philosopher,
                           Runnable pickLeftFork,
                           Runnable pickRightFork,
                           Runnable eat,
                           Runnable putLeftFork,
                           Runnable putRightFork) throws InterruptedException {
        try {
            int fork1 = philosopher;
            int fork2 = (philosopher + 1) % 5;
            synchronized (this) {
                while (forks[fork1].isLocked() || forks[fork2].isLocked()) {
                    wait();
                }
                forks[fork1].lock();
                forks[fork2].lock();

                pickLeftFork.run();
                pickRightFork.run();
                eat.run();
                putLeftFork.run();
                putRightFork.run();

                forks[fork1].unlock();
                forks[fork2].unlock();
                notifyAll();
            }

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
