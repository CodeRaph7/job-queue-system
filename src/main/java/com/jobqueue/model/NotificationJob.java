package com.jobqueue.model;

public class NotificationJob extends Job {
    private final String message;

    public NotificationJob(String id, JobPriority priority, String message) {
        super(id, priority);
        this.message = message;
    }

    @Override
    public JobResult execute(JobContext context) {
        System.out.println("Notification: " + message);
        return new JobResult(getId(), true, "Notification delivered", null);
    }

    public String getMessage() { return message; }
}
