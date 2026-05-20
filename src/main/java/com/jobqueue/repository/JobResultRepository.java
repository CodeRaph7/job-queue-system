package com.jobqueue.repository;

import com.jobqueue.model.JobResult;
import java.util.List;

public interface JobResultRepository {
    void save(JobResult result);
    JobResult findByJobId(String jobId);
}
