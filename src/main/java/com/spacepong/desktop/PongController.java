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

            // aplicar valores segun quien soy yo
            if (myPlayerNumber == 1) {
                leftPaddle.setLayoutY(p1Y);
                rightPaddle.setLayoutY(p2Y);
            } else {
                leftPaddle.setLayoutY(p2Y);
                rightPaddle.setLayoutY(p1Y);
            }

            ball.setLayoutX(ballX);
            ball.setLayoutY(ballY);

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
        leftPaddle.setLayoutY((h - paddleH) / 2);
        rightPaddle.setLayoutY((h - paddleH) / 2);

        ball.setRadius(size * BALL_RADIUS_RATIO);
        ball.setLayoutX(w / 2);
        ball.setLayoutY(h / 2);
    }

    // movimiento enviados por wsmanager desde gamecontroller

    public void handleMoveDSK(JSONObject msg) {
        // Platform.runLater(() -> {
        //     try {
        //         boolean up = msg.optBoolean("down", false);
        //         boolean down = msg.optBoolean("up", false);
        //         Rectangle myPaddle = (myPlayerNumber == 1) ? leftPaddle : rightPaddle;
        //         double delta = 5;
        //         double newY = myPaddle.getLayoutY();
        //         if(up) newY -= delta;
        //         if(down) newY += delta;
        //         if(newY < 0) newY = 0;
        //         if(newY + myPaddle.getHeight() > playfield.getHeight()) newY = playfield.getHeight() - myPaddle.getHeight();
        //         myPaddle.setLayoutY(newY);
        //     } catch (Exception e) {
        //         System.err.println("handleMoveDSK error: " + e.getMessage());
        //     }
        // });

       String direction = msg.optString("direction", "");
        WSManager.getInstance().sendMoveDSK(direction);

    }
    
    // actualización de estado del juego desde gamecontroller

    public void updateFromGameState(JSONObject msg) {
        System.out.println("updateFromGameState received: " + msg.toString());
        Platform.runLater(() -> {
            try {
                JSONObject ballObj = msg.optJSONObject("ball");
                if(ballObj != null) {
                    double x = ballObj.optDouble("x", ball.getLayoutX());
                    double y = ballObj.optDouble("y", ball.getLayoutY());
                    ball.setLayoutX(x);
                    ball.setLayoutY(y);
                }
                JSONObject paddles = msg.optJSONObject("paddles");
                if(paddles != null) {
                    double p1y = paddles.optDouble("p1", leftPaddle.getLayoutY());
                    double p2y = paddles.optDouble("p2", rightPaddle.getLayoutY());
                    if(myPlayerNumber == 1) {
                        leftPaddle.setLayoutY(p1y);
                        rightPaddle.setLayoutY(p2y);
                    } else {
                        leftPaddle.setLayoutY(p2y);
                        rightPaddle.setLayoutY(p1y);
                    }
                }
                JSONObject scores = msg.optJSONObject("scores");
                if(scores != null) {
                    int s1 = scores.optInt("p1", Integer.parseInt(scoreLeftLabel.getText()));
                    int s2 = scores.optInt("p2", Integer.parseInt(scoreRightLabel.getText()));
                    scoreLeftLabel.setText(String.valueOf(s1));
                    scoreRightLabel.setText(String.valueOf(s2));
                }
            } catch (Exception e) {
                System.err.println("updateFromGameState error: " + e.getMessage());
            }
        });
    }
}
