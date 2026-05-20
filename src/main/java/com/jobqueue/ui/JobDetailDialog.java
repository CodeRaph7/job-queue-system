package com.jobqueue.ui;

import com.jobqueue.model.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
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
        HBox header = new HBox();
        header.getStyleClass().add("header-bar");
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(job.getId());
        title.getStyleClass().add("app-name");
        header.getChildren().add(title);

        // Body
        VBox body = new VBox(16);
        body.setPadding(new Insets(20));

        body.getChildren().add(buildSection("Job", buildCoreFields(job)));
        body.getChildren().add(buildSection("Execution", buildExecutionFields(job)));
        body.getChildren().add(buildSection("Type Details", buildTypeFields(job)));

        if (result != null) {
            body.getChildren().add(buildSection("Result", buildResultFields(result)));
        }

        // Footer
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(12, 20, 16, 20));
        footer.setStyle("-fx-border-color: #30363d transparent transparent transparent; -fx-border-width: 1 0 0 0;");
        Button closeBtn = new Button("Close");
        closeBtn.getStyleClass().add("btn-primary");
        closeBtn.setOnAction(e -> dialog.close());
        footer.getChildren().add(closeBtn);

        root.getChildren().addAll(header, body, footer);

        Scene scene = new Scene(root, 500, 580);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        dialog.setScene(scene);
    }

    private VBox buildSection(String heading, GridPane grid) {
        Label label = new Label(heading.toUpperCase());
        label.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 11px; -fx-font-weight: 700; -fx-padding: 0 0 6 0;");

        VBox box = new VBox(4);
        box.getStyleClass().add("card");
        box.getChildren().addAll(label, new Separator(), grid);
        VBox.setMargin(new Separator(), new Insets(0, 0, 6, 0));
        return box;
    }

    private GridPane buildCoreFields(Job job) {
        GridPane g = new GridPane();
        g.setHgap(16);
        g.setVgap(8);
        g.setPadding(new Insets(6, 0, 0, 0));

        int row = 0;
        addRow(g, row++, "Job ID",    job.getId());
        addRow(g, row++, "Type",      job.getClass().getSimpleName());
        addRow(g, row++, "Priority",  job.getPriority().name(), priorityColor(job.getPriority()));
        addRow(g, row++, "Status",    job.getStatus().name(), statusColor(job.getStatus()));
        addRow(g, row,   "Created",   job.getCreationTime() != null ? job.getCreationTime().format(FMT) : "—");
        return g;
    }

    private GridPane buildExecutionFields(Job job) {
        GridPane g = new GridPane();
        g.setHgap(16);
        g.setVgap(8);
        g.setPadding(new Insets(6, 0, 0, 0));

        int row = 0;
        addRow(g, row++, "Started",   job.getStartedAt()   != null ? job.getStartedAt().format(FMT)   : "—");
        addRow(g, row++, "Completed", job.getCompletedAt() != null ? job.getCompletedAt().format(FMT) : "—");
        addRow(g, row++, "Duration",  job.getDurationMs()  != null ? formatDuration(job.getDurationMs()) : "—");
        addRow(g, row++, "Retries",   String.valueOf(job.getRetryCount()));
        addRow(g, row,   "Error",     job.getErrorMessage() != null ? job.getErrorMessage() : "—",
                job.getErrorMessage() != null ? "#f85149" : null);
        return g;
    }

    private GridPane buildTypeFields(Job job) {
        GridPane g = new GridPane();
        g.setHgap(16);
        g.setVgap(8);
        g.setPadding(new Insets(6, 0, 0, 0));

        if (job instanceof EmailJob ej) {
            addRow(g, 0, "Recipient", ej.getRecipient());
            addRow(g, 1, "Subject",   ej.getSubject());
            addRow(g, 2, "Body",      ej.getBody());
        } else if (job instanceof NotificationJob nj) {
            addRow(g, 0, "Message", nj.getMessage());
        } else if (job instanceof FileJob fj) {
            addRow(g, 0, "Operation",   fj.getOperation() != null ? fj.getOperation().name() : "—");
            addRow(g, 1, "Source",      fj.getSourcePath() != null ? fj.getSourcePath() : "—");
            addRow(g, 2, "Destination", fj.getDestinationPath() != null ? fj.getDestinationPath() : "—");
        } else if (job instanceof DatabaseJob dj) {
            addRow(g, 0, "Query Type", dj.getQueryType().name());
            addRow(g, 1, "SQL",        dj.getSql());
        } else if (job instanceof PrintJob pj) {
            addRow(g, 0, "Content", pj.getContent());
        } else {
            addRow(g, 0, "Details", "—");
        }
        return g;
    }

    private GridPane buildResultFields(JobResult result) {
        GridPane g = new GridPane();
        g.setHgap(16);
        g.setVgap(8);
        g.setPadding(new Insets(6, 0, 0, 0));

        addRow(g, 0, "Success", result.isSuccess() ? "Yes" : "No",
                result.isSuccess() ? "#3fb950" : "#f85149");
        addRow(g, 1, "Message", result.getMessage() != null ? result.getMessage() : "—");
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
        v.setMaxWidth(320);
        String color = valueColor != null ? valueColor : "#e6edf3";
        v.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px;");

        g.add(k, 0, row);
        g.add(v, 1, row);
    }

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
