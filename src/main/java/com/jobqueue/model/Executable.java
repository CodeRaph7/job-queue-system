package com.jobqueue.model;

public interface Executable {
    JobResult execute(JobContext context);
}
