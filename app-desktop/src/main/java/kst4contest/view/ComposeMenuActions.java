package kst4contest.view;

import javafx.scene.control.MenuItem;
import kst4contest.view.compose.MainMenuActions;

import java.util.function.Supplier;

/**
 * The Compose menu bar's actions, delegated to the JavaFX menu items they mirror.
 *
 * <p>Firing the very same {@link MenuItem} is deliberate. The two bars run side by side while
 * the operator compares the windows, and a second implementation of seventeen handlers would
 * be a second thing to compare — differences between the bars would be indistinguishable from
 * differences between the windows.</p>
 *
 * <p>When the JavaFX window goes, those handlers move into methods of their own and this class
 * calls those instead. Until then it is a bridge, not a design.</p>
 */
public final class ComposeMenuActions implements MainMenuActions {

    private final Kst4ContestApplication application;

    public ComposeMenuActions(final Kst4ContestApplication application) {
        this.application = application;
    }

    @Override
    public void connect() {
        fire(() -> application.menuItemFileConnect);
    }

    @Override
    public void disconnect() {
        fire(() -> application.menuItemFileDisconnect);
    }

    @Override
    public void switchOperatorProfile() {
        application.runOnUi(application::showOperatorProfileSwitchDialogFromCompose);
    }

    @Override
    public void exitApplication() {
        fire(() -> application.menuItemFileExit);
    }

    @Override
    public void setQrgAsNameInChat() {
        fire(() -> application.menuItemOptionsSetFrequencyAsName);
    }

    @Override
    public void toggleAwayState() {
        fire(() -> application.menuItemOptionsAwayBack);
    }

    @Override
    public void toggleSettingsWindow() {
        fire(() -> application.menuItemOptionsShow);
    }

    @Override
    public void toggleMonitorWindow() {
        fire(() -> application.menuItemWindowCluster);
    }

    @Override
    public void toggleStationMap() {
        fire(() -> application.menuItemWindowStationMap);
    }

    @Override
    public void useDarkDesign() {
        fire(() -> application.menuItemWindowDarkDesign);
    }

    @Override
    public void useDefaultDesign() {
        fire(() -> application.menuItemWindowDefaultDesign);
    }

    @Override
    public void openDonationPage() {
        fire(() -> application.menuItemInfoDonate);
    }

    @Override
    public void openHomepage() {
        fire(() -> application.menuItemInfoHomepage);
    }

    @Override
    public void openNewsgroup() {
        fire(() -> application.menuItemInfoNewsgroup);
    }

    @Override
    public void openChangelog() {
        /*
         * The JavaFX item and its handler are both commented out, so there is nothing to
         * fire. The Compose menu no longer offers this either; the method stays because the
         * interface is shared and an empty body is honest about there being no such feature.
         */
    }

    @Override
    public void openOv3tDonationPage() {
        fire(() -> application.menuItemInfoDonateOv3t);
    }

    @Override
    public void contactAuthor() {
        fire(() -> application.menuItemInfoContact);
    }

    @Override
    public void showAbout() {
        fire(() -> application.menuItemInfoAbout);
    }

    /**
     * Fires a menu item on the JavaFX thread.
     *
     * <p>The item is looked up when the action runs rather than when this class is built,
     * because initMenuBar creates them after the runtime exists. A missing item is ignored:
     * a menu entry that cannot act is better than a crash mid contest.</p>
     */
    private void fire(final Supplier<MenuItem> item) {
        application.runOnUi(() -> {
            MenuItem menuItem = item.get();
            if (menuItem != null) {
                menuItem.fire();
            }
        });
    }
}
