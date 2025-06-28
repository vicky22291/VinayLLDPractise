package com.vinay.lld.logger;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.vinay.lld.logger.modules.LoggerModule;
import com.vinay.lld.logger.service.LoggingService;
import com.vinay.lld.logger.service.impl.LoggingServiceImpl;
import com.vinay.lld.logger.storage.Storage;
import com.vinay.lld.logger.storage.impl.FileStorage;
import com.vinay.lld.logger.trigger.impl.ScheduledTrigger;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

public class Application {
    public static void main(String[] args) throws IOException, InterruptedException {
        final Storage storage = new FileStorage("Application.txt");
        final LoggingService loggingService = new LoggingServiceImpl(1000000, storage);
        final ScheduledTrigger trigger = new ScheduledTrigger(loggingService, 5);
        final Thread triggerThread = new Thread(trigger);
        triggerThread.start();
        final Logger logger = new Logger(loggingService);
        final String name = Thread.currentThread().getName();
        ExecutorService executor = Executors.newFixedThreadPool(5);

        final Instant start = Instant.now();
        System.out.println(name + " :: " + start);

        final AtomicInteger taskCount = new AtomicInteger(0);

        // Define task for threads
        Runnable task = () -> {
            String threadName = Thread.currentThread().getName() + "-" + taskCount.get();
            IntStream.range(1, 1000001).forEach(new IntConsumer() {
                @Override
                public void accept(int value) {
                    try {
                        logger.log(threadName + " :: " + value);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            taskCount.incrementAndGet();
        };

        // Submit tasks to thread pool
        for (int i = 0; i < 10; i++) {
            executor.execute(task);
        }

        // Gracefully shutdown executor
        executor.shutdown();

        while(!executor.isTerminated()) {
            Thread.sleep(100);
        }
        final Instant end = Instant.now();
        System.out.println(name + " :: " + end);
        System.out.println(name + " :: " + (end.toEpochMilli() - start.toEpochMilli()));
        triggerThread.join();
        trigger.stop();
    }
}