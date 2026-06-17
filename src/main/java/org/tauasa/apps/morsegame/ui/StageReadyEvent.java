package org.tauasa.apps.morsegame.ui;

import javafx.stage.Stage;
import org.springframework.context.ApplicationEvent;

/**
 * Spring {@link ApplicationEvent} published by the JavaFX launcher once the
 * primary {@link Stage} is available.
 *
 * <p>This is the bridge between JavaFX's lifecycle (which creates the Stage)
 * and Spring's world (where UI beans live). A Spring listener — {@link GameView}
 * — receives this event and builds the scene on the supplied stage.
 */
public class StageReadyEvent extends ApplicationEvent {

    public StageReadyEvent(Stage stage) {
        super(stage);
    }

    /** The primary JavaFX stage. */
    public Stage getStage() {
        return (Stage) getSource();
    }
}
