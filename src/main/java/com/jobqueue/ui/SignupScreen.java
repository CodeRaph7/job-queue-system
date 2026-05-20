package com.jobqueue.ui;

import com.jobqueue.model.Role;
import com.jobqueue.model.User;
import com.jobqueue.repository.DatabaseConnection;
import com.jobqueue.repository.UserRepository;
import com.jobqueue.repository.UserRepositoryDAOImpl;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class SignupScreen {
    private final Stage stage;
    private User createdUser;
    private final UserRepository userRepository;

    public SignupScreen(Stage owner) {
        this.stage = new Stage();
        this.stage.initOwner(owner);
        this.stage.initModality(Modality.WINDOW_MODAL);
        this.userRepository = new UserRepositoryDAOImpl(DatabaseConnection.getInstance());
        initUI();
    }

    private void initUI() {
        stage.setTitle("JobQueue — Create Account");
        stage.setResizable(false);

        VBox page = new VBox();
        page.setAlignment(Pos.CENTER);
        page.setStyle("-fx-background-color: #0d1117;");

        VBox card = new VBox(14);
        card.getStyleClass().add("auth-card");
        card.setMaxWidth(360);
        card.setAlignment(Pos.TOP_LEFT);

        Label title = new Label("Create an account");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #e6edf3;");

        Label subtitle = new Label("Fill in the details below to register.");
        subtitle.getStyleClass().add("label-secondary");

        Separator sep = new Separator();

        Label usernameLabel = new Label("Username");
        usernameLabel.getStyleClass().add("label-secondary");
        TextField usernameField = new TextField();
        usernameField.setPromptText("Choose a username");
        usernameField.setMaxWidth(Double.MAX_VALUE);

        Label passwordLabel = new Label("Password");
        passwordLabel.getStyleClass().add("label-secondary");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("At least 6 characters");
        passwordField.setMaxWidth(Double.MAX_VALUE);

        Label confirmLabel = new Label("Confirm password");
        confirmLabel.getStyleClass().add("label-secondary");
        PasswordField confirmField = new PasswordField();
        confirmField.setPromptText("Repeat your password");
        confirmField.setMaxWidth(Double.MAX_VALUE);

        Label emailLabel = new Label("Email (optional)");
        emailLabel.getStyleClass().add("label-secondary");
        TextField emailField = new TextField();
        emailField.setPromptText("you@company.com");
        emailField.setMaxWidth(Double.MAX_VALUE);

        Button signupBtn = new Button("Create account");
        signupBtn.getStyleClass().add("btn-primary");
        signupBtn.setMaxWidth(Double.MAX_VALUE);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");
        cancelBtn.setMaxWidth(Double.MAX_VALUE);

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("label-error");
        errorLabel.setWrapText(true);

        signupBtn.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText();
            String confirm  = confirmField.getText();
            String email    = emailField.getText().trim();

            if (username.isEmpty() || password.isEmpty()) {
                errorLabel.setText("Username and password are required.");
                return;
            }
            if (password.length() < 6) {
                errorLabel.setText("Password must be at least 6 characters.");
                return;
            }
            if (!password.equals(confirm)) {
                errorLabel.setText("Passwords do not match.");
                return;
            }
            if (userRepository.existsByUsername(username)) {
                errorLabel.setText("Username \"" + username + "\" is already taken.");
                return;
            }

            try {
                User user = new User(username, password, email.isEmpty() ? null : email, Role.USER);
                userRepository.save(user);
                createdUser = user;
                stage.close();
            } catch (Exception ex) {
                errorLabel.setText("Error creating account: " + ex.getMessage());
            }
        });

        cancelBtn.setOnAction(e -> stage.close());

        card.getChildren().addAll(
            title, subtitle, sep,
            usernameLabel, usernameField,
            passwordLabel, passwordField,
            confirmLabel, confirmField,
            emailLabel, emailField,
            signupBtn, cancelBtn, errorLabel
        );

        page.getChildren().add(card);

        Scene scene = new Scene(page, 480, 640);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        stage.setScene(scene);
    }

    public User showDialog() {
        stage.showAndWait();
        return createdUser;
    }
}
