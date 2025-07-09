package com.vinay.lld.rl.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

@Getter
@AllArgsConstructor
public class ClientState {
    private final Semaphore concurrentRequests;
    private final LinkedBlockingQueue<LocalDateTime> requestTimes;
}
