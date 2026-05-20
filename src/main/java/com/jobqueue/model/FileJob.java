package com.jobqueue.model;

public class FileJob extends Job {
    private final FileOperation operation;
    private final String sourcePath;
    private final String destinationPath;

    public FileJob(String id, JobPriority priority, FileOperation operation, String sourcePath, String destinationPath) {
        super(id, priority);
        this.operation = operation;
        this.sourcePath = sourcePath;
        this.destinationPath = destinationPath;
    }

    @Override
    public JobResult execute(JobContext context) {
        System.out.println("Performing File Operation: " + operation + " from " + sourcePath + " to " + destinationPath);
        return new JobResult(getId(), true, "File operation completed", null);
    }

    public FileOperation getOperation() { return operation; }
    public String getSourcePath() { return sourcePath; }
    public String getDestinationPath() { return destinationPath; }
}
