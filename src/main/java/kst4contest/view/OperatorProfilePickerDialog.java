package kst4contest.view;

import kst4contest.model.OperatorProfile;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;
import java.util.Optional;

/**
 * Asks the operator which profile to start with.
 *
 * <p>The dialog is shown only when more than one profile exists. It is intentionally
 * minimal, because it stands between the operator and a contest: the last used profile
 * is preselected, the list has the focus, and Enter or a double click start immediately.</p>
 */
public final class OperatorProfilePickerDialog {

    private OperatorProfilePickerDialog() {
        // Utility class.
    }

    /**
     * Shows the picker and waits for the operator's choice.
     *
     * @param selectableProfiles   profiles to choose from
     * @param preselectedProfileId identifier of the profile to preselect, may be null
     * @return the chosen profile, or empty when the operator wants to quit
     */
    public static Optional<OperatorProfile> showAndSelect(final List<OperatorProfile> selectableProfiles,
            final String preselectedProfileId) {

        Stage dialogStage = new Stage();
        GuiUtils.applyApplicationIcon(dialogStage);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("Select operator profile");

        ListView<OperatorProfile> profileListView = new ListView<>();
        profileListView.getItems().addAll(selectableProfiles);
        profileListView.setCellFactory(listView -> new OperatorProfileListCell());
        VBox.setVgrow(profileListView, Priority.ALWAYS);

        selectPreselectedProfile(profileListView, selectableProfiles, preselectedProfileId);

        OperatorProfile[] chosenProfile = new OperatorProfile[1];

        Button startButton = new Button("Start");
        startButton.setDefaultButton(true);
        startButton.setOnAction(event -> {
            chosenProfile[0] = profileListView.getSelectionModel().getSelectedItem();
            dialogStage.close();
        });

        Button quitButton = new Button("Quit");
        quitButton.setCancelButton(true);
        quitButton.setOnAction(event -> {
            chosenProfile[0] = null;
            dialogStage.close();
        });

        profileListView.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                startButton.fire();
            }
        });

        profileListView.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                startButton.fire();
            }
        });

        HBox buttonRow = new HBox(10, startButton, quitButton);
        buttonRow.setPadding(new Insets(10, 0, 0, 0));

        VBox dialogContent = new VBox(8,
                new Label("More than one operator profile is configured."),
                profileListView,
                buttonRow);
        dialogContent.setPadding(new Insets(15));

        dialogStage.setScene(new Scene(dialogContent, 380, 280));
        profileListView.requestFocus();
        dialogStage.showAndWait();

        return Optional.ofNullable(chosenProfile[0]);
    }

    private static void selectPreselectedProfile(final ListView<OperatorProfile> profileListView,
            final List<OperatorProfile> selectableProfiles,
            final String preselectedProfileId) {

        int profileIndexToSelect = 0;

        if (preselectedProfileId != null) {
            for (int profileIndex = 0; profileIndex < selectableProfiles.size(); profileIndex++) {
                if (preselectedProfileId.equalsIgnoreCase(
                        selectableProfiles.get(profileIndex).getProfileId())) {
                    profileIndexToSelect = profileIndex;
                    break;
                }
            }
        }

        profileListView.getSelectionModel().select(profileIndexToSelect);
        profileListView.scrollTo(profileIndexToSelect);
    }

    /**
     * Renders a profile with its name and the kind of worked data it uses.
     */
    private static final class OperatorProfileListCell extends ListCell<OperatorProfile> {

        @Override
        protected void updateItem(final OperatorProfile profile, final boolean empty) {

            super.updateItem(profile, empty);

            if (empty || profile == null) {
                setText(null);
                return;
            }

            String workedDataDescription = profile.isRootProfile() || profile.isSharedWorkedDatabase()
                    ? "shared station worked database"
                    : "own worked database";

            setText(profile.getDisplayName() + "\n" + workedDataDescription);
        }
    }
}
