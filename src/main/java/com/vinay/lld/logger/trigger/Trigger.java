package com.vinay.lld.logger.trigger;

public interface Trigger extends Runnable {

    /**
     * Function that runs continuously and triggers other flows.
     */
    void run();
}
