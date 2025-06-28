package com.vinay.lld.logger.trigger.impl;

import com.vinay.lld.logger.service.LoggingService;
import com.vinay.lld.logger.trigger.Trigger;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ScheduledTrigger implements Trigger {
    private final LoggingService loggingService;
    private final AtomicBoolean isRunning = new AtomicBoolean(true);
    private final ScheduledExecutorService executor;
    private final int intervalSeconds;

    @Inject
    public ScheduledTrigger(@Nonnull final LoggingService loggingService, @Named("logFlushInterval") int intervalSeconds) {
        this.intervalSeconds = intervalSeconds;
        this.executor = Executors.newSingleThreadScheduledExecutor();
        this.loggingService = loggingService;
    }

    @Override
    public void run() {
        executor.scheduleAtFixedRate(this::performTask,  0, this.intervalSeconds, TimeUnit.MILLISECONDS);
    }

    private void performTask() {
        if (!isRunning.get()) return;

        try {
            if (this.loggingService.getLines() > 100000) {
                this.loggingService.flush();
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        isRunning.set(false);
        executor.shutdown();
        try {
            if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
            this.loggingService.close();
        } catch (Exception e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
