package org.tauasa.apps.morsegame.game;

import java.util.ArrayList;
import java.util.List;

/**
 * Selectable character pools for the learning game.
 *
 * <p>Each mode defines which characters can appear as round targets and which
 * buttons are shown in the grid.
 */
public enum GameMode {

    /** A–Z only (26 letters). */
    LETTERS("Letters", build('A', 'Z')),

    /** 0–9 only (10 digits). */
    DIGITS("Digits", build('0', '9')),

    /** A–Z and 0–9 (36 characters). */
    LETTERS_AND_DIGITS("Letters + Digits", combine(build('A', 'Z'), build('0', '9')));

    private final String       label;
    private final List<Character> characters;

    GameMode(String label, List<Character> characters) {
        this.label      = label;
        this.characters = characters;
    }

    public String label() { return label; }

    /** Immutable list of characters in this mode. */
    public List<Character> characters() {
        return List.copyOf(characters);
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private static List<Character> build(char from, char to) {
        List<Character> out = new ArrayList<>();
        for (char c = from; c <= to; c++) out.add(c);
        return out;
    }

    private static List<Character> combine(List<Character> a, List<Character> b) {
        List<Character> out = new ArrayList<>(a);
        out.addAll(b);
        return out;
    }
}
