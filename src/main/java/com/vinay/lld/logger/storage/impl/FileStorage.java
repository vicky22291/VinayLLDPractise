package com.vinay.lld.logger.storage.impl;

import com.vinay.lld.logger.model.Content;
import com.vinay.lld.logger.storage.Storage;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Simple local file supported Storage Service. All the logging will be done to a local file.
 */
public class FileStorage implements Storage {

    private final BufferedWriter writer;

    @Inject
    public FileStorage(@Nonnull @Named("logFileName") final String fileName) throws IOException {
        this.writer = new BufferedWriter(new FileWriter(fileName));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void flush(@Nonnull Content content) throws IOException {

        final List<String> lines = content.logLines().stream().filter(s -> s != null).toList();
        if (!lines.isEmpty()) {
            final String value = String.join("\n", content.logLines().stream().filter(s -> s != null).toList());
            this.writer.write(value);
            this.writer.newLine();
        }
        this.writer.flush();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        this.writer.close();
    }
}
