package com.spacepong.desktop;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.animation.PauseTransition;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.json.JSONArray;
import org.json.JSONObject;

public class Main extends Application {

    public static UtilsWS wsClient;
    public static ctrlStart ctrlStart;
    public static CtrlWait ctrlWait;
    
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
            ctrlWait = UtilsViews.getWaitController();
            
            if (ctrlWait == null) {
                System.err.println("⚠️ AVISO: No se pudo cargar la vista de Waiting Room");
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
                    Image icon = new Image(getClass().getResourceAsStream("/assets/icons/icon.png"));
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
        System.out.println("Cerrando aplicación...");
        
        // ✅ ENVIAR MENSAJE exit AL SERVIDOR
        sendExitMessage();
        
        if (wsClient != null) {
            wsClient.forceExit();
        }
        Platform.exit();
        System.exit(0);
    }

    public static void pauseDuring(long milliseconds, Runnable action) {
        PauseTransition pause = new PauseTransition(Duration.millis(milliseconds));
        pause.setOnFinished(event -> Platform.runLater(action));
        pause.play();
    }

    // ✅ MÉTODO PARA CONECTAR AL SERVIDOR
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
        
        // ✅ ACTUALIZAR WAITING ROOM
        if (ctrlWait != null) {
            ctrlWait.updateTitle("Conectando al servidor...");
            ctrlWait.updatePlayer(0, clientName, true);
            ctrlWait.updateOverallStatus();
        }
        
        pauseDuring(1000, () -> {
            try {
                // 1. Crear instancia de UtilsWS
                wsClient = UtilsWS.getSharedInstance(serverUrl);
                
                // 2. Configurar callbacks esenciales
                wsClient.onOpen((message) -> {
                    Platform.runLater(() -> {
                        System.out.println("✅ Conexión WebSocket ABIERTA: " + message);
                        onConnectionOpen();
                    });
                });
                
                wsClient.onMessage((response) -> { 
                    Platform.runLater(() -> { 
                        System.out.println("📨 Mensaje del servidor: " + response);
                        wsMessage(response); 
                    }); 
                });
                
                wsClient.onError((response) -> { 
                    Platform.runLater(() -> { 
                        System.err.println("❌ Error WebSocket: " + response);
                        wsError(response); 
                    }); 
                });
                
                wsClient.onClose((response) -> {
                    Platform.runLater(() -> {
                        System.out.println("🔌 Conexión CERRADA: " + response);
                        onConnectionClose();
                    });
                });
                
                System.out.println("🔄 Cliente WebSocket configurado, conectando...");
                
            } catch (Exception e) {
                System.err.println("💥 Error creando cliente WebSocket: " + e.getMessage());
                e.printStackTrace();
                
                Platform.runLater(() -> {
                    if (ctrlWait != null) {
                        ctrlWait.updateTitle("Error de conexión");
                        ctrlWait.updatePlayer(0, clientName, false);
                    }
                    
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error de Conexión");
                    alert.setHeaderText("No se pudo conectar al servidor");
                    alert.setContentText("Error: " + e.getMessage());
                    alert.showAndWait();
                });
            }
        });
        
