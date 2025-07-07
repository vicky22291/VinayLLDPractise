// URL: https://leetcode.com/problems/print-in-order/?envType=problem-list-v2&envId=concurrency

package com.vinay.concurrency;

import java.util.concurrent.atomic.AtomicInteger;

public class PrintInOrder {
    private final AtomicInteger order;

    public PrintInOrder() {
        this.order = new AtomicInteger(0);
    }

    public void first(Runnable printFirst) throws InterruptedException {

        while (this.order.get() != 0) {
            Thread.sleep(10);
        }
        // printFirst.run() outputs "first". Do not change or remove this line.
        printFirst.run();
        this.order.incrementAndGet();
    }

    public void second(Runnable printSecond) throws InterruptedException {

        while (this.order.get() != 1) {
            Thread.sleep(10);
        }
        // printSecond.run() outputs "second". Do not change or remove this line.
        printSecond.run();
        this.order.incrementAndGet();
    }

    public void third(Runnable printThird) throws InterruptedException {

        while (this.order.get() != 2) {
            Thread.sleep(10);
        }
        // printThird.run() outputs "third". Do not change or remove this line.
        printThird.run();
        this.order.incrementAndGet();
    }
}
