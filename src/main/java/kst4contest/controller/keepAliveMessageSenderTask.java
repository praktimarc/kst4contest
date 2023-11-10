package kst4contest.controller;

import java.util.TimerTask;

import kst4contest.model.ChatMessage;

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
		keepAliveMSG.setMessageText("\r");
		keepAliveMSG.setMessageDirectedToServer(true);

//		System.out.println(new Utils4KST().time_generateCurrentMMDDhhmmTimeString() + " [keepaliveTask]: Sending keepalive: "
//				+ keepAliveMSG.getMessageText());
		/**
		 * Sending keepalive
		 */
		this.client.getMessageTXBus().add(keepAliveMSG);
	}

}
