package com.spacepong.desktop;

import javafx.application.Platform;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

public class WSManager {

    private static WSManager instance;

    private UtilsWS wsClient;
    private String clientName = "";
    private String serverUrl = "";

    private WSManager() {}

    public static WSManager getInstance() {
        if (instance == null) instance = new WSManager();
        return instance;
    }

    public void connect(String url, String playerName) {
        this.clientName = playerName;
        this.serverUrl = url;

        CtrlWait ctrlWait = UtilsViews.getWaitController();

        if (ctrlWait != null) {
            ctrlWait.updateTitle("Conectando al servidor...");
            ctrlWait.updatePlayer(0, clientName, true);
            ctrlWait.updateOverallStatus();
        }

        Main.gameController.pauseDuring(1000, () -> {
            try {
                wsClient = UtilsWS.getSharedInstance(url);

                wsClient.onOpen(msg -> Platform.runLater(() ->
                        Main.gameController.onConnectionOpen(clientName)
                ));

                // <-- REDIRECCIÓN: dejar que WSManager procese el mensaje robustamente
                wsClient.onMessage(msg -> this.onMessage(msg));

                wsClient.onError(msg -> Platform.runLater(() ->
                        Main.gameController.handleWSError(msg)
                ));

                wsClient.onClose(msg -> Platform.runLater(() ->
                        Main.gameController.onConnectionClose()
                ));

            } catch (Exception e) {
                System.err.println("Error creando WebSocket: " + e.getMessage());
                Main.gameController.showConnectionError(e.getMessage());
            }
        });
    }

    public void disconnect() {
        if (wsClient != null && wsClient.isOpen()) {
            try {
                JSONObject msg = new JSONObject();
                msg.put("type", "exit");
                wsClient.safeSend(msg.toString());
                wsClient.forceExit();
            } catch (Exception ignored) {}
        }
    }

    public void send(JSONObject msg) {
        try {
            if (wsClient != null && wsClient.isOpen()) {
                wsClient.safeSend(msg.toString());
            }
        } catch (Exception e) {
            System.err.println("Error enviando WS: " + e.getMessage());
        }
    }

    /**
     * Convenience sender for movement updates coming from the client app/desktop.
     * Legacy: sends an absolute value payload: {"type":"moveDSK","value":<0..1>}
     */
    public void sendMoveDSK(double value) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("type", "moveDSK");
            obj.put("value", value);
            send(obj);
        } catch (Exception e) {
            System.err.println("sendMoveDSK error: " + e.getMessage());
        }
    }

    /**
     * Preferred sender for keyboard-driven clients: {"type":"moveDSK","direction":"up|down|stop"}
     */
    public void sendMoveDSK(String direction) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("type", "moveDSK");
            obj.put("direction", direction);
            send(obj);
        } catch (Exception e) {
            System.err.println("sendMoveDSK(direction) error: " + e.getMessage());
        }
    }

    public String getClientName() {
        return clientName;
    }

    public void onMessage(String message) {
        if (message == null) return;

        String trimmed = message.trim();
        // Quitar BOM si existe
        if (trimmed.startsWith("\uFEFF")) trimmed = trimmed.substring(1).trim();

        // Detectar rápidamente si es un gameState (el que llega en bucle) y suprimir el log bruto para ese caso
        boolean isGameState = trimmed.contains("\"type\":\"gameState\"") || trimmed.contains("\"gameState\"");
        if (!isGameState) {
            System.out.println("WS RAW mensaje (len=" + trimmed.length() + "): [" + trimmed + "]");
        }

        // Si no empieza por { ni [, intentar buscar primer { o [
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            int idxObj = trimmed.indexOf('{');
            int idxArr = trimmed.indexOf('[');
            int idx = -1;
            if (idxObj >= 0 && idxArr >= 0) idx = Math.min(idxObj, idxArr);
            else if (idxObj >= 0) idx = idxObj;
            else if (idxArr >= 0) idx = idxArr;

            if (idx >= 0) {
                System.out.println("WS: encontrado JSON empezando en posición " + idx + " — se recorta prefijo");
                trimmed = trimmed.substring(idx);
            } else {
                int eq = trimmed.indexOf('=');
                if (eq > 0) {
                    String key = trimmed.substring(0, eq).trim();
                    String val = trimmed.substring(eq + 1).trim();

                    Object valueObj = val;
                    if ("true".equalsIgnoreCase(val) || "false".equalsIgnoreCase(val)) {
                        valueObj = Boolean.valueOf(val);
                    } else {
                        try {
                            valueObj = Integer.parseInt(val);
                        } catch (NumberFormatException nfe1) {
                            try {
                                valueObj = Double.parseDouble(val);
                            } catch (NumberFormatException nfe2) {
                                // dejar como String
                            }
                        }
                    }

                    JSONObject wrapper = new JSONObject();
                    wrapper.put("type", "kv");
                    wrapper.put("key", key);
                    wrapper.put("value", valueObj);

                    final String payload = wrapper.toString();
                    if (!isGameState) System.out.println("WS: convertido key=value a JSON: " + payload);
                    Platform.runLater(() -> {
                        try {
                            Main.gameController.handleMessage(payload);
                        } catch (Exception e) {
                            System.err.println("Error reenviando KV a GameController: " + e.getMessage());
                        }
                    });
                    return;
                }

                if (!isGameState) System.err.println("WS: mensaje no JSON recibido, se ignora: " + trimmed);
                return;
            }
        }

        try {
            Object parsed = new JSONTokener(trimmed).nextValue();
            if (parsed instanceof org.json.JSONObject) {
                handleJsonObject((org.json.JSONObject) parsed);
            } else if (parsed instanceof JSONArray) {
                handleJsonArray((JSONArray) parsed);
            } else {
                if (!isGameState) System.err.println("WS: tipo inesperado al parsear JSON: " + parsed.getClass());
                final String raw = trimmed;
                Platform.runLater(() -> Main.gameController.handleWSError("Mensaje no-JSON recibido: " + raw));
            }
        } catch (Exception e) {
            if (!isGameState) {
                System.err.println("Error procesando mensaje WS: " + e.getMessage());
                System.err.println("WS raw hex: " + toHex(message));
                e.printStackTrace();
            }
            // informar a GameController (silencioso para gameState)
            final String raw = trimmed;
            Platform.runLater(() ->
                Main.gameController.handleWSError("Error parseando mensaje WS: " + (isGameState ? "[gameState]" : raw))
            );
        }
    }

    // helper para debug (puedes añadirlo en la misma clase)
    private String toHex(String s) {
        if (s == null) return "null";
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            sb.append(String.format("%02X ", (int) c));
        }
        return sb.toString().trim();
    }

    // Añadidos para resolver el error de compilación:
    // Reenvían JSONObject/JSONArray a Main.gameController como mensaje String
    private void handleJsonObject(JSONObject obj) {
        try {
            final String payload = obj.toString();
            Platform.runLater(() -> {
                try {
                    Main.gameController.handleMessage(payload);
                } catch (Exception e) {
                    System.err.println("Error al reenviar JSONObject a GameController: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            System.err.println("Error manejando JSONObject: " + e.getMessage());
        }
    }

    private void handleJsonArray(JSONArray arr) {
        try {
            final String payload = arr.toString();
            Platform.runLater(() -> {
                try {
                    Main.gameController.handleMessage(payload);
                } catch (Exception e) {
                    System.err.println("Error al reenviar JSONArray a GameController: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            System.err.println("Error manejando JSONArray: " + e.getMessage());
        }
    }
}
