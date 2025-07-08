
package com.vinay.lld.bbq.impls;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class BoundedQueueTest {
    @Test
    public void testSingleThread() throws InterruptedException {
        final BoundedQueue<Integer> q = new BoundedQueue<>(100);
        for (int i = 0; i < 1000; i++) {
            if (i >= 100) {
                Assertions.assertEquals(i - 100, q.dequeue());
            }
            q.enqueue(i);
        }
    }

    @Test
    public void testMultipleThread() {
        final ExecutorService producers = Executors.newFixedThreadPool(10);
        final ExecutorService consumers = Executors.newFixedThreadPool(10);
        final BoundedQueue<String> q = new BoundedQueue<>(100);
        final AtomicInteger messagesInflight = new AtomicInteger();
        IntStream.range(1, 100).forEach(j -> {
            producers.execute(new Runnable() {
                @Override
                public void run() {
                    int i = 0;
                    for (; i <= 10000; i++) {
                        final String val = String.format("%d-%d", j, i);
                        try {
                            q.enqueue(val);
                            System.out.println(messagesInflight.incrementAndGet());
                        } catch (InterruptedException e) {
                            System.out.println(e);
                        }
                    }
                }
            });
        });
        IntStream.range(1, 100).forEach(j -> {
            consumers.execute(new Runnable() {
                @Override
                public void run() {
                    int i = 0;
                    for (; i <= 10000; i++) {
                        try {
                            q.dequeue();
                            System.out.println(messagesInflight.decrementAndGet());
                        } catch (InterruptedException e) {
                            System.out.println(e);
                        }
                    }
                }
            });
        });
        producers.shutdown();
        try {
            if (!producers.awaitTermination(60, TimeUnit.SECONDS)) { // Wait up to 60 seconds
                producers.shutdownNow(); // Force shutdown if tasks don't complete
            }
        } catch (InterruptedException e) {
            producers.shutdownNow();
            Thread.currentThread().interrupt(); // Restore interrupt status
        }
        consumers.shutdown();
        try {
            if (!consumers.awaitTermination(60, TimeUnit.SECONDS)) { // Wait up to 60 seconds
                consumers.shutdownNow(); // Force shutdown if tasks don't complete
            }
        } catch (InterruptedException e) {
            consumers.shutdownNow();
            Thread.currentThread().interrupt(); // Restore interrupt status
        }
        Assertions.assertEquals(0, messagesInflight.get());
    }
}
