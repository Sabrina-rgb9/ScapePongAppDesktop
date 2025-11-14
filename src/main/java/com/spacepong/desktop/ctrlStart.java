package com.spacepong.desktop;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.event.ActionEvent;
import javafx.scene.text.Font;

public class ctrlStart {

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
            if (desarrolladoLabel != null) desarrolladoLabel.setFont(Font.font(fontFamily, 12));
            if (autoresLabel != null) autoresLabel.setFont(Font.font(fontFamily, 14));
            if (confirmButton != null) confirmButton.setFont(Font.font(fontFamily, 16));
            
        } catch (Exception e) {
            System.err.println("Error cargando la fuente Solar Space: " + e.getMessage());
        }
    }

    private void setDefaultValues() {
        nameField.setText("Jugador");
    }

    @FXML
    private void handleConfirm(ActionEvent event) {
        if (validateInput()) {
            System.out.println("Conectando...");
            System.out.println("Nombre: " + nameField.getText());
            
            showSuccess("Conexión establecida correctamente");
        }
    }


    private boolean validateInput() {
        if (nameField.getText().trim().isEmpty()) {
            showError("Error", "El nombre no puede estar vacío");
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