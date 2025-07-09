package com.vinay.lld.rl.model;

public class LimitExceededException extends Exception {
    public LimitExceededException(String message) {
        super(message);
    }
}
