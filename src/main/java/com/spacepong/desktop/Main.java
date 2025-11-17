package com.spacepong.desktop;

import javafx.application.Application;
import javafx.application.Platform;

import org.json.JSONArray;
import org.json.JSONObject;

import javafx.animation.PauseTransition;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.util.Duration;

public class Main extends Application {

    public static UtilsWS wsClient;
    public static ctrlStart ctrlStart;
    public static CtrlWait CtrlWait;
    
    public static String clientName = "";
    public static String serverUrl = "";

    public static void main(String[] args) {
        System.out.println("Iniciando SpacePong...");
        try {
            launch(args);
        } catch (Exception e) {
            System.err.println("Error fatal: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void start(Stage stage) throws Exception {
        System.out.println("Inicializando JavaFX...");

        final int windowWidth = 900;
        final int windowHeight = 600;

        try {
            // Inicializar el sistema de vistas
            UtilsViews.initialize();
            
            // Obtener los controladores
            ctrlStart = UtilsViews.getStartController();
            CtrlWait = UtilsViews.getWaitController();

            if (CtrlWait == null) {
                System.err.println("⚠️ AVISO: No se pudo cargar la vista de Waiting Room");
                System.err.println("⚠️ El botón CONECTAR no funcionará hasta que se solucione");
            } else {
                System.out.println("✅ Controlador de Waiting Room cargado correctamente");
            }

            Scene scene = new Scene(UtilsViews.getParentContainer(), windowWidth, windowHeight);
            
            stage.setScene(scene);
            stage.setOnCloseRequest(e -> stop());
            stage.setTitle("SpacePong");
            stage.setMinWidth(windowWidth);
            stage.setMinHeight(windowHeight);
            stage.show();

            System.out.println("✅ Aplicación iniciada correctamente");

            // Add icon
            if (!System.getProperty("os.name").contains("Mac")) {
                try {
                    Image icon = new Image(getClass().getResourceAsStream("/assets/icons/spacepong_logo.png"));
                    stage.getIcons().add(icon);
                } catch (Exception e) {
                    System.err.println("Error cargando el icono: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Error en start(): " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
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

    public static boolean connectToServer(String url, String playerName) {
        if (url == null || url.isEmpty() || playerName == null || playerName.isEmpty()) {
            System.err.println("URL o nombre de jugador no válidos");
            return false;
        }

        clientName = playerName;
        serverUrl = url;

        System.out.println("🎯 Iniciando conexión WebSocket...");
        System.out.println("🔗 URL: " + serverUrl);
        System.out.println("👤 Jugador: " + clientName);
        
        // ✅ LA VISTA YA DEBERÍA ESTAR EN WAITING ROOM
        
        pauseDuring(1000, () -> {
            try {
                wsClient = UtilsWS.getSharedInstance(serverUrl);
                
                wsClient.onOpen((message) -> {
                    Platform.runLater(() -> {
                        System.out.println("✅ Conexión WebSocket ABIERTA: " + message);
                        onConnectionOpen();
                    });
                });
                
                // ... resto de callbacks igual
                
            } catch (Exception e) {
                System.err.println("💥 Error creando cliente WebSocket: " + e.getMessage());
                e.printStackTrace();
            }
        });
        
        return true;
    }

    private static void onConnectionOpen() {
        System.out.println("🎉 ¡CONECTADO al servidor!");
        
        // ✅ ACTUALIZAR WAITING ROOM
        if (CtrlWait != null) {
            CtrlWait.updateTitle("Conectado - Esperando jugadores...");
            CtrlWait.updatePlayer(0, clientName, true);
        }
        
        sendJoinMessage();
    }

    private static void sendJoinMessage() {
        if (wsClient != null && wsClient.isOpen()) {
            try {
                JSONObject joinMessage = new JSONObject();
                joinMessage.put("type", "join");
                joinMessage.put("playerName", clientName);
                joinMessage.put("timestamp", System.currentTimeMillis());
                
                String messageStr = joinMessage.toString();
                wsClient.safeSend(messageStr);
                System.out.println("📤 Mensaje JOIN enviado: " + messageStr);
                
            } catch (Exception e) {
                System.err.println("Error enviando mensaje join: " + e.getMessage());
            }
        } else {
            System.err.println("❌ No se puede enviar join - WebSocket no conectado");
        }
    }

    private static void onConnectionClose() {
        System.out.println("🔌 Desconectado del servidor matrixplay6");
        
        // Resetear la waiting room
        if (CtrlWait != null) {
            CtrlWait.resetWaitingRoom();
        }
        
        // Mostrar mensaje al usuario
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Conexión Perdida");
            alert.setHeaderText("Se perdió la conexión con el servidor");
            alert.setContentText("Intenta reconectarte o verifica tu conexión a internet.");
            alert.showAndWait();
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
        try {
            System.out.println("📨 Procesando mensaje: " + response);
            
            JSONObject msgObj = new JSONObject(response);
            String messageType = msgObj.optString("type", "unknown");
            
            System.out.println("🔍 Tipo de mensaje: " + messageType);
            
            switch (messageType) {
                case "welcome":
                    handleWelcomeMessage(msgObj);
                    break;
                    
                case "players":
                case "playerList":
                    handlePlayerList(msgObj);
                    break;
                    
                case "countdown":
                    handleCountdown(msgObj);
                    break;
                    
                case "gameStart":
                    handleGameStart(msgObj);
                    break;
                    
                case "error":
                    handleErrorMessage(msgObj);
                    break;
                    
                default:
                    System.out.println("❓ Mensaje no manejado - Tipo: " + messageType);
                    System.out.println("📝 Contenido: " + response);
            }
        } catch (Exception e) {
            System.err.println("💥 Error procesando mensaje: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void handleWelcomeMessage(JSONObject msgObj) {
        try {
            String welcomeMsg = msgObj.optString("message", "Bienvenido al servidor");
            System.out.println("👋 " + welcomeMsg);
            
            if (CtrlWait != null) {
                CtrlWait.updateTitle("Conectado - " + welcomeMsg);
            }
        } catch (Exception e) {
            System.err.println("Error en handleWelcomeMessage: " + e.getMessage());
        }
    }

    private static void handlePlayerList(JSONObject msgObj) {
        try {
            JSONArray players = msgObj.optJSONArray("players");
            if (players == null) {
                System.out.println("📋 No hay lista de jugadores en el mensaje");
                return;
            }
            
            System.out.println("🎮 Lista de jugadores: " + players.length() + " jugadores");
            
            if (CtrlWait != null) {
                // Actualizar la interfaz con los jugadores
                for (int i = 0; i < players.length(); i++) {
                    JSONObject player = players.getJSONObject(i);
                    String playerName = player.optString("name", "Jugador " + (i + 1));
                    boolean connected = player.optBoolean("connected", true);

                    CtrlWait.updatePlayer(i, playerName, connected);
                    System.out.println("👤 Jugador " + i + ": " + playerName + " - " + (connected ? "CONECTADO" : "DESCONECTADO"));
                }
            }
        } catch (Exception e) {
            System.err.println("Error en handlePlayerList: " + e.getMessage());
        }
    }

    private static void handleCountdown(JSONObject msgObj) {
        try {
            int countdownValue = msgObj.optInt("value", -1);
            System.out.println("⏱️ Countdown: " + countdownValue);
            
            if (CtrlWait != null && countdownValue >= 0) {
                CtrlWait.updateCountdown(countdownValue);
            }
        } catch (Exception e) {
            System.err.println("Error en handleCountdown: " + e.getMessage());
        }
    }

    private static void handleGameStart(JSONObject msgObj) {
        System.out.println("🎯 ¡EL JUEGO COMIENZA!");
        // Aquí cambiarías a la vista del juego
    }

    private static void handleErrorMessage(JSONObject msgObj) {
        try {
            String errorMsg = msgObj.optString("message", "Error desconocido");
            System.err.println("🚨 Error del servidor: " + errorMsg);
            
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error del Servidor");
                alert.setHeaderText("El servidor reportó un error");
                alert.setContentText(errorMsg);
                alert.showAndWait();
            });
        } catch (Exception e) {
            System.err.println("Error en handleErrorMessage: " + e.getMessage());
        }
    }

    private static void wsError(String response) {
        System.err.println("❌ Error de WebSocket: " + response);
        
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error de Conexión");
            alert.setHeaderText("No se pudo conectar al servidor");
            alert.setContentText("Error: " + response + 
                            "\n\nServidor: wss://matrixplay6.ieti.site:443" +
                            "\n\nVerifica:\n• Tu conexión a internet\n• Que el servidor esté disponible\n• Que no haya bloqueos de firewall");
            alert.showAndWait();
        });
    }
}