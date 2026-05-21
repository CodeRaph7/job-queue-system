package com.jobqueue.ui;

import com.jobqueue.model.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;

public class JobDetailDialog {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MMM d, yyyy  HH:mm:ss");

    private final Stage dialog;

    public JobDetailDialog(Stage owner, Job job, JobResult result) {
        dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Job Details — " + job.getId());
        dialog.setResizable(false);

        VBox root = new VBox(0);
        root.getStyleClass().add("main-layout");

        // Header
        HBox header = new HBox(10);
        header.getStyleClass().add("header-bar");
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(job.getId());
        title.getStyleClass().add("app-name");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label statusBadge = new Label(job.getStatus().name());
        statusBadge.setStyle("-fx-text-fill: " + statusColor(job.getStatus()) +
                "; -fx-font-weight: 700; -fx-font-size: 12px;");
        header.getChildren().addAll(title, spacer, statusBadge);

        // Scrollable body
        VBox body = new VBox(14);
        body.setPadding(new Insets(20));

        // ── Task card (prominent, employee-facing) ──────────────────────────
        body.getChildren().add(buildTaskCard(job));

        // ── Technical details ───────────────────────────────────────────────
        body.getChildren().add(buildSection("Info", buildCoreFields(job)));
        body.getChildren().add(buildSection("Execution", buildExecutionFields(job)));

        if (result != null) {
            body.getChildren().add(buildSection("Result", buildResultFields(result)));
        }

        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #0d1117; -fx-background-color: #0d1117; -fx-border-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        // Footer
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(12, 20, 16, 20));
        footer.setStyle("-fx-border-color: #30363d transparent transparent transparent; -fx-border-width: 1 0 0 0;");
        Button closeBtn = new Button("Close");
        closeBtn.getStyleClass().add("btn-primary");
        closeBtn.setOnAction(e -> dialog.close());
        footer.getChildren().add(closeBtn);

        root.getChildren().addAll(header, scroll, footer);

