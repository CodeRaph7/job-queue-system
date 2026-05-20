package com.jobqueue.ui;

import com.jobqueue.model.Job;
import com.jobqueue.model.JobResult;
import com.jobqueue.model.JobStatus;
import com.jobqueue.model.Role;
import com.jobqueue.model.User;
import com.jobqueue.repository.JobRepository;
import com.jobqueue.repository.JobResultRepository;
import com.jobqueue.service.JobScheduler;
import com.jobqueue.service.PermissionManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MainWindow {
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("MMM d, HH:mm");

    private final JobScheduler scheduler;
    private final JobRepository jobRepository;
    private final JobResultRepository jobResultRepository;
    private final User authenticatedUser;

    private final Stage stage;
    private final JobTableModel tableModel;
    private final StatsPanel statsPanel;
    private TableView<Job> jobTable;

    public MainWindow(JobScheduler scheduler, JobRepository jobRepo,
            JobResultRepository resultRepo, User authenticatedUser) {
        this.scheduler = scheduler;
        this.jobRepository = jobRepo;
        this.jobResultRepository = resultRepo;
        this.authenticatedUser = authenticatedUser;

        this.stage = new Stage();
        this.tableModel = new JobTableModel();
        this.statsPanel = new StatsPanel(scheduler.getStats());

        loadJobsFromDatabase();
        initUI();
    }

    // ── Data loading ─────────────────────────────────────────────────────────

    private void loadJobsFromDatabase() {
        try {
            List<Job> jobs = jobRepository.findAll();
            int total = jobs.size();
            int completed = (int) jobs.stream().filter(j -> j.getStatus() == JobStatus.COMPLETED).count();
            int failed = (int) jobs.stream().filter(j -> j.getStatus() == JobStatus.FAILED).count();
            Platform.runLater(() -> scheduler.getStats().initialize(total, completed, failed));

            // Clear existing jobs before adding new ones to prevent duplicates
            tableModel.getJobs().clear();
            for (Job job : jobs)
                tableModel.addJob(job);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── UI construction ───────────────────────────────────────────────────────

    private void initUI() {
        stage.setTitle("JobQueue — " + authenticatedUser.getUsername());

        BorderPane root = new BorderPane();
        root.getStyleClass().add("main-layout");

        root.setTop(buildHeader());
        root.setCenter(buildCenter());

        Scene scene = new Scene(root, 1150, 720);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        stage.setScene(scene);
    }

    private HBox buildHeader() {
        HBox header = new HBox(12);
        header.getStyleClass().add("header-bar");
        header.setAlignment(Pos.CENTER_LEFT);

        Label appName = new Label("■ JobQueue");
        appName.getStyleClass().add("app-name");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label userLabel = new Label(authenticatedUser.getUsername());
        userLabel.getStyleClass().add("user-badge");

        Label roleLabel = new Label(authenticatedUser.getRole().name());
        roleLabel.getStyleClass().add(
                authenticatedUser.getRole() == Role.ADMIN ? "role-badge-admin" : "role-badge-user");

        Button logoutBtn = new Button("Sign out");
        logoutBtn.getStyleClass().add("btn-secondary");
        logoutBtn.setOnAction(e -> {
            stage.close();
            Platform.runLater(() -> new LoginScreen().showAndWait());
        });

        header.getChildren().addAll(appName, spacer, userLabel, roleLabel, logoutBtn);
        return header;
    }

    private VBox buildCenter() {
        HBox statsBar = new HBox();
        statsBar.getChildren().add(statsPanel);
        statsBar.setStyle("-fx-background-color: #0d1117; -fx-padding: 16 20 0 20;");

        VBox content = new VBox(12);
        content.setPadding(new Insets(16, 20, 20, 20));
        VBox.setVgrow(content, Priority.ALWAYS);

        content.getChildren().addAll(buildSectionHeader(), buildTable());

        VBox center = new VBox(0);
        center.getChildren().addAll(statsBar, content);
        VBox.setVgrow(content, Priority.ALWAYS);
        return center;
    }

    private HBox buildSectionHeader() {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Active Jobs");
        title.getStyleClass().add("section-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Details — visible to everyone
        Button detailsBtn = new Button("Details");
        detailsBtn.getStyleClass().add("btn-secondary");
        detailsBtn.setOnAction(e -> openDetails());

        // Mark Complete — ADMIN + USER (CHANGE_JOB_STATUS)
        Button completeBtn = new Button("✓ Mark Complete");
        completeBtn.getStyleClass().add("btn-success");
        if (PermissionManager.canChangeJobStatus(authenticatedUser)) {
            completeBtn.setOnAction(e -> markStatus(JobStatus.COMPLETED));
        } else {
            completeBtn.setDisable(true);
        }

        // Mark Failed — ADMIN + USER (CHANGE_JOB_STATUS)
        Button failBtn = new Button("✕ Mark Failed");
        failBtn.getStyleClass().add("btn-danger");
        if (PermissionManager.canChangeJobStatus(authenticatedUser)) {
            failBtn.setOnAction(e -> markStatus(JobStatus.FAILED));
        } else {
            failBtn.setDisable(true);
        }

        // Cancel — ADMIN only (CANCEL_JOB)
        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");
        if (PermissionManager.canCancelJob(authenticatedUser)) {
            cancelBtn.setOnAction(e -> {
                Job sel = selectedJob();
                if (sel == null)
                    return;
                if (sel.getStatus() == JobStatus.RUNNING) {
                    alert("Cannot cancel a running job.");
                    return;
                }
                if (sel.getStatus() == JobStatus.COMPLETED || sel.getStatus() == JobStatus.CANCELLED) {
                    alert("Job is already " + sel.getStatus().name().toLowerCase() + ".");
                    return;
                }
                sel.setStatus(JobStatus.CANCELLED);
                jobRepository.save(sel);
                jobTable.refresh();
            });
        } else {
            cancelBtn.setVisible(false);
            cancelBtn.setManaged(false);
        }

        // Delete — ADMIN only
        Button deleteBtn = new Button("Delete");
        deleteBtn.getStyleClass().add("btn-danger");
        if (PermissionManager.canDeleteJob(authenticatedUser)) {
            deleteBtn.setOnAction(e -> {
                Job sel = selectedJob();
                if (sel == null)
                    return;
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                        "Delete job " + sel.getId() + "? This cannot be undone.",
                        ButtonType.OK, ButtonType.CANCEL);
                confirm.setHeaderText(null);
                confirm.getDialogPane().getStylesheets().add(
                        getClass().getResource("/style.css").toExternalForm());
                confirm.showAndWait().filter(b -> b == ButtonType.OK).ifPresent(b -> {
                    jobRepository.delete(sel.getId());
                    tableModel.getJobs().remove(sel);
                });
            });
        } else {
            deleteBtn.setVisible(false);
            deleteBtn.setManaged(false);
        }

        // New Job — ADMIN only
        Button addBtn = new Button("+ New Job");
        addBtn.getStyleClass().add("btn-primary");
        if (PermissionManager.canCreateJob(authenticatedUser)) {
            addBtn.setOnAction(e -> {
                AddJobDialog dialog = new AddJobDialog(stage);
                Job newJob = dialog.showDialog();
                if (newJob != null) {
                    tableModel.addJob(newJob);
                    jobRepository.save(newJob);
                    scheduler.scheduleJob(newJob);
                }
            });
        } else {
            addBtn.setDisable(true);
            Tooltip.install(addBtn, new Tooltip("Only ADMIN users can create jobs"));
        }

        row.getChildren().addAll(title, spacer, detailsBtn, completeBtn, failBtn, cancelBtn, deleteBtn, addBtn);
        return row;
    }

    private TableView<Job> buildTable() {
        jobTable = new TableView<>();
        jobTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        jobTable.setPlaceholder(new Label("No jobs found"));

        TableColumn<Job, String> idCol = new TableColumn<>("Job ID");
        idCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getId()));
        idCol.setMinWidth(110);

        TableColumn<Job, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getClass().getSimpleName().replace("Job", "")));

        TableColumn<Job, String> prioCol = new TableColumn<>("Priority");
        prioCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPriority().name()));
        prioCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item);
                setStyle("-fx-text-fill: " + priorityColor(item) + "; -fx-font-weight: 600;");
            }
        });

        TableColumn<Job, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus().name()));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(statusDot(item) + item);
                setStyle("-fx-text-fill: " + statusColor(item) + "; -fx-font-weight: 600; -fx-font-size: 12px;");
            }
        });

        TableColumn<Job, String> durationCol = new TableColumn<>("Duration");
        durationCol.setCellValueFactory(d -> {
            Long ms = d.getValue().getDurationMs();
            return new SimpleStringProperty(ms != null ? formatDuration(ms) : "—");
        });

        TableColumn<Job, String> createdCol = new TableColumn<>("Created");
        createdCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getCreationTime().format(TIME_FMT)));

        jobTable.getColumns().add(idCol);
        jobTable.getColumns().add(typeCol);
        jobTable.getColumns().add(prioCol);
        jobTable.getColumns().add(statusCol);
        jobTable.getColumns().add(durationCol);
        jobTable.getColumns().add(createdCol);

        // Double-click opens detail view
        jobTable.setRowFactory(tv -> {
            TableRow<Job> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty())
                    openDetails();
            });
            return row;
        });

        filterJobsByRole();
        VBox.setVgrow(jobTable, Priority.ALWAYS);
        return jobTable;
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void markStatus(JobStatus newStatus) {
        Job sel = selectedJob();
        if (sel == null)
            return;

        if (sel.getStatus() == newStatus) {
            alert("Job is already " + newStatus.name().toLowerCase() + ".");
            return;
        }
        if (sel.getStatus() == JobStatus.CANCELLED) {
            alert("Cannot change a cancelled job.");
            return;
        }

        sel.setCompletedAt(LocalDateTime.now());
        String message = newStatus == JobStatus.COMPLETED
                ? "Manually marked as completed"
                : "Manually marked as failed";
        JobResult result = new JobResult(sel.getId(), newStatus == JobStatus.COMPLETED, message, null);
        sel.setLastResult(result);
        if (newStatus == JobStatus.FAILED)
            sel.setErrorMessage(message);

        // Save job to database immediately
        jobRepository.save(sel);

        // Also notify observers for result persistence
        scheduler.notifyObservers(sel, newStatus);

        // Refresh the table to show updated status
        jobTable.refresh();
    }

    private void openDetails() {
        Job sel = selectedJob();
        if (sel == null) {
            alert("Select a job to view details.");
            return;
        }
        JobResult result = jobResultRepository.findByJobId(sel.getId());
        new JobDetailDialog(stage, sel, result).show();
    }

    private Job selectedJob() {
        Job sel = jobTable.getSelectionModel().getSelectedItem();
        if (sel == null)
            alert("No job selected.");
        return sel;
    }

    // ── Observer callback ─────────────────────────────────────────────────────

    public void onJobUpdate(Job job, JobStatus status) {
        Platform.runLater(() -> {
            // Update the job in the tableModel instead of reloading from database
            Job existingJob = tableModel.getJobs().stream()
                    .filter(j -> j.getId().equals(job.getId()))
                    .findFirst()
                    .orElse(null);
            if (existingJob != null) {
                existingJob.setStatus(status);
                existingJob.setCompletedAt(job.getCompletedAt());
                existingJob.setLastResult(job.getLastResult());
                if (job.getErrorMessage() != null) {
                    existingJob.setErrorMessage(job.getErrorMessage());
                }
            } else {
                tableModel.addJob(job);
            }
            filterJobsByRole();
            jobTable.refresh();
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void filterJobsByRole() {
        if (authenticatedUser.getRole() == Role.USER) {
            jobTable.setItems(tableModel.getJobs().filtered(job -> job.getStatus() == JobStatus.PENDING
                    || job.getStatus() == JobStatus.COMPLETED
                    || job.getStatus() == JobStatus.FAILED));
        } else {
            jobTable.setItems(tableModel.getJobs());
        }
    }

    private void alert(String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        a.setHeaderText(null);
        a.getDialogPane().getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        a.showAndWait();
    }

    private String priorityColor(String p) {
        return switch (p) {
            case "CRITICAL" -> "#f85149";
            case "HIGH" -> "#d29922";
            case "MEDIUM" -> "#388bfd";
            default -> "#8b949e";
        };
    }

    private String statusDot(String s) {
        return switch (s) {
            case "COMPLETED" -> "● ";
            case "FAILED" -> "✕ ";
            case "RUNNING" -> "◌ ";
            case "PENDING" -> "○ ";
            case "CANCELLED" -> "– ";
            default -> "  ";
        };
    }

    private String statusColor(String s) {
        return switch (s) {
            case "RUNNING" -> "#388bfd";
            case "COMPLETED" -> "#3fb950";
            case "FAILED" -> "#f85149";
            case "PENDING" -> "#d29922";
            default -> "#6e7681";
        };
    }

    private String formatDuration(long ms) {
        if (ms < 1000)
            return ms + " ms";
        if (ms < 60000)
            return String.format("%.1f s", ms / 1000.0);
        return String.format("%d m %d s", ms / 60000, (ms % 60000) / 1000);
    }

    public void show() {
        stage.show();
    }

    public void showAndWait() {
        stage.showAndWait();
    }
}
