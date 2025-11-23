package com.spacepong.desktop;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class Main extends Application {

    public static GameController gameController;

    @Override
    public void start(Stage stage) throws Exception {

        UtilsViews.initialize();

        gameController = new GameController(); // Controlador general

        Scene scene = new Scene(UtilsViews.getParentContainer(), 1200, 900);
        stage.setScene(scene);
        stage.setTitle("SpacePong");

        stage.setOnCloseRequest(e -> stopApp());

        try {
            Image icon = new Image(getClass().getResourceAsStream("/assets/icons/spacepong_logo.png"));
            stage.getIcons().add(icon);
        } catch (Exception ignored) {}

        stage.show();
    }

    private void stopApp() {
        WSManager.getInstance().disconnect();
        Platform.exit();
        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
