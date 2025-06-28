package com.vinay.lld.logger.storage;

import com.vinay.lld.logger.model.Content;

import javax.annotation.Nonnull;
import java.io.Closeable;
import java.io.IOException;

public interface Storage extends Closeable {

    /**
     * Flushes the content to the underlying Storage entity.
     * @param content - Complex object that has all the information that has to be logged to the
     *                underlying.
     */
    void flush(@Nonnull Content content) throws IOException;
}
