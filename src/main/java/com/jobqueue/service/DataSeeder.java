package com.jobqueue.service;

import com.jobqueue.model.*;
import com.jobqueue.repository.JobRepository;
import com.jobqueue.repository.UserRepository;

import java.time.LocalDateTime;

public class DataSeeder {

    public static void seed(UserRepository userRepo, JobRepository jobRepo) {
        seedUsers(userRepo);
        seedJobs(jobRepo);
    }

    private static void seedUsers(UserRepository userRepo) {
        createUser(userRepo, "admin",   "Admin@2024", "admin@company.com",   Role.ADMIN);
        createUser(userRepo, "manager", "Admin@2024", "manager@company.com", Role.ADMIN);
        createUser(userRepo, "alice",   "User@2024",  "alice@company.com",   Role.USER);
        createUser(userRepo, "bob",     "User@2024",  "bob@company.com",     Role.USER);
        createUser(userRepo, "charlie", "User@2024",  "charlie@company.com", Role.USER);
    }

    private static void createUser(UserRepository repo, String username, String password,
                                   String email, Role role) {
        if (!repo.existsByUsername(username)) {
            repo.save(new User(username, password, email, role));
            System.out.println("Seeded user: " + username + " [" + role + "]");
        }
    }

    private static void seedJobs(JobRepository repo) {
        LocalDateTime now = LocalDateTime.now();

        // JOB-001 — PENDING, submitted 2 hours ago
        EmailJob job001 = new EmailJob("JOB-001", JobPriority.CRITICAL,
                "ceo@company.com", "Q4 Financial Report Ready",
                "The Q4 report has been generated and is pending your review.");
        job001.setCreationTime(now.minusHours(2));
        saveJob(repo, job001, JobStatus.PENDING);

        // JOB-002 — RUNNING, started 8 minutes ago
        DatabaseJob job002 = new DatabaseJob("JOB-002", JobPriority.HIGH,
                QueryType.SELECT,
                "SELECT * FROM analytics WHERE date > '2024-01-01'");
        job002.setCreationTime(now.minusHours(1).minusMinutes(10));
        job002.setStartedAt(now.minusMinutes(8));
        saveJob(repo, job002, JobStatus.RUNNING);

        // JOB-003 — COMPLETED, finished 45 minutes ago
        PrintJob job003 = new PrintJob("JOB-003", JobPriority.MEDIUM,
                "Monthly invoice batch — 247 records");
        job003.setCreationTime(now.minusHours(3));
        job003.setStartedAt(now.minusMinutes(50));
        job003.setCompletedAt(now.minusMinutes(45));
        job003.setDurationMs(312_000L);
        saveJob(repo, job003, JobStatus.COMPLETED);

        // JOB-004 — FAILED, failed 30 minutes ago
        FileJob job004 = new FileJob("JOB-004", JobPriority.LOW,
                FileOperation.COPY, "/data/exports/report_2024.csv", "/archive/2024/");
        job004.setCreationTime(now.minusHours(4));
        job004.setStartedAt(now.minusMinutes(35));
        job004.setCompletedAt(now.minusMinutes(30));
        job004.setDurationMs(7_400L);
        job004.setErrorMessage("Destination path not found: /archive/2024/");
        saveJob(repo, job004, JobStatus.FAILED);

        // JOB-005 — PENDING, submitted 15 minutes ago
        NotificationJob job005 = new NotificationJob("JOB-005", JobPriority.HIGH,
                "Scheduled maintenance window: Saturday 02:00–04:00 UTC");
        job005.setCreationTime(now.minusMinutes(15));
        saveJob(repo, job005, JobStatus.PENDING);

        // JOB-006 — COMPLETED, finished 1 hour ago
        EmailJob job006 = new EmailJob("JOB-006", JobPriority.MEDIUM,
                "alerts@company.com", "System Health Check",
                "All services are operational. No incidents detected.");
        job006.setCreationTime(now.minusHours(5));
        job006.setStartedAt(now.minusHours(1).minusMinutes(5));
        job006.setCompletedAt(now.minusHours(1));
        job006.setDurationMs(2_100L);
        saveJob(repo, job006, JobStatus.COMPLETED);

        // JOB-007 — PENDING, just submitted
        DatabaseJob job007 = new DatabaseJob("JOB-007", JobPriority.CRITICAL,
                QueryType.UPDATE,
                "UPDATE sessions SET expired = 1 WHERE last_active < NOW() - INTERVAL 30 MINUTE");
        job007.setCreationTime(now.minusMinutes(3));
        saveJob(repo, job007, JobStatus.PENDING);
    }

    private static void saveJob(JobRepository repo, Job job, JobStatus status) {
        if (repo.findById(job.getId()) == null) {
            job.setStatus(status);
            repo.save(job);
            System.out.println("Seeded job: " + job.getId() + " [" + status + "]");
        }
    }
}
