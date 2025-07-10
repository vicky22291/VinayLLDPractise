package com.vinay.lld.cjs;

import com.google.common.base.Preconditions;
import com.vinay.lld.cjs.model.Schedule;

import javax.annotation.Nonnull;
import java.util.concurrent.ConcurrentHashMap;

public class ScheduleManager {
    private final ConcurrentHashMap<String, Schedule> schedules;
    private final JobManager jobManager;

    public ScheduleManager(@Nonnull final JobManager jobManager) {
        this.schedules = new ConcurrentHashMap<>();
        this.jobManager = jobManager;
    }

    public void register(final Schedule schedule) {
        Preconditions.checkArgument(!this.schedules.contains(schedule.getId()), "Looks like the schedule is already please check and recreate.");
        this.schedules.put(schedule.getId(), schedule);
        this.jobManager.manageNewSchedule(schedule);
    }

    public void deregister(final Schedule schedule) {
        Preconditions.checkArgument(this.schedules.contains(schedule.getId()), "Schedule is not present.");
        this.schedules.remove(schedule.getId());
    }
}
