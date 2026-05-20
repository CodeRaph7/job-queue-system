package com.jobqueue.model;

public class PrintJob extends Job {
    private final String content;

    public PrintJob(String id, JobPriority priority, String content) {
        super(id, priority);
        this.content = content;
    }

    @Override
    public JobResult execute(JobContext context) {
        System.out.println("Printing content: " + content);
        return new JobResult(getId(), true, "Print successful", null);
    }

    public String getContent() {
        return content;
    }
}
