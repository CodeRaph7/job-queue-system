package com.jobqueue.ui;

import com.jobqueue.model.Job;
import com.jobqueue.model.JobStatus;
import com.jobqueue.model.User;
import com.jobqueue.observer.JobLogger;
import com.jobqueue.observer.PersistenceObserver;
import com.jobqueue.repository.DatabaseConnection;
import com.jobqueue.repository.JobRepository;
import com.jobqueue.repository.JobRepositoryDAOImpl;
import com.jobqueue.repository.JobResultRepositoryDAOImpl;
import com.jobqueue.service.JobScheduler;
import javafx.application.Application;
import javafx.stage.Stage;

import java.util.List;

public class JobQueueApp extends Application {

    @Override
    public void start(Stage primaryStage) {

        DatabaseConnection db = DatabaseConnection.getInstance();
        JobRepository jobRepo = new JobRepositoryDAOImpl(db);
        JobResultRepositoryDAOImpl resultRepo = new JobResultRepositoryDAOImpl(db);

        JobScheduler scheduler = JobScheduler.getInstance();
        scheduler.addObserver(new JobLogger());
        scheduler.addObserver(new PersistenceObserver(jobRepo, resultRepo));
        scheduler.start();

        while (true) {
            LoginScreen loginScreen = new LoginScreen();
            User authenticatedUser = loginScreen.showAndWait();

            if (authenticatedUser == null) {
                System.exit(0);
                return;
            }

            MainWindow mainWindow = new MainWindow(scheduler, jobRepo, resultRepo, authenticatedUser);
            scheduler.addObserver(mainWindow::onJobUpdate);
            mainWindow.showAndWait();
            scheduler.removeObserver(mainWindow::onJobUpdate);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
