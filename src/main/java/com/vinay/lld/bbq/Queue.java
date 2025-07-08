package com.vinay.lld.bbq;

public interface Queue<I> {
    void enqueue(I item) throws InterruptedException;
    I dequeue() throws InterruptedException;
}
