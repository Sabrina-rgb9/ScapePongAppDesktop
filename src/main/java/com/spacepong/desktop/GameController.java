package com.spacepong.desktop;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

public class GameController {

    private final CtrlWait ctrlWait;

    public GameController() {
        ctrlWait = UtilsViews.getWaitController();
    }

    // -------------------------
    // UTILIDAD: pauseDuring()
    // -------------------------
    public void pauseDuring(long ms, Runnable action) {
        PauseTransition pause = new PauseTransition(Duration.millis(ms));
        pause.setOnFinished(e -> Platform.runLater(action));
        pause.play();
    }

    // -------------------------
    // WS CALLBACKS
    // -------------------------

    public void onConnectionOpen(String playerName) {
        sendRequestConfiguration();

        if (ctrlWait != null) {
            ctrlWait.updateTitle("Conectado...");
            ctrlWait.updatePlayer(0, playerName, true);
        }
    }

    public void onConnectionClose() {
        if (ctrlWait != null) {
            ctrlWait.resetWaitingRoom();
            ctrlWait.updateTitle("Conexión perdida");
        }

        showAlert(Alert.AlertType.WARNING, "Conexión Perdida", "Se perdió la conexión con el servidor", "Intenta reconectarte.");
    }

    public void handleWSError(String msg) {
        if (ctrlWait != null) {
            ctrlWait.updateTitle("Error de conexión");
            ctrlWait.updatePlayer(0, WSManager.getInstance().getClientName(), false);
        }

        showAlert(Alert.AlertType.ERROR, "Error de Conexión", "No se pudo conectar al servidor", msg);
    }

    // -------------------------
    // MENSAJES WS (UNIFICADO)
    // -------------------------

