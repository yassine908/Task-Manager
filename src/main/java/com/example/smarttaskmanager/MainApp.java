package com.example.smarttaskmanager;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                MainApp.class.getResource("/com/example/smarttaskmanager/main-view.fxml")
        );
        Scene scene = new Scene(loader.load(), 1200, 720);
        scene.getStylesheets().add(
                MainApp.class.getResource("/com/example/smarttaskmanager/style.css").toExternalForm()
        );
        stage.setTitle("Smart Task Manager 📋");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}