package kst4contest.view;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.util.Duration;
import kst4contest.model.ChatPreferences;

import java.util.Objects;

/**
 * Coalesces JavaFX layout changes into selective preferences writes.
 */
public final class LayoutAutosave {

    private static final Duration SAVE_DELAY = Duration.millis(750);

    private final ChatPreferences preferences;
    private final PauseTransition saveDelay = new PauseTransition(SAVE_DELAY);
    private boolean pending;

    public LayoutAutosave(ChatPreferences preferences) {
        this.preferences = Objects.requireNonNull(preferences, "preferences");
        saveDelay.setOnFinished(event -> flushPending());
    }

    public void requestSave() {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(this::requestSave);
            return;
        }

        pending = true;
        saveDelay.playFromStart();
    }

    public void flushPending() {
        if (!pending) {
            return;
        }

        saveDelay.stop();
        pending = false;
        preferences.writeLayoutPreferencesToXmlFile();
    }

    public void cancelPending() {
        saveDelay.stop();
        pending = false;
    }
}
