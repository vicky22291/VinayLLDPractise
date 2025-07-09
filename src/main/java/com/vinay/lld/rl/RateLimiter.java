package com.vinay.lld.rl;

import com.google.common.base.Preconditions;
import com.vinay.lld.rl.model.ClientConfig;
import com.vinay.lld.rl.model.ClientState;
import com.vinay.lld.rl.model.LimitExceededException;
import com.vinay.lld.rl.model.Request;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

public class RateLimiter {
    private final ConcurrentHashMap<String, ClientConfig> clientConfigs;
    private final ConcurrentHashMap<String, ClientState> clientStates;

    public RateLimiter() {
        this.clientConfigs = new ConcurrentHashMap<>();
        this.clientStates = new ConcurrentHashMap<>();
    }

    public void register(final ClientConfig clientConfig) {
        synchronized (this) {
            this.clientConfigs.putIfAbsent(clientConfig.getName(), clientConfig);
            this.clientStates.putIfAbsent(clientConfig.getName(), new ClientState(
                    new Semaphore(clientConfig.getMaxConcurrency()), new LinkedBlockingQueue<>(clientConfig.getLimit())
            ));
        }
    }

    public void validate(final Request request) throws LimitExceededException, InterruptedException {
        Preconditions.checkArgument(request.getHeaders().containsKey("x-client-name"), "Missing client name header, please add client name to x-client-name header.");
        final String client = request.getHeaders().get("x-client-name");
        final ClientState state = this.clientStates.get(client);

        // ClientStates is still being setup so optimistically not failing the request.
        if (Objects.isNull(state)) return;

        this.adjustWindow(client);
        state.getConcurrentRequests().acquireUninterruptibly();
        if (state.getRequestTimes().size() == this.clientConfigs.get(client).getLimit()) {
            state.getConcurrentRequests().release();
            throw new LimitExceededException("Please retry after sometime.");
        } else {
            state.getRequestTimes().put(LocalDateTime.now());
        }
    }

    public void registerResponseRecieved(final Request request) {
        Preconditions.checkArgument(request.getHeaders().containsKey("x-client-name"), "Missing client name header, please add client name to x-client-name header.");
        final String client = request.getHeaders().get("x-client-name");
        final ClientState state = this.clientStates.get(client);

        // ClientStates is still being setup so optimistically not failing the request.
        if (Objects.isNull(state)) return;

        state.getConcurrentRequests().release();
    }

    private void adjustWindow(final String client) {
        final LinkedBlockingQueue<LocalDateTime> q = this.clientStates.get(client).getRequestTimes();
        final LocalDateTime currentTime = LocalDateTime.now();
        while(Objects.nonNull(q.peek()) &&
                q.peek().isBefore(currentTime.minusSeconds(this.clientConfigs.get(client).getWindowSize()))) {
            q.poll();
        }
    }
}
