package com.jobqueue.ui;

import com.jobqueue.model.*;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.UUID;

public class AddJobDialog {
    private final Stage stage;
    private Job createdJob;

    public AddJobDialog(Stage owner) {
        stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("New Job");
        stage.setResizable(false);

        VBox root = new VBox(14);
        root.setStyle("-fx-background-color: #0d1117; -fx-padding: 24;");
        root.setMinWidth(460);

        Label title = new Label("Create New Job");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #e6edf3;");

        Separator sep = new Separator();

        // ── Type selector ──────────────────────────────────────────────────────
        ComboBox<String> typeCb = new ComboBox<>();
        typeCb.getItems().addAll("Email", "Database", "File", "Print", "Notification");
        typeCb.setValue("Email");
        typeCb.setMaxWidth(Double.MAX_VALUE);

        // ── Priority selector ──────────────────────────────────────────────────
        ComboBox<JobPriority> prioCb = new ComboBox<>();
        prioCb.getItems().addAll(JobPriority.values());
        prioCb.setValue(JobPriority.MEDIUM);
        prioCb.setMaxWidth(Double.MAX_VALUE);

        // ── Dynamic fields container ───────────────────────────────────────────
        VBox dynamicFields = new VBox(10);
        dynamicFields.setPadding(new Insets(4, 0, 0, 0));

        // Email fields
        TextField recipientField = new TextField();
        recipientField.setPromptText("e.g. manager@company.com");
        TextField subjectField   = new TextField();
        subjectField.setPromptText("Email subject");
        TextArea  bodyArea       = new TextArea();
        bodyArea.setPromptText("Email body...");
        bodyArea.setPrefRowCount(3);
        bodyArea.setWrapText(true);

        // Database fields
        ComboBox<QueryType> queryTypeCb = new ComboBox<>();
        queryTypeCb.getItems().addAll(QueryType.values());
        queryTypeCb.setValue(QueryType.SELECT);
        queryTypeCb.setMaxWidth(Double.MAX_VALUE);
        TextArea sqlArea = new TextArea();
        sqlArea.setPromptText("SELECT * FROM users WHERE ...");
        sqlArea.setPrefRowCount(3);
        sqlArea.setWrapText(true);

        // File fields
        ComboBox<FileOperation> fileOpCb = new ComboBox<>();
        fileOpCb.getItems().addAll(FileOperation.values());
        fileOpCb.setValue(FileOperation.COPY);
        fileOpCb.setMaxWidth(Double.MAX_VALUE);
        TextField srcField = new TextField();
        srcField.setPromptText("/path/to/source");
        TextField dstField = new TextField();
        dstField.setPromptText("/path/to/destination");

        // Print field
        TextArea printArea = new TextArea();
        printArea.setPromptText("Document content to print...");
        printArea.setPrefRowCount(4);
        printArea.setWrapText(true);

        // Notification field
        TextArea notifArea = new TextArea();
        notifArea.setPromptText("Notification message...");
        notifArea.setPrefRowCount(3);
        notifArea.setWrapText(true);

        // ── Populate fields based on selected type ─────────────────────────────
        Runnable populate = () -> {
            dynamicFields.getChildren().clear();
            switch (typeCb.getValue()) {
                case "Email" -> dynamicFields.getChildren().addAll(
                    labeled("To (Recipient)", recipientField),
                    labeled("Subject", subjectField),
                    labeled("Body", bodyArea));
                case "Database" -> dynamicFields.getChildren().addAll(
                    labeled("Query Type", queryTypeCb),
                    labeled("SQL", sqlArea));
                case "File" -> dynamicFields.getChildren().addAll(
                    labeled("Operation", fileOpCb),
                    labeled("Source Path", srcField),
                    labeled("Destination Path", dstField));
                case "Print" -> dynamicFields.getChildren().add(
                    labeled("Content", printArea));
                case "Notification" -> dynamicFields.getChildren().add(
                    labeled("Message", notifArea));
            }
        };

        typeCb.setOnAction(e -> populate.run());
        populate.run();

        // ── Error / submit ─────────────────────────────────────────────────────
        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #f85149; -fx-font-size: 12px;");
        errorLabel.setWrapText(true);

        Button createBtn = new Button("Add to Queue");
        createBtn.setMaxWidth(Double.MAX_VALUE);
        createBtn.setStyle("-fx-background-color: #1f6feb; -fx-text-fill: white;" +
                           "-fx-font-weight: 700; -fx-font-size: 13px; -fx-padding: 8 0;");

        createBtn.setOnAction(e -> {
            errorLabel.setText("");
            try {
                String id = "JOB-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
                JobPriority prio = prioCb.getValue();
                Job job = buildJob(id, prio, typeCb.getValue(),
                        recipientField, subjectField, bodyArea,
                        queryTypeCb, sqlArea,
                        fileOpCb, srcField, dstField,
                        printArea, notifArea);
                createdJob = job;
                stage.close();
            } catch (IllegalArgumentException ex) {
                errorLabel.setText(ex.getMessage());
            }
        });

        root.getChildren().addAll(
            title, sep,
            labeled("Job Type", typeCb),
            labeled("Priority", prioCb),
            dynamicFields,
            errorLabel, createBtn);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        stage.setScene(scene);
    }

    private Job buildJob(String id, JobPriority prio, String type,
                         TextField recipient, TextField subject, TextArea body,
                         ComboBox<QueryType> queryType, TextArea sql,
                         ComboBox<FileOperation> fileOp, TextField src, TextField dst,
                         TextArea print, TextArea notif) {
        return switch (type) {
            case "Email" -> {
                require(recipient.getText(), "Recipient is required.");
                require(subject.getText(),   "Subject is required.");
                yield new EmailJob(id, prio,
                        recipient.getText().trim(), subject.getText().trim(), body.getText().trim());
            }
            case "Database" -> {
                require(sql.getText(), "SQL query is required.");
                yield new DatabaseJob(id, prio, queryType.getValue(), sql.getText().trim());
            }
            case "File" -> {
                require(src.getText(), "Source path is required.");
                if (fileOp.getValue() != FileOperation.DELETE)
                    require(dst.getText(), "Destination path is required.");
                yield new FileJob(id, prio, fileOp.getValue(),
                        src.getText().trim(), dst.getText().trim());
            }
            case "Print" -> {
                require(print.getText(), "Content is required.");
                yield new PrintJob(id, prio, print.getText().trim());
            }
            case "Notification" -> {
                require(notif.getText(), "Message is required.");
                yield new NotificationJob(id, prio, notif.getText().trim());
            }
            default -> throw new IllegalArgumentException("Unknown type: " + type);
        };
    }

    private void require(String value, String error) {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException(error);
    }

    private VBox labeled(String label, Control control) {
        VBox box = new VBox(5);
        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 12px;");
        control.setMaxWidth(Double.MAX_VALUE);
        box.getChildren().addAll(lbl, control);
        return box;
    }

    public Job showDialog() {
        stage.showAndWait();
        return createdJob;
    }
}
