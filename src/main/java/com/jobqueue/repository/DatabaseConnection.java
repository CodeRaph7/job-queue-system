package com.jobqueue.repository;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnection {
    private static DatabaseConnection instance;
    private Connection connection;

    private DatabaseConnection() {
        try {
            Class.forName("org.sqlite.JDBC");
            Properties props = loadConfig();
            String url = props.getProperty("db.sqlite.url");
            connection = DriverManager.getConnection(url);
            initDb();
            System.out.println("DB: Connected to SQLite database");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize database connection", e);
        }
    }

    private Properties loadConfig() {
        Properties props = new Properties();
        try {
            FileInputStream fis = new FileInputStream("config.properties");
            props.load(fis);
            fis.close();
        } catch (IOException e) {
            System.out.println("DB: config.properties not found, using default SQLite configuration");
            props.setProperty("db.type", "sqlite");
            props.setProperty("db.sqlite.url", "jdbc:sqlite:./data/jobqueue.db");
        }
        return props;
    }

    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }

    private void initDb() {
        try {
            // Create tables
            String sql = "CREATE TABLE IF NOT EXISTS jobs (id VARCHAR(255), priority VARCHAR(50), status VARCHAR(50), type VARCHAR(50), data VARCHAR(1000), created_at TIMESTAMP, started_at TIMESTAMP, completed_at TIMESTAMP, error_message TEXT, retry_count INTEGER DEFAULT 0, duration_ms INTEGER)";
            connection.createStatement().execute(sql);

            sql = "CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY AUTOINCREMENT, username VARCHAR(50) UNIQUE NOT NULL, password VARCHAR(255) NOT NULL, email VARCHAR(100), role VARCHAR(20) DEFAULT 'USER', created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
            connection.createStatement().execute(sql);

            sql = "CREATE TABLE IF NOT EXISTS job_results (id INTEGER PRIMARY KEY AUTOINCREMENT, job_id VARCHAR(255), success BOOLEAN, message TEXT, data TEXT, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (job_id) REFERENCES jobs(id))";
            connection.createStatement().execute(sql);

            // Insert admin users
            String[] adminUsers = { "admin", "admin1", "admin2", "admin3" };
            for (String admin : adminUsers) {
                sql = "INSERT OR IGNORE INTO users (username, password, email, role) VALUES ('" + admin
                        + "', 'admin123', '" + admin + "@system.com', 'ADMIN')";
                connection.createStatement().execute(sql);
            }

            // Insert sample jobs with proper JSON data (set to terminal states so they
            // don't auto-execute) - use DELETE first to ensure clean state
            sql = "DELETE FROM jobs WHERE id LIKE 'SAMPLE-%'";
            connection.createStatement().execute(sql);

            String[] sampleJobs = {
                    "('SAMPLE-001', 'HIGH', 'COMPLETED', 'PrintJob', '{\"content\":\"Sample pending job\"}')",
                    "('SAMPLE-002', 'MEDIUM', 'COMPLETED', 'EmailJob', '{\"recipient\":\"test@example.com\",\"subject\":\"Test\",\"body\":\"Sample completed job\"}')",
                    "('SAMPLE-003', 'LOW', 'FAILED', 'FileJob', '{\"operation\":\"COPY\",\"sourcePath\":\"/tmp/source\",\"destinationPath\":\"/tmp/dest\"}')",
                    "('SAMPLE-004', 'CRITICAL', 'FAILED', 'NotificationJob', '{\"message\":\"Urgent pending job\"}')"
            };
            for (String jobData : sampleJobs) {
                sql = "INSERT INTO jobs (id, priority, status, type, data) VALUES " + jobData;
                connection.createStatement().execute(sql);
            }

            // Create indexes
            sql = "CREATE INDEX IF NOT EXISTS idx_jobs_status ON jobs(status)";
            connection.createStatement().execute(sql);
            sql = "CREATE INDEX IF NOT EXISTS idx_jobs_priority ON jobs(priority)";
            connection.createStatement().execute(sql);
            sql = "CREATE INDEX IF NOT EXISTS idx_users_username ON users(username)";
            connection.createStatement().execute(sql);
            sql = "CREATE INDEX IF NOT EXISTS idx_job_results_job_id ON job_results(job_id)";
            connection.createStatement().execute(sql);
            sql = "CREATE INDEX IF NOT EXISTS idx_users_role ON users(role)";
            connection.createStatement().execute(sql);

            System.out.println("DB: Database initialized with admin users and sample jobs");
        } catch (SQLException e) {
            System.out.println("DB: Error initializing database: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
