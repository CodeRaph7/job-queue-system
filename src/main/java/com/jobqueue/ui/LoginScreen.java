package com.jobqueue.ui;

import com.jobqueue.model.User;
import com.jobqueue.repository.DatabaseConnection;
import com.jobqueue.repository.UserRepository;
import com.jobqueue.repository.UserRepositoryDAOImpl;
import com.jobqueue.service.PasswordUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class LoginScreen {
    private final Stage stage;
    private User authenticatedUser;
    private final UserRepository userRepository;

    public LoginScreen() {
        this.stage = new Stage();
        this.userRepository = new UserRepositoryDAOImpl(DatabaseConnection.getInstance());
        initUI();
    }

    private void initUI() {
        stage.setTitle("JobQueue — Sign In");
        stage.setResizable(false);

        // Outer page background
        VBox page = new VBox();
        page.setAlignment(Pos.CENTER);
        page.getStyleClass().add("main-layout");
        page.setStyle("-fx-background-color: #0d1117;");

        // Auth card
        VBox card = new VBox(16);
        card.getStyleClass().add("auth-card");
        card.setMaxWidth(360);
        card.setAlignment(Pos.TOP_LEFT);

        // App name
        Label appName = new Label("■ JobQueue");
        appName.getStyleClass().add("app-name");
        appName.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #e6edf3;");

        Label subtitle = new Label("Sign in to your account");
        subtitle.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 13px;");

        Separator sep = new Separator();
        sep.getStyleClass().add("separator");

        Label usernameLabel = new Label("Username");
        usernameLabel.getStyleClass().add("label-secondary");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Enter your username");
        usernameField.setMaxWidth(Double.MAX_VALUE);

        Label passwordLabel = new Label("Password");
        passwordLabel.getStyleClass().add("label-secondary");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter your password");
        passwordField.setMaxWidth(Double.MAX_VALUE);

        Button loginBtn = new Button("Sign in");
        loginBtn.getStyleClass().add("btn-primary");
        loginBtn.setMaxWidth(Double.MAX_VALUE);

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("label-error");
        errorLabel.setWrapText(true);

        HBox signupRow = new HBox(6);
        signupRow.setAlignment(Pos.CENTER);
        Label noAccount = new Label("Don't have an account?");
        noAccount.getStyleClass().add("label-secondary");
        Button signupBtn = new Button("Create one");
        signupBtn.getStyleClass().add("label-link");
        signupBtn.setStyle(
                "-fx-background-color: transparent; -fx-border-color: transparent; -fx-cursor: hand; -fx-text-fill: #388bfd; -fx-padding: 0;");
        signupRow.getChildren().addAll(noAccount, signupBtn);

        loginBtn.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText();

            if (username.isEmpty() || password.isEmpty()) {
                errorLabel.setText("Username and password are required.");
                return;
            }

            userRepository.findByUsername(username).ifPresentOrElse(
                    user -> {
                        if (PasswordUtils.verify(password, user.getPassword())) {
                            authenticatedUser = user;
                            stage.close();
                        } else {
                            errorLabel.setText("Incorrect password.");
                        }
                    },
                    () -> errorLabel.setText("No account found for \"" + username + "\"."));
        });

        passwordField.setOnAction(e -> loginBtn.fire());

        signupBtn.setOnAction(e -> {
            SignupScreen signupScreen = new SignupScreen(stage);
            User newUser = signupScreen.showDialog();
            if (newUser != null) {
                authenticatedUser = newUser;
                stage.close();
            }
        });

        card.getChildren().addAll(
                appName, subtitle, sep,
                usernameLabel, usernameField,
                passwordLabel, passwordField,
                loginBtn, errorLabel, signupRow);

        page.getChildren().add(card);

        Scene scene = new Scene(page, 480, 560);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        stage.setScene(scene);
    }

    public User showAndWait() {
        stage.showAndWait();
        return authenticatedUser;
    }

    public void show() {
        stage.show();
    }
}
