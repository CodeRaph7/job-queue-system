package com.jobqueue.ui;

import com.jobqueue.model.Job;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.List;

public class JobTableModel {
    private final ObservableList<Job> jobs;

    public JobTableModel() {
        this.jobs = FXCollections.observableArrayList();
    }

    public void updateJobs(List<Job> newJobs) {
        jobs.clear();
        jobs.addAll(newJobs);
    }

    public ObservableList<Job> getJobs() {
        return jobs;
    }

    public void addJob(Job job) {
        jobs.add(job);
    }
}
