package com.vinay.lld.cjs.model;

import com.vinay.lld.cjs.model.enums.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class Job {
    private final Schedule schedule;
    private final String id = UUID.randomUUID().toString();
    private final Task task;
    private final LocalDateTime scheduledTime;
    private JobStatus status;
}
