package org.tauasa.apps.morsegame.ui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.tauasa.apps.morsegame.audio.MorsePlayer;
import org.tauasa.apps.morsegame.game.GameMode;
import org.tauasa.apps.morsegame.game.GameService;
import org.tauasa.apps.morsegame.game.GuessResult;

import javafx.animation.PauseTransition;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

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

    // The dot/dash pattern label inside each button, keyed by character,
    // so we can show/hide them together when the toggle changes.
    private final Map<Character, Label> patternLabels = new HashMap<>();

    // When true, each grid button shows its Morse pattern below the letter.
    private boolean showPatterns = false;

    // Status / stats labels
    private Label promptLabel;
    private Label feedbackLabel;
    private Label scoreLabel;
    private Label streakLabel;
    private Label accuracyLabel;
    private Label morsePatternLabel;

    private Button replayButton;
    private GridPane grid;

    // Captured from StageReadyEvent — needed for the About dialog.
    private Stage        primaryStage;
    private HostServices hostServices;

    private boolean inputLocked = false; // true during the post-correct pause

    public GameView(GameService gameService, MorsePlayer player) {
        this.gameService = gameService;
        this.player      = player;
    }

    // ── Spring → JavaFX entry point ──────────────────────────────────────────────

    @EventListener
    public void onStageReady(StageReadyEvent event) {
        Stage stage = event.getStage();
        this.primaryStage = stage;
        this.hostServices = event.getHostServices();

        BorderPane root = new BorderPane();
        root.getStyleClass().add("game-root");
        // Menu bar sits above the header, both stacked in the top region.
        root.setTop(new VBox(buildMenuBar(), buildHeader()));
        root.setCenter(buildCenter());
        root.setBottom(buildStatsBar());

        Scene scene = new Scene(root, 720, 670);
        loadStylesheet(scene);

        stage.setTitle("Morse Trainer");
        stage.setScene(scene);
        stage.setMinWidth(560);
        stage.setMinHeight(560);
        stage.show();

        // Start the first round once the UI is visible.
        startNewRound();
    }

    // ── Menu bar ────────────────────────────────────────────────────────────────

    private MenuBar buildMenuBar() {
        // File > Exit
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> Platform.exit());
        Menu fileMenu = new Menu("File");
        fileMenu.getItems().add(exitItem);

        // Help > About
        MenuItem aboutItem = new MenuItem("About");
        aboutItem.setOnAction(e -> showAboutDialog());
        Menu helpMenu = new Menu("Help");
        helpMenu.getItems().add(aboutItem);

        MenuBar bar = new MenuBar(fileMenu, helpMenu);
        bar.getStyleClass().add("menu-bar");
        return bar;
    }

    // ── About dialog ────────────────────────────────────────────────────────────

    private void showAboutDialog() {
        Stage dialog = new Stage();
        dialog.initOwner(primaryStage);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("About Morse Trainer");
        dialog.setResizable(false);

        Label logo = new Label("\u00B7\u2212\u2212");   // ·−−  (Morse "W")
        logo.getStyleClass().add("about-logo");

        Label name = new Label("Morse Trainer");
        name.getStyleClass().add("about-name");

        Label version = new Label("Version 1.0.0");
        version.getStyleClass().add("about-version");

        Label tagline = new Label("Learn Morse code by ear.");
        tagline.getStyleClass().add("about-tagline");

        Label copyright = new Label("Copyright \u00A9 2026 Tauasa Timoteo\n"
                                    + "Released under the MIT License.");
        copyright.getStyleClass().add("about-copyright");
        copyright.setWrapText(true);
        copyright.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        // Clickable GitHub link — opens in the system browser via HostServices.
        Hyperlink repoLink = new Hyperlink("github.com/tauasa/morse-game");
        repoLink.getStyleClass().add("about-link");
        final String repoUrl = "https://github.com/tauasa/morse-game";
        repoLink.setOnAction(e -> {
            if (hostServices != null) {
                hostServices.showDocument(repoUrl);
            }
        });

        Button okButton = new Button("OK");
        okButton.getStyleClass().add("action-btn");
        okButton.setDefaultButton(true);
        okButton.setOnAction(e -> dialog.close());

        VBox content = new VBox(10, logo, name, version, tagline,
                                copyright, repoLink, okButton);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(28, 36, 24, 36));
        content.getStyleClass().add("about-dialog");

        Scene scene = new Scene(content);
        loadStylesheet(scene);
        dialog.setScene(scene);
        dialog.showAndWait();
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

        // Toggle: show the dot/dash pattern beneath each letter (a learning aid).
        CheckBox patternToggle = new CheckBox("Show patterns");
        patternToggle.getStyleClass().add("pattern-toggle");
        patternToggle.setSelected(showPatterns);
        patternToggle.setOnAction(e -> setShowPatterns(patternToggle.isSelected()));

        HBox controls = new HBox(10, replayButton, skipButton, spacer,
                                 patternToggle, new Label("Mode:"), modeBox);
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
        patternLabels.clear();

        List<Character> chars = gameService.characters();
        int cols = computeColumns(chars.size());

        for (int i = 0; i < chars.size(); i++) {
            char c = chars.get(i);

            // Letter on top, Morse pattern below — stacked in a VBox so the
            // pattern can be shown or hidden without changing the layout.
            Label letterLbl = new Label(String.valueOf(c));
            letterLbl.getStyleClass().add("letter-glyph");

            Label patternLbl = new Label(gameService.morseFor(c));
            patternLbl.getStyleClass().add("letter-pattern");
            patternLbl.setVisible(showPatterns);
            patternLbl.setManaged(showPatterns); // collapse space when hidden

            VBox content = new VBox(2, letterLbl, patternLbl);
            content.setAlignment(Pos.CENTER);

            Button btn = new Button();
            btn.setGraphic(content);
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
            patternLabels.put(c, patternLbl);
        }
    }

    /** Toggle the dot/dash pattern under every letter. */
    private void setShowPatterns(boolean show) {
        this.showPatterns = show;
        for (Label lbl : patternLabels.values()) {
            lbl.setVisible(show);
            lbl.setManaged(show);
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
