package com.jobqueue.model;

public class EmailJob extends Job {
    private final String recipient;
    private final String subject;
    private final String body;

    public EmailJob(String id, JobPriority priority, String recipient, String subject, String body) {
        super(id, priority);
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
    }

    @Override
    public JobResult execute(JobContext context) {
        System.out.println("Sending Email to: " + recipient + " | Subject: " + subject);
        return new JobResult(getId(), true, "Email sent successfully", null);
    }

    public String getRecipient() { return recipient; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
}
