package org.tauasa.apps.morsegame.audio;

/**
 * Holds all configurable audio parameters.
 *
 * <p>Dash, symbol-gap, letter-gap, and word-gap durations are derived
 * automatically from {@link #dotMs} using the standard Morse ratios:
 * <pre>
 *   dash        = 3 × dot
 *   symbol gap  = 1 × dot
 *   letter gap  = 3 × dot
 *   word gap    = 7 × dot
 * </pre>
 *
 * <p>In the game this is a Spring-managed singleton bean, injected into
 * {@link MorsePlayer}.
 */
public class AudioSettings {

    // ── Factory defaults ──────────────────────────────────────────────────────

    public static final int   DEFAULT_SAMPLE_RATE = 44_100;
    public static final float DEFAULT_FREQUENCY   = 700f;
    public static final float DEFAULT_AMPLITUDE   = 0.5f;
    public static final int   DEFAULT_DOT_MS      = 60;

    // ── Validation bounds ─────────────────────────────────────────────────────

    public static final int   MIN_SAMPLE_RATE = 8_000;
    public static final int   MAX_SAMPLE_RATE = 48_000;

    public static final float MIN_FREQUENCY   = 200f;
    public static final float MAX_FREQUENCY   = 2_000f;

    public static final float MIN_AMPLITUDE   = 0.05f;
    public static final float MAX_AMPLITUDE   = 1.0f;

    public static final int   MIN_DOT_MS      = 20;
    public static final int   MAX_DOT_MS      = 300;

    // ── Fields ────────────────────────────────────────────────────────────────

    private int   sampleRate;
    private float frequency;
    private float amplitude;
    private int   dotMs;

    // ── Constructors ──────────────────────────────────────────────────────────

    /** Creates a settings instance with factory defaults. */
    public AudioSettings() {
        this(DEFAULT_SAMPLE_RATE, DEFAULT_FREQUENCY, DEFAULT_AMPLITUDE, DEFAULT_DOT_MS);
    }

    /** Creates a settings instance with the given values (clamped to valid ranges). */
    public AudioSettings(int sampleRate, float frequency, float amplitude, int dotMs) {
        this.sampleRate = clamp(sampleRate, MIN_SAMPLE_RATE, MAX_SAMPLE_RATE);
        this.frequency  = clamp(frequency,  MIN_FREQUENCY,   MAX_FREQUENCY);
        this.amplitude  = clamp(amplitude,  MIN_AMPLITUDE,   MAX_AMPLITUDE);
        this.dotMs      = clamp(dotMs,      MIN_DOT_MS,      MAX_DOT_MS);
    }

    /** Copy constructor. */
    public AudioSettings(AudioSettings other) {
        this(other.sampleRate, other.frequency, other.amplitude, other.dotMs);
    }

    // ── Derived timing (standard Morse ratios) ────────────────────────────────

    public int dashMs()      { return dotMs * 3; }
    public int symbolGapMs() { return dotMs;      }
    public int letterGapMs() { return dotMs * 3;  }
    public int wordGapMs()   { return dotMs * 7;  }

    // ── Getters ───────────────────────────────────────────────────────────────

    public int   getSampleRate() { return sampleRate; }
    public float getFrequency()  { return frequency;  }
    public float getAmplitude()  { return amplitude;  }
    public int   getDotMs()      { return dotMs;      }

    // ── Setters (with clamping) ───────────────────────────────────────────────

    public void setSampleRate(int v)   { sampleRate = clamp(v, MIN_SAMPLE_RATE, MAX_SAMPLE_RATE); }
    public void setFrequency(float v)  { frequency  = clamp(v, MIN_FREQUENCY,   MAX_FREQUENCY);   }
    public void setAmplitude(float v)  { amplitude  = clamp(v, MIN_AMPLITUDE,   MAX_AMPLITUDE);   }
    public void setDotMs(int v)        { dotMs      = clamp(v, MIN_DOT_MS,      MAX_DOT_MS);      }

    /** Copy all fields from {@code other} into this instance. */
    public void copyFrom(AudioSettings other) {
        this.sampleRate = other.sampleRate;
        this.frequency  = other.frequency;
        this.amplitude  = other.amplitude;
        this.dotMs      = other.dotMs;
    }

    /** Returns true if {@code sampleRate} differs from {@code other}. */
    public boolean sampleRateChanged(AudioSettings other) {
        return this.sampleRate != other.sampleRate;
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private static int   clamp(int v,   int   lo, int   hi) { return Math.max(lo, Math.min(hi, v)); }
    private static float clamp(float v, float lo, float hi) { return Math.max(lo, Math.min(hi, v)); }

    @Override
    public String toString() {
        return String.format(
            "AudioSettings{sampleRate=%d, frequency=%.1f Hz, amplitude=%.2f, dotMs=%d ms}",
            sampleRate, frequency, amplitude, dotMs);
    }
}
