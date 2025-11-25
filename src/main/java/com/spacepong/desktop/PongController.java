package com.spacepong.desktop;

import org.json.JSONObject;

import javafx.application.Platform;
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
    @FXML Pane gameArea;


    // quién soy yo: 1 = izquierda, 2 = derecha
    private int myPlayerNumber = 1;

    // proporciones
    private static final double BALL_RADIUS_RATIO = 0.02;
    private static final double PADDLE_WIDTH_RATIO = 0.03;
    private static final double PADDLE_HEIGHT_RATIO = 0.15;
    private static final double PADDLE_MARGIN_X_RATIO = 0.05;

    @FXML
    public void initialize() {

        UtilsViews.setPongController(this);

        if (playfield != null) {
            playfield.setFocusTraversable(true);
            playfield.setOnMouseClicked(e -> playfield.requestFocus());

            playfield.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    newScene.addEventFilter(KeyEvent.KEY_PRESSED, this::onKeyPressed);
                    newScene.addEventFilter(KeyEvent.KEY_RELEASED, this::onKeyReleased);
                }
            });
        }

        if (centerLine != null)
            centerLine.getStrokeDashArray().addAll(12.0, 12.0);

        // aplicar layout inicial
        playfield.widthProperty().addListener((o, a, b) -> applyRatios());
        playfield.heightProperty().addListener((o, a, b) -> applyRatios());
        applyRatios();
    }

    // ====================
    // INPUT AL SERVIDOR
    // ====================

    private void onKeyPressed(KeyEvent ev) {
        if (ev.getCode() == KeyCode.UP) {
            WSManager.getInstance().sendMoveDSK("down");
            ev.consume();
        }
        if (ev.getCode() == KeyCode.DOWN) {
            WSManager.getInstance().sendMoveDSK("up");
            ev.consume();
        }
    }

    private void onKeyReleased(KeyEvent ev) {
        if (ev.getCode() == KeyCode.UP || ev.getCode() == KeyCode.DOWN) {
            WSManager.getInstance().sendMoveDSK("stop");
            ev.consume();
        }
    }

    // ====================
    // ACTUALIZACIÓN DESDE SERVIDOR
    // ====================

    public void setPlayerNumber(int n) {
        myPlayerNumber = (n == 1 ? 1 : 2);
        if (playfield != null) playfield.requestFocus();
        System.out.println("Soy player " + myPlayerNumber);
    }

    public void updateFromServer(double p1Y, double p2Y, double ballX, double ballY, int scoreLeft, int scoreRight) {
        Platform.runLater(() -> {

            double fieldH = playfield.getHeight();
            double fieldW = playfield.getWidth();

            leftPaddle.setLayoutY(p1Y * fieldH);
            rightPaddle.setLayoutY(p2Y * fieldH);

            ball.setLayoutX(ballX * fieldW);
            ball.setLayoutY(ballY * fieldH);

            scoreLeftLabel.setText(String.valueOf(scoreLeft));
            scoreRightLabel.setText(String.valueOf(scoreRight));
        });
    }




    // ====================
    // LAYOUT
    // ====================

    private void applyRatios() {
        if (playfield == null) return;

        double w = playfield.getWidth();
        double h = playfield.getHeight();
        double size = Math.min(w, h);

        double paddleW = size * PADDLE_WIDTH_RATIO;
        double paddleH = size * PADDLE_HEIGHT_RATIO;
        double margin = size * PADDLE_MARGIN_X_RATIO;

        leftPaddle.setWidth(paddleW);
        leftPaddle.setHeight(paddleH);
        leftPaddle.setLayoutX(margin);

        rightPaddle.setWidth(paddleW);
        rightPaddle.setHeight(paddleH);
        rightPaddle.setLayoutX(w - margin - paddleW);

        // centrar
        // leftPaddle.setLayoutY((h - paddleH) / 2);
        // rightPaddle.setLayoutY((h - paddleH) / 2);

        ball.setRadius(size * BALL_RADIUS_RATIO);
        ball.setLayoutX(w / 2);
        ball.setLayoutY(h / 2);
    }

    // movimiento enviados por wsmanager desde gamecontroller
    public void handleMoveDSK(JSONObject msg) {
        Platform.runLater(() -> {
            try {
                boolean up = msg.optString("direction").equals("up");
                boolean down = msg.optString("direction").equals("down");

                Rectangle paddle = (myPlayerNumber == 1) ? leftPaddle : rightPaddle;

                double delta = 5;
                double newY = paddle.getLayoutY();

                if (up) newY -= delta;
                if (down) newY += delta;

                // límites
                if (newY < 0) newY = 0;
                if (newY + paddle.getHeight() > playfield.getHeight())
                    newY = playfield.getHeight() - paddle.getHeight();

                paddle.setLayoutY(newY);

            } catch (Exception e) {
                System.err.println("handleMoveDSK error: " + e.getMessage());
            }
        });
    }

    
    // actualización de estado del juego desde gamecontroller

    public void updateFromGameState(JSONObject msg) {
        Platform.runLater(() -> {
            try {

                // === BALL ===
                JSONObject ballObj = msg.optJSONObject("ball");
                if (ballObj != null) {
                    double x = ballObj.optDouble("x", 0) * gameArea.getWidth();
                    double y = ballObj.optDouble("y", 0) * gameArea.getHeight();
                    ball.setLayoutX(x);
                    ball.setLayoutY(y);
                    System.out.println("Ball: " + ballObj);
                }

                // === PADDLES ===
                JSONObject paddles = msg.optJSONObject("paddles");
                if (paddles != null) {
                    double p1y = paddles.optDouble("p1", 0) * gameArea.getHeight();
                    double p2y = paddles.optDouble("p2", 0) * gameArea.getHeight();
                    leftPaddle.setLayoutY(p1y);
                    rightPaddle.setLayoutY(p2y);
                    System.out.println("Paddles: " + paddles);
                }

                // === SCORES ===
                JSONObject scores = msg.optJSONObject("scores");
                if (scores != null) {
                    scoreLeftLabel.setText(String.valueOf(scores.optInt("p1", 0)));
                    scoreRightLabel.setText(String.valueOf(scores.optInt("p2", 0)));
                }

            } catch (Exception e) {
                System.err.println("updateFromGameState error: " + e.getMessage());
            }
        });
    }

}
