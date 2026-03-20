package kst4contest.controller;

import java.util.Arrays;
import java.util.TimerTask;

import kst4contest.model.ChatMessage;
import kst4contest.model.ThreadStateMessage;

/**
 * This class is for sending beacons intervalled to the public chat. Gets all
 * preferences and instances via the Chatpreferences-object of the
 * Chatcontroller.
 * <br/><br/>
 * The task will be runned out of the singleton ChatController instance in an
 * intervall as specified by the Chatpreferences-instance (typically as
 * configured in the xml file.
 * 
 * 
 * @author prakt
 *
 */
public class BeaconTask extends TimerTask {

	private ChatController chatController;
    private ThreadStatusCallback callBackToController;
    private String ThreadNickName = "MyBeacon";

	public BeaconTask(ChatController client, ThreadStatusCallback callback) {
        this.callBackToController = callback;
		this.chatController = client;

	}

	@Override
	public void run() {

        ThreadStateMessage threadStateMessage = new ThreadStateMessage(this.ThreadNickName, true, "initialized", false);
        callBackToController.onThreadStatus(ThreadNickName,threadStateMessage);

        Thread.currentThread().setName("BeaconTask");


		ChatMessage beaconMSG = new ChatMessage();
		
		String replaceVariables = this.chatController.getChatPreferences().getBcn_beaconTextMainCat();

		replaceVariables = replaceVariables.replaceAll("MYQRG", this.chatController.getChatPreferences().getMYQRGFirstCat().getValue());
		replaceVariables = replaceVariables.replaceAll("MYCALL", this.chatController.getChatPreferences().getStn_loginCallSign());
		replaceVariables = replaceVariables.replaceAll("MYLOCATOR", this.chatController.getChatPreferences().getStn_loginLocatorMainCat());
		replaceVariables = replaceVariables.replaceAll("MYQTF", this.chatController.getChatPreferences().getActualQTF().getValue() + "");
		replaceVariables = replaceVariables.replaceAll("SECONDQRG", this.chatController.getChatPreferences().getActualQTF().getValue() + "");

		
		beaconMSG.setMessageText(
				"MSG|" + this.chatController.getChatPreferences().getLoginChatCategoryMain().getCategoryNumber() + "|0|" + replaceVariables + "|0|");
		beaconMSG.setMessageDirectedToServer(true);




		ChatMessage beaconMSG2 = new ChatMessage();

		String replaceVariables2 = this.chatController.getChatPreferences().getBcn_beaconTextSecondCat();

		replaceVariables2 = replaceVariables2.replaceAll("MYQRG", this.chatController.getChatPreferences().getMYQRGFirstCat().getValue());
		replaceVariables2 = replaceVariables2.replaceAll("MYCALL", this.chatController.getChatPreferences().getStn_loginCallSign());
		replaceVariables2 = replaceVariables2.replaceAll("MYLOCATOR", this.chatController.getChatPreferences().getStn_loginLocatorMainCat());
		replaceVariables2 = replaceVariables2.replaceAll("MYQTF", this.chatController.getChatPreferences().getActualQTF().getValue() + "");
		replaceVariables2 = replaceVariables2.replaceAll("SECONDQRG", this.chatController.getChatPreferences().getMYQRGSecondCat().getValue() + "");


		beaconMSG2.setMessageText(
				"MSG|" + this.chatController.getChatPreferences().getLoginChatCategorySecond().getCategoryNumber() + "|0|" + replaceVariables + "|0|");
		beaconMSG2.setMessageDirectedToServer(true);



		/**
		 * beacon 1st Chatcategory
		 */
		if (this.chatController.getChatPreferences().isBcn_beaconsEnabledMainCat() ) {

			System.out.println(new Utils4KST().time_generateCurrentMMDDhhmmTimeString()
					+ " [BeaconTask, Info]: Sending CQ: " + beaconMSG.getMessageText());
			this.chatController.getMessageTXBus().add(beaconMSG);

            threadStateMessage = new ThreadStateMessage(this.ThreadNickName + " 1", true, "on", false);
            callBackToController.onThreadStatus(ThreadNickName,threadStateMessage);

		} else {
            threadStateMessage = new ThreadStateMessage(this.ThreadNickName + " 1", false, "off", false);
            callBackToController.onThreadStatus(ThreadNickName,threadStateMessage);
		}

		/**
		 * beacon 2nd Chatcategory
		 */
		if (this.chatController.getChatPreferences().isLoginToSecondChatEnabled()) { //only send if 2nd cat enabled

			if (this.chatController.getChatPreferences().isBcn_beaconsEnabledSecondCat()) {

				beaconMSG2.setMessageText(
						"MSG|" + this.chatController.getChatPreferences().getLoginChatCategorySecond().getCategoryNumber() + "|0|" + replaceVariables2 + "|0|");
				beaconMSG2.setMessageDirectedToServer(true);

				System.out.println(new Utils4KST().time_generateCurrentMMDDhhmmTimeString()
						+ " [BeaconTask, Info]: Sending CQ 2nd Cat: " + beaconMSG2.getMessageText());
				this.chatController.getMessageTXBus().add(beaconMSG2);

                threadStateMessage = new ThreadStateMessage(this.ThreadNickName + " 2", true, "on", false);
                callBackToController.onThreadStatus(ThreadNickName,threadStateMessage);

			} else {
                threadStateMessage = new ThreadStateMessage(this.ThreadNickName + " 2", false, "off", false);
                callBackToController.onThreadStatus(ThreadNickName,threadStateMessage);
			}
		}

	}

}
