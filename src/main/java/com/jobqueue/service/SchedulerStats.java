package com.jobqueue.service;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

public class SchedulerStats {
    private final IntegerProperty totalJobs = new SimpleIntegerProperty(0);
    private final IntegerProperty completedJobs = new SimpleIntegerProperty(0);
    private final IntegerProperty failedJobs = new SimpleIntegerProperty(0);

    public void incrementTotalJobs() {
        totalJobs.set(totalJobs.get() + 1);
    }

    public void incrementCompletedJobs() {
        completedJobs.set(completedJobs.get() + 1);
    }

    public void incrementFailedJobs() {
        failedJobs.set(failedJobs.get() + 1);
    }

    public void initialize(int total, int completed, int failed) {
        totalJobs.set(total);
        completedJobs.set(completed);
        failedJobs.set(failed);
    }

    public IntegerProperty totalJobsProperty() { return totalJobs; }
    public IntegerProperty completedJobsProperty() { return completedJobs; }
    public IntegerProperty failedJobsProperty() { return failedJobs; }
}
