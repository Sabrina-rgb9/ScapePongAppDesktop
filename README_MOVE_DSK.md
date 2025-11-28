Notes about moveDSK messages and Pong sizing

- The desktop client may send JSON messages of type `moveDSK` with boolean fields `up` and `down`.
- Example payload: {"type":"moveDSK","up":true,"down":false}. These messages are forwarded by `GameController` to the `PongController`.
- `PongController` exposes `handleMoveDSK(org.json.JSONObject)` which updates movement flags; the controller's AnimationTimer moves the left paddle accordingly.
- Sizes and positions are computed from playfield size using these ratios:
  - ballRadiusRatio = 0.02
  - paddleWidthRatio = 0.03
  - paddleHeightRatio = 0.15
  - paddleMarginX = 0.05
