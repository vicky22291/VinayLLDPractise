//URL: https://leetcode.com/problems/print-foobar-alternately/description/?envType=problem-list-v2&envId=concurrency
package com.vinay.concurrency;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class PrintFoobarAlternatively {
    private int n;
    private final AtomicInteger printStatus;
    private final ReentrantLock lock;
    private final Condition fooCondition;
    private final Condition barCondition;

    public PrintFoobarAlternatively(int n) {
        this.n = n;
        this.printStatus = new AtomicInteger(0);
        this.lock = new ReentrantLock(true);
        this.fooCondition = lock.newCondition();
        this.barCondition = lock.newCondition();
    }

    public void foo(Runnable printFoo) throws InterruptedException {

        for (int i = 0; i < n; i++) {
            this.lock.lock();
            while(this.printStatus.get() % 2 != 0) {
                fooCondition.await();
            }
            // printFoo.run() outputs "foo". Do not change or remove this line.
            printFoo.run();
            this.printStatus.incrementAndGet();
            barCondition.signalAll();
            this.lock.unlock();
        }
    }

    public void bar(Runnable printBar) throws InterruptedException {

        for (int i = 0; i < n; i++) {
            this.lock.lock();
            while(this.printStatus.get() % 2 != 1) {
                barCondition.await();
            }
            // printBar.run() outputs "bar". Do not change or remove this line.
            printBar.run();
            this.printStatus.incrementAndGet();
            this.fooCondition.signalAll();
            this.lock.unlock();
        }
    }
}
