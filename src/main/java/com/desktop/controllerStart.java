package com.desktop;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.event.ActionEvent;
import javafx.scene.text.Font;

public class controllerStart {

    @FXML private TextField nameField;
    @FXML private TextField protocolField;
    @FXML private TextField hostField;
    @FXML private TextField portField;
    @FXML private Button confirmButton;
    
    @FXML private Label configLabel;
    @FXML private Label nameLabel;
    @FXML private Label protocolLabel;
    @FXML private Label hostLabel;
    @FXML private Label portLabel;
    @FXML private Label desarrolladoLabel;
    @FXML private Label autoresLabel;

    @FXML
    private void initialize() {
        // Cargar y aplicar la fuente Solar Space
        loadAndApplyFont();
        
        // Configurar validación básica
        setupFieldValidation();
        
        // Valores por defecto
        setDefaultValues();
    }

    private void loadAndApplyFont() {
        try {
            // Cargar la fuente desde el archivo .ttf
            Font solarFont = Font.loadFont(getClass().getResourceAsStream("/assets/fonts/solar-space.ttf"), 16);
            String fontFamily = solarFont.getFamily();
            
            System.out.println("Fuente cargada: " + fontFamily);
            
            // Aplicar la fuente a todos los elementos
            if (configLabel != null) configLabel.setFont(Font.font(fontFamily, 36));
            if (nameLabel != null) nameLabel.setFont(Font.font(fontFamily, 16));
            if (protocolLabel != null) protocolLabel.setFont(Font.font(fontFamily, 16));
            if (hostLabel != null) hostLabel.setFont(Font.font(fontFamily, 16));
            if (portLabel != null) portLabel.setFont(Font.font(fontFamily, 16));
            if (desarrolladoLabel != null) desarrolladoLabel.setFont(Font.font(fontFamily, 12));
            if (autoresLabel != null) autoresLabel.setFont(Font.font(fontFamily, 14));
            if (confirmButton != null) confirmButton.setFont(Font.font(fontFamily, 16));
            
        } catch (Exception e) {
            System.err.println("Error cargando la fuente Solar Space: " + e.getMessage());
        }
    }

    private void setDefaultValues() {
        nameField.setText("Jugador");
        protocolField.setText("TCP");
        hostField.setText("localhost");
        portField.setText("8080");
    }

    @FXML
    private void handleConfirm(ActionEvent event) {
        if (validateInput()) {
            System.out.println("Conectando...");
            System.out.println("Nombre: " + nameField.getText());
            System.out.println("Protocolo: " + protocolField.getText());
            System.out.println("Host: " + hostField.getText());
            System.out.println("Puerto: " + portField.getText());
            
            showSuccess("Conexión establecida correctamente");
        }
    }

    private void setupFieldValidation() {
        // Solo validar que el puerto sean números
        portField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                portField.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });
    }

    private boolean validateInput() {
        if (nameField.getText().trim().isEmpty()) {
            showError("Error", "El nombre no puede estar vacío");
            return false;
        }
        if (protocolField.getText().trim().isEmpty()) {
            showError("Error", "El protocolo no puede estar vacío");
            return false;
        }
        if (hostField.getText().trim().isEmpty()) {
            showError("Error", "El host no puede estar vacío");
            return false;
        }
        if (portField.getText().trim().isEmpty()) {
            showError("Error", "El puerto no puede estar vacío");
            return false;
        }
        
        try {
            int port = Integer.parseInt(portField.getText());
            if (port < 1 || port > 65535) {
                showError("Error", "El puerto debe estar entre 1 y 65535");
                return false;
            }
        } catch (NumberFormatException e) {
            showError("Error", "El puerto debe ser un número válido");
            return false;
        }
        
        return true;
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