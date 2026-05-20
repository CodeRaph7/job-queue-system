package com.jobqueue.observer;

import com.jobqueue.model.Job;
import com.jobqueue.model.JobStatus;
import com.jobqueue.repository.JobRepository;
import com.jobqueue.repository.JobResultRepository;

public class PersistenceObserver implements JobObserver {
    private final JobRepository jobRepository;
    private final JobResultRepository jobResultRepository;

    public PersistenceObserver(JobRepository jobRepo, JobResultRepository resultRepo) {
        this.jobRepository = jobRepo;
        this.jobResultRepository = resultRepo;
    }

    @Override
    public void onJobUpdate(Job job, JobStatus status) {
        System.out.println("PersistenceObserver: Saving job " + job.getId() + " with status " + status);
        jobRepository.save(job);

        // Persist the execution result whenever a job reaches a terminal state
        if ((status == JobStatus.COMPLETED || status == JobStatus.FAILED)
                && job.getLastResult() != null) {
            jobResultRepository.save(job.getLastResult());
        }
    }
}