        return true;
    }

    // ✅ MÉTODO CUANDO LA CONEXIÓN SE ABRE
    private static void onConnectionOpen() {
        System.out.println("🎉 ¡CONECTADO al servidor SpacePong!");
        
        // ✅ SOLICITAR CONFIGURACIÓN AL CONECTAR
        sendRequestConfiguration();
        
        if (ctrlWait != null) {
            ctrlWait.updateTitle("Conectado...");
            ctrlWait.updatePlayer(0, clientName, true);
        }
    }

    // ✅ MÉTODO CUANDO LA CONEXIÓN SE CIERRA
    private static void onConnectionClose() {
        System.out.println("🔌 Desconectado del servidor");
        
        // ✅ RESETEAR WAITING ROOM
        if (ctrlWait != null) {
            ctrlWait.resetWaitingRoom();
            ctrlWait.updateTitle("Conexión perdida");
        }
        
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Conexión Perdida");
            alert.setHeaderText("Se perdió la conexión con el servidor");
            alert.setContentText("Intenta reconectarte.");
            alert.showAndWait();
        });
    }

    // ✅ MÉTODO PRINCIPAL PARA PROCESAR MENSAJES DEL SERVIDOR
    private static void wsMessage(String response) {
        try {
            System.out.println("📨 Mensaje del servidor: " + response);
            
            if (response == null || response.trim().isEmpty()) {
                return;
            }
            
            String trimmed = response.trim();
            
            if (trimmed.startsWith("{")) {
                try {
                    JSONObject msgObj = new JSONObject(response);
                    String messageType = msgObj.optString("type", "unknown");
                    
                    System.out.println("🔍 Tipo de mensaje: " + messageType);
                    
                    switch (messageType) {
                        case "configuration":
                            handleConfiguration(msgObj);
                            break;
                            
                        case "acceptRegister":
                            handleAcceptRegister(msgObj);
                            break;
                            
                        case "denyRegister":
                            handleDenyRegister(msgObj);
                            break;
                            
                        case "startGame":
                            handleStartGame(msgObj);
                            break;
                            
                        case "startCountdown":
                            handleStartCountdown(msgObj);
                            break;
                            
                        case "remainingCountdown":
                            handleRemainingCountdown(msgObj);
                            break;
                            
                        case "endCountdown":
                            handleEndCountdown(msgObj);
                            break;
                            
                        case "gameOutcome":
                            handleGameOutcome(msgObj);
                            break;
                            
                        case "error":
                            handleErrorMessage(msgObj);
                            break;
                            
                        default:
                            System.out.println("❓ Mensaje no manejado: " + messageType);
                    }
                } catch (Exception e) {
                    System.err.println("❌ Error parseando JSON: " + e.getMessage());
                }
            } 
            else {
                System.out.println("💬 Mensaje de texto: " + response);
            }
            
        } catch (Exception e) {
            System.err.println("💥 Error procesando mensaje: " + e.getMessage());
        }
    }

    // ✅ MANEJADORES ESENCIALES DE MENSAJES:

    // ✅ configuration - Server sends the asked configuration to the player
    private static void handleConfiguration(JSONObject msgObj) {
        try {
            String configMessage = msgObj.optString("configMessage", "SpacePong");
            System.out.println("⚙️ Configuración recibida: " + configMessage);
            
            // ✅ ENVIAR REGISTRO AUTOMÁTICO DESPUÉS DE RECIBIR CONFIGURACIÓN
            sendRegisterMessage();
            
        } catch (Exception e) {
            System.err.println("Error en handleConfiguration: " + e.getMessage());
        }
    }

    // ✅ acceptRegister - Server confirms to client that has been correctly registered
    private static void handleAcceptRegister(JSONObject msgObj) {
        System.out.println("✅ Registro aceptado - Jugador: " + clientName);
        
        if (ctrlWait != null) {
            ctrlWait.updateTitle("Registrado - Esperando partida...");
            ctrlWait.updatePlayer(0, clientName, true);
            ctrlWait.updateOverallStatus();
        }
        
        System.out.println("🎯 Esperando a que el servidor encuentre partida...");
    }

    // ✅ denyRegister - Server doesn't allow client to register for a certain reason
    private static void handleDenyRegister(JSONObject msgObj) {
        String reason = msgObj.optString("reason", "Razón desconocida");
        System.err.println("❌ Registro denegado: " + reason);
        
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Registro Denegado");
            alert.setHeaderText("No se pudo registrar el jugador");
            alert.setContentText("Razón: " + reason + "\n\nIntenta con otro nombre.");
            alert.showAndWait();
            
            // ✅ VOLVER A LA VISTA INICIAL
            if (UtilsViews.getStartController() != null) {
                UtilsViews.showStartViewWithAnimation();
            }
        });
    }

    // ✅ startGame - Server has created a game with 2 players
    private static void handleStartGame(JSONObject msgObj) {
        try {
            JSONArray playersArray = msgObj.getJSONArray("players");
            String player1 = playersArray.optString(0, "Jugador1");
            String player2 = playersArray.optString(1, "Jugador2");
            
            System.out.println("🎮 Partida iniciada: " + player1 + " vs " + player2);
            
            if (ctrlWait != null) {
                ctrlWait.updateTitle("Partida Encontrada!");
                
                // ✅ MOSTRAR JUGADORES
                if (clientName.equals(player1)) {
                    ctrlWait.updatePlayer(0, player1, true);
                    ctrlWait.updatePlayer(1, player2, true);
                } else {
                    ctrlWait.updatePlayer(0, player2, true);
                    ctrlWait.updatePlayer(1, player1, true);
                }
                
                ctrlWait.updateOverallStatus();
                
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("¡Partida Encontrada!");
                    alert.setHeaderText("Tu oponente: " + (clientName.equals(player1) ? player2 : player1));
                    alert.setContentText("La partida comenzará en unos segundos...");
                    alert.showAndWait();
                });
            }
            
        } catch (Exception e) {
            System.err.println("Error en handleStartGame: " + e.getMessage());
        }
    }

    // ✅ startCountdown - Server notifies players that it will start a countdown
    private static void handleStartCountdown(JSONObject msgObj) {
        System.out.println("⏱️ Countdown iniciado");
        
        if (ctrlWait != null) {
            ctrlWait.updateTitle("Preparando...");
            ctrlWait.updateCountdown(3);
        }
    }

    // ✅ remainingCountdown - Server sends the remaining ticks of the countdown
    private static void handleRemainingCountdown(JSONObject msgObj) {
        int remaining = msgObj.optInt("remainingCountdown", 0);
        System.out.println("⏱️ Countdown: " + remaining);
        
        if (ctrlWait != null) {
            ctrlWait.updateCountdown(remaining);
        }
    }

    // ✅ endCountdown - Game will start
    private static void handleEndCountdown(JSONObject msgObj) {
        System.out.println("🎯 Countdown terminado - ¡Juego comenzando!");
        
        if (ctrlWait != null) {
            ctrlWait.updateCountdown(0);
            ctrlWait.updateTitle("¡GO!");
        }
        
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("¡Juego Iniciado!");
            alert.setHeaderText("La partida ha comenzado");
            alert.setContentText("Simulación de juego en curso...");
            alert.showAndWait();
        });
    }

    // ✅ gameOutcome - Who won and lost the game
    private static void handleGameOutcome(JSONObject msgObj) {
        String winner = msgObj.optString("winner", "Desconocido");
        String loser = msgObj.optString("loser", "Desconocido");
        
        System.out.println("🏆 Resultado: " + winner + " ganó vs " + loser);
        
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Partida Terminada");
            alert.setHeaderText("¡" + winner + " es el ganador!");
            alert.setContentText("Ganador: " + winner + "\nPerdedor: " + loser);
            alert.showAndWait();
            
            // ✅ VOLVER A WAITING ROOM
            if (UtilsViews.getWaitController() != null) {
                UtilsViews.showWaitViewWithAnimation();
                if (ctrlWait != null) {
                    ctrlWait.resetWaitingRoom();
                    ctrlWait.updatePlayer(0, clientName, true);
                    ctrlWait.updateTitle("Esperando nueva partida...");
                }
            }
        });
    }

    // ✅ MANEJADOR DE ERRORES
    private static void handleErrorMessage(JSONObject msgObj) {
        String errorMessage = msgObj.optString("message", "Error desconocido");
        System.err.println("🚨 Error del servidor: " + errorMessage);
        
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error del Servidor");
            alert.setHeaderText("El servidor reportó un error");
            alert.setContentText(errorMessage);
            alert.showAndWait();
        });
    }

    // ✅ MANEJADOR DE ERRORES DE WEBSOCKET
    private static void wsError(String response) {
        System.err.println("❌ Error de WebSocket: " + response);
        
        Platform.runLater(() -> {
            if (ctrlWait != null) {
                ctrlWait.updateTitle("Error de conexión");
                ctrlWait.updatePlayer(0, clientName, false);
            }
            
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error de Conexión");
            alert.setHeaderText("No se pudo conectar al servidor");
            alert.setContentText("Error: " + response);
            alert.showAndWait();
        });
    }

    // ✅ MÉTODOS ESENCIALES PARA ENVIAR MENSAJES AL SERVIDOR:

    // ✅ register - Client wants to join the server
    private static void sendRegisterMessage() {
        if (wsClient != null && wsClient.isOpen()) {
            try {
                JSONObject registerMsg = new JSONObject();
                registerMsg.put("type", "register");
                registerMsg.put("clientName", clientName);
                
                wsClient.safeSend(registerMsg.toString());
                System.out.println("📤 Enviando register: " + clientName);
                
            } catch (Exception e) {
                System.err.println("Error enviando register: " + e.getMessage());
            }
        }
    }

    // ✅ exit - Client exits the application and server
    private static void sendExitMessage() {
        if (wsClient != null && wsClient.isOpen()) {
            try {
                JSONObject exitMsg = new JSONObject();
                exitMsg.put("type", "exit");
                
                wsClient.safeSend(exitMsg.toString());
                System.out.println("🚪 Enviando exit");
                
            } catch (Exception e) {
                System.err.println("Error enviando exit: " + e.getMessage());
            }
        }
    }

    // ✅ requestConfiguration - Client asks to receive the configuration
    private static void sendRequestConfiguration() {
        if (wsClient != null && wsClient.isOpen()) {
            try {
                JSONObject configMsg = new JSONObject();
                configMsg.put("type", "requestConfiguration");
                
                wsClient.safeSend(configMsg.toString());
                System.out.println("⚙️ Solicitando configuración");
                
            } catch (Exception e) {
                System.err.println("Error solicitando configuración: " + e.getMessage());
            }
        }
    }
}