package com.spacepong.desktop;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.text.Font;

public class CtrlWait {

    @FXML private Label txtTitle;
    @FXML private Label txtPlayer0;
    @FXML private Label txtPlayer1;
    @FXML private Label statusPlayer0;
    @FXML private Label statusPlayer1;
    @FXML private Label countdownLabel;
    @FXML private Label statusLabel;

    @FXML
    private void initialize() {
        // Cargar y aplicar la fuente Solar Space
        loadAndApplyFont();
        
        // Inicializar valores por defecto
        setDefaultValues();
    }

    private void loadAndApplyFont() {
        try {
            // Cargar la fuente desde el archivo .ttf
            Font solarFont = Font.loadFont(getClass().getResourceAsStream("/assets/fonts/solar-space.ttf"), 16);
            String fontFamily = solarFont.getFamily();
            
            System.out.println("Fuente cargada en Waiting Room: " + fontFamily);
            
            // Aplicar la fuente a todos los elementos
            if (txtTitle != null) txtTitle.setFont(Font.font(fontFamily, 36));
            if (txtPlayer0 != null) txtPlayer0.setFont(Font.font(fontFamily, 32));
            if (txtPlayer1 != null) txtPlayer1.setFont(Font.font(fontFamily, 32));
            if (statusPlayer0 != null) statusPlayer0.setFont(Font.font(fontFamily, 14));
            if (statusPlayer1 != null) statusPlayer1.setFont(Font.font(fontFamily, 14));
            if (countdownLabel != null) countdownLabel.setFont(Font.font(fontFamily, 48));
            if (statusLabel != null) statusLabel.setFont(Font.font(fontFamily, 18));
            
        } catch (Exception e) {
            System.err.println("Error cargando la fuente Solar Space en Waiting Room: " + e.getMessage());
            // Usar fuentes por defecto si falla
            setDefaultFonts();
        }
    }

    private void setDefaultFonts() {
        if (txtTitle != null) txtTitle.setStyle("-fx-font-family: 'Arial'; -fx-font-size: 36; -fx-font-weight: bold;");
        if (txtPlayer0 != null) txtPlayer0.setStyle("-fx-font-family: 'Arial'; -fx-font-size: 32; -fx-font-weight: bold;");
        if (txtPlayer1 != null) txtPlayer1.setStyle("-fx-font-family: 'Arial'; -fx-font-size: 32; -fx-font-weight: bold;");
        if (countdownLabel != null) countdownLabel.setStyle("-fx-font-family: 'Arial'; -fx-font-size: 48; -fx-font-weight: bold;");
        if (statusLabel != null) statusLabel.setStyle("-fx-font-family: 'Arial'; -fx-font-size: 18; -fx-font-weight: bold;");
    }

    private void setDefaultValues() {
        // Valores iniciales
        if (txtPlayer0 != null) txtPlayer0.setText("?");
        if (txtPlayer1 != null) txtPlayer1.setText("?");
        if (statusPlayer0 != null) statusPlayer0.setText("ESPERANDO...");
        if (statusPlayer1 != null) statusPlayer1.setText("ESPERANDO...");
        if (countdownLabel != null) countdownLabel.setText("...");
        if (statusLabel != null) statusLabel.setText("ESPERANDO JUGADORES");
        if (txtTitle != null) txtTitle.setText("SALA DE ESPERA");
    }

    // Métodos para actualizar la interfaz desde el Main
    public void updatePlayer(int playerIndex, String playerName, boolean connected) {
        if (playerIndex == 0) {
            if (txtPlayer0 != null) {
                txtPlayer0.setText(playerName != null ? playerName : "?");
            }
            if (statusPlayer0 != null) {
                statusPlayer0.setText(connected ? "CONECTADO" : "ESPERANDO...");
                statusPlayer0.setStyle(connected ? 
                    "-fx-text-fill: #00ff9c; -fx-font-size: 14;" : 
                    "-fx-text-fill: #61c89c; -fx-font-size: 14;");
            }
        } else if (playerIndex == 1) {
            if (txtPlayer1 != null) {
                txtPlayer1.setText(playerName != null ? playerName : "?");
            }
            if (statusPlayer1 != null) {
                statusPlayer1.setText(connected ? "CONECTADO" : "ESPERANDO...");
                statusPlayer1.setStyle(connected ? 
                    "-fx-text-fill: #00ff9c; -fx-font-size: 14;" : 
                    "-fx-text-fill: #61c89c; -fx-font-size: 14;");
            }
        }
        updateOverallStatus();
    }

