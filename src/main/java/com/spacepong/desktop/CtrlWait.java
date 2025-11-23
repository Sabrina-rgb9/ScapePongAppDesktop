package com.spacepong.desktop;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.text.Font;
import javafx.application.Platform;

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
        loadAndApplyFont();
        setDefaultValues();
    }

    private void loadAndApplyFont() {
        try {
            Font solarFont = Font.loadFont(getClass().getResourceAsStream("/assets/fonts/solar-space.ttf"), 16);
            String fontFamily = solarFont.getFamily();
            
            if (txtTitle != null) txtTitle.setFont(Font.font(fontFamily, 36));
            if (txtPlayer0 != null) txtPlayer0.setFont(Font.font(fontFamily, 32));
            if (txtPlayer1 != null) txtPlayer1.setFont(Font.font(fontFamily, 32));
            if (statusPlayer0 != null) statusPlayer0.setFont(Font.font(fontFamily, 14));
            if (statusPlayer1 != null) statusPlayer1.setFont(Font.font(fontFamily, 14));
            if (countdownLabel != null) countdownLabel.setFont(Font.font(fontFamily, 48));
            if (statusLabel != null) statusLabel.setFont(Font.font(fontFamily, 18));
            
        } catch (Exception e) {
            System.err.println("Error cargando fuente: " + e.getMessage());
        }
    }

    private void setDefaultValues() {
        if (txtPlayer0 != null) txtPlayer0.setText("?");
        if (txtPlayer1 != null) txtPlayer1.setText("?");
        if (statusPlayer0 != null) statusPlayer0.setText("ESPERANDO...");
        if (statusPlayer1 != null) statusPlayer1.setText("ESPERANDO...");
        if (countdownLabel != null) countdownLabel.setText("...");
        if (statusLabel != null) statusLabel.setText("ESPERANDO JUGADORES");
        if (txtTitle != null) txtTitle.setText("SALA DE ESPERA");
    }

    public void updatePlayer(int playerIndex, String playerName, boolean connected) {
        Platform.runLater(() -> {
            if (playerIndex == 0) {
                if (txtPlayer0 != null) txtPlayer0.setText(playerName != null ? playerName : "?");
                if (statusPlayer0 != null) {
                    statusPlayer0.setText(connected ? "CONECTADO" : "ESPERANDO...");
                    statusPlayer0.setStyle(connected ? 
                        "-fx-text-fill: #00ff9c;" : 
                        "-fx-text-fill: #61c89c;");
                }
            } else if (playerIndex == 1) {
                if (txtPlayer1 != null) txtPlayer1.setText(playerName != null ? playerName : "?");
                if (statusPlayer1 != null) {
                    statusPlayer1.setText(connected ? "CONECTADO" : "ESPERANDO...");
                    statusPlayer1.setStyle(connected ? 
                        "-fx-text-fill: #00ff9c;" : 
                        "-fx-text-fill: #61c89c;");
                }
            }
            updateOverallStatus();
        });
    }
    
    public void updateBothPlayers(String player1Name, String player2Name) {
        Platform.runLater(() -> {
            if (txtPlayer0 != null) txtPlayer0.setText(player1Name);
            if (txtPlayer1 != null) txtPlayer1.setText(player2Name);
            if (statusPlayer0 != null) {
                statusPlayer0.setText("CONECTADO");
                statusPlayer0.setStyle("-fx-text-fill: #00ff9c;");
            }
            if (statusPlayer1 != null) {
                statusPlayer1.setText("CONECTADO");
                statusPlayer1.setStyle("-fx-text-fill: #00ff9c;");
            }
            updateOverallStatus();
        });
    }
    
    public void updateCountdown(int seconds) {
        Platform.runLater(() -> {
            if (countdownLabel != null) {
                if (seconds == -1) {
                    countdownLabel.setText("PREPARANDO...");
                    countdownLabel.setStyle("-fx-text-fill: #00ff9c; -fx-font-size: 36;");
                } else if (seconds > 0) {
                    countdownLabel.setText(String.valueOf(seconds));
                    countdownLabel.setStyle("-fx-text-fill: #00ff9c; -fx-font-size: 48;");
                } else if (seconds == 0) {
                    countdownLabel.setText("¡GO!");
                    countdownLabel.setStyle("-fx-text-fill: #ff0066; -fx-font-size: 48;");
                }
            }
            
            if (statusLabel != null) {
                if (seconds == -1) {
                    statusLabel.setText("PARTIDA ENCONTRADA - PREPARANDO...");
                    statusLabel.setStyle("-fx-text-fill: #00ff9c;");
                } else if (seconds > 0) {
                    statusLabel.setText("INICIANDO EN " + seconds + " SEGUNDOS");
                    statusLabel.setStyle("-fx-text-fill: #00ff9c;");
                } else if (seconds == 0) {
                    statusLabel.setText("¡COMIENZA EL JUEGO!");
                    statusLabel.setStyle("-fx-text-fill: #ff0066;");
                }
            }
        });
    }

    public void updateTitle(String title) {
        Platform.runLater(() -> {
            if (txtTitle != null) txtTitle.setText(title);
        });
    }

    public void updateOverallStatus() {
        Platform.runLater(() -> {
            if (statusLabel != null) {
                boolean player0Connected = !"?".equals(txtPlayer0.getText());
                boolean player1Connected = !"?".equals(txtPlayer1.getText());
                
                if (player0Connected && player1Connected) {
                    statusLabel.setText("¡JUGADORES LISTOS!");
                    statusLabel.setStyle("-fx-text-fill: #00ff9c;");
                } else if (player0Connected || player1Connected) {
                    statusLabel.setText("ESPERANDO SEGUNDO JUGADOR");
                    statusLabel.setStyle("-fx-text-fill: #61c89c;");
                } else {
                    statusLabel.setText("ESPERANDO JUGADORES");
                    statusLabel.setStyle("-fx-text-fill: #61c89c;");
                }
            }
        });
    }

    public void resetWaitingRoom() {
        Platform.runLater(() -> {
            setDefaultValues();
        });
    }

    public void handleGameStart(String[] playerNames) {
        Platform.runLater(() -> {
            if (playerNames.length >= 2) {
                updateBothPlayers(playerNames[0], playerNames[1]);
                updateTitle("PARTIDA ENCONTRADA!");
                updateCountdown(-1);
            }
        });
    }

    public void handleCountdown(int seconds) {
        Platform.runLater(() -> {
            updateCountdown(seconds);
        });
    }

    public void handleGameReady() {
        Platform.runLater(() -> {
            updateCountdown(0);
            if (statusLabel != null) {
                statusLabel.setText("¡REDIRIGIENDO AL JUEGO!");
                statusLabel.setStyle("-fx-text-fill: #ff0066;");
            }
        });
    }
}