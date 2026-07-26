package kst4contest.controller;

import kst4contest.model.ChatMessage;

import java.util.TimerTask;

/**
 * This class is updateing the scoreboard at https://slovhf.net/claimed/. Gets scores of all bands out of the
 * preferences which is updated via ReadUdpByUCXLog Thread.
 *
 * api description: https://slovhf.net/claimed-score-api/
 *
 * <br/><br/>
 * The task will be runned out of the singleton ChatController instance in an
 * intervall as specified by the Chatpreferences-instance (typically as
 * configured in the xml file.
 *
 *
 * @author prakt
 *
 */
public class ScoreboardUpdateTask extends TimerTask {

	private ChatController chatController;

	public ScoreboardUpdateTask(ChatController client) {

		this.chatController = client;

	}

	@Override
	public void run() {
		Thread.currentThread().setName("BeaconTask");

		ChatMessage beaconMSG = new ChatMessage();

		MessageVariableResolver variableResolver =
				new MessageVariableResolver(this.chatController.getChatPreferences());
		String replaceVariables = variableResolver.resolveGlobalVariables(
				this.chatController.getChatPreferences().getBcn_beaconTextMainCat()
		);


		beaconMSG.setMessageText(
				"MSG|" + this.chatController.getChatPreferences().getLoginChatCategoryMain().getCategoryNumber() + "|0|" + replaceVariables + "|0|");
		beaconMSG.setMessageDirectedToServer(true);

//		System.out.println("########### " + replaceVariables);

		if (this.chatController.getChatPreferences().isBcn_beaconsEnabledMainCat() ) {

			System.out.println(new Utils4KST().time_generateCurrentMMDDhhmmTimeString()
					+ " [BeaconTask, Info]: Sending CQ: " + beaconMSG.getMessageText());
			this.chatController.getMessageTXBus().add(beaconMSG);
		} else {
			//do nothing, CQ is disabled
		}


	}

}