package com.aimtrainer;

import com.aimtrainer.SoundManager.SoundOption;
import com.aimtrainer.Target.Mode;
import java.security.SecureRandom;
import java.sql.ResultSet;
import java.util.*;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeType;
import javafx.util.Duration;



public class Controller {

   // Expliziter leerer Konstruktor für FXMLLoader
    public Controller() {
        System.out.println("DEBUG: Controller Constructor called");
    }


  // ============================================================================================
  // FXML INJECTIONS (UI ELEMENTS)
  // ============================================================================================
  @FXML
  private Pane gameArea;

  // Labels
  @FXML
  private Label scoreLabel;

  @FXML
  private Label missclicksLabel;

  @FXML
  private Label bestScoreLabel;

  @FXML
  private Label timeLabel;

  @FXML
  private Label accuracyLabel;

  @FXML
  private Label comboLabel;

  @FXML
  private Label avgTimeLabel;

  @FXML
  private Label modeLabel;

  @FXML
  private Label volumeLabel;

  // Buttons & Inputs
  @FXML
  private Button modeBtn;

  @FXML
  private Button sizeBtn;

  @FXML
  private Button stopBtn;

  @FXML
  private TextField speedButton;

  @FXML
  private Button toggleTargetBtn; // New Target Image Toggle

  // Sound Buttons
  @FXML
  private Button classicBtn, beepBtn, popBtn, laserBtn, customBtn;

  @FXML
  private Button uploadBtn;
 @FXML
  private Button historyBtn;

  // Effect Buttons
  @FXML
  private Button toggleHitEffectBtn;

  @FXML
  private Button hitEffectOnOffBtn;

  // ============================================================================================
  // CORE SYSTEM VARIABLES (DB, RNG, MANAGERS)
  // ============================================================================================
  private DatabaseConnection dbConnection;
  private  SoundManager soundManager ;

  private SecureRandom sr = new SecureRandom();
  private final Xorshift128Plus rng = new Xorshift128Plus(
    sr.nextLong(),
    sr.nextLong()
  );
  private  TargetManager manager;

  private final List<Target> targets = new ArrayList<>();
  private final Map<Target, Circle> viewMap = new HashMap<>();

  // ============================================================================================
  // GAME CONFIGURATION & STATE
  // ============================================================================================
  private Target.Mode currentMode = Target.Mode.BOUNCE;
  private TargetSize currentSize = TargetSize.MEDIUM;
  private double margin = 20;
  private double targetRadius = 45;

  // Rendering Settings
  private boolean useTargetImage = false;
  private Image targetImage;
  private HitEffect currentHitEffect = HitEffect.EXPAND_CONTRACT;
  private boolean hitEffectsEnabled = true;

  // Game Loop & Logic
  private AnimationTimer timer;
  private boolean gameActive = false;
  private long gameStartTime;
  private final int gameDuration = 60; // seconds
  private Label gameStateLabel;

  // Input State
  private Point2D currentMousePosition = new Point2D(0, 0);
  private boolean mKeyPressed = false;
  private Scene scene;

  // Statistics
  private int score = 0;
  private int missclicks = 0;
  private int combo = 0;
  private int maxCombo = 0;
  private int hits = 0;
  private double totalReactionTime = 0;
  private long lastHitTime = 0;
  private SoundOption currentSound = SoundOption.CLASSIC;

  // History Mode State
  private List<GameRecord> gameHistory = new ArrayList<>();
  private int historyIndex = 0;
  private boolean historyMode = false;

  // Enums
  private enum TargetSize {
    SMALL,
    MEDIUM,
    LARGE,
  }

  private enum HitEffect {
    EXPAND_CONTRACT,
    SPARKLE,
  }

  // ============================================================================================
  // INITIALIZATION & SETUP
  // ============================================================================================
  @FXML
  
