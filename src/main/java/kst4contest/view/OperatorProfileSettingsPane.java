package kst4contest.view;

import kst4contest.controller.ActiveOperatorProfile;
import kst4contest.controller.OperatorProfileManagementService;
import kst4contest.controller.OperatorProfilePaths;
import kst4contest.model.OperatorProfile;
import kst4contest.model.OperatorProfileSelection;

import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Pair;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Settings tab that manages the operator profiles.
 *
 * <p>Every profile keeps its own settings and window layout. Whether it also keeps its
 * own worked stations is chosen per profile, because a multi operator contest station
 * shares one log while two operators on a private computer usually do not.</p>
 */
public class OperatorProfileSettingsPane extends VBox {

    private static final DateTimeFormatter LAST_USED_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private final OperatorProfileManagementService managementService;
    private final Consumer<OperatorProfile> profileActivationRequest;

    private final TableView<OperatorProfile> profileTable = new TableView<>();
    private final Label activeProfileLabel = new Label();
    private final Label preferencesPathLabel = new Label();
    private final Label workedDatabasePathLabel = new Label();

    public OperatorProfileSettingsPane(final OperatorProfileManagementService managementService,
            final Consumer<OperatorProfile> profileActivationRequest) {

        this.managementService = managementService;
        this.profileActivationRequest = profileActivationRequest;

        setSpacing(10);
        setPadding(new Insets(15));

        getChildren().addAll(
                buildActiveProfileHeader(),
                buildProfileTable(),
                buildButtonRows(),
                buildExplanationLabel());

        refreshActiveProfileHeader();
        refreshProfileTable();
    }

    private GridPane buildActiveProfileHeader() {

        GridPane headerGrid = new GridPane();
        headerGrid.setHgap(10);
        headerGrid.setVgap(4);

        headerGrid.add(new Label("Active profile:"), 0, 0);
        headerGrid.add(activeProfileLabel, 1, 0);
        headerGrid.add(new Label("Settings file:"), 0, 1);
        headerGrid.add(preferencesPathLabel, 1, 1);
        headerGrid.add(new Label("Worked stations:"), 0, 2);
        headerGrid.add(workedDatabasePathLabel, 1, 2);

        return headerGrid;
    }

