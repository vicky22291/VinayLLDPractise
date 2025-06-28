package com.vinay.lld.logger.service;

import javax.annotation.Nonnull;
import java.io.IOException;

public interface LoggingService extends AutoCloseable {

    /**
     * Writes a string to the log.
     * @throws InterruptedException - Thrown when the service is interrupted for some reason.
     */
    void write(@Nonnull String logString) throws InterruptedException;

    /**
     * Flushes to buffered logs to the underlying storage service.
     * @throws InterruptedException - Thrown when the service is interrupted for some reason.
     */
    void flush() throws InterruptedException, IOException;

    int getLines();
}
