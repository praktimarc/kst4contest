package kst4contest.view;

import kst4contest.controller.ChatController;
import kst4contest.model.ChatMember;

import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GuiUtils {

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

    private static void triggerUpdate(ChatController chatController) {
        {
            //trick to trigger gui changes on property changes of obects

            Predicate<ChatMember> dummyPredicate = new Predicate<ChatMember>() {
                @Override
                public boolean test(ChatMember chatMember) {
                    return true;
                }
            };

            /**
             * //TODO: following 2 lines are a quick fix to making disappear worked chatmembers of the list
             * Thats uncomfortable due to this also causes selection changes,
             * Better way is to change all worked and qrv values to observables and then trigger the underlying
             * list to fire an invalidationevent. Really Todo!
             */
            chatController.getLst_chatMemberListFilterPredicates().add(dummyPredicate);
            chatController.getLst_chatMemberListFilterPredicates().remove(dummyPredicate);

        }
    }
}