    /**
     * Maneja mensajes que llegan desde WSManager/Main.
     * Soporta:
     *  - mensajes JSON con campo "type" (comportamiento original)
     *  - mensajes envoltorio {type:"kv", key: "...", value: ...} para avisos simples (player-count, countdown)
     */
    public void handleMessage(String response) {
        try {
            if (response == null || response.trim().isEmpty()) return;

            JSONObject msg = new JSONObject(response);
            String msgType = msg.optString("type", "").trim();

            // 1) Caso kv (clave=valor convertido por WSManager)
            if ("kv".equalsIgnoreCase(msgType)) {
                String key = msg.optString("key", "").trim();
                Object val = msg.opt("value");

                // playerRegistry flag
                if (key.contains("playerRegistry.isAtLeastTwoPlayersAvalible")) {
                    boolean v = false;
                    if (val instanceof Boolean) v = (Boolean) val;
                    else if (val != null) v = "true".equalsIgnoreCase(val.toString());
                    atLeastTwoPlayers = v;
                }

                // countdown value
                if (key.toLowerCase().contains("count")) {
                    int n = 0;
                    if (val instanceof Number) n = ((Number) val).intValue();
                    else if (val != null) {
                        try { n = Integer.parseInt(val.toString()); } catch (NumberFormatException ignored) {}
                    }
                    countdownReachedZero = (n == 0);

                    // actualizar UI de ctrlWait si existe
                    if (ctrlWait != null) ctrlWait.updateCountdown(n);
                }

                // comprobar apertura de vista juego
                checkAndOpenGameIfReady();
                return;
            }

            // 2) Mensajes JSON por tipo (comportamiento original)
            if (msgType == null || msgType.isEmpty()) return;

            switch (msgType) {
                case "configuration" -> sendRequestConfiguration();
                case "acceptRegister" -> handleAcceptRegister();
                case "denyRegister" -> handleDenyRegister(msg);
                case "startGame" -> handleStartGame(msg);
                case "startCountdown" -> handleStartCountdown();
                case "remainingCountdown" -> handleRemainingCountdown(msg);
                case "endCountdown" -> handleEndCountdown();
                case "gameOutcome" -> handleGameOutcome(msg);
                case "error" -> handleErrorMessage(msg);
                case "gameState" -> handleGameState(msg);
                case "countdown" -> handleCountdown(msg);
                case "gameStart" -> handleGameStart();
            }

        } catch (Exception e) {
            System.err.println("Error procesando mensaje WS: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // -------------------------
    // HANDLERS ORIGINALES
    // -------------------------

    public void handleAcceptRegister() {
        if (ctrlWait != null) {
            ctrlWait.updateTitle("Registrado - Esperando partida...");
            ctrlWait.updatePlayer(0, WSManager.getInstance().getClientName(), true);
            ctrlWait.updateOverallStatus();
        }
    }

    public void handleDenyRegister(JSONObject msg) {
        String reason = msg.optString("reason", "Razón desconocida");

        showAlert(Alert.AlertType.ERROR, "Registro Denegado", "No se pudo registrar el jugador", reason);

        UtilsViews.showStartViewWithAnimation();
    }

    public void handleStartGame(JSONObject msg) {
        try {
            JSONArray arr = msg.getJSONArray("players");
            String p1 = arr.optString(0, "Jugador1");
            String p2 = arr.optString(1, "Jugador2");

            String me = WSManager.getInstance().getClientName();

            if (ctrlWait != null) {
                ctrlWait.updateTitle("Partida Encontrada!");

                if (me.equals(p1)) {
                    ctrlWait.updatePlayer(0, p1, true);
                    ctrlWait.updatePlayer(1, p2, true);
                } else {
                    ctrlWait.updatePlayer(0, p2, true);
                    ctrlWait.updatePlayer(1, p1, true);
                }

                ctrlWait.updateOverallStatus();
            }

        } catch (Exception e) {
            System.err.println("Error en handleStartGame: " + e.getMessage());
        }
    }

    public void handleStartCountdown() {
        if (ctrlWait != null) {
            ctrlWait.updateTitle("Preparando...");
            ctrlWait.updateCountdown(3);
        }
    }

    public void handleRemainingCountdown(JSONObject msg) {
        int remaining = msg.optInt("remainingCountdown", 0);
        if (ctrlWait != null) ctrlWait.updateCountdown(remaining);
    }

    public void handleEndCountdown() {
        if (ctrlWait != null) {
            ctrlWait.updateCountdown(0);
            ctrlWait.updateTitle("¡GO!");
        }
    }

    public void handleGameOutcome(JSONObject msg) {
        String winner = msg.optString("winner", "Desconocido");
        String loser = msg.optString("loser", "Desconocido");

        showAlert(Alert.AlertType.INFORMATION,
                "Partida Terminada",
                "¡" + winner + " es el ganador!",
                "Ganador: " + winner + "\nPerdedor: " + loser);

        UtilsViews.showWaitViewWithAnimation();

        if (ctrlWait != null) {
            ctrlWait.resetWaitingRoom();
            ctrlWait.updatePlayer(0, WSManager.getInstance().getClientName(), true);
            ctrlWait.updateTitle("Esperando nueva partida...");
        }
    }

    public void handleErrorMessage(JSONObject msg) {
        String error = msg.optString("message", "Error desconocido");
        showAlert(Alert.AlertType.ERROR, "Error del Servidor", "El servidor reportó un error", error);
    }

    public void handleGameState(JSONObject msg) {
        try {
            if (msg.has("activeGames")) {
                JSONArray active = msg.getJSONArray("activeGames");
                if (active.length() > 0) {
                    JSONObject g = active.getJSONObject(0);

                    String p1 = g.getString("player1");
                    String p2 = g.getString("player2");

                    String me = WSManager.getInstance().getClientName();

                    String[] order = me.equals(p1)
                            ? new String[]{p1, p2}
                            : new String[]{p2, p1};

                    if (ctrlWait != null) {
                        ctrlWait.handleGameStart(order);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error procesando gameState: " + e.getMessage());
        }
    }

    public void handleCountdown(JSONObject msg) {
        int value = msg.optInt("value", 0);
        if (ctrlWait != null) ctrlWait.handleCountdown(value);
    }

    public void handleGameStart() {
        if (ctrlWait != null) ctrlWait.handleGameReady();
    }

    // -------------------------
    // SENDERS
    // -------------------------

    private void sendRegisterMessage() {
        JSONObject obj = new JSONObject();
        obj.put("type", "register");
        obj.put("clientName", WSManager.getInstance().getClientName());
        WSManager.getInstance().send(obj);
    }

    private void sendRequestConfiguration() {
        JSONObject obj = new JSONObject();
        obj.put("type", "requestConfiguration");
        WSManager.getInstance().send(obj);
    }

    // -------------------------
    // ALERT UTIL
    // -------------------------

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(header);
        a.setContentText(content);
        a.showAndWait();
    }

    public void showConnectionError(String msg) {
        showAlert(Alert.AlertType.ERROR, "Error de conexión", "No se pudo conectar al servidor", msg);
    }

    // Flags para decidir cuándo arrancar la vista de Pong
    private boolean atLeastTwoPlayers = false;
    private boolean countdownReachedZero = false;
    private boolean gameViewOpened = false;

    private synchronized void checkAndOpenGameIfReady() {
        if (gameViewOpened) return;
        if (atLeastTwoPlayers && countdownReachedZero) {
            gameViewOpened = true; // evitar abrir varias veces
            Platform.runLater(() -> {
                try {
                    // Cargar FXML de pong
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/spacepong/desktop/pong.fxml"));
                    Parent root = loader.load();

                    // Intentar obtener el Stage principal buscando una ventana visible
                    Stage primary = null;
                    try {
                        for (Window w : Window.getWindows()) {
                            if (w instanceof Stage) {
                                Stage s = (Stage) w;
                                if (s.isShowing()) {
                                    primary = s;
                                    break;
                                }
                            }
                        }
                    } catch (Throwable ignored) {}

                    if (primary != null && primary.getScene() != null) {
                        primary.getScene().setRoot(root);
                        primary.sizeToScene();
                        primary.setTitle("SpacePong - Juego");
                    } else {
                        Stage newStage = new Stage();
                        newStage.setScene(new Scene(root));
                        newStage.setTitle("SpacePong - Juego");
                        newStage.show();
                    }
                } catch (IOException e) {
                    System.err.println("No se pudo cargar pong.fxml: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        }
    }
}
