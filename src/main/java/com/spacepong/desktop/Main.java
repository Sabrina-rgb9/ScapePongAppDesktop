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

    // ✅ MÉTODO PARA DEBUG - MOSTRAR INFORMACIÓN DEL SERVIDOR
    public static void printServerInfo() {
        System.out.println("=" .repeat(50));
        System.out.println("🔍 INFORMACIÓN DEL SERVIDOR");
        System.out.println("📡 URL: " + serverUrl);
        System.out.println("👤 Jugador: " + clientName);
        System.out.println("🔗 WebSocket conectado: " + (wsClient != null && wsClient.isOpen()));
        System.out.println("=" .repeat(50));
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
        System.out.println("Cerrando aplicación...");
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

    // ✅ MÉTODO DE CONEXIÓN ADAPTADO AL SERVIDOR
    public static boolean connectToServer(String url, String playerName) {
        if (url == null || url.isEmpty() || playerName == null || playerName.isEmpty()) {
            System.err.println("URL o nombre de jugador no válidos");
            return false;
        }

        clientName = playerName;
        serverUrl = url;

        System.out.println("🎯 Iniciando conexión WebSocket al servidor SpacePong...");
        System.out.println("🔗 URL: " + serverUrl);
        System.out.println("👤 Jugador: " + clientName);
        
        // ✅ ACTUALIZAR WAITING ROOM SI ESTÁ DISPONIBLE
        if (ctrlWait != null) {
            ctrlWait.updatePlayer(0, clientName, true);
            ctrlWait.updateTitle("Conectando al servidor SpacePong...");
            ctrlWait.updateOverallStatus();
        }
        
        pauseDuring(1000, () -> {
            try {
                // 1. Crear instancia de UtilsWS
                wsClient = UtilsWS.getSharedInstance(serverUrl);
                
                // 2. Configurar TODOS los callbacks
                wsClient.onOpen((message) -> {
                    Platform.runLater(() -> {
                        System.out.println("✅ Conexión WebSocket ABIERTA: " + message);
                        onConnectionOpen();
                    });
                });
                
                wsClient.onMessage((response) -> { 
                    Platform.runLater(() -> { 
                        System.out.println("📨 Mensaje CRUDO del servidor: " + response);
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
                    alert.setHeaderText("No se pudo conectar al servidor SpacePong");
                    alert.setContentText("Error: " + e.getMessage() + 
                                    "\n\nAsegúrate de que:\n• El servidor esté ejecutándose en puerto 3000\n• La URL sea: ws://localhost:3000");
                    alert.showAndWait();
                });
            }
        });
        
        return true;
    }

    // almacenar jugadores disponibles y los jugadores conectados al servidor 

    private static void onConnectionOpen() {
        System.out.println("🎉 ¡CONECTADO al servidor SpacePong!");
        
        // ✅ ACTUALIZAR WAITING ROOM
        if (ctrlWait != null) {
            ctrlWait.updateTitle("Conectado - Esperando jugadores...");
            ctrlWait.updatePlayer(0, clientName, true);
            ctrlWait.updateOverallStatus();
        }
        
        // ✅ ENVIAR SOLICITUD DE CONFIGURACIÓN Y UNIÓN
        sendConfigurationRequest();
        sendJoinMessage(); // ✅ NUEVO: ENVIAR MENSAJE DE UNIÓN
    }

    // ✅ MÉTODO PARA ENVIAR MENSAJE DE UNIÓN
    private static void sendJoinMessage() {
        if (wsClient != null && wsClient.isOpen()) {
            try {
                JSONObject joinMessage = new JSONObject();
                joinMessage.put("type", "join");
                joinMessage.put("playerName", clientName);
                
                String messageStr = joinMessage.toString();
                wsClient.safeSend(messageStr);
                System.out.println("📤 Mensaje JOIN enviado: " + messageStr);
                
            } catch (Exception e) {
                System.err.println("Error enviando mensaje join: " + e.getMessage());
            }
        }
    }

    // ✅ MÉTODO PARA ENVIAR SOLICITUD DE CONFIGURACIÓN (según el servidor)
    private static void sendConfigurationRequest() {
        if (wsClient != null && wsClient.isOpen()) {
            try {
                JSONObject configRequest = new JSONObject();
                configRequest.put("type", "requestConfiguration");
                
                String messageStr = configRequest.toString();
                wsClient.safeSend(messageStr);
                System.out.println("📤 Solicitud de configuración enviada: " + messageStr);
                
            } catch (Exception e) {
                System.err.println("Error enviando solicitud de configuración: " + e.getMessage());
            }
        } else {
            System.err.println("❌ No se puede enviar solicitud - WebSocket no conectado");
        }
    }



    // ✅ MÉTODO CUANDO LA CONEXIÓN SE CIERRA
    private static void onConnectionClose() {
        System.out.println("🔌 Desconectado del servidor matrixplay6");
        
        // ✅ RESETEAR WAITING ROOM
        if (ctrlWait != null) {
            ctrlWait.resetWaitingRoom();
            ctrlWait.updateTitle("Conexión perdida");
        }
        
        // ✅ MOSTRAR MENSAJE AL USUARIO
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Conexión Perdida");
            alert.setHeaderText("Se perdió la conexión con el servidor");
            alert.setContentText("Intenta reconectarte o verifica tu conexión a internet.");
            alert.showAndWait();
        });
    }

    // ✅ ACTUALIZA EL MÉTODO wsMessage PARA MANEJAR LOS NUEVOS TIPOS:
    private static void wsMessage(String response) {
        try {
            System.out.println("📨 Mensaje del servidor SpacePong: " + response);
            
            if (response == null || response.trim().isEmpty()) {
                return;
            }
            
            String trimmed = response.trim();
            
            if (trimmed.startsWith("Hola")) {
                handleWelcomeText(response);
            }
            else if (trimmed.startsWith("{")) {
                try {
                    JSONObject msgObj = new JSONObject(response);
                    String messageType = msgObj.optString("type", "unknown");
                    
                    System.out.println("🔍 Tipo de mensaje JSON: " + messageType);
                    
                    switch (messageType) {
                        case "configuration":
                            handleConfigurationMessage(msgObj);
                            break;
                            
                        case "playerConnected": // ✅ NUEVO: JUGADOR CONECTADO
                            handlePlayerConnected(msgObj);
                            break;
                            
                        case "playersUpdate": // ✅ NUEVO: ACTUALIZACIÓN DE JUGADORES
                            handlePlayersUpdate(msgObj);
                            break;
                            
                        case "countdown": // ✅ NUEVO: COUNTDOWN
                            handleCountdown(msgObj);
                            break;
                            
                        case "gameStart": // ✅ NUEVO: INICIO DE JUEGO
                            handleGameStart(msgObj);
                            break;
                            
                        case "welcome": // ✅ NUEVO: BIENVENIDA PERSONALIZADA
                            handleWelcomeMessage(msgObj);
                            break;
                            
                        case "playerJoined": // ✅ NUEVO: JUGADOR SE UNIÓ
                            handlePlayerJoined(msgObj);
                            break;
                            
                        default:
                            System.out.println("❓ Mensaje JSON no manejado: " + messageType);
                    }
                } catch (Exception e) {
                    System.err.println("❌ Error parseando JSON: " + e.getMessage());
                }
            } 
            else {
                handleOtherTextMessage(response);
            }
            
        } catch (Exception e) {
            System.err.println("💥 Error procesando mensaje: " + e.getMessage());
        }
    }

    // ✅ NUEVO MANEJADOR: JUGADOR CONECTADO
    private static void handlePlayerConnected(JSONObject msgObj) {
        try {
            String playerName = msgObj.optString("playerName", "Jugador");
            int playerIndex = msgObj.optInt("playerIndex", -1);
            int totalPlayers = msgObj.optInt("totalPlayers", 0);
            
            System.out.println("➕ Jugador conectado: " + playerName + " (índice: " + playerIndex + ")");
            System.out.println("👥 Total de jugadores: " + totalPlayers);
            
            if (ctrlWait != null && playerIndex >= 0) {
                ctrlWait.updatePlayer(playerIndex, playerName, true);
                ctrlWait.updateOverallStatus();
                ctrlWait.updateTitle("Jugadores: " + totalPlayers + "/2");
            }
            
        } catch (Exception e) {
            System.err.println("Error en handlePlayerConnected: " + e.getMessage());
        }
    }

    // ✅ NUEVO MANEJADOR: ACTUALIZACIÓN DE LISTA DE JUGADORES
    private static void handlePlayersUpdate(JSONObject msgObj) {
        try {
            JSONArray players = msgObj.optJSONArray("players");
            int totalPlayers = msgObj.optInt("totalPlayers", 0);
            int maxPlayers = msgObj.optInt("maxPlayers", 2);
            
            System.out.println("🎮 Actualización de jugadores: " + totalPlayers + "/" + maxPlayers);
            
            if (ctrlWait != null && players != null) {
                // ✅ LIMPIAR JUGADORES ANTERIORES
                ctrlWait.updatePlayer(0, "?", false);
                ctrlWait.updatePlayer(1, "?", false);
                
                // ✅ ACTUALIZAR CON NUEVA LISTA
                for (int i = 0; i < players.length(); i++) {
                    JSONObject player = players.getJSONObject(i);
                    int index = player.optInt("index", i);
                    String playerName = player.optString("name", "Jugador " + (i + 1));
                    boolean connected = player.optBoolean("connected", true);
                    
                    ctrlWait.updatePlayer(index, playerName, connected);
                    System.out.println("👤 Jugador " + index + ": " + playerName);
                }
                
                ctrlWait.updateOverallStatus();
                ctrlWait.updateTitle("Jugadores: " + totalPlayers + "/" + maxPlayers);
            }
            
        } catch (Exception e) {
            System.err.println("Error en handlePlayersUpdate: " + e.getMessage());
        }
    }

    // ✅ NUEVO MANEJADOR: COUNTDOWN (actualizado)
    private static void handleCountdown(JSONObject msgObj) {
        try {
            int countdownValue = msgObj.optInt("value", -1);
            String countdownMessage = msgObj.optString("message", "Iniciando...");
            
            System.out.println("⏱️ Countdown: " + countdownValue + " - " + countdownMessage);
            
            if (ctrlWait != null && countdownValue >= 0) {
                ctrlWait.updateCountdown(countdownValue);
                
                if (countdownValue == 0) {
                    ctrlWait.updateTitle("¡GO!");
                } else {
                    ctrlWait.updateTitle(countdownMessage);
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error en handleCountdown: " + e.getMessage());
        }
    }

    // ✅ NUEVO MANEJADOR: INICIO DE JUEGO
    private static void handleGameStart(JSONObject msgObj) {
        try {
            String message = msgObj.optString("message", "¡El juego ha comenzado!");
            System.out.println("🎯 " + message);
            
            if (ctrlWait != null) {
                ctrlWait.updateTitle("¡Juego Iniciado!");
                ctrlWait.updateCountdown(0);
                
                // ✅ AQUÍ DEBERÍAS CAMBIAR A LA VISTA DEL JUEGO
                // UtilsViews.setViewAnimating("ViewGame");
            }
            
            // ✅ MOSTRAR ALERTA DE INICIO
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("¡Juego Iniciado!");
                alert.setHeaderText("El juego está comenzando");
                alert.setContentText("Preparate para jugar...");
                alert.showAndWait();
            });
            
        } catch (Exception e) {
            System.err.println("Error en handleGameStart: " + e.getMessage());
        }
    }

    // ✅ NUEVO MANEJADOR: JUGADOR SE UNIÓ
    private static void handlePlayerJoined(JSONObject msgObj) {
        try {
            String playerName = msgObj.optString("playerName", "Jugador");
            int playerIndex = msgObj.optInt("playerIndex", -1);
            
            System.out.println("🎮 Jugador se unió: " + playerName + " (posición: " + playerIndex + ")");
            
            if (ctrlWait != null && playerIndex >= 0) {
                ctrlWait.updatePlayer(playerIndex, playerName, true);
                ctrlWait.updateOverallStatus();
            }
            
        } catch (Exception e) {
            System.err.println("Error en handlePlayerJoined: " + e.getMessage());
        }
    }

    // ✅ MANEJADOR DE BIENVENIDA (actualizado)
    private static void handleWelcomeMessage(JSONObject msgObj) {
        try {
            String welcomeMsg = msgObj.optString("message", "Bienvenido al servidor");
            String playerName = msgObj.optString("playerName", clientName);
            
            System.out.println("👋 " + welcomeMsg);
            
            // ✅ ACTUALIZAR NOMBRE SI EL SERVIDOR ASIGNA UNO
            if (!playerName.equals(clientName)) {
                clientName = playerName;
                System.out.println("🏷️ Nombre asignado por servidor: " + clientName);
            }
            
            if (ctrlWait != null) {
                ctrlWait.updatePlayer(0, clientName, true);
                ctrlWait.updateTitle(welcomeMsg);
            }
            
        } catch (Exception e) {
            System.err.println("Error en handleWelcomeMessage: " + e.getMessage());
        }
    }

// FIN DEL NUEVO METDO 

    // ✅ MANEJAR MENSAJE DE BIENVENIDA "Hola [IP]"
    private static void handleWelcomeText(String message) {
        System.out.println("👋 Mensaje de bienvenida del servidor: " + message);
        
        if (ctrlWait != null) {
            ctrlWait.updateTitle("Conectado al servidor SpacePong");
            // El servidor confirma la conexión pero no envía info de otros jugadores aún
        }
        
        // ✅ EL SERVIDOR ESPERA QUE LE PIDAMOS CONFIGURACIÓN
        System.out.println("🔄 Servidor listo, esperando solicitud de configuración...");
    }

    // ✅ MANEJAR MENSAJE DE CONFIGURACIÓN
    private static void handleConfigurationMessage(JSONObject msgObj) {
        try {
            String configMessage = msgObj.optString("configMessage", "SpacePong");
            System.out.println("⚙️ Configuración recibida del servidor: " + configMessage);
            
            if (ctrlWait != null) {
                ctrlWait.updateTitle("Grupo: " + configMessage);
                ctrlWait.updatePlayer(0, clientName, true);
                ctrlWait.updateOverallStatus();
            }
            
            // ✅ MOSTRAR CONFIRMACIÓN AL USUARIO
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Configuración Recibida");
                alert.setHeaderText("Conectado al grupo: " + configMessage);
                alert.setContentText("El servidor ha aceptado tu conexión.\n\nEsperando a que más jugadores se conecten...");
                alert.showAndWait();
            });
            
        } catch (Exception e) {
            System.err.println("Error en handleConfigurationMessage: " + e.getMessage());
        }
    }

    // ✅ MANEJAR OTROS MENSAJES DE TEXTO
    private static void handleOtherTextMessage(String message) {
        System.out.println("💬 Otro mensaje del servidor: " + message);
        
        // Podrías mostrar mensajes del servidor en la interfaz
        if (ctrlWait != null && message.length() < 100) {
            ctrlWait.updateTitle("Servidor: " + message);
        }
    }



    // ✅ MANEJADOR DE ERRORES DE WEBSOCKET
    private static void wsError(String response) {
        System.err.println("❌ Error de WebSocket: " + response);
    
        Platform.runLater(() -> {
            // ✅ ACTUALIZAR WAITING ROOM
            if (ctrlWait != null) {
                ctrlWait.updateTitle("Error de conexión");
                ctrlWait.updatePlayer(0, clientName, false);
            }
        
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error de Conexión");
            alert.setHeaderText("No se pudo conectar al servidor");
            alert.setContentText("Error: " + response + 
                               "\n\nServidor: " + serverUrl +
                               "\n\nVerifica:\n• Tu conexión a internet\n• Que el servidor esté disponible\n• Que no haya bloqueos de firewall");
            alert.showAndWait();
        });
    }

    // ✅ MÉTODO DE DIAGNÓSTICO COMPLETO
    public static void diagnoseConnection() {
        System.out.println("=" .repeat(60));
        System.out.println("🔍 DIAGNÓSTICO DE CONEXIÓN");
        System.out.println("=" .repeat(60));
        System.out.println("📡 URL del servidor: " + serverUrl);
        System.out.println("👤 Nombre del jugador: " + clientName);
        System.out.println("🔗 WebSocket estado: " + (wsClient != null ? 
            (wsClient.isOpen() ? "CONECTADO" : "DESCONECTADO") : "NO INICIALIZADO"));
        System.out.println("🎮 Controlador Wait: " + (ctrlWait != null ? "PRESENTE" : "AUSENTE"));
        System.out.println("🔄 Hilos activos: " + Thread.activeCount());
        
        if (wsClient != null && wsClient.isOpen()) {
            System.out.println("✅ WebSocket funcionando correctamente");
        } else {
            System.err.println("❌ WebSocket NO está conectado");
        }
        System.out.println("=" .repeat(60));
    }

    // ✅ MÉTODO PARA VERIFICAR EL ESTADO ACTUAL
    public static void printCurrentState() {
        System.out.println("📊 ESTADO ACTUAL:");
        System.out.println("  - Jugador local: " + clientName);
        if (ctrlWait != null) {
            System.out.println("  - Jugador 0 en UI: " + ctrlWait.getPlayerName(0));
            System.out.println("  - Jugador 1 en UI: " + ctrlWait.getPlayerName(1));
            System.out.println("  - Jugadores conectados: " + ctrlWait.getConnectedPlayersCount());
        }
    }

    // ✅ MÉTODO PARA FORZAR ACTUALIZACIÓN MANUAL (para testing)
    public static void forceRefresh() {
        System.out.println("🔄 Forzando actualización manual...");
        diagnoseConnection();
        printCurrentState();
        
        if (wsClient != null && wsClient.isOpen()) {
            // Enviar mensaje de "refresh" al servidor
            try {
                JSONObject refreshMsg = new JSONObject();
                refreshMsg.put("type", "refresh");
                refreshMsg.put("playerName", clientName);
                wsClient.safeSend(refreshMsg.toString());
                System.out.println("📤 Mensaje refresh enviado");
            } catch (Exception e) {
                System.err.println("Error enviando refresh: " + e.getMessage());
            }
        }
    }

}