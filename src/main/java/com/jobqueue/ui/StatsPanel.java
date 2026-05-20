package com.jobqueue.ui;

import com.jobqueue.service.SchedulerStats;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public class StatsPanel extends HBox {

    public StatsPanel(SchedulerStats stats) {
        setSpacing(12);
        setPadding(new Insets(0, 0, 0, 0));

        getChildren().addAll(
            createCard("Total Jobs",  stats.totalJobsProperty(),     "#8b949e"),
            createCard("Completed",   stats.completedJobsProperty(), "#3fb950"),
            createCard("Failed",      stats.failedJobsProperty(),    "#f85149")
        );
    }

    private VBox createCard(String title, javafx.beans.property.IntegerProperty property, String colorHex) {
        VBox card = new VBox(4);
        card.getStyleClass().add("card");
        card.setPrefWidth(160);
        card.setAlignment(Pos.TOP_LEFT);

        Label label = new Label(title);
        label.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 11px; -fx-font-weight: 600;");

        Text value = new Text();
        value.textProperty().bind(property.asString());
        value.setFill(Color.web(colorHex));
        value.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));

        card.getChildren().addAll(value, label);
        return card;
    }

    public void updateStats(SchedulerStats stats) {
        // bindings handle updates automatically
    }
}