        Scene scene = new Scene(root, 520, 620);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        dialog.setScene(scene);
    }

    // ── Task card — what the employee needs to know ───────────────────────────

    private VBox buildTaskCard(Job job) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: #161b22; -fx-background-radius: 8;" +
                      "-fx-border-color: #1f6feb; -fx-border-width: 1; -fx-border-radius: 8;" +
                      "-fx-padding: 16;");

        Label heading = new Label("TASK");
        heading.setStyle("-fx-text-fill: #1f6feb; -fx-font-size: 11px; -fx-font-weight: 700;");

        Label typeLabel = new Label(jobTypeTitle(job));
        typeLabel.setStyle("-fx-text-fill: #e6edf3; -fx-font-size: 15px; -fx-font-weight: 700;");
        typeLabel.setWrapText(true);

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #30363d;");

        Label desc = new Label(buildTaskDescription(job));
        desc.setStyle("-fx-text-fill: #c9d1d9; -fx-font-size: 13px;");
        desc.setWrapText(true);
        desc.setMaxWidth(460);

        card.getChildren().addAll(heading, typeLabel, sep, desc);
        return card;
    }

    private String jobTypeTitle(Job job) {
        if (job instanceof EmailJob ej)        return "Send Email — " + ej.getSubject();
        if (job instanceof NotificationJob)    return "Deliver Notification";
        if (job instanceof FileJob fj)         return "File " + (fj.getOperation() != null ? fj.getOperation().name() : "Operation");
        if (job instanceof PrintJob)           return "Print Document";
        if (job instanceof DatabaseJob dj)     return "Database " + dj.getQueryType().name() + " Query";
        return "Job";
    }

    private String buildTaskDescription(Job job) {
        if (job instanceof EmailJob ej) {
            return "To: " + ej.getRecipient() + "\n\n" + ej.getBody();
        }
        if (job instanceof NotificationJob nj) {
            return nj.getMessage();
        }
        if (job instanceof FileJob fj) {
            String op = fj.getOperation() != null ? fj.getOperation().name() : "COPY";
            return op + " file\nFrom: " + orDash(fj.getSourcePath())
                   + "\nTo:   " + orDash(fj.getDestinationPath());
        }
        if (job instanceof PrintJob pj) {
            return pj.getContent();
        }
        if (job instanceof DatabaseJob dj) {
            return dj.getSql();
        }
        return "No description available.";
    }

    // ── Info / execution sections ─────────────────────────────────────────────

    private VBox buildSection(String heading, GridPane grid) {
        Label label = new Label(heading.toUpperCase());
        label.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 11px; -fx-font-weight: 700; -fx-padding: 0 0 6 0;");

        VBox box = new VBox(4);
        box.getStyleClass().add("card");
        box.getChildren().addAll(label, new Separator(), grid);
        return box;
    }

    private GridPane buildCoreFields(Job job) {
        GridPane g = grid();
        int row = 0;
        addRow(g, row++, "Job ID",   job.getId());
        addRow(g, row++, "Type",     job.getClass().getSimpleName());
        addRow(g, row++, "Priority", job.getPriority().name(), priorityColor(job.getPriority()));
        addRow(g, row++, "Status",   job.getStatus().name(), statusColor(job.getStatus()));
        addRow(g, row,   "Created",  job.getCreationTime() != null ? job.getCreationTime().format(FMT) : "—");
        return g;
    }

    private GridPane buildExecutionFields(Job job) {
        GridPane g = grid();
        int row = 0;
        addRow(g, row++, "Started",   job.getStartedAt()   != null ? job.getStartedAt().format(FMT)   : "—");
        addRow(g, row++, "Completed", job.getCompletedAt() != null ? job.getCompletedAt().format(FMT) : "—");
        addRow(g, row++, "Duration",  job.getDurationMs()  != null ? formatDuration(job.getDurationMs()) : "—");
        addRow(g, row++, "Retries",   String.valueOf(job.getRetryCount()));
        addRow(g, row,   "Error",     orDash(job.getErrorMessage()),
               job.getErrorMessage() != null ? "#f85149" : null);
        return g;
    }

    private GridPane buildResultFields(JobResult result) {
        GridPane g = grid();
        addRow(g, 0, "Success", result.isSuccess() ? "Yes" : "No",
               result.isSuccess() ? "#3fb950" : "#f85149");
        addRow(g, 1, "Message", orDash(result.getMessage()));
        return g;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private GridPane grid() {
        GridPane g = new GridPane();
        g.setHgap(16);
        g.setVgap(8);
        g.setPadding(new Insets(6, 0, 0, 0));
        return g;
    }

    private void addRow(GridPane g, int row, String key, String value) {
        addRow(g, row, key, value, null);
    }

    private void addRow(GridPane g, int row, String key, String value, String valueColor) {
        Label k = new Label(key);
        k.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 12px; -fx-min-width: 90;");

        Label v = new Label(value != null ? value : "—");
        v.setWrapText(true);
        v.setMaxWidth(360);
        String color = valueColor != null ? valueColor : "#e6edf3";
        v.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px;");

        g.add(k, 0, row);
        g.add(v, 1, row);
    }

    private String orDash(String s) { return (s != null && !s.isBlank()) ? s : "—"; }

    private String priorityColor(JobPriority p) {
        return switch (p) {
            case CRITICAL -> "#f85149";
            case HIGH     -> "#d29922";
            case MEDIUM   -> "#388bfd";
            default       -> "#8b949e";
        };
    }

    private String statusColor(JobStatus s) {
        return switch (s) {
            case RUNNING   -> "#388bfd";
            case COMPLETED -> "#3fb950";
            case FAILED    -> "#f85149";
            case PENDING   -> "#d29922";
            default        -> "#6e7681";
        };
    }

    private String formatDuration(long ms) {
        if (ms < 1000)  return ms + " ms";
        if (ms < 60000) return String.format("%.1f s", ms / 1000.0);
        return String.format("%d m %d s", ms / 60000, (ms % 60000) / 1000);
    }

    public void show() { dialog.show(); }
}
