package com.spacepong.desktop;

import javafx.animation.AnimationTimer;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
// no canvas imports for desktop-only communication
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import org.json.JSONObject;

public class PongController {

    @FXML private Pane playfield;
    @FXML private Rectangle leftPaddle;
    @FXML private Rectangle rightPaddle;
    @FXML private Circle ball;
    // sliders removed: keyboard arrows control movement
    @FXML private Label scoreLeftLabel;
    @FXML private Label scoreRightLabel;
    @FXML private Line centerLine;

    // movimiento
    private volatile boolean upPressed = false;
    private volatile boolean downPressed = false;
    private final double PADDLE_SPEED = 300.0; // px / s

    // Ratios requeridos
    private static final double BALL_RADIUS_RATIO = 0.02d;
    private static final double PADDLE_WIDTH_RATIO = 0.03d;
    private static final double PADDLE_HEIGHT_RATIO = 0.15d;
    private static final double PADDLE_MARGIN_X_RATIO = 0.05d;

    private AnimationTimer gameLoop;
    private int myPlayerNumber = 0; // 1 or 2; decides which slider is active

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

        // Registrar instancia para que otros controladores (p.ej. GameController) puedan reenviar mensajes
        try {
            UtilsViews.setPongController(this);
        } catch (Exception ignored) {}

                // Aplicar tamaños y posiciones iniciales y cuando el playfield cambie de tamaño
                if (playfield != null) {
                    Runnable layoutUpdate = this::applyRatiosAndLayout;
                    // listeners de tamaño
                    playfield.widthProperty().addListener((o, oldV, newV) -> layoutUpdate.run());
                    playfield.heightProperty().addListener((o, oldV, newV) -> layoutUpdate.run());
                    // ejecutar inicialmente (posiblemente valores pref desde FXML)
                    playfield.sceneProperty().addListener((obs, oldS, newS) -> {
                        // Delay the first layout until scene is set so pref sizes are resolved
                        applyRatiosAndLayout();
                    });
                    // también invocar ahora por si ya hay escena
                    applyRatiosAndLayout();
                }

                            // No sliders for desktop. Movement is controlled via keyboard arrows only.

