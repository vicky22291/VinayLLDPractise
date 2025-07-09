package com.vinay.lld.rl.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.concurrent.TimeUnit;

@AllArgsConstructor
@Getter
public class ClientConfig {
    private final String name;
    private final int maxConcurrency;
    private final int limit;
    private final int windowSize;
    private final TimeUnit unit;
}
