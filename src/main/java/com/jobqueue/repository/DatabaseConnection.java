package com.jobqueue.repository;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Properties;

public class DatabaseConnection {
    private static DatabaseConnection instance;
    private Connection connection;

    private DatabaseConnection() {
        try {
            new java.io.File("data").mkdirs();
            Class.forName("org.sqlite.JDBC");
            Properties props = loadConfig();
            String url = props.getProperty("db.sqlite.url");
            connection = DriverManager.getConnection(url);
            connection.createStatement().execute("PRAGMA foreign_keys = ON");
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

    private void initDb() throws SQLException {
        createSchema();
        seedUsers();
        seedSampleJobs();
        System.out.println("DB: Database initialized");
    }

    private void createSchema() throws SQLException {
        exec("CREATE TABLE IF NOT EXISTS jobs (" +
             "  id            VARCHAR(255) PRIMARY KEY," +
             "  priority      VARCHAR(50)," +
             "  status        VARCHAR(50)," +
             "  type          VARCHAR(50)," +
             "  data          TEXT," +
             "  created_at    TIMESTAMP," +
             "  started_at    TIMESTAMP," +
             "  completed_at  TIMESTAMP," +
             "  error_message TEXT," +
             "  retry_count   INTEGER DEFAULT 0," +
             "  duration_ms   INTEGER," +
             "  assigned_to   VARCHAR(50)" +
             ")");

        exec("CREATE TABLE IF NOT EXISTS users (" +
             "  id         INTEGER PRIMARY KEY AUTOINCREMENT," +
             "  username   VARCHAR(50) UNIQUE NOT NULL," +
             "  password   VARCHAR(255) NOT NULL," +
             "  email      VARCHAR(100)," +
             "  role       VARCHAR(20) DEFAULT 'USER'," +
             "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
             ")");

        exec("CREATE TABLE IF NOT EXISTS job_results (" +
             "  id         INTEGER PRIMARY KEY AUTOINCREMENT," +
             "  job_id     VARCHAR(255) UNIQUE," +
             "  success    BOOLEAN," +
             "  message    TEXT," +
             "  data       TEXT," +
             "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
             "  FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE" +
             ")");

        exec("CREATE INDEX IF NOT EXISTS idx_jobs_status        ON jobs(status)");
        exec("CREATE INDEX IF NOT EXISTS idx_jobs_priority      ON jobs(priority)");
        exec("CREATE INDEX IF NOT EXISTS idx_users_username     ON users(username)");
        exec("CREATE INDEX IF NOT EXISTS idx_job_results_job_id ON job_results(job_id)");

        try { exec("ALTER TABLE jobs ADD COLUMN assigned_to VARCHAR(50)"); }
        catch (SQLException ignored) {}
    }

    private void seedUsers() throws SQLException {
        String sql = "INSERT OR IGNORE INTO users (username, password, email, role) VALUES (?, ?, ?, ?)";
        PreparedStatement stmt = connection.prepareStatement(sql);

        Object[][] users = {
            {"CEO",       "CEO123",  "ceo@company.com",       "ADMIN"},
            {"Adam",      "Adam123", "adam@company.com",      "USER"},
            {"mouhamed",  "mouha18", "mouhamed@company.com",  "USER"},
        };

        for (Object[] u : users) {
            stmt.setString(1, (String) u[0]);
            stmt.setString(2, (String) u[1]);
            stmt.setString(3, (String) u[2]);
            stmt.setString(4, (String) u[3]);
            stmt.executeUpdate();
        }
    }

    private void seedSampleJobs() throws SQLException {
        exec("DELETE FROM job_results WHERE job_id LIKE 'JOB-%'");
        exec("DELETE FROM jobs WHERE id LIKE 'JOB-%'");

        LocalDateTime now = LocalDateTime.now();

        String sql = "INSERT INTO jobs " +
                     "(id, priority, status, type, data, created_at, started_at, completed_at, duration_ms, error_message, assigned_to)" +
                     " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement s = connection.prepareStatement(sql);

        insertJob(s, "JOB-001", "CRITICAL", "COMPLETED", "EmailJob",
            "{\"recipient\":\"ceo@company.com\",\"subject\":\"Q4 Financial Report Ready\",\"body\":\"The Q4 report has been generated and is pending your review.\"}",
            now.minusHours(5), now.minusHours(5).plusSeconds(10), now.minusHours(5).plusSeconds(13), 3200L, null, "Adam");

        insertJob(s, "JOB-002", "HIGH", "PENDING", "DatabaseJob",
            "{\"queryType\":\"SELECT\",\"sql\":\"SELECT * FROM users WHERE active = 1\"}",
            now.minusHours(2), null, null, null, null, "Adam");

        insertJob(s, "JOB-003", "MEDIUM", "COMPLETED", "PrintJob",
            "{\"content\":\"Monthly invoice batch — 247 records\"}",
            now.minusHours(2), now.minusHours(2).plusSeconds(30), now.minusHours(2).plusMinutes(5), 312000L, null, "Adam");

        insertJob(s, "JOB-004", "LOW", "PENDING", "FileJob",
            "{\"operation\":\"COPY\",\"sourcePath\":\"/data/exports/report_2024.csv\",\"destinationPath\":\"/archive/2024/\"}",
            now.minusHours(1).minusMinutes(30), null, null, null, null, "Adam");

        insertJob(s, "JOB-005", "HIGH", "FAILED", "NotificationJob",
            "{\"message\":\"Scheduled maintenance window: Saturday 02:00–04:00 UTC\"}",
            now.minusMinutes(90), now.minusMinutes(89), now.minusMinutes(88), 1800L,
            "Push service unavailable", "Adam");

        insertJob(s, "JOB-006", "CRITICAL", "PENDING", "EmailJob",
            "{\"recipient\":\"alerts@company.com\",\"subject\":\"System Health Check\",\"body\":\"Please verify all services are operational and report status.\"}",
            now.minusMinutes(45), null, null, null, null, "mouhamed");

        insertJob(s, "JOB-007", "HIGH", "COMPLETED", "DatabaseJob",
            "{\"queryType\":\"UPDATE\",\"sql\":\"UPDATE sessions SET expired = 1 WHERE last_active < NOW() - INTERVAL 30 MINUTE\"}",
            now.minusHours(3), now.minusHours(3).plusSeconds(5), now.minusHours(3).plusSeconds(12), 7100L, null, "mouhamed");

        insertJob(s, "JOB-008", "MEDIUM", "PENDING", "PrintJob",
            "{\"content\":\"Q4 Sales Summary Report — 18 pages\"}",
            now.minusMinutes(20), null, null, null, null, "mouhamed");

        insertJob(s, "JOB-009", "LOW", "FAILED", "FileJob",
            "{\"operation\":\"MOVE\",\"sourcePath\":\"/tmp/upload_batch_42.zip\",\"destinationPath\":\"/data/archive/\"}",
            now.minusHours(1), now.minusHours(1).plusSeconds(3), now.minusHours(1).plusSeconds(8), 5200L,
            "Destination directory not found", "mouhamed");

        String rSql = "INSERT OR IGNORE INTO job_results (job_id, success, message) VALUES (?, ?, ?)";
        PreparedStatement r = connection.prepareStatement(rSql);

        Object[][] results = {
            {"JOB-001", true,  "Email sent successfully"},
            {"JOB-003", true,  "Print successful"},
            {"JOB-005", false, "Push service unavailable"},
            {"JOB-007", true,  "Query executed successfully"},
            {"JOB-009", false, "Destination directory not found"},
        };
        for (Object[] res : results) {
            r.setString(1, (String) res[0]);
            r.setBoolean(2, (boolean) res[1]);
            r.setString(3, (String) res[2]);
            r.executeUpdate();
        }
    }

    private void insertJob(PreparedStatement s, String id, String priority, String status,
                           String type, String data, LocalDateTime created,
                           LocalDateTime started, LocalDateTime completed,
                           Long duration, String error, String assignedTo) throws SQLException {
        s.setString(1, id);
        s.setString(2, priority);
        s.setString(3, status);
        s.setString(4, type);
        s.setString(5, data);
        s.setString(6, created   != null ? created.toString()    : null);
        s.setString(7, started   != null ? started.toString()    : null);
        s.setString(8, completed != null ? completed.toString()  : null);
        if (duration != null) s.setLong(9, duration);
        else                  s.setNull(9, java.sql.Types.INTEGER);
        s.setString(10, error);
        s.setString(11, assignedTo);
        s.executeUpdate();
    }

    private void exec(String sql) throws SQLException {
        connection.createStatement().execute(sql);
    }
}
