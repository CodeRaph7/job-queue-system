-- Create jobs table
CREATE TABLE IF NOT EXISTS jobs (
    id VARCHAR(255),
    priority VARCHAR(50),
    status VARCHAR(50),
    type VARCHAR(50),
    data VARCHAR(1000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    role VARCHAR(20) DEFAULT 'USER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create job_results table
CREATE TABLE IF NOT EXISTS job_results (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    job_id VARCHAR(255),
    success BOOLEAN,
    message TEXT,
    data TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (job_id) REFERENCES jobs(id)
);

-- Insert admin users
INSERT OR IGNORE INTO users (username, password, email, role)
VALUES ('admin', 'admin123', 'admin@system.com', 'ADMIN');


-- Insert sample jobs with different states
INSERT OR IGNORE INTO jobs (id, priority, status, type, data)
VALUES ('SAMPLE-001', 'HIGH', 'PENDING', 'PrintJob', 'Sample pending job');

INSERT OR IGNORE INTO jobs (id, priority, status, type, data)
VALUES ('SAMPLE-002', 'MEDIUM', 'COMPLETED', 'EmailJob', 'Sample completed job');

INSERT OR IGNORE INTO jobs (id, priority, status, type, data)
VALUES ('SAMPLE-003', 'LOW', 'FAILED', 'FileJob', 'Sample failed job');

INSERT OR IGNORE INTO jobs (id, priority, status, type, data)
VALUES ('SAMPLE-004', 'CRITICAL', 'PENDING', 'NotificationJob', 'Urgent pending job');


CREATE INDEX IF NOT EXISTS idx_jobs_status ON jobs(status);
CREATE INDEX IF NOT EXISTS idx_jobs_priority ON jobs(priority);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_job_results_job_id ON job_results(job_id);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
