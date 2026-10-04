package kst4contest.view;

import kst4contest.controller.ChatController;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.net.URL;

public class GuiUtils {

	private static final String APPLICATION_ICON_RESOURCE = "/icons/kst4contest.png";

	private static Image applicationIcon;

	/**
	 * Applies the common KST4Contest application icon to a JavaFX stage.
	 *
	 * <p>The icon is loaded only once and reused for all application windows.
	 * A missing icon resource must never prevent a window from opening.</p>
	 *
	 * @param stage stage that should receive the application icon
	 */
	public static void applyApplicationIcon(Stage stage) {

		if (stage == null) {
			return;
		}

		Image icon = getApplicationIcon();

		if (icon != null && !stage.getIcons().contains(icon)) {
			stage.getIcons().add(icon);
		}
	}

	/**
	 * Loads and caches the common KST4Contest application icon.
	 *
	 * @return application icon or null if the resource is unavailable
	 */
	private static Image getApplicationIcon() {

		if (applicationIcon != null) {
			return applicationIcon;
		}

		URL iconUrl = GuiUtils.class.getResource(APPLICATION_ICON_RESOURCE);

		if (iconUrl == null) {
			System.err.println(
					"Application icon resource not found: "
							+ APPLICATION_ICON_RESOURCE
			);
			return null;
		}

		applicationIcon = new Image(iconUrl.toExternalForm());
		return applicationIcon;
	}

	private static final String PTRN_CALLSIGNSYNTAX = "^(?:[A-Z]{1,2}[0-9]|[0-9][A-Z])[0-9A-Z]{1,3}$";
	/**
	 * Checks wheter the input value of the String is numeric or not, true if yes
	 * TODO: Move to a utils class for checking input values by user... 
	 * @param str
	 * @return
	 */
	static boolean isNumeric(String str){
        return str != null && str.matches("[0-9.]+");
    }

	/**
	 * Checks wheter the given String has a HAM radio callsign syntax or not
	 * @param maybeCallSignValue
	 * @return true if yes
	 */
	static boolean isCallSignSyntax(String maybeCallSignValue) {

		Pattern pattern = Pattern.compile(PTRN_CALLSIGNSYNTAX, Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(maybeCallSignValue);

		try {
			if (matcher.find()) {
				return true;
			}
				else return false;

		} catch (Exception exc) {
			return false;
		}
	}


	public static void triggerGUIFilteredChatMemberListChange(ChatController chatController) {

        if  (javafx.application.Platform.isFxApplicationThread()) {
            triggerUpdate(chatController);
        } else{
            javafx.application.Platform.runLater(() -> triggerUpdate(chatController));
        }
	}

	/**
	 * Requests a safe UI refresh of the filtered ChatMember list.
	 *
	 * <p>Older versions used the trick of adding/removing a dummy predicate. That can
	 * break JavaFX SortedList internals when the table is sorted and a FilteredList
	 * refilter happens at the same time. The controller-level refresh path is safer
	 * because Kst4ContestApplication now re-applies the existing predicates directly.</p>
	 *
	 * @param chatController central controller
	 */
	private static void triggerUpdate(ChatController chatController) {
		if (chatController == null) {
			return;
		}

		chatController.fireUserListUpdate("Forced filtered ChatMember refresh");
	}
}
