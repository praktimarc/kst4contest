package kst4contest.controller;

import java.util.TimerTask;

import kst4contest.model.ChatMessage;

/**
 * Enqueues the empty application-level reply expected by ON4KST keepalive
 * handling.
 *
 * <p>The writer owns line termination and appends exactly one CR/LF sequence.
 * Supplying an empty payload here avoids the historic double-terminator frame.</p>
 */
public class keepAliveMessageSenderTask extends TimerTask {

	private ChatController client;

	public keepAliveMessageSenderTask(ChatController client) {

		this.client = client;

	}

	@Override
	public void run() {

		Thread.currentThread().setName("KeepAliveMessageSenderTask");

//		System.out.println("[keepalive: ] Thread runned now");

		ChatMessage keepAliveMSG = new ChatMessage();
		// WriteThread appends exactly one CRLF. An empty frame is the protocol reply.
		keepAliveMSG.setMessageText("");
		keepAliveMSG.setMessageDirectedToServer(true);

//		System.out.println(new Utils4KST().time_generateCurrentMMDDhhmmTimeString() + " [keepaliveTask]: Sending keepalive: "
//				+ keepAliveMSG.getMessageText());
		/**
		 * Sending keepalive
		 */
		this.client.getMessageTXBus().add(keepAliveMSG);
	}

}