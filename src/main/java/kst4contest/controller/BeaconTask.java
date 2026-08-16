package kst4contest.controller;

import java.util.TimerTask;

import kst4contest.model.ChatCategory;
import kst4contest.model.ChatMessage;
import kst4contest.model.ThreadStateMessage;

/**
 * Sends the configured public-chat beacons for both active chat categories.
 *
 * <p>Both categories deliberately share one timer and therefore the same
 * interval. Their enable flags and message templates remain independent. Every
 * run reads the current preferences, resolves global message variables and
 * sends only the categories which are currently enabled.</p>
 *
 * <p>Beacon messages use the regular outbound chat-message pipeline. They are
 * not assembled as raw ON4KST frames, because that would bypass the common
 * category, delimiter and message-text validation.</p>
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

		sendMainCategoryBeacon();
		sendSecondCategoryBeacon();
	}

	/**
	 * Sends the main-category beacon if it is currently enabled.
	 */
	private void sendMainCategoryBeacon() {
		if (!chatController.getChatPreferences()
				.isBcn_beaconsEnabledMainCat()) {
			reportStatus(
					THREAD_NICKNAME + " 1",
					false,
					"off",
					false
			);
			return;
		}

		ChatMessage beaconMessage = buildBeaconMessage(
				chatController.getChatPreferences()
						.getLoginChatCategoryMain(),
				chatController.getChatPreferences()
						.getBcn_beaconTextMainCat(),
				"main category"
		);

		if (beaconMessage == null) {
			reportStatus(
					THREAD_NICKNAME + " 1",
					false,
					"invalid text",
					true
			);
			return;
		}

		System.out.println(
				new Utils4KST().time_generateCurrentMMDDhhmmTimeString()
						+ " [BeaconTask, Info]: Sending main-category CQ: "
						+ beaconMessage.getMessageText()
		);

		chatController.getMessageTXBus().add(beaconMessage);

		reportStatus(
				THREAD_NICKNAME + " 1",
				true,
				"on",
				false
		);
	}

	/**
	 * Sends the second-category beacon if the second login and its beacon are
	 * currently enabled.
	 */
	private void sendSecondCategoryBeacon() {
		if (!chatController.getChatPreferences()
				.isLoginToSecondChatEnabled()
				|| !chatController.getChatPreferences()
				.isBcn_beaconsEnabledSecondCat()) {
			reportStatus(
					THREAD_NICKNAME + " 2",
					false,
					"off",
					false
			);
			return;
		}

		ChatMessage beaconMessage = buildBeaconMessage(
				chatController.getChatPreferences()
						.getLoginChatCategorySecond(),
				chatController.getChatPreferences()
						.getBcn_beaconTextSecondCat(),
				"second category"
		);

		if (beaconMessage == null) {
			reportStatus(
					THREAD_NICKNAME + " 2",
					false,
					"invalid text",
					true
			);
			return;
		}

		System.out.println(
				new Utils4KST().time_generateCurrentMMDDhhmmTimeString()
						+ " [BeaconTask, Info]: Sending second-category CQ: "
						+ beaconMessage.getMessageText()
		);

		chatController.getMessageTXBus().add(beaconMessage);

		reportStatus(
				THREAD_NICKNAME + " 2",
				true,
				"on",
				false
		);
	}

	/**
	 * Resolves and validates one beacon before placing it in the regular outbound
	 * message queue.
	 *
	 * <p>The returned message contains only the public-chat payload and its chat
	 * category. {@link WriteThread} creates the final ON4KST frame through
	 * {@link On4KstProtocol#chatMessage(int, String)}. This prevents a configurable
	 * beacon text from bypassing the common protocol validation.</p>
	 *
	 * @param category target ON4KST chat category
	 * @param configuredText configured beacon template
	 * @param categoryDescription text used in diagnostic output
	 * @return prepared message, or {@code null} if the category or text is invalid
	 */
	private ChatMessage buildBeaconMessage(
			ChatCategory category,
			String configuredText,
			String categoryDescription
	) {
		try {
			if (category == null) {
				throw new IllegalArgumentException(
						"No chat category is configured."
				);
			}

			On4KstProtocol.category(category.getCategoryNumber());

			String resolvedText =
					chatController.resolveAndValidateBeaconText(
							configuredText
					);

			ChatMessage beaconMessage = new ChatMessage();
			beaconMessage.setMessageText(resolvedText);
			beaconMessage.setChatCategory(category);
			beaconMessage.setMessageDirectedToServer(false);

			return beaconMessage;
		} catch (IllegalArgumentException exception) {
			System.out.println(
					"[BeaconTask, Warning]: Beacon for "
							+ categoryDescription
							+ " was not queued: "
							+ exception.getMessage()
			);
			return null;
		}
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
		callbackToController.onThreadStatus(
				THREAD_NICKNAME,
				stateMessage
		);
	}
}