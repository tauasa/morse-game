package org.tauasa.apps.morsegame.audio;

import javax.sound.sampled.*;
import java.io.Closeable;
import java.util.function.IntConsumer;

/**
 * Plays Morse code as audio tones using javax.sound.sampled (no external deps).
 *
 * <p>All audio parameters are read from a shared {@link AudioSettings} instance,
 * so changes made in the Settings dialog take effect on the next playback call
 * without needing to reconstruct the player.
 *
 * <p>If the sample rate changes, call {@link #reopen()} to re-open the audio
 * line with the new rate. Other parameters (frequency, amplitude, dotMs) are
 * read fresh on every call to {@link #play} and need no re-open.
 *
 * <p>Highlighting support:
 * {@link #playAsync(String, IntConsumer, Runnable)} fires a callback with the
 * index of each '.' or '-' character in the Morse string immediately before it
 * is sounded, then -1 when playback ends (clear-highlight signal).
 */
public class MorsePlayer implements Closeable {

    private final AudioSettings settings;
    private SourceDataLine line;

    // ── Constructor ───────────────────────────────────────────────────────────

    /**
     * Creates a player that reads all parameters from {@code settings}.
     * Opens the audio output line immediately. Safe even if audio is unavailable.
     */
    public MorsePlayer(AudioSettings settings) {
        this.settings = settings;
        this.line     = openLine(settings.getSampleRate());
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Returns true if audio output is available on this system. */
    public boolean isAvailable() { return line != null; }

    /**
     * Re-opens the audio line with the current sample rate from {@link AudioSettings}.
     * Call this after the sample rate setting changes.
     */
    public void reopen() {
        if (line != null) {
            line.drain();
            line.stop();
            line.close();
        }
        this.line = openLine(settings.getSampleRate());
    }

    /**
     * Plays the given Morse string on a background thread.
     *
     * @param morse       Morse string (dots, dashes, spaces, "/" word separators)
     * @param onCharIndex Called on the audio thread immediately before each '.' or '-'
     *                    symbol is sounded, with that character's index into {@code morse}.
     *                    Called with {@code -1} after the last symbol finishes
     *                    (clear-highlight signal). May be {@code null}.
     * @param onDone      Runnable called on the audio thread when playback fully ends.
     * @return            The daemon thread running playback.
     */
    public Thread playAsync(String morse, IntConsumer onCharIndex, Runnable onDone) {
        Thread t = new Thread(() -> {
            play(morse, onCharIndex);
            if (onCharIndex != null) onCharIndex.accept(-1); // clear highlight
            if (onDone != null) onDone.run();
        }, "morse-audio");
        t.setDaemon(true);
        t.start();
        return t;
    }

    /** Backwards-compatible overload: plays without a character-index callback. */
    public Thread playAsync(String morse, Runnable onDone) {
        return playAsync(morse, null, onDone);
    }

    /**
     * Plays synchronously, walking {@code morse} character by character.
     * All timing and waveform parameters are read fresh from {@link AudioSettings}.
     */
    public void play(String morse, IntConsumer onCharIndex) {
        if (line == null) return;

        // Snapshot settings for this playback so they stay consistent
        // even if the user opens Settings mid-playback.
        int   sampleRate   = settings.getSampleRate();
        float frequency    = settings.getFrequency();
        float amplitude    = settings.getAmplitude();
        int   dotMs        = settings.getDotMs();
        int   dashMs       = settings.dashMs();
        int   letterGapMs  = settings.letterGapMs();
        int   wordGapMs    = settings.wordGapMs();

        int i = 0;
        while (i < morse.length()) {
            char ch = morse.charAt(i);

            if (ch == '.') {
                if (onCharIndex != null) onCharIndex.accept(i);
                tone(dotMs, frequency, amplitude, sampleRate);
            } else if (ch == '-') {
                if (onCharIndex != null) onCharIndex.accept(i);
                tone(dashMs, frequency, amplitude, sampleRate);
            } else if (ch == '/') {
                // Word separator " / " — the spaces around it add letter gaps,
                // so we only write the extra delta to reach the full word gap.
                silence(wordGapMs - letterGapMs, sampleRate);
            } else {
                // Space between letters (or between symbols within a word separator)
                silence(letterGapMs, sampleRate);
            }

            i++;
        }

        line.drain();
    }

    /** Plays synchronously with no highlighting. */
    public void play(String morse) {
        play(morse, null);
    }

    // ── Audio line management ─────────────────────────────────────────────────

    private static SourceDataLine openLine(int sampleRate) {
        try {
            AudioFormat fmt  = new AudioFormat(sampleRate, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, fmt);
            if (!AudioSystem.isLineSupported(info)) return null;

            SourceDataLine l = (SourceDataLine) AudioSystem.getLine(info);
            l.open(fmt, sampleRate / 10);   // small buffer for low latency
            l.start();
            return l;
        } catch (LineUnavailableException e) {
            return null; // headless / sandboxed — degrade gracefully
        }
    }

    // ── Waveform synthesis ────────────────────────────────────────────────────

    private void tone(int durationMs, float frequency, float amplitude, int sampleRate) {
        int n    = msToSamples(durationMs, sampleRate);
        int ramp = msToSamples(10, sampleRate);
        byte[] buf = new byte[n * 2];

        for (int i = 0; i < n; i++) {
            double env = (i < ramp)     ? (double) i / ramp
                       : (i > n - ramp) ? (double)(n - i) / ramp
                       : 1.0;
            double v = amplitude * env *
                       Math.sin(2 * Math.PI * frequency * i / sampleRate);
            short s = (short)(v * Short.MAX_VALUE);
            buf[2 * i]     = (byte)(s & 0xFF);
            buf[2 * i + 1] = (byte)((s >> 8) & 0xFF);
        }

        line.write(buf, 0, buf.length);
    }

    private void silence(int durationMs, int sampleRate) {
        int n = msToSamples(durationMs, sampleRate) * 2;
        line.write(new byte[n], 0, n);
    }

    private static int msToSamples(int ms, int sampleRate) {
        return (int)(sampleRate * ms / 1000.0);
    }

    // ── Closeable ─────────────────────────────────────────────────────────────

    @Override
    public void close() {
        if (line != null) {
            line.drain();
            line.stop();
            line.close();
        }
    }
}
