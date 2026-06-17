package org.tauasa.apps.morsegame.game;

import org.junit.jupiter.api.Test;
import org.tauasa.apps.morsegame.audio.MorseConverter;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link GameService}.
 *
 * <p>Uses a seeded {@link Random} for deterministic round selection, and a real
 * {@link MorseConverter} (it has no dependencies of its own).
 */
class GameServiceTest {

    private GameService newService(long seed) {
        return new GameService(new MorseConverter(), new Random(seed));
    }

    // ── Round lifecycle ──────────────────────────────────────────────────────────

    @Test
    void newRoundPicksATargetFromTheMode() {
        GameService game = newService(42);
        char target = game.newRound();
        assertTrue(game.getMode().characters().contains(target));
        assertTrue(game.isRoundActive());
        assertNotNull(game.currentMorse());
    }

    @Test
    void currentMorseMatchesTheTarget() {
        GameService game = newService(1);
        char target = game.newRound();
        String expected = new MorseConverter().encode(String.valueOf(target));
        assertEquals(expected, game.currentMorse());
    }

    @Test
    void guessingBeforeNewRoundThrows() {
        GameService game = newService(1);
        assertThrows(IllegalStateException.class, () -> game.guess('A'));
    }

    // ── Correct / incorrect guesses ────────────────────────────────────────────────

    @Test
    void correctGuessEndsRoundAndScores() {
        GameService game = newService(7);
        char target = game.newRound();

        GuessResult r = game.guess(target);

        assertTrue(r.correct());
        assertEquals(target, r.target());
        assertEquals(target, r.guessed());
        assertFalse(game.isRoundActive(), "round should end after a correct guess");
        assertEquals(1, game.stats().getCorrect());
        assertEquals(0, game.stats().getIncorrect());
        assertEquals(1, game.stats().getCurrentStreak());
    }

    @Test
    void incorrectGuessKeepsRoundActiveAndResetsStreak() {
        GameService game = newService(7);
        char target = game.newRound();
        char wrong = (target == 'A') ? 'B' : 'A';

        GuessResult r = game.guess(wrong);

        assertFalse(r.correct());
        assertTrue(game.isRoundActive(), "round stays active after a wrong guess");
        assertEquals(0, game.stats().getCorrect());
        assertEquals(1, game.stats().getIncorrect());
        assertEquals(0, game.stats().getCurrentStreak());
    }

    @Test
    void guessIsCaseInsensitive() {
        // Find a round whose target is a letter, then guess lowercase.
        GameService game = newService(3);
        char target = game.newRound();
        // target is uppercase A-Z in LETTERS mode
        GuessResult r = game.guess(Character.toLowerCase(target));
        assertTrue(r.correct(), "lowercase guess should match uppercase target");
    }

    // ── Streaks ────────────────────────────────────────────────────────────────────

    @Test
    void streakTracksConsecutiveCorrectAnswers() {
        GameService game = newService(99);

        for (int i = 0; i < 3; i++) {
            char target = game.newRound();
            game.guess(target);
        }
        assertEquals(3, game.stats().getCurrentStreak());
        assertEquals(3, game.stats().getBestStreak());

        // A miss resets the current streak but not the best.
        char target = game.newRound();
        char wrong = (target == 'Z') ? 'Y' : 'Z';
        game.guess(wrong);
        assertEquals(0, game.stats().getCurrentStreak());
        assertEquals(3, game.stats().getBestStreak());
    }

    // ── Accuracy ────────────────────────────────────────────────────────────────────

    @Test
    void accuracyIsComputedCorrectly() {
        GameService game = newService(5);

        // 2 correct
        char t1 = game.newRound(); game.guess(t1);
        char t2 = game.newRound(); game.guess(t2);
        // 1 incorrect
        char t3 = game.newRound();
        char wrong = (t3 == 'A') ? 'B' : 'A';
        game.guess(wrong);

        assertEquals(3, game.stats().getTotal());
        assertEquals(2, game.stats().getCorrect());
        assertEquals(1, game.stats().getIncorrect());
        assertEquals(66.67, game.stats().getAccuracy(), 0.1);
    }

    @Test
    void accuracyIsZeroBeforeAnyRounds() {
        GameService game = newService(1);
        assertEquals(0.0, game.stats().getAccuracy());
        assertEquals(0, game.stats().getTotal());
    }

    // ── Modes ────────────────────────────────────────────────────────────────────

    @Test
    void digitsModeOnlyPicksDigits() {
        GameService game = newService(11);
        game.setMode(GameMode.DIGITS);
        for (int i = 0; i < 30; i++) {
            char target = game.newRound();
            assertTrue(Character.isDigit(target),
                "DIGITS mode should only pick digits, got: " + target);
        }
    }

    @Test
    void lettersModeOnlyPicksLetters() {
        GameService game = newService(13);
        game.setMode(GameMode.LETTERS);
        for (int i = 0; i < 30; i++) {
            char target = game.newRound();
            assertTrue(Character.isLetter(target),
                "LETTERS mode should only pick letters, got: " + target);
        }
    }

    @Test
    void lettersAndDigitsModeHas36Characters() {
        GameService game = newService(1);
        game.setMode(GameMode.LETTERS_AND_DIGITS);
        assertEquals(36, game.characters().size());
    }

    // ── Reset ────────────────────────────────────────────────────────────────────

    @Test
    void resetStatsClearsEverything() {
        GameService game = newService(1);
        char target = game.newRound();
        game.guess(target);
        assertTrue(game.stats().getCorrect() > 0);

        game.resetStats();
        assertEquals(0, game.stats().getCorrect());
        assertEquals(0, game.stats().getIncorrect());
        assertEquals(0, game.stats().getBestStreak());
        assertEquals(0, game.stats().getCurrentStreak());
    }

    // ── No immediate repeat ──────────────────────────────────────────────────────

    @Test
    void newRoundAvoidsImmediateRepeat() {
        GameService game = newService(1);
        char prev = game.newRound();
        for (int i = 0; i < 50; i++) {
            char next = game.newRound();
            assertNotEquals(prev, next, "should not repeat the same target back-to-back");
            prev = next;
        }
    }
}
