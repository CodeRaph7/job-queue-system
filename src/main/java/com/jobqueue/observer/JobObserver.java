package com.jobqueue.observer;

import com.jobqueue.model.Job;
import com.jobqueue.model.JobStatus;

public interface JobObserver {
    void onJobUpdate(Job job, JobStatus status);
}
