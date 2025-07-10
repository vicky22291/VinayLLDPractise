package com.vinay.lld.cjs.model;

import com.vinay.lld.cjs.model.enums.ScheduleType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class Schedule {
    private final String id = UUID.randomUUID().toString();
    private final ScheduleType type;
    private final String owner;
    private final String description;
    private final Map<String, Object> config;
    private final Task task;
}