   public void initialize() {
    System.out.println("DEBUG: Controller initialize START");
    
    // Alles in einen großen Try-Catch Block, damit Fehler angezeigt werden!
    try {
        // 1. Manager & DB initialisieren (Lazy Loading)
        manager = new TargetManager(rng);
        
        System.out.println("DEBUG: Init DB...");
        try {
            dbConnection = new DatabaseConnection();
        } catch (Exception e) {
            System.err.println("DB Init Failed: " + e.getMessage());
            // Optional: Alert zeigen, aber weitermachen
        }

        System.out.println("DEBUG: Init SoundManager...");
        try {
            soundManager = SoundManager.getInstance();
        } catch (Exception e) {
            System.err.println("SoundManager Init Failed: " + e.getMessage());
        }

        // 2. Assets laden (mit Fehlerprüfung)
        System.out.println("DEBUG: Load Image...");
        try {
          var imgStream = getClass().getResourceAsStream("/images/ball3.png");
          if (imgStream != null) {
              targetImage = new Image(imgStream);
          } else {
              System.err.println("WARNING: Image /images/ball3.png not found!");
          }
        } catch (Exception e) {
          System.err.println("Could not load target image: " + e.getMessage());
        }

        // 3. Daten laden (Nur wenn DB da ist)
        if (dbConnection != null) {
            try { loadPreferences(); } catch (Exception e) { System.err.println("Prefs load error: " + e); }
            try { loadBestScore(); } catch (Exception e) { System.err.println("Score load error: " + e); }
            try { loadGameHistory(); } catch (Exception e) { System.err.println("History load error: " + e); }
        }

        // 4. UI Init
        initUI();
        initKeyListeners();

        // 5. Game Loop starten
        timer = new AnimationTimer() {
          public void handle(long now) {
            if (gameActive) {
              updateAndRender();
              updateGameTimer();
            }
          }
        };
        timer.start();
        
        System.out.println("DEBUG: Controller initialize DONE");

    } catch (Throwable t) {
        // DER RETTER: Zeigt den wahren Fehler auf dem Handy an!
        t.printStackTrace();
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Init Error");
            alert.setHeaderText("Controller abgestürzt");
            alert.setContentText(t.getClass().getSimpleName() + ": " + t.getMessage());
            alert.showAndWait();
        });
    }
  }

  private void initUI() {
    // Game State Label setup
    gameStateLabel = new Label("Click anywhere to start!");
    gameStateLabel.setStyle(
      "-fx-font-size: 24px; -fx-text-fill: white; -fx-font-weight: bold;"
    );
    gameArea.getChildren().add(gameStateLabel);
    centerGameStateLabel();

    // Button Listeners - Main
    modeBtn.setOnAction(e -> switchMode());
    sizeBtn.setOnAction(e -> changeTargetSize());
    stopBtn.setOnAction(e -> toggleGameState());
    toggleTargetBtn.setOnAction(e -> toggleTargetImageMode());

    // Button Listeners - Sound
    classicBtn.setOnAction(e -> setSound(SoundOption.CLASSIC));
    beepBtn.setOnAction(e -> setSound(SoundOption.BEEP));
    popBtn.setOnAction(e -> setSound(SoundOption.POP));
    laserBtn.setOnAction(e -> setSound(SoundOption.LASER));
    customBtn.setOnAction(e -> setSound(SoundOption.CUSTOM));
    uploadBtn.setOnAction(e -> handleUpload());

    // Button Listeners - Effects
    toggleHitEffectBtn.setOnAction(e -> switchHitEffect());
    hitEffectOnOffBtn.setOnAction(e -> toggleHitEffects());

    // Initial UI Updates
    updateSoundButtonSelection();
    updateSizeButtonText();
    updateHitEffectButtons();
    updateTargetImageButton();
    modeBtn.setText("Mode: " + currentMode.name());
    modeLabel.setText(currentMode.name());

    // Mouse Handlers
    gameArea.setOnMouseClicked(e -> handleMouseClick(e.getX(), e.getY()));
    gameArea.setOnMouseMoved(e ->
      currentMousePosition = new Point2D(e.getX(), e.getY())
    );
    gameArea.setOnMouseEntered(e -> {
      if (!gameArea.isFocused() && !historyMode) gameArea.requestFocus();
    });

    // Window Resize Listeners
    gameArea.widthProperty().addListener((obs, o, n) -> centerGameStateLabel());
    gameArea
      .heightProperty()
      .addListener((obs, o, n) -> centerGameStateLabel());

    gameArea.setFocusTraversable(true);
    gameArea.requestFocus();
  }

  private void initKeyListeners() {
    // History Navigation Keys
    gameArea.setOnKeyPressed(event -> {
      if (historyMode) {
        switch (event.getCode()) {
          case LEFT:
          case A:
            navigateHistory(-1);
            event.consume();
            break;
          case RIGHT:
          case D:
            navigateHistory(1);
            event.consume();
            break;
          case BACK_SPACE:
            deleteCurrentHistoryEntry();
            event.consume();
            break;
        }
      }
    });

    // Global Keys (nach Scene Load)
    Platform.runLater(() -> {
      scene = gameArea.getScene();
      if (scene != null) setupGlobalKeyBindings(scene);
    });
  }

  private void setupGlobalKeyBindings(Scene scene) {
    // KEY PRESS
    scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
      if (event.getTarget() instanceof TextInputControl) return;

      // Shoot (M)
      if (
        event.getCode() == KeyCode.M &&
        gameActive &&
        !historyMode &&
        !mKeyPressed
      ) {
        mKeyPressed = true;
        handleGameAreaClick(
          currentMousePosition.getX(),
          currentMousePosition.getY()
        );
        event.consume();
        return;
      }

      // Volume (W/S)
      if (event.getCode() == KeyCode.W) {
        changeVolume(0.05);
        event.consume();
        return;
      } else if (event.getCode() == KeyCode.S) {
        changeVolume(-0.05);
        event.consume();
        return;
      }

      // Controls (H, ESC, ENTER)
      if (event.getCode() == KeyCode.H && !gameActive && !historyMode) {
        enterHistoryMode();
        event.consume();
      } else if (event.getCode() == KeyCode.ESCAPE) {
        if (historyMode) exitHistoryMode();
        else if (gameActive) stopGame(true);
        event.consume();
      } else if (
        event.getCode() == KeyCode.ENTER && !gameActive && !historyMode
      ) {
        startNewGame();
        event.consume();
      } else if (event.getCode() == KeyCode.BACK_SPACE && historyMode) {
        deleteCurrentHistoryEntry();
      } else if (event.getCode() == KeyCode.DELETE && historyMode) {
        deleteHistory();
      }
    });

    // KEY RELEASE (Reset M)
    scene.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
      if (event.getCode() == KeyCode.M) mKeyPressed = false;
    });
  }

  // ============================================================================================
  // GAME LOGIC & LOOP
  // ============================================================================================
  private void startNewGame() {
    if (gameActive) return;
    resetGameStats();
    gameActive = true;
    gameStartTime = System.currentTimeMillis();
    clearGameArea();
    gameArea.requestFocus();
    startRound(currentMode);
    stopBtn.setText("Stop Game");
    updateLabels();
    Platform.runLater(() -> gameArea.requestFocus());
  }

  private void startRound(Target.Mode mode) {
    if (!gameActive) return;
    if (historyMode) exitHistoryMode();
    clearGameArea();
    currentMode = mode;
    double width = Math.max(gameArea.getWidth(), 1.0);
    double height = Math.max(gameArea.getHeight(), 1.0);

    List<Target> spawned = manager.spawnForMode(
      mode,
      width,
      height,
      margin,
      targetRadius
    );
    for (Target t : spawned) {
      targets.add(t);
      addTargetToScene(t);
    }

    modeBtn.setText("Mode: " + currentMode.name());
    modeLabel.setText(currentMode.name());
  }

  private void updateAndRender() {
    if (targets.isEmpty()) return;
    double width = gameArea.getWidth(), height = gameArea.getHeight();

    // 2. Update Physics & Cleanup
    List<Target> toRemove = new ArrayList<>();
    for (Target t : new ArrayList<>(targets)) {
      if (t.mode() == Target.Mode.BOUNCE) t.update(width, height, rng);
      if (!t.isAlive()) toRemove.add(t);

      Circle c = viewMap.get(t);
      if (c != null) {
        c.setCenterX(t.pos().x());
        c.setCenterY(t.pos().y());
      }
    }

    for (Target dead : toRemove) {
      removeTargetVisuals(dead);
      targets.remove(dead);
    }
  }

  private void handleGameAreaClick(double x, double y) {
    Target bestHit = null;
    double bestDistance = Double.MAX_VALUE;
    for (Target t : targets) {
      if (!t.isAlive()) continue;
      double dx = x - t.pos().x();
      double dy = y - t.pos().y();
      double distance = Math.sqrt(dx * dx + dy * dy);
      double tolerance = (t.mode() == Target.Mode.BOUNCE) ? 8.0 : 3.0;

      if (distance <= (t.radius() + tolerance) && distance < bestDistance) {
        bestHit = t;
        bestDistance = distance;
      }
    }
    if (bestHit != null) onTargetHit(bestHit);
    else handleMissClick();
  }

  private void handleMouseClick(double x, double y) {
    if (!gameActive && !historyMode) {
      startNewGame();
    } else if (gameActive) {
      // Ruft SOFORT die Treffer-Prüfung auf
      handleGameAreaClick(x, y);
    }
  }

  private void onTargetHit(Target t) {
    soundManager.playHit();

    // Logic: Reaction Time
    long currentTime = System.currentTimeMillis();
    if (lastHitTime > 0) {
      totalReactionTime += (currentTime - lastHitTime) / 1000.0;
    }
    lastHitTime = currentTime;

    // Logic: Visual Removal
    if (hitEffectsEnabled) createHitEffect(
      t.pos().x(),
      t.pos().y(),
      t.radius()
    );
    if (hitEffectsEnabled) animateTargetDeath(t);
    else removeTargetVisuals(t);

    targets.remove(t);
    t.markHit();

    // Logic: Score
    score += 10;
    hits++;
    combo++;
    if (combo > maxCombo) maxCombo = combo;
    updateLabels();

    // Logic: Respawn
    handleRespawn(t);
  }

  private void handleRespawn(Target t) {
    double width = Math.max(gameArea.getWidth(), 1.0);
    double height = Math.max(gameArea.getHeight(), 1.0);

    if (currentMode == Target.Mode.BOUNCE) {
      Target slug = manager.spawnSlug(width, height, 40, t.radius(), targets);
      targets.add(slug);
      addTargetToScene(slug);
      return;
    }

    Target replacement = null;
    if (currentMode == Target.Mode.SNIPER) {
      replacement = Target.createSniperReplacement(
        rng,
        width,
        height,
        margin,
        t.radius(),
        targets
      );
    } else if (currentMode == Target.Mode.RADIAL) {
      replacement = Target.createRadialReplacement(
        rng,
        width,
        height,
        margin,
        t.radius(),
        targets
      );
    }

    if (replacement != null) {
      targets.add(replacement);
      addTargetToScene(replacement);
    }
  }

  private void handleMissClick() {
    missclicks++;
    combo = 0;
    updateLabels();
  }

  private void stopGame(boolean showMsg) {
    if (!gameActive) return;
    gameActive = false;
    saveGameResults();
    clearGameArea();

    if (showMsg) {
      showGameOverMessage();
      stopBtn.setText("Start Game");
    } else {
      startNewGame();
      stopBtn.setText("Stop Game");
    }
    loadBestScore();
    Platform.runLater(() -> gameArea.requestFocus());
  }

  private void endGame() {
    if (!gameActive) return;
    saveGameResults();
    gameActive = false;
    clearGameArea();
    showGameOverMessage();
    stopBtn.setText("Start Game");
  }

  private void updateGameTimer() {
    long elapsed = (System.currentTimeMillis() - gameStartTime) / 1000;
    int timeLeft = gameDuration - (int) elapsed;
    if (timeLeft <= 0) endGame();
    else timeLabel.setText(String.valueOf(timeLeft));
  }

  private void toggleGameState() {
    if (gameActive) stopGame(true);
    else startNewGame();
  }

  public void stop() {
    if (timer != null) timer.stop();
  }

  // ============================================================================================
  // VISUALS & RENDERING (TARGETS & EFFECTS)
  // ============================================================================================
  private void addTargetToScene(Target t) {
    Circle circle = new Circle(t.pos().x(), t.pos().y(), t.radius());
    boolean isImageMode =
      useTargetImage && targetImage != null && !targetImage.isError();

    // Appearance (Fill/Stroke)
    if (isImageMode) {
      circle.setFill(new ImagePattern(targetImage));
      circle.setStroke(Color.TRANSPARENT);

      RotateTransition rt = new RotateTransition();
      rt.setNode(circle); // Welches Objekt soll sich drehen?
      rt.setDuration(Duration.seconds(2)); // Dauer für EINE volle Umdrehung
      rt.setByAngle(360); // Drehung um 360 Grad
      rt.setCycleCount(Animation.INDEFINITE); // Unendlich oft wiederholen

      // Linearer Interpolator sorgt für gleichmäßige Geschwindigkeit
      // (Ohne das bremst der Kreis am Ende jeder Runde kurz ab)
      rt.setInterpolator(Interpolator.LINEAR);

      // 3. Starten
      rt.play();
    } else {
      circle.setFill(t.mode() == Target.Mode.BOUNCE ? Color.ORANGE : Color.RED);
      circle.setStroke(Color.DARKRED);
    }

    // Scale Animations
    ScaleTransition scaleUp = new ScaleTransition(Duration.millis(200), circle);
    scaleUp.setToX(1.10);
    scaleUp.setToY(1.10);
    ScaleTransition scaleDown = new ScaleTransition(
      Duration.millis(150),
      circle
    );
    scaleDown.setToX(1.0);
    scaleDown.setToY(1.0);
    circle.setOnMouseEntered(e -> scaleUp.play());
    circle.setOnMouseExited(e -> scaleDown.play());

    // Stroke Width
    double strokeWidth = isImageMode ? 0 : getStrokeWidthForSize();
    circle.setStrokeWidth(strokeWidth);
    circle.setStrokeType(StrokeType.INSIDE);

     // Glow Effect
    if(useTargetImage ==false){
     DropShadow glow = new DropShadow();
     glow.setColor(Color.WHITE);
     glow.setBlurType(BlurType.GAUSSIAN);
     glow.setRadius(getGlowSize());
     glow.setSpread(0.4);
     glow.setInput(new javafx.scene.effect.Blend());
     circle.setEffect(glow);
    }
    

    viewMap.put(t, circle);
    gameArea.getChildren().add(circle);
  }

  private void animateTargetDeath(Target t) {
    Circle c = viewMap.remove(t);
    if (c != null) {
      FadeTransition fade = new FadeTransition(Duration.millis(100), c);
      fade.setFromValue(1.0);
      fade.setToValue(0.0);
      fade.setOnFinished(e -> gameArea.getChildren().remove(c));
      fade.play();
    }
  }

  private void removeTargetVisuals(Target t) {
    Circle c = viewMap.remove(t);
    if (c != null) gameArea.getChildren().remove(c);
  }

  private void clearGameArea() {
    targets.clear();
    viewMap.clear();
    gameArea.getChildren().clear();
  }

  private void showGameOverMessage() {
    gameStateLabel.setText(
      String.format(
        "Game Over!\nScore: %d\nAccuracy: %s\nMax Combo: %d\nTarget Size: %s\nClick to play again",
        score,
        accuracyLabel.getText(),
        maxCombo,
        currentSize.toString()
      )
    );
    gameStateLabel.setStyle(
      "-fx-font-size: 20px; -fx-text-fill: white; -fx-font-weight: bold;"
    );
    gameArea.getChildren().add(gameStateLabel);
    centerGameStateLabel();
  }

  private void centerGameStateLabel() {
    if (gameStateLabel != null) {
      gameStateLabel.setLayoutX(
        (gameArea.getWidth() - gameStateLabel.getWidth()) / 2
      );
      gameStateLabel.setLayoutY(
        (gameArea.getHeight() - gameStateLabel.getHeight()) / 2
      );
    }
  }

  // --- Effects Helper ---
  private void createHitEffect(double x, double y, double radius) {
    if (
      currentHitEffect == HitEffect.EXPAND_CONTRACT
    ) createExpandContractEffect(x, y, radius);
    else createSparkleEffect(x, y, radius);
  }

  private void createExpandContractEffect(double x, double y, double radius) {
    Circle ring = new Circle(x, y, radius);
    ring.setFill(null);
    ring.setStroke(
      currentMode == Target.Mode.BOUNCE ? Color.ORANGE : Color.DARKRED
    );
    if (useTargetImage) {
      ring.setStroke(Color.DARKKHAKI);
    }
    ring.setStrokeWidth(4.0);
    ring.setOpacity(0.8);
    gameArea.getChildren().add(ring);

    Timeline timeline = new Timeline();
    Duration expandTime = getExpandDuration();
    Duration delayTime = getContractDelay();
    Duration totalTime = expandTime.add(delayTime).add(getContractDuration());
    double maxRadius = radius + 50;

    timeline
      .getKeyFrames()
      .addAll(
        new KeyFrame(
          Duration.ZERO,
          new KeyValue(ring.radiusProperty(), radius),
          new KeyValue(ring.opacityProperty(), 0.8)
        ),
        new KeyFrame(
          expandTime,
          new KeyValue(ring.radiusProperty(), maxRadius),
          new KeyValue(ring.opacityProperty(), 0.6)
        ),
        new KeyFrame(
          totalTime,
          new KeyValue(ring.radiusProperty(), 0),
          new KeyValue(ring.opacityProperty(), 0.0)
        )
      );
    timeline.setOnFinished(e -> gameArea.getChildren().remove(ring));
    timeline.play();
  }

  private void createSparkleEffect(double x, double y, double radius) {
    int count = getSparkleCount();
    double size = getSparkleSize();
    double dist = getSparkleDistance();
    Duration dur = getSparkleDuration();

    for (int i = 0; i < count; i++) {
      double angle = (i * 2 * Math.PI) / count;
      Circle sparkle = new Circle(
        x + Math.cos(angle) * radius * 0.5,
        y + Math.sin(angle) * radius * 0.5,
        size
      );
      sparkle.setFill(Color.CYAN);
      sparkle.setOpacity(0.9);
      gameArea.getChildren().add(sparkle);

      TranslateTransition move = new TranslateTransition(dur, sparkle);
      move.setByX(Math.cos(angle) * dist);
      move.setByY(Math.sin(angle) * dist);
      RotateTransition rotate = new RotateTransition(dur, sparkle);
      rotate.setByAngle(720);
      FadeTransition fade = new FadeTransition(dur, sparkle);
      fade.setToValue(0.0);

      ParallelTransition pt = new ParallelTransition(
        sparkle,
        move,
        rotate,
        fade
      );
      pt.setOnFinished(e -> gameArea.getChildren().remove(sparkle));
      pt.play();
    }
  }

  // ============================================================================================
  // SETTINGS & CONFIGURATION (MODES, SIZES, SOUNDS)
  // ============================================================================================

  // --- Mode ---
  private void switchMode() {
    switch (currentMode) {
      case SNIPER -> currentMode = Target.Mode.RADIAL;
      case RADIAL -> currentMode = Target.Mode.BOUNCE;
      case BOUNCE -> currentMode = Target.Mode.SNIPER;
    }
    modeLabel.setText(currentMode.name());
    loadBestScore();
    saveCurrentPreferences();
    if (gameActive) stopGame(false);
  }

  // --- Size ---
  private void changeTargetSize() {
    switch (currentSize) {
      case SMALL -> {
        currentSize = TargetSize.MEDIUM;
        targetRadius = 45;
      }
      case MEDIUM -> {
        currentSize = TargetSize.LARGE;
        targetRadius = 65;
      }
      case LARGE -> {
        currentSize = TargetSize.SMALL;
        targetRadius = 30;
      }
    }
    updateSizeButtonText();
    saveCurrentPreferences();
    if (gameActive) startRound(currentMode);
    gameArea.requestFocus();
  }

  // --- Sound ---
  private void setSound(SoundOption option) {
    currentSound = option;
    if (option == SoundOption.CUSTOM && !soundManager.hasCustomSound()) {
      System.out.println("Warning: No custom sound loaded.");
    }
    soundManager.playSound(currentSound);
    soundManager.setCurrentSound(currentSound);
    updateSoundButtonSelection();
    saveCurrentPreferences();
    gameArea.requestFocus();
  }

  private void handleUpload() {
    if (SoundManager.getInstance().chooseCustomSound(null)) {
      setSound(SoundOption.CUSTOM);
      SoundManager.getInstance().playHit();
    }
  }

  private void changeVolume(double delta) {
    soundManager.changeVolume(delta);
    if (volumeLabel != null) volumeLabel.setText(
      String.format("%.2f%%", soundManager.getVolume() * 100)
    );
  }

  // --- Effects ---
  private void switchHitEffect() {
    currentHitEffect = (currentHitEffect == HitEffect.EXPAND_CONTRACT)
      ? HitEffect.SPARKLE
      : HitEffect.EXPAND_CONTRACT;
    saveCurrentPreferences();
    updateHitEffectButtons();
  }

  private void toggleHitEffects() {
    hitEffectsEnabled = !hitEffectsEnabled;
    saveCurrentPreferences();
    updateHitEffectButtons();
  }

  // --- Target Image ---
  private void toggleTargetImageMode() {
    useTargetImage = !useTargetImage;
    saveCurrentPreferences();
    updateTargetImageButton();

    clearGameArea();

    double width = Math.max(gameArea.getWidth(), 1.0);
    double height = Math.max(gameArea.getHeight(), 1.0);

    List<Target> spawned = manager.spawnForMode(
      currentMode,
      width,
      height,
      margin,
      targetRadius
    );
    for (Target t : spawned) {
      targets.add(t);
      addTargetToScene(t);
    }
    gameArea.requestFocus();
  }

  public void handleSpeedBtn() {
    try {
      Target.setSpeed(Double.parseDouble(speedButton.getText()));
      saveCurrentPreferences();
    } catch (NumberFormatException ignored) {}
    gameArea.requestFocus();
  }

  // --- UI Update Helpers ---
  private void updateSizeButtonText() {
    sizeBtn.setText(
      "Size: " + currentSize.toString() + " (" + (int) targetRadius + " px)"
    );
  }

  private void updateSoundButtonSelection() {
    for (Button b : Arrays.asList(
      classicBtn,
      beepBtn,
      popBtn,
      laserBtn,
      customBtn
    )) b.getStyleClass().remove("sound-option-selected");

    switch (currentSound) {
      case CLASSIC -> classicBtn.getStyleClass().add("sound-option-selected");
      case BEEP -> beepBtn.getStyleClass().add("sound-option-selected");
      case POP -> popBtn.getStyleClass().add("sound-option-selected");
      case LASER -> laserBtn.getStyleClass().add("sound-option-selected");
      case CUSTOM -> customBtn.getStyleClass().add("sound-option-selected");
    }
  }

  private void updateHitEffectButtons() {
    toggleHitEffectBtn.setText(
      currentHitEffect == HitEffect.EXPAND_CONTRACT
        ? "Switch to Sparkle"
        : "Switch to Expand"
    );
    hitEffectOnOffBtn.setText(
      hitEffectsEnabled ? "Turn hit effects OFF" : "Turn hit effects ON"
    );
    hitEffectOnOffBtn
      .getStyleClass()
      .removeAll("hit-effect-toggle-active", "hit-effect-toggle-inactive");
    hitEffectOnOffBtn
      .getStyleClass()
      .add(
        hitEffectsEnabled
          ? "hit-effect-toggle-active"
          : "hit-effect-toggle-inactive"
      );
  }

  private void updateTargetImageButton() {
    toggleTargetBtn.setText(
      useTargetImage ? "Switch to Colors" : "Switch to Image"
    );
    toggleTargetBtn.setStyle(
      useTargetImage
        ? "-fx-background-color: #4CAF50; -fx-text-fill: white;"
        : ""
    );
  }

  private void updateLabels() {
    scoreLabel.setText(String.valueOf(score));
    missclicksLabel.setText(String.valueOf(missclicks));
    comboLabel.setText(String.valueOf(combo));
    if (hits > 0) {
      accuracyLabel.setText(
        String.format("%.1f%%", ((double) hits / (hits + missclicks)) * 100)
      );
      avgTimeLabel.setText(String.format("%.2fs", totalReactionTime / hits));
    } else {
      accuracyLabel.setText("0%");
      avgTimeLabel.setText("0.00s");
    }
  }

  // ============================================================================================
  // DATA PERSISTENCE & HISTORY
  // ============================================================================================
  private void loadPreferences() {
    DatabaseConnection.Preferences prefs = dbConnection.getPreferences();

    try {
      currentMode = Target.Mode.valueOf(prefs.mode());
    } catch (Exception e) {
      currentMode = Target.Mode.BOUNCE;
    }
    Target.setSpeed(prefs.speed());
    if (speedButton != null) speedButton.setText(String.valueOf(prefs.speed()));

    try {
      currentSound = SoundOption.valueOf(prefs.sound());
      if (prefs.customSoundPath() != null) soundManager.setCustomSoundPath(
        prefs.customSoundPath()
      );
      soundManager.setCurrentSound(currentSound);
    } catch (Exception e) {
      currentSound = SoundOption.CLASSIC;
    }

    try {
      currentSize = TargetSize.valueOf(prefs.size());
      targetRadius = switch (currentSize) {
        case SMALL -> 30;
        case LARGE -> 65;
        default -> 45;
      };
    } catch (Exception e) {
      currentSize = TargetSize.MEDIUM;
      targetRadius = 45;
    }

    try {
      currentHitEffect = HitEffect.valueOf(prefs.effect());
    } catch (Exception e) {
      currentHitEffect = HitEffect.EXPAND_CONTRACT;
    }

    hitEffectsEnabled = prefs.hitEffectsEnabled();
    useTargetImage = prefs.useTarget();
  }

  private void saveCurrentPreferences() {
    String path = soundManager.getCustomSoundPath();
    if (path == null) path = dbConnection.getPreferences().customSoundPath();

    dbConnection.setPreferences(
      currentMode.name(),
      Target.getSpeed(),
      currentSound.name(),
      currentSize.name(),
      currentHitEffect.name(),
      path,
      hitEffectsEnabled,
      useTargetImage
    );
  }

  private void saveGameResults() {
    double acc = hits > 0 ? ((double) hits / (hits + missclicks)) * 100 : 0;
    double avg = hits > 0 ? totalReactionTime / hits : 0;
    dbConnection.saveGame(
      score,
      missclicks,
      acc,
      combo,
      avg,
      currentMode.name(),
      currentSize.toString()
    );
    loadBestScore();
    loadGameHistory();
  }

  private void loadBestScore() {
    bestScoreLabel.setText(
      "\n" + dbConnection.getBestScoreForMode(currentMode.name())
    );
  }

  private void resetGameStats() {
    score = 0;
    missclicks = 0;
    combo = 0;
    maxCombo = 0;
    hits = 0;
    totalReactionTime = 0;
    lastHitTime = 0;
    updateLabels();
  }

  // --- History Logic ---
  public void enterHistoryMode() {
    loadGameHistory();
    if (gameHistory.isEmpty()) {
      System.out.println("No history.");
      return;
    }
    historyMode = true;
    gameActive = false;
    clearGameArea();
    historyIndex = gameHistory.size() - 1;
    showCurrentHistory();
  }

  private void exitHistoryMode() {
    historyMode = false;
    clearGameArea();
    gameStateLabel.setText("Click anywhere to start!");
    gameStateLabel.setStyle(
      "-fx-font-size: 24px; -fx-text-fill: white; -fx-font-weight: bold;"
    );
    gameArea.getChildren().add(gameStateLabel);
    centerGameStateLabel();
  }

  private void loadGameHistory() {
    gameHistory.clear();
    String sql = "SELECT * FROM games ORDER BY game_id ASC";
    try (
      java.sql.Statement stmt = dbConnection.getConnection().createStatement();
      ResultSet rs = stmt.executeQuery(sql)
    ) {
      while (rs.next()) {
        String tSize = "MEDIUM";
        try {
          tSize = rs.getString("target_size");
        } catch (Exception e) {}
        gameHistory.add(
          new GameRecord(
            rs.getLong("game_id"),
            rs.getString("timestamp"),
            rs.getInt("score"),
            rs.getInt("missclicks"),
            rs.getDouble("accuracy"),
            rs.getInt("combo"),
            rs.getDouble("avg_time"),
            rs.getString("mode"),
            tSize
          )
        );
      }
    } catch (Exception e) {
      System.err.println("Load Error: " + e.getMessage());
    }
  }

   // ============================================================================================
