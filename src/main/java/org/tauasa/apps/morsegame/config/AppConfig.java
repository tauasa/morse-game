package org.tauasa.apps.morsegame.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.tauasa.apps.morsegame.audio.AudioSettings;
import org.tauasa.apps.morsegame.audio.MorsePlayer;

/**
 * Spring bean definitions.
 *
 * <p>{@link AudioSettings} and {@link MorsePlayer} are declared here rather than
 * component-scanned because they need explicit construction:
 * {@code AudioSettings} uses its no-arg defaults constructor, and
 * {@code MorsePlayer} depends on it and is {@link AutoCloseable} (Spring will
 * call {@code close()} on context shutdown automatically for beans implementing
 * {@link AutoCloseable}).
 */
@Configuration
public class AppConfig {

    /** Single shared audio configuration for the whole application. */
    @Bean
    public AudioSettings audioSettings() {
        // Game default: a slightly longer dot for clearer learning playback.
        return new AudioSettings(
            AudioSettings.DEFAULT_SAMPLE_RATE,
            AudioSettings.DEFAULT_FREQUENCY,
            AudioSettings.DEFAULT_AMPLITUDE,
            90 /* dotMs — gentler than the 60 ms default for beginners */);
    }

    /**
     * The audio player. Spring calls {@link MorsePlayer#close()} on shutdown
     * because the class implements {@link java.io.Closeable}.
     */
    @Bean
    public MorsePlayer morsePlayer(AudioSettings audioSettings) {
        return new MorsePlayer(audioSettings);
    }
}
