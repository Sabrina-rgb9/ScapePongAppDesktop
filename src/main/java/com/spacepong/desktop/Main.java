package com.spacepong.desktop;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.animation.PauseTransition;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.util.Duration;

public class Main extends Application {

    public static UtilsWS wsClient;

    public static String clientName = "";

    public static void main(String[] args) {
        // Iniciar app JavaFX   
        launch(args);
    }
    
    @Override
    public void start(Stage stage) throws Exception {

        final int windowWidth = 400;
        final int windowHeight = 300;

        // Configuración básica de la escena
        Scene scene = new Scene(new javafx.scene.layout.StackPane(), windowWidth, windowHeight);
        
        stage.setScene(scene);
        stage.onCloseRequestProperty(); // Call close method when closing window
        stage.setTitle("JavaFX");
        stage.setMinWidth(windowWidth);
        stage.setMinHeight(windowHeight);
        stage.show();

    }

    @Override
    public void stop() { 
        if (wsClient != null) {
            wsClient.forceExit();
        }
        System.exit(1); // Kill all executor services
    }

    public static void pauseDuring(long milliseconds, Runnable action) {
        PauseTransition pause = new PauseTransition(Duration.millis(milliseconds));
        pause.setOnFinished(event -> Platform.runLater(action));
        pause.play();
    }

    public static <T> List<T> jsonArrayToList(JSONArray array, Class<T> clazz) {
        List<T> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            T value = clazz.cast(array.get(i));
            list.add(value);
        }
        return list;
    }

    public static void connectToServer(String protocol, String host, String port) {

        pauseDuring(1500, () -> { // Give time to show connecting message ...
            wsClient = UtilsWS.getSharedInstance(protocol + "://" + host + ":" + port);
    
            wsClient.onMessage((response) -> { Platform.runLater(() -> { wsMessage(response); }); });
            wsClient.onError((response) -> { Platform.runLater(() -> { wsError(response); }); });
        });
    }
   
    private static void wsMessage(String response) {
        
        // System.out.println(response);
        
        JSONObject msgObj = new JSONObject(response);
        switch (msgObj.getString("type")) {
            case "serverData":
                clientName = msgObj.getString("clientName");
                
                System.out.println("Connected as: " + clientName);
                break;
            
            case "countdown":
                int value = msgObj.getInt("value");
                String txt = String.valueOf(value);
                if (value == 0) {
                    txt = "GO";
                }
                System.out.println("Countdown: " + txt);
                break;
                
            default:
                System.out.println("Message received: " + msgObj.getString("type"));
                break;
        }
    }

    private static void wsError(String response) {
        String connectionRefused = "Connection refused";
        if (response.indexOf(connectionRefused) != -1) {
            System.out.println("Connection error: " + connectionRefused);
        } else {
            System.out.println("WebSocket error: " + response);
        }
    }
}