    public void updateCountdown(int seconds) {
        if (countdownLabel != null) {
            if (seconds > 0) {
                countdownLabel.setText(String.valueOf(seconds));
                countdownLabel.setStyle("-fx-text-fill: #00ff9c; -fx-font-size: 48; -fx-font-weight: bold;");
            } else if (seconds == 0) {
                countdownLabel.setText("¡YA!");
                countdownLabel.setStyle("-fx-text-fill: #ff0066; -fx-font-size: 48; -fx-font-weight: bold;");
            }
        }
        
        if (statusLabel != null) {
            if (seconds > 0) {
                statusLabel.setText("INICIANDO EN " + seconds + " SEGUNDOS");
                statusLabel.setStyle("-fx-text-fill: #00ff9c; -fx-font-size: 18; -fx-font-weight: bold;");
            } else if (seconds == 0) {
                statusLabel.setText("¡COMIENZA EL JUEGO!");
                statusLabel.setStyle("-fx-text-fill: #ff0066; -fx-font-size: 18; -fx-font-weight: bold;");
            }
        }
    }

    public void updateTitle(String title) {
        if (txtTitle != null) {
            txtTitle.setText(title);
        }
    }

    // ✅ CAMBIAR DE private A public
    public void updateOverallStatus() {
        boolean player0Connected = !"?".equals(txtPlayer0.getText()) && !"".equals(txtPlayer0.getText());
        boolean player1Connected = !"?".equals(txtPlayer1.getText()) && !"".equals(txtPlayer1.getText());
        
        if (statusLabel != null) {
            if (player0Connected && player1Connected) {
                statusLabel.setText("¡JUGADORES LISTOS!");
                statusLabel.setStyle("-fx-text-fill: #00ff9c; -fx-font-size: 18; -fx-font-weight: bold;");
            } else if (player0Connected || player1Connected) {
                statusLabel.setText("ESPERANDO SEGUNDO JUGADOR");
                statusLabel.setStyle("-fx-text-fill: #61c89c; -fx-font-size: 18; -fx-font-weight: bold;");
            } else {
                statusLabel.setText("ESPERANDO JUGADORES");
                statusLabel.setStyle("-fx-text-fill: #61c89c; -fx-font-size: 18; -fx-font-weight: bold;");
            }
        }
    }

    // Método para resetear la sala de espera
    public void resetWaitingRoom() {
        setDefaultValues();
        if (statusPlayer0 != null) {
            statusPlayer0.setStyle("-fx-text-fill: #61c89c; -fx-font-size: 14;");
        }
        if (statusPlayer1 != null) {
            statusPlayer1.setStyle("-fx-text-fill: #61c89c; -fx-font-size: 14;");
        }
        if (statusLabel != null) {
            statusLabel.setStyle("-fx-text-fill: #61c89c; -fx-font-size: 18; -fx-font-weight: bold;");
        }
        if (countdownLabel != null) {
            countdownLabel.setStyle("-fx-text-fill: #00ff9c; -fx-font-size: 48; -fx-font-weight: bold;");
        }
    }

    // Métodos para obtener el estado actual (útiles para el Main)
    public String getPlayerName(int playerIndex) {
        if (playerIndex == 0 && txtPlayer0 != null) {
            return txtPlayer0.getText();
        } else if (playerIndex == 1 && txtPlayer1 != null) {
            return txtPlayer1.getText();
        }
        return "?";
    }

    public boolean isPlayerConnected(int playerIndex) {
        String playerName = getPlayerName(playerIndex);
        return playerName != null && !"?".equals(playerName) && !playerName.trim().isEmpty();
    }

    public int getConnectedPlayersCount() {
        int count = 0;
        if (isPlayerConnected(0)) count++;
        if (isPlayerConnected(1)) count++;
        return count;
    }
}