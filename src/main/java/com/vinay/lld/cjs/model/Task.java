package com.vinay.lld.cjs.model;

import lombok.AllArgsConstructor;

import java.util.Map;

public @AllArgsConstructor
abstract class Task implements Runnable {
    private final Map<String, Object> input;
}
