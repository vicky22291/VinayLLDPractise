package com.vinay.lld.cjs;

import com.vinay.lld.cjs.model.Job;
import com.vinay.lld.cjs.model.Schedule;
import com.vinay.lld.cjs.model.enums.JobStatus;
import com.vinay.lld.cjs.model.enums.ScheduleType;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;

public class JobManager implements Runnable {
    private final PriorityBlockingQueue<Job> jobs;
    private final TaskExecutor taskExecutor;

    public JobManager (@Nonnull final TaskExecutor taskExecutor) {
        final Comparator<Job> jobComparator = Comparator.comparing(Job::getScheduledTime);
        this.jobs = new PriorityBlockingQueue<>(100, jobComparator);
        this.taskExecutor = taskExecutor;
    }

    public void manageNewSchedule(@Nonnull final Schedule schedule) {
        final LocalDateTime scheduledTime = schedule.getType().getNextSchedule(schedule.getConfig());
        final Job nextJob = new Job(schedule, schedule.getTask(), scheduledTime, JobStatus.SCHEDULED);
        this.jobs.add(nextJob);
    }

    public void manageJobComplete(@Nonnull final Job job) {
        // This is function is used for each Job from the Queue that is shared between Task Manager and Current Job Manager.
        if (job.getSchedule().getType() != ScheduleType.FIXED) {
            this.manageNewSchedule(job.getSchedule());
        }
    }

    @Override
    public void run() {
        while (true) {
            if (this.jobs.isEmpty()) {
                this.currentThreadSleep(1000);
            }
            final Job topJob = this.jobs.peek();
            final LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(topJob.getScheduledTime())) {
                final long delay = Duration.between(topJob.getScheduledTime(), now).toMillis();
                this.currentThreadSleep(delay);
            }
            this.taskExecutor.submit(topJob);
        }
    }

    private void currentThreadSleep(final long delay) {
        try {
            Thread.currentThread().sleep(delay);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
