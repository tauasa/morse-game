package org.tauasa.apps.morsegame.ui;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.tauasa.apps.morsegame.audio.MorsePlayer;
import org.tauasa.apps.morsegame.game.GameMode;
import org.tauasa.apps.morsegame.game.GameService;
import org.tauasa.apps.morsegame.game.GuessResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The JavaFX user interface, managed as a Spring {@link Component}.
 *
 * <p>Built lazily when {@link #onStageReady(StageReadyEvent)} fires, which
 * happens once the JavaFX runtime has created the primary stage and the
 * launcher publishes the {@link StageReadyEvent}.
 *
 * <p>Game flow:
 * <ul>
 *   <li>A round starts: {@link GameService#newRound()} picks a target letter,
 *       and its Morse code is played automatically.</li>
 *   <li>The player clicks a letter in the grid.</li>
 *   <li>Correct → green flash, score updates, next round after a short pause.</li>
 *   <li>Wrong → red flash on the wrong button, streak resets, player retries.</li>
 *   <li>A "Replay" button replays the current letter's Morse at any time.</li>
 * </ul>
 */
@Component
public class GameView {

    private final GameService gameService;
    private final MorsePlayer player;

    // Letter buttons keyed by character, so we can flash the right one.
    private final Map<Character, Button> letterButtons = new HashMap<>();

    // Status / stats labels
    private Label promptLabel;
    private Label feedbackLabel;
    private Label scoreLabel;
    private Label streakLabel;
    private Label accuracyLabel;
    private Label morsePatternLabel;

    private Button replayButton;
    private GridPane grid;

    private boolean inputLocked = false; // true during the post-correct pause

    public GameView(GameService gameService, MorsePlayer player) {
        this.gameService = gameService;
        this.player      = player;
    }

    // ── Spring → JavaFX entry point ──────────────────────────────────────────────

    @EventListener
    public void onStageReady(StageReadyEvent event) {
        Stage stage = event.getStage();

        BorderPane root = new BorderPane();
        root.getStyleClass().add("game-root");
        root.setTop(buildHeader());
        root.setCenter(buildCenter());
        root.setBottom(buildStatsBar());

        Scene scene = new Scene(root, 720, 640);
        loadStylesheet(scene);

        stage.setTitle("Morse Trainer");
        stage.setScene(scene);
        stage.setMinWidth(560);
        stage.setMinHeight(560);
        stage.show();

        // Start the first round once the UI is visible.
        startNewRound();
    }

    // ── Header: prompt + controls ────────────────────────────────────────────────

    private VBox buildHeader() {
        Label title = new Label("MORSE TRAINER");
        title.getStyleClass().add("title");

        promptLabel = new Label("Listen and choose the letter");
        promptLabel.getStyleClass().add("prompt");

        feedbackLabel = new Label(" ");
        feedbackLabel.getStyleClass().add("feedback");

        morsePatternLabel = new Label(" ");
        morsePatternLabel.getStyleClass().add("morse-pattern");

        replayButton = new Button("\u25B6  Replay");   // ▶
        replayButton.getStyleClass().add("action-btn");
        replayButton.setOnAction(e -> playCurrent());

        Button skipButton = new Button("Skip \u21BB");  // ↻
        skipButton.getStyleClass().add("ghost-btn");
        skipButton.setOnAction(e -> startNewRound());

        // Mode selector
        ComboBox<GameMode> modeBox = new ComboBox<>();
        modeBox.getItems().setAll(GameMode.values());
        modeBox.setValue(gameService.getMode());
        modeBox.getStyleClass().add("mode-box");
        // Render the human-readable label rather than the enum name.
        modeBox.setCellFactory(cb -> new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(GameMode m, boolean empty) {
                super.updateItem(m, empty);
                setText(empty || m == null ? null : m.label());
            }
        });
        modeBox.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(GameMode m, boolean empty) {
                super.updateItem(m, empty);
                setText(empty || m == null ? null : m.label());
            }
        });
        modeBox.setOnAction(e -> {
            gameService.setMode(modeBox.getValue());
            rebuildGrid();
            startNewRound();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox controls = new HBox(10, replayButton, skipButton, spacer,
                                 new Label("Mode:"), modeBox);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.getStyleClass().add("controls");

        VBox header = new VBox(6, title, promptLabel, feedbackLabel,
                               morsePatternLabel, controls);
        header.setPadding(new Insets(20, 24, 12, 24));
        header.getStyleClass().add("header");
        return header;
    }

    // ── Center: alphabet grid ──────────────────────────────────────────────────────

    private StackPane buildCenter() {
        grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(12, 24, 12, 24));
        grid.getStyleClass().add("letter-grid");

        populateGrid();

        StackPane wrap = new StackPane(grid);
        wrap.getStyleClass().add("grid-wrap");
        return wrap;
    }

    /** Lay out the current mode's characters into a roughly-square grid. */
    private void populateGrid() {
        grid.getChildren().clear();
        letterButtons.clear();

        List<Character> chars = gameService.characters();
        int cols = computeColumns(chars.size());

        for (int i = 0; i < chars.size(); i++) {
            char c = chars.get(i);
            Button btn = new Button(String.valueOf(c));
            btn.getStyleClass().add("letter-btn");
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setMaxHeight(Double.MAX_VALUE);
            btn.setOnAction(e -> onLetterClicked(c));
            GridPane.setHgrow(btn, Priority.ALWAYS);
            GridPane.setVgrow(btn, Priority.ALWAYS);

            int row = i / cols;
            int col = i % cols;
            grid.add(btn, col, row);
            letterButtons.put(c, btn);
        }
    }

    private void rebuildGrid() {
        populateGrid();
    }

    /** Choose a column count that yields a pleasant near-square layout. */
    private static int computeColumns(int n) {
        if (n <= 10) return 5;     // digits → 2 rows of 5
        if (n <= 26) return 6;     // letters → 5 rows (last partial)
        return 6;                  // letters+digits → 6 rows
    }

    // ── Stats bar ────────────────────────────────────────────────────────────────

    private HBox buildStatsBar() {
        scoreLabel    = statLabel("Score: 0");
        streakLabel   = statLabel("Streak: 0  (best 0)");
        accuracyLabel = statLabel("Accuracy: —");

        Button resetButton = new Button("Reset Stats");
        resetButton.getStyleClass().add("ghost-btn");
        resetButton.setOnAction(e -> {
            gameService.resetStats();
            refreshStats();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(20, scoreLabel, streakLabel, accuracyLabel,
                            spacer, resetButton);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(12, 24, 18, 24));
        bar.getStyleClass().add("stats-bar");
        return bar;
    }

    private Label statLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("stat");
        return l;
    }

    // ── Round logic ────────────────────────────────────────────────────────────────

    private void startNewRound() {
        inputLocked = false;
        clearButtonStates();
        feedbackLabel.setText(" ");
        feedbackLabel.getStyleClass().removeAll("feedback-correct", "feedback-wrong");
        morsePatternLabel.setText(" ");
        promptLabel.setText("Listen and choose the letter");

        gameService.newRound();
        playCurrent();
    }

    /** Play the current target's Morse code on the audio thread. */
    private void playCurrent() {
        String morse = gameService.currentMorse();
        if (morse == null) return;

        if (!player.isAvailable()) {
            // No audio on this system — reveal the pattern so the game is still playable.
            morsePatternLabel.setText(morse);
            feedbackLabel.setText("(audio unavailable — pattern shown)");
            return;
        }
        replayButton.setDisable(true);
        player.playAsync(morse, () -> Platform.runLater(() -> replayButton.setDisable(false)));
    }

    private void onLetterClicked(char c) {
        if (inputLocked || !gameService.isRoundActive()) return;

        GuessResult result = gameService.guess(c);
        refreshStats();

        if (result.correct()) {
            inputLocked = true;
            flashCorrect(letterButtons.get(c));
            feedbackLabel.setText("\u2713  Correct!  " + result.target()
                                  + " = " + result.targetMorse());   // ✓
            setFeedbackStyle(true);
            morsePatternLabel.setText(result.targetMorse());

            // Advance to the next round after a short pause.
            PauseTransition pause = new PauseTransition(Duration.millis(1100));
            pause.setOnFinished(e -> startNewRound());
            pause.play();
        } else {
            flashWrong(letterButtons.get(c));
            feedbackLabel.setText("\u2717  Not " + result.guessed()
                                  + " — try again");                  // ✗
            setFeedbackStyle(false);
        }
    }

    // ── Visual feedback ────────────────────────────────────────────────────────────

    private void flashCorrect(Button btn) {
        if (btn == null) return;
        btn.getStyleClass().add("correct");
    }

    private void flashWrong(Button btn) {
        if (btn == null) return;
        if (!btn.getStyleClass().contains("wrong"))
            btn.getStyleClass().add("wrong");

        // Remove the red after a moment so the player can keep trying.
        PauseTransition fade = new PauseTransition(Duration.millis(600));
        fade.setOnFinished(e -> btn.getStyleClass().remove("wrong"));
        fade.play();
    }

    private void clearButtonStates() {
        for (Button b : letterButtons.values()) {
            b.getStyleClass().removeAll("correct", "wrong");
            b.setDisable(false);
        }
    }

    private void setFeedbackStyle(boolean correct) {
        feedbackLabel.getStyleClass().removeAll("feedback-correct", "feedback-wrong");
        feedbackLabel.getStyleClass().add(correct ? "feedback-correct" : "feedback-wrong");
    }

    // ── Stats refresh ────────────────────────────────────────────────────────────

    private void refreshStats() {
        var s = gameService.stats();
        scoreLabel.setText("Score: " + s.getCorrect());
        streakLabel.setText("Streak: " + s.getCurrentStreak()
                            + "  (best " + s.getBestStreak() + ")");
        accuracyLabel.setText(s.getTotal() == 0
            ? "Accuracy: —"
            : String.format("Accuracy: %.0f%%  (%d/%d)",
                            s.getAccuracy(), s.getCorrect(), s.getTotal()));
    }

    // ── Stylesheet ────────────────────────────────────────────────────────────────

    private static void loadStylesheet(Scene scene) {
        var url = GameView.class.getResource("/styles.css");
        if (url != null) scene.getStylesheets().add(url.toExternalForm());
    }
}
