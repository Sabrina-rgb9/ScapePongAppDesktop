package com.spacepong.desktop;

import javafx.animation.AnimationTimer;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

public class PongController {

    @FXML private Pane playfield;
    @FXML private Rectangle leftPaddle;
    @FXML private Rectangle rightPaddle;
    @FXML private Circle ball;
    @FXML private Label scoreLeftLabel;
    @FXML private Label scoreRightLabel;
    @FXML private Line centerLine;

    // movimiento
    private volatile boolean upPressed = false;
    private volatile boolean downPressed = false;
    private final double PADDLE_SPEED = 300.0; // px / s

    private AnimationTimer gameLoop;

    @FXML
    public void initialize() {
        // Línea discontinua
        if (centerLine != null) centerLine.getStrokeDashArray().addAll(12.0, 12.0);

        // Posicionar las puntuaciones (si se necesita mantener binding)
        if (playfield != null && scoreLeftLabel != null && scoreRightLabel != null) {
            scoreLeftLabel.layoutXProperty().bind(
                    Bindings.createDoubleBinding(
                            () -> playfield.getWidth() * 0.25 - scoreLeftLabel.getWidth() / 2.0,
                            playfield.widthProperty(), scoreLeftLabel.widthProperty()
                    )
            );
            scoreRightLabel.layoutXProperty().bind(
                    Bindings.createDoubleBinding(
                            () -> playfield.getWidth() * 0.75 - scoreRightLabel.getWidth() / 2.0,
                            playfield.widthProperty(), scoreRightLabel.widthProperty()
                    )
            );

            scoreLeftLabel.layoutYProperty().bind(
                    Bindings.createDoubleBinding(
                            () -> playfield.getHeight() * 0.5 - scoreLeftLabel.getHeight() / 2.0,
                            playfield.heightProperty(), scoreLeftLabel.heightProperty()
                    )
            );
            scoreRightLabel.layoutYProperty().bind(
                    Bindings.createDoubleBinding(
                            () -> playfield.getHeight() * 0.5 - scoreRightLabel.getHeight() / 2.0,
                            playfield.heightProperty(), scoreRightLabel.heightProperty()
                    )
            );

            scoreLeftLabel.setMouseTransparent(true);
            scoreRightLabel.setMouseTransparent(true);
        }

        // Asegurar que playfield puede tomar foco y pedirlo al hacer click
        if (playfield != null) {
            playfield.setFocusTraversable(true);
            playfield.setOnMouseClicked(e -> playfield.requestFocus());

            // cuando la Scene esté lista, añadimos handlers de teclado
            playfield.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    newScene.addEventHandler(KeyEvent.KEY_PRESSED, this::onKeyPressed);
                    newScene.addEventHandler(KeyEvent.KEY_RELEASED, this::onKeyReleased);
                }
            });
        }

        // Loop para mover la pala con suavidad mientras se mantienen las teclas
        gameLoop = new AnimationTimer() {
            private long last = 0;
            @Override
            public void handle(long now) {
                if (last == 0) { last = now; return; }
                double delta = (now - last) / 1_000_000_000.0;
                last = now;
                if (leftPaddle != null && playfield != null) {
                    double dy = 0;
                    if (upPressed) dy -= PADDLE_SPEED * delta;
                    if (downPressed) dy += PADDLE_SPEED * delta;
                    if (dy != 0) moveLeftPaddleBy(dy);
                }
            }
        };
        gameLoop.start();
    }

    private void onKeyPressed(KeyEvent ev) {
        if (ev.getCode() == KeyCode.UP) {
            upPressed = true;
            ev.consume();
        } else if (ev.getCode() == KeyCode.DOWN) {
            downPressed = true;
            ev.consume();
        }
    }

    private void onKeyReleased(KeyEvent ev) {
        if (ev.getCode() == KeyCode.UP) {
            upPressed = false;
            ev.consume();
        } else if (ev.getCode() == KeyCode.DOWN) {
            downPressed = false;
            ev.consume();
        }
    }

    private void moveLeftPaddleBy(double dy) {
        double newY = leftPaddle.getLayoutY() + dy;
        double minY = 0;
        double maxY = Math.max(0, playfield.getHeight() - leftPaddle.getHeight());
        if (newY < minY) newY = minY;
        if (newY > maxY) newY = maxY;
        leftPaddle.setLayoutY(newY);
    }

    // llamados desde la red / lógica para actualizar marcador (ejemplo)
    public void setScoreLeft(int s) {
        if (scoreLeftLabel != null) scoreLeftLabel.setText(Integer.toString(s));
    }
    public void setScoreRight(int s) {
        if (scoreRightLabel != null) scoreRightLabel.setText(Integer.toString(s));
    }
}