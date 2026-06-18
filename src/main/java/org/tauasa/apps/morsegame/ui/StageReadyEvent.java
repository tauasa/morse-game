package org.tauasa.apps.morsegame.ui;

import javafx.application.HostServices;
import javafx.stage.Stage;
import org.springframework.context.ApplicationEvent;

/**
 * Spring {@link ApplicationEvent} published by the JavaFX launcher once the
 * primary {@link Stage} is available.
 *
 * <p>This is the bridge between JavaFX's lifecycle (which creates the Stage)
 * and Spring's world (where UI beans live). A Spring listener — {@link GameView}
 * — receives this event and builds the scene on the supplied stage.
 *
 * <p>The event also carries {@link HostServices}, which JavaFX only exposes via
 * the {@code Application} instance. The UI needs it to open the GitHub link in
 * the system browser from the About dialog. Passing it through the event avoids
 * any bean-creation-order issues that constructor-injecting it would cause.
 */
public class StageReadyEvent extends ApplicationEvent {

    private final HostServices hostServices;

    public StageReadyEvent(Stage stage, HostServices hostServices) {
        super(stage);
        this.hostServices = hostServices;
    }

    /** The primary JavaFX stage. */
    public Stage getStage() {
        return (Stage) getSource();
    }

    /** JavaFX host services — used to open external links in the browser. */
    public HostServices getHostServices() {
        return hostServices;
    }
}
