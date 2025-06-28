package com.vinay.lld.logger.model;

import javax.annotation.Nonnull;
import java.util.List;

public record Content(List<String> logLines) {

    public Content(@Nonnull final List<String> logLines) {
        this.logLines = logLines;
    }
}
