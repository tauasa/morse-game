package org.tauasa.apps.morsegame;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.tauasa.apps.morsegame.ui.StageReadyEvent;

/**
 * The JavaFX {@link Application}. This is the class actually launched by
 * {@link MorseGameApplication#main(String[])}.
 *
 * <p>Lifecycle bridge between JavaFX and Spring Boot:
 * <ol>
 *   <li>{@link #init()} — runs on the JavaFX-Launcher thread before the UI
 *       starts; boots the Spring {@link ConfigurableApplicationContext}.</li>
 *   <li>{@link #start(Stage)} — runs on the JavaFX Application thread; publishes
 *       a {@link StageReadyEvent} so Spring beans can build the scene.</li>
 *   <li>{@link #stop()} — closes the Spring context and exits the platform.</li>
 * </ol>
 */
public class JavaFxApplication extends Application {

    private ConfigurableApplicationContext context;

    @Override
    public void init() {
        // Boot Spring. getParameters() forwards any CLI args supplied to JavaFX.
        String[] args = getParameters().getRaw().toArray(new String[0]);

        this.context = new SpringApplicationBuilder()
            .sources(MorseGameApplication.class)
            .run(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // Hand the primary stage and host services to Spring;
        // GameView (a bean) listens for this event.
        context.publishEvent(new StageReadyEvent(primaryStage, getHostServices()));
    }

    @Override
    public void stop() {
        context.close();
        Platform.exit();
    }
}
