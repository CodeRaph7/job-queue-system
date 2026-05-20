package com.jobqueue.model;

public class JobResult {
    private final String jobId;
    private final boolean success;
    private final String message;
    private final Object data;

    public JobResult(String jobId, boolean success, String message, Object data) {
        this.jobId = jobId;
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public String getJobId() { return jobId; }
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public Object getData() { return data; }
}
