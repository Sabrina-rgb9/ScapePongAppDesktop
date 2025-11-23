package com.spacepong.desktop;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

import java.net.URL;
import java.util.ResourceBundle;

public class PongController implements Initializable {

    @FXML private Pane playfield;
    @FXML private Rectangle leftPaddle;
    @FXML private Rectangle rightPaddle;
    @FXML private Circle ball;
    @FXML private Label scoreLeftLabel;
    @FXML private Label scoreRightLabel;
    @FXML private Line centerLine;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Configurar línea discontinua desde el controller para evitar problemas en FXML
        if (centerLine != null) {
            centerLine.getStrokeDashArray().addAll(12.0, 12.0);
        }
        // Por ahora no hace nada más. Inicializaciones futuras aquí.
    }

    // Métodos y lógica del juego se añadirán más adelante.
}