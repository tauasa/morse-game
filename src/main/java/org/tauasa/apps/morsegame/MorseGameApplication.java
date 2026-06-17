package org.tauasa.apps.morsegame;

import org.springframework.boot.autoconfigure.SpringBootApplication;

import javafx.application.Application;

/**
 * Spring Boot application entry point for the Morse Code learning game.
 *
 * <p>This class carries the {@link SpringBootApplication} annotation (so
 * component scanning finds {@code @Service}/{@code @Component}/{@code @Configuration}
 * beans under this package), but {@code main()} does <em>not</em> call
 * {@code SpringApplication.run} directly. Instead it launches the JavaFX
 * runtime via {@link JavaFxApplication}, which in turn boots the Spring context
 * inside {@link JavaFxApplication#init()}.
 *
 * <p>This ordering is required because JavaFX must own the main thread for its
 * own launcher, while Spring is started as part of the JavaFX {@code init()}
 * phase.
 */
@SpringBootApplication
public class MorseGameApplication {

    public static void main(String[] args) {
        Application.launch(JavaFxApplication.class, args);
    }
}