    private TableView<OperatorProfile> buildProfileTable() {

        TableColumn<OperatorProfile, String> nameColumn = new TableColumn<>("Profile");
        nameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDisplayName()));
        nameColumn.setPrefWidth(200);

        TableColumn<OperatorProfile, String> workedDataColumn = new TableColumn<>("Worked stations");
        workedDataColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(describeWorkedDataMode(cellData.getValue())));
        workedDataColumn.setPrefWidth(200);

        TableColumn<OperatorProfile, String> lastUsedColumn = new TableColumn<>("Last used");
        lastUsedColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(describeLastUsed(cellData.getValue())));
        lastUsedColumn.setPrefWidth(140);

        profileTable.getColumns().add(nameColumn);
        profileTable.getColumns().add(workedDataColumn);
        profileTable.getColumns().add(lastUsedColumn);
        profileTable.setPlaceholder(new Label("No operator profile configured."));

        VBox.setVgrow(profileTable, Priority.ALWAYS);

        return profileTable;
    }

    private VBox buildButtonRows() {

        Button newProfileButton = new Button("New profile...");
        newProfileButton.setOnAction(event -> createProfile());

        Button duplicateProfileButton = new Button("Duplicate...");
        duplicateProfileButton.setOnAction(event -> duplicateSelectedProfile());

        Button renameProfileButton = new Button("Rename...");
        renameProfileButton.setOnAction(event -> renameSelectedProfile());

        Button deleteProfileButton = new Button("Delete...");
        deleteProfileButton.setOnAction(event -> deleteSelectedProfile());

        Button changeWorkedDataButton = new Button("Change worked stations...");
        changeWorkedDataButton.setOnAction(event -> changeWorkedDataModeOfSelectedProfile());

        Button switchProfileButton = new Button("Switch to selected profile...");
        switchProfileButton.setOnAction(event -> activateSelectedProfile());

        HBox managementRow = new HBox(8,
                newProfileButton, duplicateProfileButton, renameProfileButton, deleteProfileButton);
        HBox activationRow = new HBox(8, changeWorkedDataButton, switchProfileButton);

        return new VBox(8, managementRow, activationRow);
    }

    private Label buildExplanationLabel() {

        Label explanation = new Label(
                "Each profile has its own settings and window layout. A profile can either share the "
                        + "common station worked stations, which is what a multi operator station wants, "
                        + "or keep its own. Duplicating a profile copies everything except callsign and "
                        + "password, and never copies worked stations.");
        explanation.setWrapText(true);

        return explanation;
    }

    private void refreshActiveProfileHeader() {

        OperatorProfileSelection activeProfile = ActiveOperatorProfile.get();

        if (activeProfile == null) {
            activeProfileLabel.setText("unknown");
            return;
        }

        activeProfileLabel.setText(activeProfile.getProfile().getDisplayName());
        preferencesPathLabel.setText(activeProfile.getPreferencesAbsolutePath());
        workedDatabasePathLabel.setText(activeProfile.getWorkedDatabaseAbsolutePath());
    }

    private void refreshProfileTable() {

        List<OperatorProfile> knownProfiles = managementService.listProfiles();
        OperatorProfile previouslySelected = profileTable.getSelectionModel().getSelectedItem();

        profileTable.getItems().setAll(knownProfiles);

        if (previouslySelected != null && knownProfiles.contains(previouslySelected)) {
            profileTable.getSelectionModel().select(previouslySelected);
        } else if (!knownProfiles.isEmpty()) {
            profileTable.getSelectionModel().select(0);
        }
    }

    private void createProfile() {

        Optional<Pair<String, Boolean>> enteredProfile =
                showProfileCreationDialog("New operator profile", "");

        if (enteredProfile.isEmpty()) {
            return;
        }

        OperatorProfile createdProfile = managementService.createProfile(
                enteredProfile.get().getKey(), enteredProfile.get().getValue());

        if (createdProfile == null) {
            showError("The profile could not be created. The profile registry could not be written.");
            return;
        }

        refreshProfileTable();
        profileTable.getSelectionModel().select(createdProfile);

        showInformation("The profile \"" + createdProfile.getDisplayName() + "\" was created without "
                + "callsign and password. Enter them on the Station tab after switching to it.");
    }

    private void duplicateSelectedProfile() {

        OperatorProfile selectedProfile = requireSelectedProfile();

        if (selectedProfile == null) {
            return;
        }

        TextInputDialog nameDialog =
                new TextInputDialog("Copy of " + selectedProfile.getDisplayName());
        nameDialog.setTitle("Duplicate operator profile");
        nameDialog.setHeaderText("Name of the new profile");
        nameDialog.setContentText(
                "Everything is copied except callsign and password. Worked stations are never copied.");

        Optional<String> enteredName = nameDialog.showAndWait();

        if (enteredName.isEmpty() || enteredName.get().isBlank()) {
            return;
        }

        OperatorProfile duplicatedProfile =
                managementService.duplicateProfile(selectedProfile, enteredName.get());

        if (duplicatedProfile == null) {
            showError("The profile could not be duplicated.");
            return;
        }

        refreshProfileTable();
        profileTable.getSelectionModel().select(duplicatedProfile);
    }

    private void renameSelectedProfile() {

        OperatorProfile selectedProfile = requireSelectedProfile();

        if (selectedProfile == null) {
            return;
        }

        TextInputDialog nameDialog = new TextInputDialog(selectedProfile.getDisplayName());
        nameDialog.setTitle("Rename operator profile");
        nameDialog.setHeaderText("New name of the profile");
        nameDialog.setContentText("Files and folders of the profile are not touched.");

        Optional<String> enteredName = nameDialog.showAndWait();

        if (enteredName.isEmpty() || enteredName.get().isBlank()) {
            return;
        }

        managementService.renameProfile(selectedProfile, enteredName.get());
        refreshProfileTable();
        refreshActiveProfileHeader();
    }

    private void deleteSelectedProfile() {

        OperatorProfile selectedProfile = requireSelectedProfile();

        if (selectedProfile == null) {
            return;
        }

        if (selectedProfile.isRootProfile()) {
            showError("The default profile uses the files of the installation itself "
                    + "and cannot be deleted.");
            return;
        }

        if (isActiveProfile(selectedProfile)) {
            showError("The profile currently in use cannot be deleted. Switch to another "
                    + "profile first.");
            return;
        }

        Alert confirmation = new Alert(AlertType.CONFIRMATION);
        confirmation.setTitle("Delete operator profile");
        confirmation.setHeaderText("Delete the profile \"" + selectedProfile.getDisplayName() + "\"?");
        confirmation.setContentText(
                "The following folder is removed permanently:\n"
                        + managementService.getProfileDirectory(selectedProfile)
                        + "\n\n"
                        + (selectedProfile.isSharedWorkedDatabase()
                                ? "The common station worked stations are not touched."
                                : "The worked stations of this profile are deleted as well."));

        ButtonType deleteButton = new ButtonType("Delete profile", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmation.getButtonTypes().setAll(deleteButton, cancelButton);

        if (confirmation.showAndWait().orElse(cancelButton) != deleteButton) {
            return;
        }

        if (!managementService.deleteProfile(selectedProfile)) {
            showError("The profile could not be deleted.");
        }

        refreshProfileTable();
    }

    private void changeWorkedDataModeOfSelectedProfile() {

        OperatorProfile selectedProfile = requireSelectedProfile();

        if (selectedProfile == null) {
            return;
        }

        if (selectedProfile.isRootProfile()) {
            showError("The default profile always uses the common station worked stations, "
                    + "because that database is the one of the installation itself.");
            return;
        }

        Optional<Boolean> chosenMode = showWorkedDataModeDialog(selectedProfile);

        if (chosenMode.isEmpty() || chosenMode.get() == selectedProfile.isSharedWorkedDatabase()) {
            return;
        }

        managementService.setSharedWorkedDatabase(selectedProfile, chosenMode.get());
        refreshProfileTable();

        if (isActiveProfile(selectedProfile)) {
            showInformation("The change takes effect after switching to this profile again.");
        }
    }

    private void activateSelectedProfile() {

        OperatorProfile selectedProfile = requireSelectedProfile();

        if (selectedProfile == null) {
            return;
        }

        if (isActiveProfile(selectedProfile)) {
            showInformation("This profile is already active.");
            return;
        }

        profileActivationRequest.accept(selectedProfile);
    }

    private Optional<Pair<String, Boolean>> showProfileCreationDialog(final String title,
            final String initialName) {

        Dialog<Pair<String, Boolean>> creationDialog = new Dialog<>();
        creationDialog.setTitle(title);
        creationDialog.setHeaderText("Name and worked stations of the new profile");

        ButtonType createButton = new ButtonType("Create profile", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        creationDialog.getDialogPane().getButtonTypes().setAll(createButton, cancelButton);

        TextField nameField = new TextField(initialName);
        nameField.setPromptText("for example DN9APW");

        ToggleGroup workedDataGroup = new ToggleGroup();

        RadioButton ownDatabaseOption = new RadioButton("Own worked stations for this profile");
        ownDatabaseOption.setToggleGroup(workedDataGroup);
        ownDatabaseOption.setSelected(true);

        RadioButton sharedDatabaseOption =
                new RadioButton("Share the common station worked stations (multi operator station)");
        sharedDatabaseOption.setToggleGroup(workedDataGroup);

        VBox dialogContent = new VBox(8,
                new Label("Profile name"),
                nameField,
                new Label("Worked stations"),
                ownDatabaseOption,
                sharedDatabaseOption);
        dialogContent.setPadding(new Insets(10));

        creationDialog.getDialogPane().setContent(dialogContent);

        creationDialog.setResultConverter(pressedButton -> {
            if (pressedButton != createButton || nameField.getText().isBlank()) {
                return null;
            }

            return new Pair<>(nameField.getText().trim(), sharedDatabaseOption.isSelected());
        });

        return creationDialog.showAndWait();
    }

    private Optional<Boolean> showWorkedDataModeDialog(final OperatorProfile profile) {

        Dialog<Boolean> modeDialog = new Dialog<>();
        modeDialog.setTitle("Worked stations");
        modeDialog.setHeaderText("Worked stations of \"" + profile.getDisplayName() + "\"");

        ButtonType applyButton = new ButtonType("Apply", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        modeDialog.getDialogPane().getButtonTypes().setAll(applyButton, cancelButton);

        ToggleGroup workedDataGroup = new ToggleGroup();

        RadioButton ownDatabaseOption = new RadioButton("Own worked stations for this profile");
        ownDatabaseOption.setToggleGroup(workedDataGroup);

        RadioButton sharedDatabaseOption =
                new RadioButton("Share the common station worked stations (multi operator station)");
        sharedDatabaseOption.setToggleGroup(workedDataGroup);

        sharedDatabaseOption.setSelected(profile.isSharedWorkedDatabase());
        ownDatabaseOption.setSelected(!profile.isSharedWorkedDatabase());

        Label pathHint = new Label("Switching does not move any data. Worked stations already "
                + "collected under the other setting stay where they are.");
        pathHint.setWrapText(true);

        VBox dialogContent = new VBox(8, ownDatabaseOption, sharedDatabaseOption, pathHint);
        dialogContent.setPadding(new Insets(10));

        modeDialog.getDialogPane().setContent(dialogContent);
        modeDialog.setResultConverter(pressedButton ->
                pressedButton == applyButton ? sharedDatabaseOption.isSelected() : null);

        return modeDialog.showAndWait();
    }

    private OperatorProfile requireSelectedProfile() {

        OperatorProfile selectedProfile = profileTable.getSelectionModel().getSelectedItem();

        if (selectedProfile == null) {
            showInformation("Select a profile in the table first.");
        }

        return selectedProfile;
    }

    private static boolean isActiveProfile(final OperatorProfile profile) {

        OperatorProfileSelection activeProfile = ActiveOperatorProfile.get();

        return activeProfile != null
                && activeProfile.getProfile().getProfileId().equals(profile.getProfileId());
    }

    private static String describeWorkedDataMode(final OperatorProfile profile) {

        if (profile.isRootProfile() || profile.isSharedWorkedDatabase()) {
            return "common station database";
        }

        return "own database";
    }

    private static String describeLastUsed(final OperatorProfile profile) {

        if (profile.getLastUsedEpochMs() <= 0L) {
            return "";
        }

        return LAST_USED_FORMATTER.format(Instant.ofEpochMilli(profile.getLastUsedEpochMs()));
    }

    private static void showInformation(final String message) {
        Alert information = new Alert(AlertType.INFORMATION);
        information.setTitle("Operator profiles");
        information.setContentText(message);
        information.showAndWait();
    }

    private static void showError(final String message) {
        Alert error = new Alert(AlertType.ERROR);
        error.setTitle("Operator profiles");
        error.setContentText(message);
        error.showAndWait();
    }
}
