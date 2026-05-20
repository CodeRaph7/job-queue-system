package com.jobqueue.repository;

import com.jobqueue.model.JobResult;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class JobResultRepositoryDAOImpl implements JobResultRepository {
    private final DatabaseConnection dbConnection;

    public JobResultRepositoryDAOImpl(DatabaseConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public void save(JobResult result) {
        try {
            Connection conn = dbConnection.getConnection();
            String sql = "INSERT OR REPLACE INTO job_results (job_id, success, message, data) VALUES (?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, result.getJobId());
            stmt.setBoolean(2, result.isSuccess());
            stmt.setString(3, result.getMessage());
            stmt.setString(4, result.getData() != null ? result.getData().toString() : null);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public JobResult findByJobId(String jobId) {
        try {
            Connection conn = dbConnection.getConnection();
            String sql = "SELECT * FROM job_results WHERE job_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, jobId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new JobResult(
                    rs.getString("job_id"),
                    rs.getBoolean("success"),
                    rs.getString("message"),
                    rs.getString("data")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
