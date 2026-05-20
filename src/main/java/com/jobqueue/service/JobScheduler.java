package com.jobqueue.service;

import com.jobqueue.model.Job;
import com.jobqueue.model.JobStatus;
import com.jobqueue.observer.JobObserver;
import javafx.application.Platform;
import java.util.ArrayList;
import java.util.List;

public class JobScheduler {
    private static JobScheduler instance;
    private final JobQueue jobQueue;
    private final WorkerPool workerPool;
    private final SchedulerStats stats;
    private final List<JobObserver> observers;

    private JobScheduler() {
        this.jobQueue = new JobQueue();
        this.stats = new SchedulerStats();
        this.observers = new ArrayList<>();
        this.workerPool = new WorkerPool(4, this, jobQueue);
    }

    public static synchronized JobScheduler getInstance() {
        if (instance == null) {
            instance = new JobScheduler();
        }
        return instance;
    }

    public void scheduleJob(Job job) {
        Platform.runLater(() -> stats.incrementTotalJobs());
        jobQueue.addJob(job);
        notifyObservers(job, JobStatus.PENDING);
    }

    public void start() {
        workerPool.start();
    }

    public void stop() {
        workerPool.shutdown();
    }

    public void notifyObservers(Job job, JobStatus status) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        System.out.println("JobScheduler: Notifying observers for job " + job.getId() + " status " + status);
        System.out.println(
                "JobScheduler: Called from: " + stackTrace[2].getClassName() + "." + stackTrace[2].getMethodName());
        job.setStatus(status);

        Platform.runLater(() -> {
            if (status == JobStatus.COMPLETED)
                stats.incrementCompletedJobs();
            if (status == JobStatus.FAILED)
                stats.incrementFailedJobs();
        });

        for (JobObserver observer : observers) {
            observer.onJobUpdate(job, status);
        }
    }

    public void addObserver(JobObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(JobObserver observer) {
        observers.remove(observer);
    }

    public JobQueue getJobQueue() {
        return jobQueue;
    }

    public SchedulerStats getStats() {
        return stats;
    }
}
