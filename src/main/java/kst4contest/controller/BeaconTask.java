package kst4contest.controller;

import java.util.TimerTask;

import kst4contest.model.ChatMessage;

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

	public BeaconTask(ChatController client) {

		this.chatController = client;

	}

	@Override
	public void run() {
		Thread.currentThread().setName("BeaconTask");

		ChatMessage beaconMSG = new ChatMessage();
		
		String replaceVariables = this.chatController.getChatPreferences().getBcn_beaconText();
//		replaceVariables = bcn_beaconText;
		
		replaceVariables = replaceVariables.replaceAll("MYQRG", this.chatController.getChatPreferences().getMYQRG().getValue());
		replaceVariables = replaceVariables.replaceAll("MYCALL", this.chatController.getChatPreferences().getLoginCallSign());
		replaceVariables = replaceVariables.replaceAll("MYLOCATOR", this.chatController.getChatPreferences().getLoginLocator());
		replaceVariables = replaceVariables.replaceAll("MYQTF", this.chatController.getChatPreferences().getActualQTF().getValue() + "");

		
		beaconMSG.setMessageText(
				"MSG|" + this.chatController.getChatPreferences().getLoginChatCategory().getCategoryNumber() + "|0|" + replaceVariables + "|0|");
		beaconMSG.setMessageDirectedToServer(true);
		
//		System.out.println("########### " + replaceVariables);
		
		if (this.chatController.getChatPreferences().isBcn_beaconsEnabled() ) {

			System.out.println(new Utils4KST().time_generateCurrentMMDDhhmmTimeString()
					+ " [BeaconTask, Info]: Sending CQ: " + beaconMSG.getMessageText());
			this.chatController.getMessageTXBus().add(beaconMSG);
		} else {
			//do nothing, CQ is disabled
		}
		

	}

}