// TOUCH-FREUNDLICHE HISTORY NAVIGATION (ERSETZEN SIE DIE ALTE METHODE DAMIT)
// ============================================================================================
// ============================================================================================
// FINALE NAVIGATIONS-METHODE MIT "DELETE ALL"
// ============================================================================================
private void showCurrentHistory() {
    if (gameHistory.isEmpty()) return;
    
    clearGameArea();
    GameRecord current = gameHistory.get(historyIndex);

    // Hauptcontainer
    javafx.scene.layout.VBox mainContainer = new javafx.scene.layout.VBox(15);
    mainContainer.setAlignment(javafx.geometry.Pos.CENTER);
    mainContainer.layoutXProperty().bind(gameArea.widthProperty().subtract(mainContainer.widthProperty()).divide(2));
    mainContainer.layoutYProperty().bind(gameArea.heightProperty().subtract(mainContainer.heightProperty()).divide(2));

    // A. Info Karte
    Label historyLabel = new Label(current.toDisplayString());
    historyLabel.setStyle(
        "-fx-font-size: 18px; -fx-text-fill: white; -fx-font-weight: bold; " +
        "-fx-background-color: rgba(0,0,0,0.85); -fx-padding: 20px; " + 
        "-fx-background-radius: 15px; -fx-border-color: gold; -fx-border-width: 2px;"
    );
    historyLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
    historyLabel.setWrapText(true);
    historyLabel.maxWidthProperty().bind(gameArea.widthProperty().multiply(0.9)); 
    
        // B. Navigation Buttons (Pfeile & Einzel-Löschen)
    // Verwende Unicode-Escapes für Sicherheit gegen Encoding-Fehler
    Button btnPrev = new Button("\u25C4"); // Linker Pfeil
    Button btnNext = new Button("\u25BA"); // Rechter Pfeil
    Button btnDel  = new Button("\u274C"); // Rotes Kreuz (X) statt Mülleimer-Emoji (sicherer)
    
    String bigBtnStyle = "-fx-font-size: 24px; -fx-min-width: 60px; -fx-min-height: 60px; -fx-background-radius: 10px; -fx-cursor: hand;";
    btnPrev.setStyle(bigBtnStyle + "-fx-base: #444; -fx-text-fill: white;");
    btnNext.setStyle(bigBtnStyle + "-fx-base: #444; -fx-text-fill: white;");
    btnDel.setStyle(bigBtnStyle + "-fx-base: #8B0000; -fx-text-fill: white;"); 

    btnPrev.setDisable(historyIndex == 0);
    btnNext.setDisable(historyIndex == gameHistory.size() - 1);

    btnPrev.setOnAction(e -> navigateHistory(-1));
    btnNext.setOnAction(e -> navigateHistory(1));
    btnDel.setOnAction(e -> deleteCurrentHistoryEntry());

    javafx.scene.layout.HBox navBar = new javafx.scene.layout.HBox(20, btnPrev, btnDel, btnNext);
    navBar.setAlignment(javafx.geometry.Pos.CENTER);

    // C. Untere Leiste (Close & CLEAR ALL)
    Button btnClose = new Button("Back");
    btnClose.setStyle("-fx-font-size: 16px; -fx-min-height: 40px; -fx-min-width: 100px; -fx-base: #222; -fx-text-fill: #aaa;");
    btnClose.setOnAction(e -> exitHistoryMode());

    // DER NEUE BUTTON
    // \u26A0 ist das Warn-Dreieck
    Button btnClearAll = new Button("\u26A0 Clear All History");
    btnClearAll.setStyle("-fx-font-size: 16px; -fx-min-height: 40px; -fx-base: red; -fx-text-fill: white; -fx-font-weight: bold;");
    btnClearAll.setOnAction(e -> deleteHistory()); 


    javafx.scene.layout.HBox bottomBar = new javafx.scene.layout.HBox(20, btnClose, btnClearAll);
    bottomBar.setAlignment(javafx.geometry.Pos.CENTER);

    // D. Zusammenbauen
    Label pageInfo = new Label(String.format("Game %d / %d", historyIndex + 1, gameHistory.size()));
    pageInfo.setStyle("-fx-text-fill: gold; -fx-font-weight: bold;");

    mainContainer.getChildren().addAll(historyLabel, pageInfo, navBar, bottomBar);
    gameArea.getChildren().add(mainContainer);
}



  private void navigateHistory(int dir) {
    if (gameHistory.isEmpty()) return;
    historyIndex += dir;
    if (historyIndex < 0) historyIndex = 0;
    if (historyIndex >= gameHistory.size()) historyIndex =
      gameHistory.size() - 1;
    showCurrentHistory();
  }

  private void deleteCurrentHistoryEntry() {
    if (gameHistory.isEmpty()) return;
    GameRecord current = gameHistory.get(historyIndex);
    Alert alert = new Alert(
      Alert.AlertType.CONFIRMATION,
      "Delete Game #" + current.gameId + "?"
    );
    alert.setHeaderText("Delete Entry?");

    if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
      if (dbConnection.deleteGame(current.gameId)) {
        gameHistory.remove(historyIndex);
        if (gameHistory.isEmpty()) exitHistoryMode();
        else {
          if (historyIndex >= gameHistory.size()) historyIndex =
            gameHistory.size() - 1;
          showCurrentHistory();
        }
      }
    }
  }

  private void deleteHistory() {
    if (gameHistory.isEmpty()) return;

    Alert alert = new Alert(
      Alert.AlertType.CONFIRMATION,
      "Delet Complet History"
    );
    alert.setHeaderText("Delete History?");

    if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
      dbConnection.clearHistory();
      exitHistoryMode();
    }
  }

  // ============================================================================================
  // CONFIG VALUES (CONSTANTS)
  // ============================================================================================
  private double getStrokeWidthForSize() {
    return switch (currentSize) {
      case SMALL -> 8.0;
      case MEDIUM -> 15;
      case LARGE -> 22;
    };
  }

  private double getGlowSize() {
    return switch (currentSize) {
      case SMALL -> 15.0;
      case MEDIUM -> 20.0;
      case LARGE -> 30.0;
    };
  }

  private Duration getExpandDuration() {
    return switch (currentSize) {
      case SMALL -> Duration.millis(100);
      case MEDIUM -> Duration.millis(120);
      case LARGE -> Duration.millis(140);
    };
  }

  private double getExpandScale() {
    return switch (currentSize) {
      case SMALL -> 30.0;
      case MEDIUM -> 40.0;
      case LARGE -> 50.0;
    };
  }

  private Duration getContractDuration() {
    return switch (currentSize) {
      case SMALL -> Duration.millis(50);
      case MEDIUM -> Duration.millis(80);
      case LARGE -> Duration.millis(115);
    };
  }

  private Duration getContractDelay() {
    return switch (currentSize) {
      case SMALL -> Duration.millis(100);
      case MEDIUM -> Duration.millis(250);
      case LARGE -> Duration.millis(350);
    };
  }

  private int getSparkleCount() {
    return switch (currentSize) {
      case SMALL -> 4;
      case MEDIUM -> 6;
      case LARGE -> 8;
    };
  }

  private double getSparkleSize() {
    return switch (currentSize) {
      case SMALL -> 4.0;
      case MEDIUM -> 6.0;
      case LARGE -> 9.0;
    };
  }

  private double getSparkleDistance() {
    return switch (currentSize) {
      case SMALL -> 100.0;
      case MEDIUM -> 150.0;
      case LARGE -> 200.0;
    };
  }

  private Duration getSparkleDuration() {
    return switch (currentSize) {
      case SMALL -> Duration.millis(400);
      case MEDIUM -> Duration.millis(600);
      case LARGE -> Duration.millis(800);
    };
  }

  // --- INNER CLASS: GAME RECORD ---
  public static class GameRecord {

    long gameId;
    String timestamp;
    int score;
    int missclicks;
    double accuracy;
    int combo;
    double avgTime;
    String mode;
    String targetSize;

    public GameRecord(
      long id,
      String ts,
      int sc,
      int mc,
      double acc,
      int cb,
      double avg,
      String md,
      String sz
    ) {
      this.gameId = id;
      this.timestamp = ts;
      this.score = sc;
      this.missclicks = mc;
      this.accuracy = acc;
      this.combo = cb;
      this.avgTime = avg;
      this.mode = md;
      this.targetSize = sz;
    }

    public String toDisplayString() {
    return String.format(
        // 🎮 = \uD83C\uDFAE  |  🕐 = \uD83D\uDD50
        "\uD83C\uDFAE GAME #%d\n\uD83D\uDD50 %s\n\n" +
        // 📊 = \uD83D\uDCCA  |  🎯 = \uD83C\uDFAF
        "\uD83D\uDCCA Score: %d\n\uD83C\uDFAF Acc: %.1f%%\n" +
        // ⚡ = \u26A1         |  ⏱️ = \u23F1 (oder \u231A für Uhr)
        "\u26A1 Combo: %d\n\u23F1 Avg: %.2fs\n" +
        // 🎮 = \uD83C\uDFAE  |  📐 = \uD83D\uDCD0
        "\uD83C\uDFAE Mode: %s\n\uD83D\uDCD0 Size: %s",
        gameId,
        timestamp,
        score,
        accuracy,
        combo,
        avgTime,
        mode,
        targetSize
    );
}

  }
}
