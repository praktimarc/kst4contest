package kst4contest.controller;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.function.LongPredicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import kst4contest.ApplicationConstants;
import kst4contest.model.ChatCategory;
import kst4contest.model.ChatMessage;

/**
 * Serializes and writes exactly one immutable ON4KST connection session.
 *
 * <p>The writer owns one private queue and appends exactly one CR/LF terminator
 * per frame. All category selection and delimiter validation happens before bytes
 * reach the socket.</p>
 */
public class WriteThread extends Thread {
	private static final Logger LOGGER =
			Logger.getLogger(WriteThread.class.getName());
	private final long sessionId;
	private final Socket socket;
	private final LinkedBlockingQueue<ChatMessage> transmitQueue;
	private final LongPredicate sessionIsActive;
	private final Consumer<Throwable> connectionFailure;
	private final Consumer<String> rejectedFrame;
	private final BufferedWriter writer;
	private final int defaultCategory;

	/**
	 * Compatibility constructor for the pre-session controller path.
	 *
	 * @deprecated new connections should be created by
	 *             {@link On4KstConnectionManager}
	 */
	@Deprecated
	public WriteThread(Socket socket, ChatController client) throws IOException {
		this(0L, socket, client.getMessageTXBus(),
				client.getChatPreferences().getLoginChatCategoryMain().getCategoryNumber(),
				ignored -> true,
				ignored -> { }, System.out::println);
	}

	/**
	 * Creates the writer for one connection generation.
	 *
	 * @param sessionId immutable id of the owning socket session
	 * @param socket connected ON4KST socket
	 * @param transmitQueue private transmit queue belonging to this session
	 * @param defaultCategory fallback category for unqualified chat messages
	 * @param sessionIsActive guard against writes from an obsolete session
	 * @param connectionFailure callback for socket and unexpected runtime errors
	 * @param rejectedFrame callback for locally rejected protocol content
	 * @throws IOException if the socket output stream cannot be opened
	 */
	public WriteThread(
			long sessionId,
			Socket socket,
			LinkedBlockingQueue<ChatMessage> transmitQueue,
			int defaultCategory,
			LongPredicate sessionIsActive,
			Consumer<Throwable> connectionFailure,
			Consumer<String> rejectedFrame
	) throws IOException {
		this.sessionId = sessionId;
		this.socket = socket;
		this.transmitQueue = transmitQueue;
		this.defaultCategory = defaultCategory;
		this.sessionIsActive = sessionIsActive;
		this.connectionFailure = connectionFailure;
		this.rejectedFrame = rejectedFrame;
		this.writer = new BufferedWriter(new OutputStreamWriter(
				socket.getOutputStream(), StandardCharsets.UTF_8));
	}

	@Override
	public void run() {
		Thread.currentThread().setName("WriteToOn4Kst-" + sessionId);
		LOGGER.log(Level.FINE,
				"ON4KST writer started for session {0}", sessionId);

		try {
			while (!isInterrupted() && sessionIsActive.test(sessionId)) {
				ChatMessage message = transmitQueue.take();
				if (isPoisonPill(message)) {
					break;
				}
				if (!sessionIsActive.test(sessionId)) {
					break;
				}

				try {
					writeFrame(formatFrame(message));
				} catch (IllegalArgumentException invalidFrame) {
					LOGGER.log(Level.FINE,
							"Rejected outbound ON4KST frame in session {0}: {1}",
							new Object[] {sessionId, invalidFrame.getMessage()});
					rejectedFrame.accept(invalidFrame.getMessage());
				}
			}
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
		} catch (IOException exception) {
			if (sessionIsActive.test(sessionId)) {
				LOGGER.log(Level.FINE,
						"ON4KST write failed for session " + sessionId,
						exception);
				connectionFailure.accept(exception);
			}
		} catch (RuntimeException exception) {
			if (sessionIsActive.test(sessionId)) {
				LOGGER.log(Level.SEVERE,
						"Unexpected ON4KST writer failure for session "
								+ sessionId,
						exception);
				connectionFailure.accept(exception);
			}
		} finally {
			LOGGER.log(Level.FINE,
					"ON4KST writer stopped for session {0}", sessionId);
		}
	}

	private String formatFrame(ChatMessage message) {
		if (message == null) {
			throw new IllegalArgumentException("Cannot send an empty ON4KST message");
		}

		if (message.isMessageDirectedToServer()) {
			return On4KstProtocol.normalizeRawFrame(message.getMessageText());
		}

		ChatCategory category = message.getChatCategory();
		return On4KstProtocol.chatMessage(
				category == null ? defaultCategory : category.getCategoryNumber(),
				message.getMessageText());
	}

	private void writeFrame(String frame) throws IOException {
		writer.write(frame);
		writer.write("\r\n");
		writer.flush();
	}

	private boolean isPoisonPill(ChatMessage message) {
		return message != null
				&& ApplicationConstants.DISCONNECT_RDR_POISONPILL.equals(
				message.getMessageText())
				&& ApplicationConstants.DISCONNECT_RDR_POISONPILL.equals(
				message.getMessageSenderName());
	}

	/**
	 * Interrupts the write loop and closes the session socket.
	 *
	 * @return always {@code true} after a successful close
	 * @throws IOException if closing the writer or socket fails
	 */
	public boolean terminateConnection() throws IOException {
		interrupt();
		writer.close();
		socket.close();
		return true;
	}
}

