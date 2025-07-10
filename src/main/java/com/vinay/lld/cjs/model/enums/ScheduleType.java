package com.vinay.lld.cjs.model.enums;

import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.time.LocalDateTime;
import java.util.Map;

public enum ScheduleType {
    FIXED {
        @Override
        public LocalDateTime getNextSchedule(@NotNull Map<String, Object> config) {
            return null;
        }
    },
    FIXED_RATE {
        @Override
        public LocalDateTime getNextSchedule(@NotNull Map<String, Object> config) {
            return null;
        }
    },
    FIXED_DELAY {
        @Override
        public LocalDateTime getNextSchedule(@NotNull Map<String, Object> config) {
            return null;
        }
    };

    public abstract LocalDateTime getNextSchedule(@Nonnull final Map<String, Object> config);
}
