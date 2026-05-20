package com.jobqueue.repository;

import com.jobqueue.model.Job;
import java.util.List;

public interface JobRepository {
    void save(Job job);
    Job findById(String id);
    List<Job> findAll();
    void delete(String id);
}
