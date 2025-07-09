package com.vinay.lld.rl;

import com.vinay.lld.rl.model.ClientConfig;
import com.vinay.lld.rl.model.LimitExceededException;
import com.vinay.lld.rl.model.Request;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class RateLimiterTest {
    private RateLimiter rateLimiter;
    @BeforeEach
    public void setup() {
        this.rateLimiter = new RateLimiter();
    }

    @Test
    public void testWithNoClientHeader() throws LimitExceededException, InterruptedException {
        try {
            this.rateLimiter.validate(new Request(new HashMap<>(), "Dummy String"));
        } catch (final IllegalArgumentException e) {
            System.out.println("Got the right exception");
        }
    }

    private HashMap<String, String> getSampleClientHeaders() {
        final HashMap<String, String> headers = new HashMap<>();
        headers.put("x-client-name", "SampleClient");
        return headers;
    }

    @Test
    public void testWithNoRegisteredClient() throws LimitExceededException, InterruptedException {
        this.rateLimiter.validate(new Request(getSampleClientHeaders(), "Dummy String"));
    }

    @Test
    public void testWithRegisteredValidRequest() throws LimitExceededException, InterruptedException {
        this.rateLimiter.register(new ClientConfig(
                "SampleClient", 1, 1, 1, TimeUnit.SECONDS
        ));

        final Request request = new Request(getSampleClientHeaders(), "DummyString");
        this.rateLimiter.validate(request);
        this.rateLimiter.registerResponseRecieved(request);
    }

    @Test
    public void testWithRegisteredInvalidRequest() throws InterruptedException {
        this.rateLimiter.register(new ClientConfig(
                "SampleClient", 2, 3, 1, TimeUnit.SECONDS
        ));

        int i = 0;
        try {
            for (; i < 5; i++) {
                final Request request = new Request(getSampleClientHeaders(), "DummyString");
                try {
                    this.rateLimiter.validate(request);
                } catch (final LimitExceededException e) {
                    System.out.println("Received expected Exception.");
                    throw e;
                } finally {
                    this.rateLimiter.registerResponseRecieved(request);
                }
            }
        } catch (final LimitExceededException e) {
            // Do Nothing
        }
        Assertions.assertEquals(i, 3);
    }
}
