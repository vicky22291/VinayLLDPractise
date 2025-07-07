// URL: https://leetcode.com/problems/design-bounded-blocking-queue/description/?envType=problem-list-v2&envId=concurrency

package com.vinay.concurrency;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class DesignBoundedBlockingQueue {
    private final AtomicInteger size;
    private final ReentrantLock lock;
    private final int[] buffer;
    private final Condition notFull;
    private final Condition notEmpty;
    private int head;
    private int tail;
    private final int capacity;

    public DesignBoundedBlockingQueue(int capacity) {
        this.capacity = capacity;
        this.buffer = new int[capacity];
        this.size = new AtomicInteger(0);
        this.lock = new ReentrantLock(true);
        this.tail = 0;
        this.head = 0;
        this.notFull = this.lock.newCondition();
        this.notEmpty = this.lock.newCondition();
    }

    public void enqueue(int element) throws InterruptedException {
        this.lock.lock();
        while (this.size.get() == this.capacity) {
            notFull.await();
        }
        this.buffer[this.head] = element;
        this.head = (this.head + 1) % this.capacity;
        this.size.incrementAndGet();
        notEmpty.signalAll();
        this.lock.unlock();
    }

    public int dequeue() throws InterruptedException {
        this.lock.lock();
        while (this.size.get() == 0) {
            notEmpty.await();
        }
        final int result = this.buffer[this.tail];
        this.tail = (this.tail + 1) % this.capacity;
        this.size.decrementAndGet();
        notFull.signalAll();
        this.lock.unlock();
        return result;
    }

    public int size() {
        return this.size.get();
    }
}
