package com.jobqueue.service;

import com.jobqueue.model.Job;
import java.util.PriorityQueue;

public class JobQueue {
    private final PriorityQueue<Job> queue;

    public JobQueue() {
        this.queue = new PriorityQueue<>();
    }

    public synchronized void addJob(Job job) {
        queue.add(job);
        notifyAll();
    }

    public synchronized Job getNextJob() throws InterruptedException {
        while (queue.isEmpty()) {
            wait();
        }
        return queue.poll();
    }

    public synchronized boolean isEmpty() {
        return queue.isEmpty();
    }

    public synchronized int size() {
        return queue.size();
    }
}
