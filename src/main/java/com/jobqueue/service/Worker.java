package com.jobqueue.service;

import com.jobqueue.model.Job;
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
        // Skip jobs already resolved (e.g. manually marked before the worker picked them up)
        JobStatus current = job.getStatus();
        if (current == JobStatus.COMPLETED || current == JobStatus.FAILED
                || current == JobStatus.CANCELLED) {
            return;
        }

        // Set to RUNNING — job stays here until admin/user manually marks it complete or failed
        job.setStartedAt(LocalDateTime.now());
        scheduler.notifyObservers(job, JobStatus.RUNNING);
    }

    public void stopWorker() {
        this.running = false;
        this.interrupt();
    }
}
