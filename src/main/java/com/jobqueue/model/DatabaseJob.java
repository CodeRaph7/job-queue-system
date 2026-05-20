package com.jobqueue.model;

public class DatabaseJob extends Job {
    private final QueryType queryType;
    private final String sql;

    public DatabaseJob(String id, JobPriority priority, QueryType queryType, String sql) {
        super(id, priority);
        this.queryType = queryType;
        this.sql = sql;
    }

    @Override
    public JobResult execute(JobContext context) {
        System.out.println("Executing DB Query [" + queryType + "]: " + sql);
        return new JobResult(getId(), true, "Query executed", 10);
    }

    public QueryType getQueryType() { return queryType; }
    public String getSql() { return sql; }
}
