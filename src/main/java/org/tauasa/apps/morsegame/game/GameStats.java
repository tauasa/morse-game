package org.tauasa.apps.morsegame.game;

/**
 * Mutable score tracker for a game session.
 *
 * <p>Tracks correct/incorrect counts, the current answer streak, and the best
 * streak achieved. Accuracy is derived. Designed to be owned by
 * {@link GameService} and read by the UI after each guess.
 */
public class GameStats {

    private int correct;
    private int incorrect;
    private int currentStreak;
    private int bestStreak;

    // ── Mutators ────────────────────────────────────────────────────────────────

    /** Record a correct answer: bumps correct count and streak. */
    void recordCorrect() {
        correct++;
        currentStreak++;
        if (currentStreak > bestStreak) bestStreak = currentStreak;
    }

    /** Record a wrong answer: bumps incorrect count and resets the streak. */
    void recordIncorrect() {
        incorrect++;
        currentStreak = 0;
    }

    /** Reset all counters to zero. */
    void reset() {
        correct = incorrect = currentStreak = bestStreak = 0;
    }

    // ── Accessors ────────────────────────────────────────────────────────────────

    public int getCorrect()       { return correct; }
    public int getIncorrect()     { return incorrect; }
    public int getCurrentStreak() { return currentStreak; }
    public int getBestStreak()    { return bestStreak; }

    /** Total number of answered rounds. */
    public int getTotal() { return correct + incorrect; }

    /** Accuracy as a percentage 0–100 (0 when no rounds answered yet). */
    public double getAccuracy() {
        int total = getTotal();
        return total == 0 ? 0.0 : (correct * 100.0) / total;
    }

    @Override
    public String toString() {
        return String.format(
            "GameStats{correct=%d, incorrect=%d, streak=%d, best=%d, accuracy=%.1f%%}",
            correct, incorrect, currentStreak, bestStreak, getAccuracy());
    }
}
