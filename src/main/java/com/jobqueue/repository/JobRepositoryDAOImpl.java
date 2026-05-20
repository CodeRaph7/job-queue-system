package com.jobqueue.repository;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jobqueue.model.*;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class JobRepositoryDAOImpl implements JobRepository {
    private final DatabaseConnection dbConnection;

    public JobRepositoryDAOImpl(DatabaseConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public void save(Job job) {
        try {
            Connection conn = dbConnection.getConnection();
            String sql = """
                    INSERT OR REPLACE INTO jobs
                      (id, priority, status, type, data,
                       created_at, started_at, completed_at,
                       error_message, retry_count, duration_ms)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, job.getId());
            stmt.setString(2, job.getPriority().name());
            stmt.setString(3, job.getStatus().name());
            stmt.setString(4, job.getClass().getSimpleName());
            stmt.setString(5, serializeJobData(job));
            stmt.setString(6, ts(job.getCreationTime()));
            stmt.setString(7, ts(job.getStartedAt()));
            stmt.setString(8, ts(job.getCompletedAt()));
            stmt.setString(9, job.getErrorMessage());
            stmt.setInt(10, job.getRetryCount());
            if (job.getDurationMs() != null)
                stmt.setLong(11, job.getDurationMs());
            else
                stmt.setNull(11, Types.INTEGER);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String serializeJobData(Job job) {
        JsonObject obj = new JsonObject();
        if (job instanceof EmailJob ej) {
            obj.addProperty("recipient", ej.getRecipient());
            obj.addProperty("subject", ej.getSubject());
            obj.addProperty("body", ej.getBody());
        } else if (job instanceof NotificationJob nj) {
            obj.addProperty("message", nj.getMessage());
        } else if (job instanceof FileJob fj) {
            obj.addProperty("operation", fj.getOperation() != null ? fj.getOperation().name() : "");
            obj.addProperty("sourcePath", fj.getSourcePath() != null ? fj.getSourcePath() : "");
            obj.addProperty("destinationPath", fj.getDestinationPath() != null ? fj.getDestinationPath() : "");
        } else if (job instanceof PrintJob pj) {
            obj.addProperty("content", pj.getContent());
        } else if (job instanceof DatabaseJob dj) {
            obj.addProperty("queryType", dj.getQueryType().name());
            obj.addProperty("sql", dj.getSql());
        }
        return obj.toString();
    }

    @Override
    public Job findById(String id) {
        try {
            PreparedStatement stmt = dbConnection.getConnection()
                    .prepareStatement("SELECT * FROM jobs WHERE id = ?");
            stmt.setString(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next())
                return createJobFromResultSet(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Job> findAll() {
        List<Job> jobs = new ArrayList<>();
        try {
            System.out.println("JobRepository: Loading jobs from database");
            ResultSet rs = dbConnection.getConnection()
                    .createStatement().executeQuery("SELECT * FROM jobs ORDER BY created_at DESC");
            while (rs.next()) {
                Job job = createJobFromResultSet(rs);
                System.out.println("JobRepository: Loaded job " + job.getId() + " with status " + job.getStatus());
                jobs.add(job);
            }
            System.out.println("JobRepository: Total jobs loaded: " + jobs.size());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return jobs;
    }

    @Override
    public void delete(String id) {
        try {
            PreparedStatement stmt = dbConnection.getConnection()
                    .prepareStatement("DELETE FROM jobs WHERE id = ?");
            stmt.setString(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ── Deserialization ──────────────────────────────────────────────────────

    private Job createJobFromResultSet(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        JobPriority prio = JobPriority.valueOf(rs.getString("priority"));
        JobStatus status = JobStatus.valueOf(rs.getString("status"));
        String type = rs.getString("type");
        String dataStr = rs.getString("data");

        JsonObject data = (dataStr != null && !dataStr.isBlank())
                ? JsonParser.parseString(dataStr).getAsJsonObject()
                : new JsonObject();

        Job job = switch (type) {
            case "EmailJob" -> new EmailJob(id, prio,
                    getString(data, "recipient", ""),
                    getString(data, "subject", ""),
                    getString(data, "body", ""));
            case "NotificationJob" -> new NotificationJob(id, prio,
                    getString(data, "message", ""));
            case "FileJob" -> new FileJob(id, prio,
                    fileOp(getString(data, "operation", "COPY")),
                    getString(data, "sourcePath", ""),
                    getString(data, "destinationPath", ""));
            case "PrintJob" -> new PrintJob(id, prio, getString(data, "content", ""));
            case "DatabaseJob" -> new DatabaseJob(id, prio,
                    QueryType.valueOf(getString(data, "queryType", "SELECT")),
                    getString(data, "sql", ""));
            default -> new PrintJob(id, prio, "Loaded from DB");
        };

        job.setStatus(status);
        parseTs(rs.getString("created_at")).ifPresent(job::setCreationTime);
        parseTs(rs.getString("started_at")).ifPresent(job::setStartedAt);
        parseTs(rs.getString("completed_at")).ifPresent(job::setCompletedAt);
        job.setErrorMessage(rs.getString("error_message"));
        job.setRetryCount(rs.getInt("retry_count"));
        long durMs = rs.getLong("duration_ms");
        if (!rs.wasNull())
            job.setDurationMs(durMs);
        return job;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String getString(JsonObject obj, String key, String def) {
        return obj.has(key) ? obj.get(key).getAsString() : def;
    }

    private FileOperation fileOp(String name) {
        return name.isBlank() ? FileOperation.COPY : FileOperation.valueOf(name);
    }

    private String ts(LocalDateTime ldt) {
        return ldt != null ? ldt.toString() : null;
    }

    private java.util.Optional<LocalDateTime> parseTs(String s) {
        if (s == null || s.isBlank())
            return java.util.Optional.empty();
        try {
            return java.util.Optional.of(LocalDateTime.parse(s));
        } catch (Exception e) {
            return java.util.Optional.empty();
        }
    }
}
