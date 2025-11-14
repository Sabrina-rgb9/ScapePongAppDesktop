package com.spacepong.desktop;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.animation.PauseTransition;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.util.Duration;

public class Main extends Application {

    public static UtilsWS wsClient;
    public static ctrlStart ctrlStart;

    public static void main(String[] args) {
        // Iniciar app JavaFX   
        launch(args);
    }
    
    @Override
    public void start(Stage stage) throws Exception {

        final int windowWidth = 900;
        final int windowHeight = 600;

        // Inicializar el sistema de vistas usando UtilsViews
        UtilsViews.initialize();
        
        // Obtener el controlador de la vista Start
        ctrlStart = UtilsViews.getStartController();

        // Crear la escena con el contenedor padre de UtilsViews
        Scene scene = new Scene(UtilsViews.getParentContainer(), windowWidth, windowHeight);
        
        stage.setScene(scene);
        stage.onCloseRequestProperty().set(e -> stop()); // Call close method when closing window
        stage.setTitle("SpacePong");
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

    public static void connectToServer() {

        // Obtener la URL del archivo de configuración
        String serverUrl = getServerUrlFromConfig();
        
        if (serverUrl == null || serverUrl.isEmpty()) {
            System.err.println("No se encontró URL de servidor en la configuración");
            return;
        }

        System.out.println("Conectando a: " + serverUrl);
    
        pauseDuring(1500, () -> { // Give time to show connecting message ...

            wsClient = UtilsWS.getSharedInstance(serverUrl);
    
            wsClient.onMessage((response) -> { Platform.runLater(() -> { wsMessage(response); }); });
            wsClient.onError((response) -> { Platform.runLater(() -> { wsError(response); }); });
        });
    }

    private static String getServerUrlFromConfig() {
        try {
            java.nio.file.Path configPath = java.nio.file.Paths.get("config.json");
            if (java.nio.file.Files.exists(configPath)) {
                String content = new String(java.nio.file.Files.readAllBytes(configPath));
                org.json.JSONObject config = new org.json.JSONObject(content);
                if (config.has("url")) {
                    return config.getString("url");
                }
            }
        } catch (Exception e) {
            System.err.println("Error leyendo configuración: " + e.getMessage());
        }
        return null;
    }
   
    private static void wsMessage(String response) {
        System.out.println("Mensaje del servidor: " + response);
        
        // Aquí puedes manejar los mensajes del servidor según sea necesario
        // Por ejemplo, procesar datos del juego o cambiar estados
    }

    private static void wsError(String response) {
        System.err.println("Error de conexión: " + response);
        
        // Mostrar error en la interfaz si es necesario
        if (ctrlStart != null) {
            // Puedes agregar un método en ctrlStart para mostrar errores
            Platform.runLater(() -> {
                // ctrlStart.mostrarError("Error de conexión: " + response);
            });
        }
    }
}