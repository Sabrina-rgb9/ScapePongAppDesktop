package com.spacepong.desktop;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.text.Font;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.json.JSONObject;

public class ctrlStart {

    @FXML private TextField nameField;
    @FXML private Button confirmButton;
    
    @FXML private Label configLabel;
    @FXML private Label nameLabel;
    @FXML private Label desarrolladoLabel;
    @FXML private Label autoresLabel;
    @FXML private Label urlDisplayLabel;

    public static CtrlWait ctrlWait;


    // Ruta relativa al directorio del proyecto
    private static final String CONFIG_DIR = "assets/data";
    private static final String CONFIG_FILE = CONFIG_DIR + "/config.json";
private static final String DEFAULT_URL = "ws://localhost:3000"; // Cambia a HTTP y puerto 3000
    @FXML
    private void initialize() {
        // Cargar y aplicar la fuente Solar Space
        loadAndApplyFont();
        
        // Verificar y cargar configuración al iniciar
        checkAndLoadConfig();
    }

    private void loadAndApplyFont() {
        try {
            Font solarFont = Font.loadFont(getClass().getResourceAsStream("/assets/fonts/solar-space.ttf"), 16);
            String fontFamily = solarFont.getFamily();
            
            System.out.println("Fuente cargada: " + fontFamily);
            
            if (configLabel != null) configLabel.setFont(Font.font(fontFamily, 36));
            if (nameLabel != null) nameLabel.setFont(Font.font(fontFamily, 16));
            if (desarrolladoLabel != null) desarrolladoLabel.setFont(Font.font(fontFamily, 12));
            if (autoresLabel != null) autoresLabel.setFont(Font.font(fontFamily, 14));
            if (confirmButton != null) confirmButton.setFont(Font.font(fontFamily, 16));
            if (urlDisplayLabel != null) urlDisplayLabel.setFont(Font.font(fontFamily, 14));
            
        } catch (Exception e) {
            System.err.println("Error cargando la fuente Solar Space: " + e.getMessage());
        }
    }


    private void checkAndLoadConfig() {
        try {
            if (Files.exists(Paths.get(CONFIG_FILE))) {
                String content = new String(Files.readAllBytes(Paths.get(CONFIG_FILE)));
                JSONObject config = new JSONObject(content);
                
                if (config.has("url")) {
                    String url = config.getString("url");
                    updateUrlDisplayLabel(url, "Configuración cargada");
                    System.out.println("Configuración cargada: " + url);
                } else {
                    // Si no tiene URL, usar la de matrixplay6
                    updateUrlDisplayLabel("wss://matrixplay6.ieti.site:443", "URL por defecto");
                }
            } else {
                // Si no existe archivo, usar matrixplay6
                updateUrlDisplayLabel("wss://matrixplay6.ieti.site:443", "URL por defecto");
            }
        } catch (Exception e) {
            System.err.println("Error cargando configuración: " + e.getMessage());
            updateUrlDisplayLabel("wss://matrixplay6.ieti.site:443", "URL por defecto");
        }
    }

    private void createDefaultConfig() {
        try {
            System.out.println("=== CREANDO CONFIGURACIÓN ===");
            System.out.println("Intentando crear directorio: " + CONFIG_DIR);
            System.out.println("Ruta absoluta del directorio: " + Paths.get(CONFIG_DIR).toAbsolutePath());
            
            // Crear el directorio si no existe
            Files.createDirectories(Paths.get(CONFIG_DIR));
            System.out.println("Directorio creado exitosamente");
            
            JSONObject config = new JSONObject();
            config.put("url", DEFAULT_URL);
            
            System.out.println("Intentando crear archivo: " + CONFIG_FILE);
            System.out.println("Ruta absoluta del archivo: " + Paths.get(CONFIG_FILE).toAbsolutePath());
            
            try (FileWriter file = new FileWriter(CONFIG_FILE)) {
                file.write(config.toString(4));
                file.flush();
            }
            
            updateUrlDisplayLabel(DEFAULT_URL, "Configuración creada en assets/data");
            System.out.println("Archivo de configuración creado exitosamente");
            
        } catch (Exception e) {
            System.err.println("Error creando configuración: " + e.getMessage());
            e.printStackTrace();
            
            // Fallback al directorio raíz del proyecto
            tryFallbackConfig();
        }
    }

