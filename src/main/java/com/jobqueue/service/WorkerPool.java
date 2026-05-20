package com.jobqueue.service;

import java.util.ArrayList;
import java.util.List;

public class WorkerPool {
    private final List<Worker> workers;
    private final int poolSize;
    private final JobScheduler scheduler;
    private final JobQueue queue;

    public WorkerPool(int size, JobScheduler scheduler, JobQueue queue) {
        this.poolSize = size;
        this.scheduler = scheduler;
        this.queue = queue;
        this.workers = new ArrayList<>();
    }

    public void start() {
        for (int i = 0; i < poolSize; i++) {
            Worker worker = new Worker(scheduler, queue);
            worker.setName("JobWorker-" + i);
            workers.add(worker);
            worker.start();
        }
    }

    public void shutdown() {
        for (Worker worker : workers) {
            worker.stopWorker();
        }
        workers.clear();
    }
}
