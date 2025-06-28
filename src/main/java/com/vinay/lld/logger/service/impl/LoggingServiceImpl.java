package com.vinay.lld.logger.service.impl;

import com.vinay.lld.logger.model.Content;
import com.vinay.lld.logger.service.LoggingService;
import com.vinay.lld.logger.storage.Storage;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Singleton
public class LoggingServiceImpl implements LoggingService {
    private static final int LIMIT = 1000000;

    private final String[] buffer;
    private final Storage storage;
    private final int capacity;
    private int head = 0, tail = 0, count = 0;
    private final Lock lock = new ReentrantLock();
    private final Condition notFull = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();

    @Inject
    public LoggingServiceImpl(@Named("logCapacity") final int capacity, @Nonnull final Storage storage) {
        this.buffer = new String[capacity];
        this.capacity = capacity;
        this.storage = storage;
    }

    @Override
    public int getLines() {
        return this.count;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(@Nonnull String logString) throws InterruptedException {
        this.lock.lock();
        try {
            while (count == capacity) {
                this.notFull.await();
            }
            this.buffer[tail] = logString;
            this.tail = (tail + 1) % this.capacity;
            this.count++;
            notEmpty.signal();
        } finally {
            this.lock.unlock();
        }
    }

    public void flush() throws InterruptedException, IOException {
        this.lock.lock();
        try {
            while (count == 0) {
                this.notEmpty.await();
            }
            if (Math.min(this.count, LIMIT) > 0) {
                int readCount = 0;
                final List<String> lines = new ArrayList<>();
                while (readCount < Math.min(this.count, LIMIT)) {
                    lines.add(this.buffer[(this.head + readCount) % this.capacity]);
                    readCount++;
                }
                this.storage.flush(new Content(lines));
                this.head = (this.head + readCount) % this.capacity;
                this.count -= readCount;
                System.out.println(readCount);
                notFull.signal();
            }
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException, InterruptedException {
        while (this.count != 0) {
            this.flush();
        }
        this.storage.close();
    }
}
