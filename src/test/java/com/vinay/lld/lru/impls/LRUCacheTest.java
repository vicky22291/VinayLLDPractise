package com.vinay.lld.lru.impls;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.IntSummaryStatistics;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class LRUCacheTest {
    @Test
    public void testSingleThread() {
        final int capacity = 10;
        final LRUCache<Integer, Integer> cache = new LRUCache<>(capacity);
        for (int i = 0; i <= 100; i++) {
            cache.put(i, i);
            Assertions.assertEquals(i, cache.get(i));
            System.out.println(String.format("%d == %d", i, cache.get(i)));
            if (i >= capacity) {
                Assertions.assertNull(cache.get(i - 10));
                System.out.println(String.format("%d is not present", i - 10));
            }
        }
    }

    @Test
    public void testMultipleThreads() {
        final int capacity = 1000;
        final ExecutorService executors = Executors.newFixedThreadPool(10);
        final LRUCache<String, String> cache = new LRUCache<>(capacity);
        IntStream.range(1, 100).forEach(j -> {
            executors.execute(new Runnable() {
                @Override
                public void run() {
                    int i = 0;
                    for (; i <= 1000; i++) {
                        final String val = String.format("%d-%d", j, i);
                        cache.put(val, val);
                        Assertions.assertEquals(val, cache.get(val));
                        System.out.println(String.format("%s == %s", val, cache.get(val)));
                    }
                    Assertions.assertEquals(i, 1001);
                }
            });
        });
        executors.shutdown();
        try {
            if (!executors.awaitTermination(60, TimeUnit.SECONDS)) { // Wait up to 60 seconds
                executors.shutdownNow(); // Force shutdown if tasks don't complete
            }
        } catch (InterruptedException e) {
            executors.shutdownNow();
            Thread.currentThread().interrupt(); // Restore interrupt status
        }
        System.out.println("ExecutorService shut down.");
    }
}
