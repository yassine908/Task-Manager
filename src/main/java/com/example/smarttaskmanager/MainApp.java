package com.example.smarttaskmanager;

import com.example.smarttaskmanager.controller.MainController;
import com.example.smarttaskmanager.model.User;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        showAuthView();
    }

    public static void showAuthView() throws Exception {
        FXMLLoader loader = new FXMLLoader(
                MainApp.class.getResource("/com/example/smarttaskmanager/auth-view.fxml")
        );
        Scene scene = new Scene(loader.load(), 900, 620);
        scene.getStylesheets().add(
                MainApp.class.getResource("/com/example/smarttaskmanager/style.css").toExternalForm()
        );
        primaryStage.setTitle("Connexion - Smart Task Manager");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void showMainView(User user) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                MainApp.class.getResource("/com/example/smarttaskmanager/main-view.fxml")
        );
        Scene scene = new Scene(loader.load(), 1200, 720);
        scene.getStylesheets().add(
                MainApp.class.getResource("/com/example/smarttaskmanager/style.css").toExternalForm()
        );

        MainController controller = loader.getController();
        controller.setCurrentUser(user);

        primaryStage.setTitle("Smart Task Manager - " + user.getFullName());
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