        // Loop para mover la pala con suavidad mientras se mantienen las teclas
        gameLoop = new AnimationTimer() {
            private long last = 0;
            @Override
            public void handle(long now) {
                if (last == 0) { last = now; return; }
                double delta = (now - last) / 1_000_000_000.0;
                last = now;
                if (playfield != null) {
                    double dyLocal = 0;
                    if (upPressed) dyLocal -= PADDLE_SPEED * delta;
                    if (downPressed) dyLocal += PADDLE_SPEED * delta;
                    if (dyLocal != 0) {
                        if (myPlayerNumber == 1) moveLeftPaddleBy(dyLocal);
                        else moveRightPaddleBy(dyLocal);
                    }

                    // opponent movement from server (up/down booleans)
                    double dyOpp = 0;
                    if (opponentUpPressed) dyOpp -= PADDLE_SPEED * delta;
                    if (opponentDownPressed) dyOpp += PADDLE_SPEED * delta;
                    if (dyOpp != 0) {
                        if (myPlayerNumber == 1) moveRightPaddleBy(dyOpp);
                        else moveLeftPaddleBy(dyOpp);
                    }
                }
            }
        };
        gameLoop.start();
    }

    /**
     * Handler público para mensajes JSON de tipo moveDSK enviados por el cliente Desktop.
     * Soporta dos formatos:
     *  - {"type":"moveDSK","up":true,"down":false}
     *  - {"type":"moveDSK","value":0.0..1.0, "clientName":"..."}
     * Si llega "value" actualiza la paleta/oponente correspondiente y el slider remoto.
     */
    public void handleMoveDSK(JSONObject msg) {
        if (msg == null) return;
        try {
            // value -> normalized opponent movement (server sent absolute position)
            if (msg.has("value")) {
                double val = msg.optDouble("value", Double.NaN);
                if (!Double.isNaN(val)) {
                    String sender = msg.optString("clientName", "");
                    String me = WSManager.getInstance().getClientName();
                    if (sender != null && sender.equals(me)) return; // ignore own echoes

                    if (myPlayerNumber == 1) {
                        // opponent is player 2 -> update right paddle/slider
                        applySliderToPaddle(rightPaddle, val);
                    } else if (myPlayerNumber == 2) {
                        applySliderToPaddle(leftPaddle, val);
                    }
                    return;
                }
            }

            // New preferred format: direction string ("up", "down", "stop")
            if (msg.has("direction")) {
                String dir = msg.optString("direction", "").toLowerCase();
                switch (dir) {
                    case "up":
                        this.opponentUpPressed = true;
                        this.opponentDownPressed = false;
                        break;
                    case "down":
                        this.opponentUpPressed = false;
                        this.opponentDownPressed = true;
                        break;
                    default:
                        this.opponentUpPressed = false;
                        this.opponentDownPressed = false;
                        break;
                }
            } else {
                // fallback: up/down booleans (legacy support)
                boolean up = msg.optBoolean("up", false);
                boolean down = msg.optBoolean("down", false);
                // These correspond to opponent controls — don't override local key state.
                this.opponentUpPressed = up;
                this.opponentDownPressed = down;
            }
        } catch (Exception e) {
            System.err.println("handleMoveDSK: error procesando mensaje: " + e.getMessage());
        }
    }

    private double clamp01(double v) {
        if (Double.isNaN(v)) return 0.0;
        if (v < 0) return 0.0;
        if (v > 1) return 1.0;
        return v;
    }

    private void applySliderToPaddle(Rectangle paddle, double norm) {
        if (paddle == null || playfield == null) return;
        double h = playfield.getHeight();
        double ph = paddle.getHeight();
        double y = (h - ph) * (1.0 - norm); // slider 0 = bottom, 1 = top -> invert
        if (Double.isNaN(y)) return;
        paddle.setLayoutY(y);
    }

    // opponent flags (set from network messages)
    private volatile boolean opponentUpPressed = false;
    private volatile boolean opponentDownPressed = false;


    /**
     * Set which player this client is (1 or 2). This will enable the proper slider and disable the other.
     */
    public void setPlayerNumber(int n) {
        myPlayerNumber = (n == 1) ? 1 : 2;
        // focus the playfield so keyboard events go to it
        if (playfield != null) playfield.requestFocus();
    }

    private void onKeyPressed(KeyEvent ev) {
        if (ev.getCode() == KeyCode.UP) {
            upPressed = true;
            // notify server of input via booleans
            WSManager.getInstance().sendMoveDSK("up");
            ev.consume();
        } else if (ev.getCode() == KeyCode.DOWN) {
            downPressed = true;
            WSManager.getInstance().sendMoveDSK("down");
            ev.consume();
        }
    }

    private void onKeyReleased(KeyEvent ev) {
        if (ev.getCode() == KeyCode.UP) {
            upPressed = false;
            WSManager.getInstance().sendMoveDSK("stop");
            ev.consume();
        } else if (ev.getCode() == KeyCode.DOWN) {
            downPressed = false;
            WSManager.getInstance().sendMoveDSK("stop");
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

    private void moveRightPaddleBy(double dy) {
        double newY = rightPaddle.getLayoutY() + dy;
        double minY = 0;
        double maxY = Math.max(0, playfield.getHeight() - rightPaddle.getHeight());
        if (newY < minY) newY = minY;
        if (newY > maxY) newY = maxY;
        rightPaddle.setLayoutY(newY);
    }

    /**
     * Aplica las proporciones a la pelota y a las palas y las posiciona en el playfield.
     * Usa el menor de ancho/alto del playfield como referencia para mantener la cuadratura esperada.
     */
    private void applyRatiosAndLayout() {
        try {
            if (playfield == null) return;

            double w = playfield.getWidth();
            double h = playfield.getHeight();
            if (w <= 0 || h <= 0) return;

            double size = Math.min(w, h);

            // Ball
            if (ball != null) {
                double r = size * BALL_RADIUS_RATIO;
                ball.setRadius(r);
                // centrar
                ball.setLayoutX(w / 2.0);
                ball.setLayoutY(h / 2.0);
            }

            // Paddles
            double paddleW = size * PADDLE_WIDTH_RATIO;
            double paddleH = size * PADDLE_HEIGHT_RATIO;
            double marginX = size * PADDLE_MARGIN_X_RATIO;

            if (leftPaddle != null) {
                leftPaddle.setWidth(paddleW);
                leftPaddle.setHeight(paddleH);
                leftPaddle.setLayoutX(marginX);
                leftPaddle.setLayoutY((h - paddleH) / 2.0);
            }
            if (rightPaddle != null) {
                rightPaddle.setWidth(paddleW);
                rightPaddle.setHeight(paddleH);
                rightPaddle.setLayoutX(w - marginX - paddleW);
                rightPaddle.setLayoutY((h - paddleH) / 2.0);
            }

            // Si hay etiquetas de puntuación, reposicionarlas (las bindings existentes manejan eso,
            // pero forzamos una solicitud de layout)
            if (scoreLeftLabel != null) scoreLeftLabel.requestLayout();
            if (scoreRightLabel != null) scoreRightLabel.requestLayout();

        } catch (Exception e) {
            System.err.println("applyRatiosAndLayout: " + e.getMessage());
        }
    }

    // llamados desde la red / lógica para actualizar marcador (ejemplo)
    public void setScoreLeft(int s) {
        if (scoreLeftLabel != null) scoreLeftLabel.setText(Integer.toString(s));
    }
    public void setScoreRight(int s) {
        if (scoreRightLabel != null) scoreRightLabel.setText(Integer.toString(s));
    }

    // Desktop envia un json de tipo mensaje que se llama moveDSK con fields de tipo up and down

    // las dimensiones y la posicion de la pelota y las palas tienen que ser como las siguientes: 

    // ballRadiusRatio = 0.02f
    // paddleWidthRatio = 0.03f
    // paddleHeightRatio = 0.15f
    // paddleMarginX = 0.05f
}