    private void tryFallbackConfig() {
        String[] fallbackLocations = {
            "config.json",  // Directorio raíz del proyecto
            "src/main/resources/config.json"  // En resources
        };
        
        for (String fallbackPath : fallbackLocations) {
            try {
                System.out.println("Intentando fallback en: " + fallbackPath);
                
                JSONObject config = new JSONObject();
                config.put("url", DEFAULT_URL);
                
                // Crear directorios padres si es necesario
                java.nio.file.Path parentDir = Paths.get(fallbackPath).getParent();
                if (parentDir != null) {
                    Files.createDirectories(parentDir);
                }
                
                try (FileWriter file = new FileWriter(fallbackPath)) {
                    file.write(config.toString(4));
                    file.flush();
                }
                
                updateUrlDisplayLabel(DEFAULT_URL, "Configuración en: " + fallbackPath);
                System.out.println("Configuración creada exitosamente en: " + fallbackPath);
                return;
                
            } catch (Exception e2) {
                System.err.println("Fallback falló en " + fallbackPath + ": " + e2.getMessage());
            }
        }
        
        updateUrlDisplayLabel(DEFAULT_URL, "Usando URL por defecto (sin archivo)");
        System.err.println("Todos los intentos de crear configuración fallaron");
    }

    private void updateUrlDisplayLabel(String url, String status) {
        if (urlDisplayLabel != null) {
            String displayUrl = url;
            if (url.length() > 30) {
                displayUrl = url.substring(0, 27) + "...";
            }
            urlDisplayLabel.setText("URL: " + displayUrl + "\n(" + status + ")");
        }
    }

    @FXML
    private void handleConfirm(ActionEvent event) {
        if (validateInput()) {
            // ✅ MOSTRAR WAITING ROOM INMEDIATAMENTE
            showWaitingRoom();
        }
    }

    private void showWaitingRoom() {
        try {
            System.out.println("🔄 Cambiando a Waiting Room...");
            
            // 1. Cambiar a la vista de waiting room
            UtilsViews.showWaitViewWithAnimation();
            
            // 2. Obtener el controlador de waiting room
            CtrlWait waitController = UtilsViews.getWaitController();
            
            if (waitController != null) {
                // 3. Actualizar la waiting room con la información del jugador
                String playerName = nameField.getText().trim();
                
                // Actualizar jugador 1 (el propio jugador)
                waitController.updatePlayer(0, playerName, true);
                
                // Actualizar título
                waitController.updateTitle("Conectando al servidor...");
                
                // ✅ ACTUALIZAR ESTADO GENERAL (este método SÍ existe en CtrlWait)
                waitController.updateOverallStatus();
                
                System.out.println("✅ Waiting Room actualizada - Jugador: " + playerName);
                
            } else {
                System.err.println("❌ Error: No se pudo obtener el controlador de Waiting Room");
                UtilsViews.showStartViewWithAnimation();
                showError("Error", "No se pudo cargar la sala de espera");
                return;
            }
            
    // 4. Iniciar la conexión WebSocket al servidor SpacePong
            String playerName = nameField.getText().trim();
            String serverUrl = "ws://localhost:3000"; // Servidor local en puerto 3000
            
            boolean connectionStarted = Main.connectToServer(serverUrl, playerName);
            
            if (!connectionStarted) {
                System.err.println("❌ No se pudo iniciar la conexión WebSocket");
                UtilsViews.showStartViewWithAnimation();
                showError("Error de Conexión", "No se pudo conectar al servidor SpacePong en localhost:3000");
            }
            
            
        } catch (Exception e) {
            System.err.println("❌ Error en showWaitingRoom: " + e.getMessage());
            e.printStackTrace();
            
            try {
                UtilsViews.showStartViewWithAnimation();
            } catch (Exception ex) {
                System.err.println("Error crítico volviendo a start: " + ex.getMessage());
            }
            
            showError("Error", "No se pudo cambiar a la sala de espera: " + e.getMessage());
        }
    }
    // ✅ MÉTODO PARA VOLVER A START VIEW DESDE WAITING ROOM (útil para errores)
    private void returnToStartView() {
        try {
            Platform.runLater(() -> {
                UtilsViews.showStartViewWithAnimation();
            });
        } catch (Exception e) {
            System.err.println("Error volviendo a Start View: " + e.getMessage());
        }
    }


    private boolean validateInput() {
        if (nameField.getText().trim().isEmpty()) {
            showError("Error", "El nombre no puede estar vacío");
            return false;
        }
        return true;
    }

    private boolean verifyConfig() {
        String[] possiblePaths = {
            CONFIG_FILE,
            "config.json",
            "src/main/resources/config.json"
        };
        
        for (String configPath : possiblePaths) {
            try {
                if (Files.exists(Paths.get(configPath))) {
                    String content = new String(Files.readAllBytes(Paths.get(configPath)));
                    JSONObject config = new JSONObject(content);
                    if (config.has("url") && !config.getString("url").isEmpty()) {
                        System.out.println("Configuración válida encontrada en: " + configPath);
                        return true;
                    }
                }
            } catch (Exception e) {
                System.err.println("Error verificando " + configPath + ": " + e.getMessage());
            }
        }
        
        return false;
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Éxito");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}