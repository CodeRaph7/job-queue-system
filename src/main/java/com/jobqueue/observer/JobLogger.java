package com.jobqueue.observer;

import com.jobqueue.model.Job;
import com.jobqueue.model.JobStatus;
import java.time.LocalDateTime;

public class JobLogger implements JobObserver {
    @Override
    public void onJobUpdate(Job job, JobStatus status) {
        System.out.printf("[%s] LOG: Job %s changed status to %s%n", 
                LocalDateTime.now(), job.getId(), status);
    }
}
