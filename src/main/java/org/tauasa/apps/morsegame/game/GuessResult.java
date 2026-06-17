package org.tauasa.apps.morsegame.game;

/**
 * Outcome of a single guess in the game.
 *
 * @param correct       whether the guessed character matched the target
 * @param guessed       the character the player guessed
 * @param target        the round's actual target character
 * @param targetMorse   the Morse code for the target (for display after answering)
 */
public record GuessResult(boolean correct,
                          char guessed,
                          char target,
                          String targetMorse) {
}
