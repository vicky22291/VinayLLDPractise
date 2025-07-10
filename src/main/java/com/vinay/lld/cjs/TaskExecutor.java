package com.vinay.lld.cjs;

import com.vinay.lld.cjs.model.Job;
import com.vinay.lld.cjs.model.Task;

import javax.annotation.Nonnull;
import java.util.concurrent.*;

public class TaskExecutor {
    private final ExecutorService executors;

    public TaskExecutor(final int poolSize) {
        this.executors = Executors.newFixedThreadPool(poolSize);
    }

    public void submit(@Nonnull final Job job) {
        CompletableFuture.runAsync(job.getTask(), executors)
                .thenAccept(_ -> this.notifyJobManagerAboutCompletion(job));
    }

    private void notifyJobManagerAboutCompletion(final Job job) {
        //  Introduce a queue that can be used for communication between task executor and job manager
    }

    public void shutdown() {
        executors.shutdown();
        try {
            if (!executors.awaitTermination(10, TimeUnit.SECONDS)) { // Wait up to 60 seconds
                executors.shutdownNow(); // Force shutdown if tasks don't complete
            }
        } catch (InterruptedException e) {
            executors.shutdownNow();
            Thread.currentThread().interrupt(); // Restore interrupt status
        }
    }
}
