package kst4contest.view;

import kst4contest.controller.ChatController;
import kst4contest.model.ChatMember;

import java.util.function.Predicate;

public class GuiUtils {

	/**
	 * Checks wheter the input value of the String is numeric or not, true if yes
	 * TODO: Move to a utils class for checking input values by user... 
	 * @param str
	 * @return
	 */
	static boolean isNumeric(String str){
        return str != null && str.matches("[0-9.]+");
    }


	public static void triggerGUIFilteredChatMemberListChange(ChatController chatController) {

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
