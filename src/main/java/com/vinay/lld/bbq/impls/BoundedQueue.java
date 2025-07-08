package com.vinay.lld.bbq.impls;

import com.vinay.lld.bbq.Queue;

import java.util.concurrent.Phaser;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class BoundedQueue<I> implements Queue<I> {
    private final I[] buffer;
    private int count;
    private final int capacity;
    private int head;
    private int tail;
    private final ReentrantLock lock;
    private final Condition notFull;
    private final Condition notEmpty;

    public BoundedQueue(final int capacity) {
        this.buffer = (I[]) new Object[capacity];
        this.count = 0;
        this.capacity = capacity;
        this.lock = new ReentrantLock(true);
        this.notFull = this.lock.newCondition();
        this.notEmpty = this.lock.newCondition();
        this.head = 0;
        this.tail = 0;
    }

    @Override
    public void enqueue(I item) throws InterruptedException {
        this.lock.lock();
        try {
            while (this.count == this.capacity) {
                this.notFull.await();
            }
            this.buffer[this.head] = item;
            this.head = (this.head + 1) % this.capacity;
            this.count++;
            this.notEmpty.signalAll();
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public I dequeue() throws InterruptedException {
        this.lock.lock();
        try {
            while(this.count == 0) {
                this.notEmpty.await();
            }
            final I result = this.buffer[tail];
            this.tail = (this.tail + 1) % this.capacity;
            this.count--;
            this.notFull.signalAll();
            return result;
        } finally {
            this.lock.unlock();
        }
    }
}
