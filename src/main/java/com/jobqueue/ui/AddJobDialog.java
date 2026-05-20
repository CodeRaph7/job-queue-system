package com.jobqueue.ui;

import com.jobqueue.model.*;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
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

        VBox root = new VBox(16);
        root.getStyleClass().add("main-layout");
        root.setStyle("-fx-background-color: #0d1117; -fx-padding: 24;");
        root.setMinWidth(420);

        Label title = new Label("Create New Job");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #e6edf3;");

        Separator sep = new Separator();

        // Priority + Type row
        HBox typeRow = new HBox(12);
        typeRow.setAlignment(Pos.CENTER_LEFT);

        VBox priorityBox = new VBox(6);
        Label priorityLbl = new Label("Priority");
        priorityLbl.getStyleClass().add("label-secondary");
        ComboBox<JobPriority> priorityCb = new ComboBox<>();
        priorityCb.getItems().addAll(JobPriority.values());
        priorityCb.setValue(JobPriority.MEDIUM);
        priorityBox.getChildren().addAll(priorityLbl, priorityCb);

        VBox typeBox = new VBox(6);
        Label typeLbl = new Label("Job Type");
        typeLbl.getStyleClass().add("label-secondary");
        ComboBox<String> typeCb = new ComboBox<>();
        typeCb.getItems().addAll("Email", "Print", "Database", "File", "Notification");
        typeCb.setValue("Email");
        typeBox.getChildren().addAll(typeLbl, typeCb);

        typeRow.getChildren().addAll(priorityBox, typeBox);

        // Dynamic fields area
        GridPane fields = new GridPane();
        fields.setHgap(12);
        fields.setVgap(10);

        buildEmailFields(fields);

        typeCb.setOnAction(e -> {
            fields.getChildren().clear();
            switch (typeCb.getValue()) {
                case "Email"        -> buildEmailFields(fields);
                case "Print"        -> buildPrintFields(fields);
                case "Database"     -> buildDatabaseFields(fields);
                case "File"         -> buildFileFields(fields);
                case "Notification" -> buildNotificationFields(fields);
            }
        });

        Button createBtn = new Button("Add to Queue");
        createBtn.getStyleClass().add("btn-primary");
        createBtn.setMaxWidth(Double.MAX_VALUE);

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("label-error");

        createBtn.setOnAction(e -> {
            String id = "J-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            JobPriority prio = priorityCb.getValue();
            String type = typeCb.getValue();

            try {
                createdJob = buildJob(id, prio, type, fields);
                stage.close();
            } catch (IllegalArgumentException ex) {
                errorLabel.setText(ex.getMessage());
            }
        });

        root.getChildren().addAll(title, sep, typeRow, fields, createBtn, errorLabel);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        stage.setScene(scene);
    }

    // ── Field builders ───────────────────────────────────────────────

    private void buildEmailFields(GridPane g) {
        g.add(label("Recipient"), 0, 0); g.add(field("to@company.com", "email-recipient"), 1, 0);
        g.add(label("Subject"),   0, 1); g.add(field("Subject line",  "email-subject"),    1, 1);
        g.add(label("Body"),      0, 2); g.add(area("Message body…",  "email-body"),       1, 2);
    }

    private void buildPrintFields(GridPane g) {
        g.add(label("Content"), 0, 0); g.add(area("Content to print…", "print-content"), 1, 0);
    }

    private void buildDatabaseFields(GridPane g) {
        g.add(label("Query Type"), 0, 0);
        ComboBox<QueryType> qtCb = new ComboBox<>();
        qtCb.getItems().addAll(QueryType.values());
        qtCb.setValue(QueryType.SELECT);
        qtCb.setId("db-querytype");
        g.add(qtCb, 1, 0);
        g.add(label("SQL"),        0, 1); g.add(area("SELECT * FROM …", "db-sql"), 1, 1);
    }

    private void buildFileFields(GridPane g) {
        g.add(label("Operation"), 0, 0);
        ComboBox<FileOperation> opCb = new ComboBox<>();
        opCb.getItems().addAll(FileOperation.values());
        opCb.setValue(FileOperation.COPY);
        opCb.setId("file-operation");
        g.add(opCb, 1, 0);
        g.add(label("Source"),      0, 1); g.add(field("/source/path",      "file-source"), 1, 1);
        g.add(label("Destination"), 0, 2); g.add(field("/destination/path", "file-dest"),   1, 2);
    }

    private void buildNotificationFields(GridPane g) {
        g.add(label("Message"), 0, 0); g.add(area("Notification message…", "notif-message"), 1, 0);
    }

    // ── Job factory ──────────────────────────────────────────────────

    private Job buildJob(String id, JobPriority prio, String type, GridPane g) {
        switch (type) {
            case "Email" -> {
                String to      = require(text(g, "email-recipient"), "Recipient is required");
                String subject = require(text(g, "email-subject"),   "Subject is required");
                String body    = text(g, "email-body");
                return new EmailJob(id, prio, to, subject, body);
            }
            case "Print" -> {
                String content = require(text(g, "print-content"), "Content is required");
                return new PrintJob(id, prio, content);
            }
            case "Database" -> {
                QueryType qt = comboVal(g, "db-querytype", QueryType.class);
                String sql = require(text(g, "db-sql"), "SQL is required");
                return new DatabaseJob(id, prio, qt, sql);
            }
            case "File" -> {
                FileOperation op  = comboVal(g, "file-operation", FileOperation.class);
                String src  = require(text(g, "file-source"), "Source path is required");
                String dest = text(g, "file-dest");
                return new FileJob(id, prio, op, src, dest);
            }
            case "Notification" -> {
                String msg = require(text(g, "notif-message"), "Message is required");
                return new NotificationJob(id, prio, msg);
            }
            default -> throw new IllegalArgumentException("Unknown job type: " + type);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private Label label(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("label-secondary");
        l.setMinWidth(90);
        return l;
    }

    private TextField field(String prompt, String id) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        f.setId(id);
        f.setMinWidth(240);
        return f;
    }

    private TextArea area(String prompt, String id) {
        TextArea a = new TextArea();
        a.setPromptText(prompt);
        a.setId(id);
        a.setPrefRowCount(3);
        a.setMinWidth(240);
        a.setWrapText(true);
        return a;
    }

    private String text(GridPane g, String nodeId) {
        return g.getChildren().stream()
                .filter(n -> nodeId.equals(n.getId()))
                .findFirst()
                .map(n -> n instanceof TextArea ta ? ta.getText().trim()
                                                   : ((TextField) n).getText().trim())
                .orElse("");
    }

    @SuppressWarnings("unchecked")
    private <T> T comboVal(GridPane g, String nodeId, Class<T> type) {
        return (T) g.getChildren().stream()
                .filter(n -> nodeId.equals(n.getId()))
                .findFirst()
                .map(n -> ((ComboBox<?>) n).getValue())
                .orElse(null);
    }

    private String require(String value, String errorMsg) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(errorMsg);
        return value;
    }

    public Job showDialog() {
        stage.showAndWait();
        return createdJob;
    }
}
