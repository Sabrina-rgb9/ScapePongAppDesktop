package com.broadcast.client;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class UtilsWS {

    private static UtilsWS sharedInstance = null;
    private WebSocketClient client;
    private final String location;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final AtomicBoolean exitRequested = new AtomicBoolean(false);

    private Consumer<String> onOpenCallBack;
    private Consumer<String> onMessageCallBack;
    private Consumer<String> onCloseCallBack;
    private Consumer<String> onErrorCallBack;

    // Flag para saber si estamos en entorno gráfico
    private boolean isJavaFXAvailable = false;

    private UtilsWS(String location) {
        this.location = location;
        checkJavaFXAvailability();
        createNewWebSocketClient();
    }

    private void checkJavaFXAvailability() {
        try {
            Class.forName("javafx.application.Platform");
            isJavaFXAvailable = true;
        } catch (ClassNotFoundException e) {
            isJavaFXAvailable = false;
        }
    }

    private void createNewWebSocketClient() {
        try {
            this.client = new WebSocketClient(new URI(location), new Draft_6455()) {

                @Override
                public void onOpen(ServerHandshake handshake) {
                    String message = "✅ Conectado al servidor: " + getURI();
                    System.out.println(message);
                    runLaterIfSet(onOpenCallBack, message);
                }

                @Override
                public void onMessage(String message) {
                    System.out.println("📨 Mensaje recibido: " + message);
                    runLaterIfSet(onMessageCallBack, message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    String message = "🔌 Desconectado: " + reason;
                    System.out.println(message);
                    runLaterIfSet(onCloseCallBack, message);

                    if (!exitRequested.get() && remote) {
                        scheduleReconnect();
                    }
                }

                @Override
                public void onError(Exception e) {
                    String message = "❌ Error: " + e.getMessage();
                    System.out.println(message);
                    runLaterIfSet(onErrorCallBack, message);

                    if (!exitRequested.get() && 
                        (e.getMessage().contains("Connection refused") || 
                         e.getMessage().contains("failed to connect"))) {
                        scheduleReconnect();
                    }
                }
            };

            this.client.connect();

        } catch (URISyntaxException e) {
            System.err.println("❌ Error: URI inválida -> " + location);
        }
    }

    private void runLaterIfSet(Consumer<String> callback, String msg) {
        if (callback != null) {
            if (isJavaFXAvailable) {
                // Usar JavaFX si está disponible
                try {
                    javafx.application.Platform.runLater(() -> callback.accept(msg));
                } catch (IllegalStateException e) {
                    // Si JavaFX no está inicializado, ejecutar directamente
                    callback.accept(msg);
                }
            } else {
                // Modo consola - ejecutar directamente
                callback.accept(msg);
            }
        }
    }

    private void scheduleReconnect() {
        if (!exitRequested.get()) {
            System.out.println("🔄 Reconectando en 5 segundos...");
            scheduler.schedule(this::reconnect, 5, TimeUnit.SECONDS);
        }
    }

    private void reconnect() {
        if (exitRequested.get()) return;
        System.out.println("🔄 Reconectando...");
        try {
            if (client != null && client.isOpen()) {
                client.close();
            }
        } catch (Exception ignored) {}
        createNewWebSocketClient();
    }

    public static UtilsWS getSharedInstance(String location) {
        if (sharedInstance == null || !sharedInstance.location.equals(location)) {
            if (sharedInstance != null) {
                sharedInstance.forceExit();
            }
            sharedInstance = new UtilsWS(location);
        }
        return sharedInstance;
    }

    public void onOpen(Consumer<String> callback) { this.onOpenCallBack = callback; }
    public void onMessage(Consumer<String> callback) { this.onMessageCallBack = callback; }
    public void onClose(Consumer<String> callback) { this.onCloseCallBack = callback; }
    public void onError(Consumer<String> callback) { this.onErrorCallBack = callback; }

    public void safeSend(String text) {
        try {
            if (client != null && client.isOpen()) {
                client.send(text);
                System.out.println("📤 Enviado: " + text);
            } else {
                System.out.println("⚠️ No conectado, reconectando...");
                scheduleReconnect();
            }
        } catch (Exception e) {
            System.err.println("❌ Error enviando: " + e.getMessage());
        }
    }

    public void forceExit() {
        exitRequested.set(true);
        try {
            if (client != null) {
                client.close();
            }
        } catch (Exception e) {
            System.out.println("Error cerrando: " + e.getMessage());
        } finally {
            scheduler.shutdown();
        }
    }

    public boolean isOpen() {
        return client != null && client.isOpen();
    }

    public static void clearSharedInstance() {
        if (sharedInstance != null) {
            sharedInstance.forceExit();
            sharedInstance = null;
        }
    }
}