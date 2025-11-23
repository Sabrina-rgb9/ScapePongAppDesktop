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

    // Añadidos: flags para gestionar petición/recepción de configuración
    private boolean configRequested = false;
    private boolean configReceived = false;

    // Nuevo: control de ejecución de partida y deduplicación de gameState
    private volatile boolean gameRunning = false;
    private String lastGameStatePayload = null;

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
        // detener procesado de estados de juego al cerrar conexión
        gameRunning = false;
        lastGameStatePayload = null;

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

            // manejar caso donde el servidor manda un array con un objeto: [{"type":"gameState", ...}]
            JSONObject msg;
            String trimmed = response.trim();
            if (trimmed.startsWith("[")) {
                JSONArray arr = new JSONArray(trimmed);
                if (arr.length() == 0) return;
                msg = arr.getJSONObject(0);
            } else {
                msg = new JSONObject(trimmed);
            }

            String msgType = msg.optString("type", "").trim();

            // Filtrar y deduplicar gameState para no procesar en bucle
            if ("gameState".equalsIgnoreCase(msgType)) {
                // Solo procesar si la partida está en curso
                if (!gameRunning) return;

                String normalized = msg.toString();
                if (normalized.equals(lastGameStatePayload)) {
                    // mensaje idéntico al anterior → ignorar
                    return;
                }
                lastGameStatePayload = normalized;
                // Aquí actualizarías la vista del juego (posición de pelota/palas/puntuación)
                // Por ahora solo loguear o reenviar a la vista, p.ej.:
                Platform.runLater(() -> {
                    try {
                        // Si tienes un controlador de la vista Pong puedes obtenerlo y actualizarlo
                        // PongController pc = UtilsViews.getPongController();
                        // pc.updateFromGameState(msg);
                    } catch (Exception ignored) {}
                });
                return;
            }

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
                // EN VEZ DE volver a pedir configuración al recibirla, la procesamos:
                case "configuration" -> handleConfiguration(msg);
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

    public void handleConfiguration(JSONObject msg) {
        try {
            // marcar que la configuración ha sido recibida y permitir futuras peticiones si aplica
            configReceived = true;
            configRequested = false;

            // procesar campos de configuración básicos si existen
            String title = msg.optString("title", "");
            if (ctrlWait != null) {
                if (!title.isEmpty()) ctrlWait.updateTitle(title);
                ctrlWait.updateOverallStatus();
            }

            // enviar registro del cliente tras recibir la configuración
            sendRegisterMessage();
        } catch (Exception e) {
            System.err.println("Error procesando configuration: " + e.getMessage());
        }
    }

    public void handleDenyRegister(JSONObject msg) {
        String reason = msg.optString("reason", "Razón desconocida");

        showAlert(Alert.AlertType.ERROR, "Registro Denegado", "No se pudo registrar el jugador", reason);

        UtilsViews.showStartViewWithAnimation();
    }

    public void handleStartGame(JSONObject msg) {
        try {
            // Intentar obtener players como JSONArray directamente
            JSONArray arr = msg.optJSONArray("players");

            // Si no existe, intentar deducir un array dentro del objeto (por si el servidor usa otra estructura)
            if (arr == null) {
                for (String key : msg.keySet()) {
                    Object v = msg.opt(key);
                    if (v instanceof JSONArray) {
                        // usar el primer JSONArray que parezca contener jugadores (strings)
                        JSONArray candidate = (JSONArray) v;
                        boolean allStrings = true;
                        for (int i = 0; i < candidate.length(); i++) {
                            if (!(candidate.opt(i) instanceof String)) {
                                allStrings = false;
                                break;
                            }
                        }
                        if (allStrings && candidate.length() >= 2) {
                            arr = candidate;
                            break;
                        }
                    }
                }
            }

            // Si aún no hay players válidos, registrar y salir sin excepcionar
            if (arr == null || arr.length() < 2) {
                System.err.println("handleStartGame: 'players' missing or invalid. payload: " + msg.toString());
                return;
            }

            // marcar que hay al menos dos jugadores
            atLeastTwoPlayers = true;

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
        // Tras procesar, intentar abrir si ya terminó el countdown
        checkAndOpenGameIfReady();
    }

    public void handleStartCountdown() {
        // Countdown empezando: resetear flag que indica que llegó a 0
        countdownReachedZero = false;
        if (ctrlWait != null) {
            ctrlWait.updateTitle("Preparando...");
            ctrlWait.updateCountdown(3);
        }
    }

    public void handleRemainingCountdown(JSONObject msg) {
        int remaining = msg.optInt("remainingCountdown", -1);
        if (remaining >= 0 && ctrlWait != null) ctrlWait.updateCountdown(remaining);

        // Si llega 0 en remainingCountdown, también tratamos como fin
        if (remaining == 0) {
            handleEndCountdown();
        }
    }

    public void handleEndCountdown() {
        // Marca que el countdown terminó
        countdownReachedZero = true;

        if (ctrlWait != null) {
            ctrlWait.updateCountdown(0);
            ctrlWait.updateTitle("¡GO!");
        }

        // Reemplazar la escena actual por la vista de juego (no abrir nueva ventana)
        Platform.runLater(() -> {
            try {
                Parent root = FXMLLoader.load(getClass().getResource("/assets/viewPong.fxml"));

                // Buscar un Stage existente y sustituir su root
                Stage target = null;
                for (Window w : Window.getWindows()) {
                    if (w instanceof Stage s && s.isShowing()) {
                        target = s;
                        break;
                    }
                }

                if (target != null && target.getScene() != null) {
                    target.getScene().setRoot(root);
                    target.sizeToScene();
                    target.setTitle("SpacePong - Juego");
                } else {
                    // Fallback mínimo: si no hay Stage visible, usar el primero disponible o abrir nueva Stage
                    if (target == null) {
                        for (Window w : Window.getWindows()) {
                            if (w instanceof Stage s) {
                                target = s;
                                break;
                            }
                        }
                    }
                    if (target != null && target.getScene() != null) {
                        target.getScene().setRoot(root);
                        target.sizeToScene();
                        target.setTitle("SpacePong - Juego");
                    } else {
                        // última opción: abrir nueva Stage (rara vez ocurrirá)
                        Stage newStage = new Stage();
                        newStage.setScene(new Scene(root));
                        newStage.setTitle("SpacePong - Juego");
                        newStage.show();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void handleGameOutcome(JSONObject msg) {
        // finalizar partida: detener procesado de estados
        gameRunning = false;
        lastGameStatePayload = null;

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

    public void handleGameStart() {
        // marcar partida como activa para procesar gameState
        gameRunning = true;
        lastGameStatePayload = null;
        if (ctrlWait != null) ctrlWait.handleGameReady();
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
        // evitar enviar múltiples peticiones seguidas
        if (configRequested) return;
        configRequested = true;

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
                    String[] candidates = {
                            "/com/spacepong/desktop/viewPong.fxml"
                    };

                    FXMLLoader loader = null;
                    java.net.URL resUrl = null;
                    for (String c : candidates) {
                        resUrl = getClass().getResource(c);
                        if (resUrl != null) {
                            loader = new FXMLLoader(resUrl);
                            break;
                        }
                    }

                    if (loader == null) {
                        System.err.println("checkAndOpenGameIfReady: no se encontró viewPong.fxml en las rutas probadas.");
                        gameViewOpened = false;
                        return;
                    }

                    Parent root = loader.load();

                    // buscar Stage visible para reemplazar su root, si no, abrir nueva ventana
                    Stage primary = null;
                    try {
                        for (Window w : Window.getWindows()) {
                            if (w instanceof Stage s && s.isShowing()) {
                                primary = s;
                                break;
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

                    // marcar partida en curso para procesar gameState
                    gameRunning = true;

                } catch (IOException e) {
                    System.err.println("No se pudo cargar viewPong FXML: " + e.getMessage());
                    e.printStackTrace();
                    gameViewOpened = false;
                }
            });
        }
    }
}
