package org.tauasa.apps.morsegame.game;

import java.util.List;
import java.util.Random;

import org.springframework.stereotype.Service;
import org.tauasa.apps.morsegame.audio.MorseConverter;

/**
 * Core game logic for the Morse learning game.
 *
 * <p>Holds the current round's target character, the selected {@link GameMode},
 * and the running {@link GameStats}. Pure logic — no JavaFX or audio code — so
 * it can be unit-tested directly.
 *
 * <p>Typical round flow:
 * <pre>
 *   char target = service.newRound();       // pick a new target, returns it
 *   String morse = service.currentMorse();   // play this via the audio layer
 *   GuessResult r = service.guess('A');      // player answers
 *   if (r.correct()) { ... }
 * </pre>
 */
@Service
public class GameService {

    private MorseConverter converter = new MorseConverter(); // Spring injects the shared bean, but allow default for tests
    private Random         random = new Random();

    private GameMode  mode    = GameMode.LETTERS;
    private final GameStats stats = new GameStats();

    private char    target;          // current round's target character
    private String  targetMorse;     // cached Morse for the target
    private boolean roundActive;     // true between newRound() and a correct guess

    public GameService() {
        
    }
    /** Spring injects the shared {@link MorseConverter} bean. */
    public GameService(MorseConverter converter) {
        this(converter, new Random());
    }

    /** Test-friendly constructor allowing a seeded Random for determinism. */
    public GameService(MorseConverter converter, Random random) {
        this.converter = converter;
        this.random    = random;
    }

    // ── Configuration ────────────────────────────────────────────────────────────

    public GameMode getMode() { return mode; }

    /** Change the character pool. Does not start a new round automatically. */
    public void setMode(GameMode mode) {
        this.mode = mode;
    }

    /** The characters currently in play (for laying out the grid). */
    public List<Character> characters() {
        return mode.characters();
    }

    /** The Morse code pattern for a single character (e.g. 'A' → ".-"). */
    public String morseFor(char c) {
        return converter.encode(String.valueOf(c));
    }

    // ── Round lifecycle ────────────────────────────────────────────────────────────

    /**
     * Start a new round by picking a random target from the current mode.
     * Avoids repeating the immediately-previous target when possible.
     *
     * @return the new target character.
     */
    public char newRound() {
        List<Character> pool = mode.characters();
        char next;
        do {
            next = pool.get(random.nextInt(pool.size()));
        } while (pool.size() > 1 && next == target);

        target      = next;
        targetMorse = converter.encode(String.valueOf(target));
        roundActive = true;
        return target;
    }

    /** The current target character (0 if no round started). */
    public char currentTarget() { return target; }

    /** The Morse code for the current target (null if no round started). */
    public String currentMorse() { return targetMorse; }

    /** True if a round is in progress and awaiting a correct answer. */
    public boolean isRoundActive() { return roundActive; }

    /**
     * Submit a guess for the current round.
     *
     * <p>A correct guess ends the round (sets {@code roundActive=false}) and
     * records a point. An incorrect guess records a miss but leaves the round
     * active so the player can try again.
     *
     * @param guess the guessed character (case-insensitive for letters).
     * @return a {@link GuessResult} describing the outcome.
     * @throws IllegalStateException if no round is active.
     */
    public GuessResult guess(char guess) {
        if (!roundActive)
            throw new IllegalStateException("No active round. Call newRound() first.");

        char normalised = Character.toUpperCase(guess);
        boolean correct = normalised == target;

        if (correct) {
            stats.recordCorrect();
            roundActive = false;
        } else {
            stats.recordIncorrect();
        }

        return new GuessResult(correct, normalised, target, targetMorse);
    }

    // ── Stats ────────────────────────────────────────────────────────────────────

    public GameStats stats() { return stats; }

    /** Reset all statistics (does not start a new round). */
    public void resetStats() {
        stats.reset();
    }
}
