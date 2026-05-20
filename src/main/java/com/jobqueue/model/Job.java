package com.jobqueue.model;

import java.time.LocalDateTime;

public abstract class Job implements Executable, Schedulable, Comparable<Job> {
    private final String id;
    private JobStatus status;
    private final JobPriority priority;
    private LocalDateTime creationTime;       // non-final so DAO can restore original value

    // Execution metadata — persisted to DB
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String errorMessage;
    private int retryCount = 0;
    private Long durationMs;

    // Transient — held in memory during execution so PersistenceObserver can flush it
    private transient JobResult lastResult;

    protected Job(String id, JobPriority priority) {
        this.id = id;
        this.priority = priority;
        this.status = JobStatus.PENDING;
        this.creationTime = LocalDateTime.now();
    }

    @Override
    public int compareTo(Job other) {
        return Integer.compare(other.priority.ordinal(), this.priority.ordinal());
    }

    @Override
    public void schedule(LocalDateTime time) {
        System.out.println("Job " + id + " scheduled for " + time);
    }

    @Override
    public boolean cancel() {
        this.status = JobStatus.CANCELLED;
        return true;
    }

    @Override
    public abstract JobResult execute(JobContext context);

    // ── Core getters ──────────────────────────────────────────────

    public String getId()                   { return id; }
    public JobStatus getStatus()            { return status; }
    public JobPriority getPriority()        { return priority; }
    public LocalDateTime getCreationTime()  { return creationTime; }

    // ── Core setters ──────────────────────────────────────────────

    public void setStatus(JobStatus status)                 { this.status = status; }
    public void setCreationTime(LocalDateTime creationTime) { this.creationTime = creationTime; }

    // ── Execution metadata getters ────────────────────────────────

    public LocalDateTime getStartedAt()   { return startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public String getErrorMessage()       { return errorMessage; }
    public int getRetryCount()            { return retryCount; }
    public Long getDurationMs()           { return durationMs; }
    public JobResult getLastResult()      { return lastResult; }

    // ── Execution metadata setters ────────────────────────────────

    public void setStartedAt(LocalDateTime startedAt)     { this.startedAt = startedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public void setErrorMessage(String errorMessage)      { this.errorMessage = errorMessage; }
    public void setRetryCount(int retryCount)             { this.retryCount = retryCount; }
    public void setDurationMs(Long durationMs)            { this.durationMs = durationMs; }
    public void setLastResult(JobResult lastResult)       { this.lastResult = lastResult; }
}
