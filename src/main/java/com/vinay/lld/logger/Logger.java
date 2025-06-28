package com.vinay.lld.logger;

import com.vinay.lld.logger.service.LoggingService;

import javax.annotation.Nonnull;
import javax.inject.Inject;

public class Logger implements AutoCloseable {

    private final LoggingService loggingService;

    @Inject
    public Logger(@Nonnull final LoggingService loggingService) {
        this.loggingService = loggingService;
    }

    void log(@Nonnull final String line) throws InterruptedException {
        this.loggingService.write(line);
    }

    @Override
    public void close() throws Exception {
        this.loggingService.close();
    }
}
