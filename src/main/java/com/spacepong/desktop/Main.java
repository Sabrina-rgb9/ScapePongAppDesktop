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
        launch(args);
    }
    
    @Override
    public void start(Stage stage) throws Exception {
        final int windowWidth = 1200;
        final int windowHeight = 900;

        try {
            UtilsViews.initialize();
            
            ctrlStart = UtilsViews.getStartController();
            ctrlWait = UtilsViews.getWaitController();

            Scene scene = new Scene(UtilsViews.getParentContainer(), windowWidth, windowHeight);
            
            stage.setScene(scene);
            stage.setOnCloseRequest(e -> stop());
            stage.setTitle("SpacePong");
            stage.setMinWidth(windowWidth);
            stage.setMinHeight(windowHeight);
            stage.show();

            if (!System.getProperty("os.name").contains("Mac")) {
                try {
                    Image icon = new Image(getClass().getResourceAsStream("/assets/icons/spacepong_logo.png"));
                    stage.getIcons().add(icon);
                } catch (Exception e) {
                    System.err.println("Error cargando icono: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Error en start(): " + e.getMessage());
            throw e;
        }
    }

    @Override
    public void stop() { 
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

    public static boolean connectToServer(String url, String playerName) {
        if (url == null || url.isEmpty() || playerName == null || playerName.isEmpty()) {
            return false;
        }

        clientName = playerName;
        serverUrl = url;
        
        if (ctrlWait != null) {
            ctrlWait.updateTitle("Conectando al servidor...");
            ctrlWait.updatePlayer(0, clientName, true);
            ctrlWait.updateOverallStatus();
        }
        
        pauseDuring(1000, () -> {
            try {
                wsClient = UtilsWS.getSharedInstance(serverUrl);
                
                wsClient.onOpen((message) -> {
                    Platform.runLater(() -> {
                        onConnectionOpen();
                    });
                });
                
                wsClient.onMessage((response) -> { 
                    Platform.runLater(() -> { 
                        wsMessage(response); 
                    }); 
                });
                
                wsClient.onError((response) -> { 
                    Platform.runLater(() -> { 
                        wsError(response); 
                    }); 
                });
                
                wsClient.onClose((response) -> {
                    Platform.runLater(() -> {
                        onConnectionClose();
                    });
                });
                
            } catch (Exception e) {
                System.err.println("Error creando WebSocket: " + e.getMessage());
                
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

    private static void onConnectionOpen() {
        sendRequestConfiguration();
        
        if (ctrlWait != null) {
            ctrlWait.updateTitle("Conectado...");
            ctrlWait.updatePlayer(0, clientName, true);
        }
    }

    private static void onConnectionClose() {
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

    private static void wsMessage(String response) {
        try {
            if (response == null || response.trim().isEmpty()) {
                return;
            }
            
            String trimmed = response.trim();
            
            if (trimmed.startsWith("{")) {
                try {
                    JSONObject msgObj = new JSONObject(response);
                    String messageType = msgObj.optString("type", "unknown");
                    
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
                            
                        case "gameState":
                            handleGameState(msgObj);
                            break;
                            
                        case "countdown":
                            handleCountdown(msgObj);
                            break;
                            
                        case "gameStart":
                            handleGameStart(msgObj);
                            break;
                    }
                } catch (Exception e) {
                    System.err.println("Error parseando JSON: " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error procesando mensaje: " + e.getMessage());
        }
    }

    private static void handleConfiguration(JSONObject msgObj) {
        try {
            sendRegisterMessage();
        } catch (Exception e) {
            System.err.println("Error en handleConfiguration: " + e.getMessage());
        }
    }

    private static void handleAcceptRegister(JSONObject msgObj) {
        if (ctrlWait != null) {
            ctrlWait.updateTitle("Registrado - Esperando partida...");
            ctrlWait.updatePlayer(0, clientName, true);
            ctrlWait.updateOverallStatus();
        }
    }

    private static void handleDenyRegister(JSONObject msgObj) {
        String reason = msgObj.optString("reason", "Razón desconocida");
        
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Registro Denegado");
            alert.setHeaderText("No se pudo registrar el jugador");
            alert.setContentText("Razón: " + reason + "\n\nIntenta con otro nombre.");
            alert.showAndWait();
            
            if (UtilsViews.getStartController() != null) {
                UtilsViews.showStartViewWithAnimation();
            }
        });
    }

    private static void handleStartGame(JSONObject msgObj) {
        try {
            JSONArray playersArray = msgObj.getJSONArray("players");
            String player1 = playersArray.optString(0, "Jugador1");
            String player2 = playersArray.optString(1, "Jugador2");
            
            if (ctrlWait != null) {
                ctrlWait.updateTitle("Partida Encontrada!");
                
                if (clientName.equals(player1)) {
                    ctrlWait.updatePlayer(0, player1, true);
                    ctrlWait.updatePlayer(1, player2, true);
                } else {
                    ctrlWait.updatePlayer(0, player2, true);
                    ctrlWait.updatePlayer(1, player1, true);
                }
                
                ctrlWait.updateOverallStatus();
            }
            
        } catch (Exception e) {
            System.err.println("Error en handleStartGame: " + e.getMessage());
        }
    }

    private static void handleStartCountdown(JSONObject msgObj) {
        if (ctrlWait != null) {
            ctrlWait.updateTitle("Preparando...");
            ctrlWait.updateCountdown(3);
        }
    }

    private static void handleRemainingCountdown(JSONObject msgObj) {
        int remaining = msgObj.optInt("remainingCountdown", 0);
        
        if (ctrlWait != null) {
            ctrlWait.updateCountdown(remaining);
        }
    }

    private static void handleEndCountdown(JSONObject msgObj) {
        if (ctrlWait != null) {
            ctrlWait.updateCountdown(0);
            ctrlWait.updateTitle("¡GO!");
        }
    }

    private static void handleGameOutcome(JSONObject msgObj) {
        String winner = msgObj.optString("winner", "Desconocido");
        String loser = msgObj.optString("loser", "Desconocido");
        
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Partida Terminada");
            alert.setHeaderText("¡" + winner + " es el ganador!");
            alert.setContentText("Ganador: " + winner + "\nPerdedor: " + loser);
            alert.showAndWait();
            
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

    private static void handleErrorMessage(JSONObject msgObj) {
        String errorMessage = msgObj.optString("message", "Error desconocido");
        
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error del Servidor");
            alert.setHeaderText("El servidor reportó un error");
            alert.setContentText(errorMessage);
            alert.showAndWait();
        });
    }

    private static void handleGameState(JSONObject msgObj) {
        try {
            if (msgObj.has("activeGames")) {
                JSONArray activeGames = msgObj.getJSONArray("activeGames");
                if (activeGames.length() > 0) {
                    JSONObject game = activeGames.getJSONObject(0);
                    String player1 = game.getString("player1");
                    String player2 = game.getString("player2");
                    
                    String myName = Main.getPlayerName();
                    String[] playerNames = player1.equals(myName) ? 
                        new String[]{player1, player2} : new String[]{player2, player1};
                    
                    CtrlWait waitController = UtilsViews.getWaitController();
                    if (waitController != null) {
                        waitController.handleGameStart(playerNames);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error procesando gameState: " + e.getMessage());
        }
    }

    private static void handleCountdown(JSONObject msgObj) {
        try {
            int value = msgObj.getInt("value");
            CtrlWait waitController = UtilsViews.getWaitController();
            if (waitController != null) {
                waitController.handleCountdown(value);
            }
        } catch (Exception e) {
            System.err.println("Error procesando countdown: " + e.getMessage());
        }
    }

    private static void handleGameStart(JSONObject msgObj) {
        CtrlWait waitController = UtilsViews.getWaitController();
        if (waitController != null) {
            waitController.handleGameReady();
        }
    }

    private static void wsError(String response) {
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

    private static void sendRegisterMessage() {
        if (wsClient != null && wsClient.isOpen()) {
            try {
                JSONObject registerMsg = new JSONObject();
                registerMsg.put("type", "register");
                registerMsg.put("clientName", clientName);
                
                wsClient.safeSend(registerMsg.toString());
            } catch (Exception e) {
                System.err.println("Error enviando register: " + e.getMessage());
            }
        }
    }

    private static void sendExitMessage() {
        if (wsClient != null && wsClient.isOpen()) {
            try {
                JSONObject exitMsg = new JSONObject();
                exitMsg.put("type", "exit");
                
                wsClient.safeSend(exitMsg.toString());
            } catch (Exception e) {
                System.err.println("Error enviando exit: " + e.getMessage());
            }
        }
    }

    private static void sendRequestConfiguration() {
        if (wsClient != null && wsClient.isOpen()) {
            try {
                JSONObject configMsg = new JSONObject();
                configMsg.put("type", "requestConfiguration");
                
                wsClient.safeSend(configMsg.toString());
            } catch (Exception e) {
                System.err.println("Error solicitando configuración: " + e.getMessage());
            }
        }
    }

    private static String playerName = "";

    public static void setPlayerName(String name) {
        playerName = name;
    }

    public static String getPlayerName() {
        return playerName;
    }
}