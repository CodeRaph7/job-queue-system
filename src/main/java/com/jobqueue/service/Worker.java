package com.jobqueue.service;

import com.jobqueue.model.Job;
import com.jobqueue.model.JobContext;
import com.jobqueue.model.JobResult;
import com.jobqueue.model.JobStatus;

import java.time.LocalDateTime;

public class Worker extends Thread {
    private final JobScheduler scheduler;
    private final JobQueue queue;
    private volatile boolean running = true;

    public Worker(JobScheduler scheduler, JobQueue queue) {
        this.scheduler = scheduler;
        this.queue = queue;
    }

    @Override
    public void run() {
        while (running) {
            try {
                Job job = queue.getNextJob();
                if (job != null)
                    executeJob(job);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void executeJob(Job job) {
        System.out.println("Worker: Executing job " + job.getId() + " with status " + job.getStatus());

        // Skip execution if job is already in terminal state
        if (job.getStatus() == JobStatus.COMPLETED ||
                job.getStatus() == JobStatus.FAILED ||
                job.getStatus() == JobStatus.CANCELLED) {
            System.out.println("Worker: Skipping job " + job.getId() + " - already in terminal state");
            return;
        }

        job.setStartedAt(LocalDateTime.now());
        scheduler.notifyObservers(job, JobStatus.RUNNING);

        long startMs = System.currentTimeMillis();
        JobResult result;
        try {
            result = job.execute(new JobContext());
        } catch (Exception e) {
            result = new JobResult(job.getId(), false, "Unhandled exception: " + e.getMessage(), null);
        }

        job.setDurationMs(System.currentTimeMillis() - startMs);
        job.setCompletedAt(LocalDateTime.now());
        job.setLastResult(result);

        if (result.isSuccess()) {
            scheduler.notifyObservers(job, JobStatus.COMPLETED);
        } else {
            job.setErrorMessage(result.getMessage());
            scheduler.notifyObservers(job, JobStatus.FAILED);
        }
    }

    public void stopWorker() {
        this.running = false;
        this.interrupt();
    }
}
