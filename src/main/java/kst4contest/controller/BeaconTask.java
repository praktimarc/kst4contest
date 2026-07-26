package kst4contest.controller;

import java.util.TimerTask;

import kst4contest.model.ChatMessage;
import kst4contest.model.ThreadStateMessage;

/**
 * Sends the configured public-chat beacons for both active chat categories.
 *
 * <p>Both categories deliberately share one timer and therefore the same
 * interval. Their enable flags and message templates remain independent. Every
 * run reads the current preferences, resolves global message variables and
 * sends only the categories which are currently enabled.</p>
 */
public class BeaconTask extends TimerTask {

	private static final String THREAD_NICKNAME = "MyBeacon";

	private final ChatController chatController;
	private final ThreadStatusCallback callbackToController;

	/**
	 * Creates one execution of the shared beacon timer.
	 *
	 * @param chatController controller providing preferences and the TX queue
	 * @param callbackToController callback used by the thread-status display
	 */
	public BeaconTask(
			ChatController chatController,
			ThreadStatusCallback callbackToController
	) {
		this.chatController = chatController;
		this.callbackToController = callbackToController;
	}

	@Override
	public void run() {
		Thread.currentThread().setName("BeaconTask");
		reportStatus(THREAD_NICKNAME, true, "initialized", false);

		MessageVariableResolver variableResolver =
				new MessageVariableResolver(chatController.getChatPreferences());

		sendMainCategoryBeacon(variableResolver);
		sendSecondCategoryBeacon(variableResolver);
	}

	/**
	 * Sends the main-category beacon if it is currently enabled.
	 */
	private void sendMainCategoryBeacon(MessageVariableResolver variableResolver) {
		if (!chatController.getChatPreferences().isBcn_beaconsEnabledMainCat()) {
			reportStatus(THREAD_NICKNAME + " 1", false, "off", false);
			return;
		}

		String resolvedText = variableResolver.resolveGlobalVariables(
				chatController.getChatPreferences().getBcn_beaconTextMainCat()
		);
		ChatMessage beaconMessage = buildBeaconMessage(
				chatController.getChatPreferences()
						.getLoginChatCategoryMain()
						.getCategoryNumber(),
				resolvedText,
				"main category"
		);

		if (beaconMessage == null) {
			reportStatus(THREAD_NICKNAME + " 1", false, "invalid text", true);
			return;
		}

		System.out.println(
				new Utils4KST().time_generateCurrentMMDDhhmmTimeString()
						+ " [BeaconTask, Info]: Sending main-category CQ: "
						+ beaconMessage.getMessageText()
		);
		chatController.getMessageTXBus().add(beaconMessage);
		reportStatus(THREAD_NICKNAME + " 1", true, "on", false);
	}

	/**
	 * Sends the second-category beacon if the second login and its beacon are
	 * currently enabled.
	 */
	private void sendSecondCategoryBeacon(
			MessageVariableResolver variableResolver
	) {
		if (!chatController.getChatPreferences().isLoginToSecondChatEnabled()
				|| !chatController.getChatPreferences()
				.isBcn_beaconsEnabledSecondCat()) {
			reportStatus(THREAD_NICKNAME + " 2", false, "off", false);
			return;
		}

		String resolvedText = variableResolver.resolveGlobalVariables(
				chatController.getChatPreferences().getBcn_beaconTextSecondCat()
		);
		ChatMessage beaconMessage = buildBeaconMessage(
				chatController.getChatPreferences()
						.getLoginChatCategorySecond()
						.getCategoryNumber(),
				resolvedText,
				"second category"
		);

		if (beaconMessage == null) {
			reportStatus(THREAD_NICKNAME + " 2", false, "invalid text", true);
			return;
		}

		System.out.println(
				new Utils4KST().time_generateCurrentMMDDhhmmTimeString()
						+ " [BeaconTask, Info]: Sending second-category CQ: "
						+ beaconMessage.getMessageText()
		);
		chatController.getMessageTXBus().add(beaconMessage);
		reportStatus(THREAD_NICKNAME + " 2", true, "on", false);
	}

	/**
	 * Builds the server-directed message after validating the resolved payload.
	 *
	 * <p>The resolved text is checked rather than only the configured template
	 * because inserted values can increase the final message length.</p>
	 *
	 * @param categoryNumber ON4KST category number
	 * @param resolvedText fully resolved beacon payload
	 * @param categoryDescription text used in diagnostic output
	 * @return prepared message, or {@code null} if the payload is invalid
	 */
	private ChatMessage buildBeaconMessage(
			int categoryNumber,
			String resolvedText,
			String categoryDescription
	) {
		if (resolvedText == null
				|| resolvedText.length() > ChatController.MAX_BEACON_TEXT_LENGTH) {
			int actualLength = resolvedText == null ? 0 : resolvedText.length();
			System.out.println(
					"[BeaconTask, Warning]: Beacon for "
							+ categoryDescription
							+ " was not sent because the resolved text contains "
							+ actualLength
							+ " characters; maximum is "
							+ ChatController.MAX_BEACON_TEXT_LENGTH
							+ "."
			);
			return null;
		}

		ChatMessage beaconMessage = new ChatMessage();
		beaconMessage.setMessageText(
				"MSG|" + categoryNumber + "|0|" + resolvedText + "|0|"
		);
		beaconMessage.setMessageDirectedToServer(true);
		return beaconMessage;
	}

	/**
	 * Forwards one state update to the existing thread-status display.
	 */
	private void reportStatus(
			String threadName,
			boolean running,
			String information,
			boolean criticalState
	) {
		ThreadStateMessage stateMessage = new ThreadStateMessage(
				threadName,
				running,
				information,
				criticalState
		);
		callbackToController.onThreadStatus(THREAD_NICKNAME, stateMessage);
	}
}