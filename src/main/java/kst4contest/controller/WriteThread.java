package kst4contest.controller;

import java.io.*;
import java.net.*;

import kst4contest.model.ChatMessage;

/**
 * This thread is responsible for sending content to the chat. As we only use
 * the tx function, there is no content in run() method
 *
 *
 */
public class WriteThread extends Thread {
	private PrintWriter writer;
	private Socket socket;
	private ChatController client;
	private OutputStream output;

	private ChatMessage messageTextRaw;

	public WriteThread(Socket socket, ChatController client) throws InterruptedException {
		this.socket = socket;
		this.client = client;

		try {
			output = socket.getOutputStream();
			writer = new PrintWriter(output, true);
		} catch (IOException ex) {
			System.out.println("Error getting output stream: " + ex.getMessage());
			ex.printStackTrace();
		}
	}

	/**
	 * This method is used to send a message to the server, raw formatted. E.g. for
	 * the keepalive message.
	 * 
	 * @param messageToServer
	 * @throws InterruptedException
	 */
	public void tx(ChatMessage messageToServer) throws InterruptedException {

//	   	writer.println(messageToServer.getMessage()); //kst4contest.test 4 23001
//	   	writer.flush(); //kst4contest.test 4 23001
		System.out.println(messageToServer.getMessageText() + "< sended to the writer");
		writer.println(messageToServer.getMessageText());

	}

	/**
	 * This method gets a textmessage to the chat and adds some characters to hit
	 * the neccessarry format to send a message in the on4kst chat either to another
	 * station or to the public.
	 * 
	 * @param messageToServer
	 * @throws InterruptedException
	 */
	public void txKSTFormatted(ChatMessage messageToServer) throws InterruptedException {

//		writer.println(messageToServer.getMessageText());
		messageTextRaw = messageToServer;

		try {

			messageTextRaw = client.getMessageTXBus().take();
//			this.client.getmesetChatsetServerready(true);

		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		String messageLine = messageTextRaw.getMessageText();

		if (messageTextRaw.isMessageDirectedToServer()) {
			/**
			 * We have to check if we only commands the server (keepalive) or want do talk
			 * to the community
			 */

			try {
				tx(messageTextRaw);
				System.out.println("BUS: tx: " + messageTextRaw.getMessageText());

			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} else {

			ChatMessage ownMSG = new ChatMessage();

//		ownMSG.setMessageText(
//				"MSG|" + this.client.getCategory().getCategoryNumber() + "|0|" + messageLine + "|0|");

			ownMSG.setMessageText("MSG|" + this.client.getChatPreferences().getLoginChatCategory().getCategoryNumber()
					+ "|0|" + messageLine + "|0|");

			try {
				tx(ownMSG);
				System.out.println("BUS: tx: " + ownMSG.getMessageText());

			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		if (messageTextRaw.equals("/QUIT")) {
			try {
				this.client.getReadThread().terminateConnection();
				this.client.getReadThread().interrupt();
				this.client.getWriteThread().terminateConnection();
				this.client.getWriteThread().interrupt();
				this.interrupt();

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	public boolean terminateConnection() throws IOException {

		this.output.close();
		this.socket.close();

		return true;
	}

	public void run() {
		Thread.currentThread().setName("WriteToTelnetThread");

		while (true) {
			try {
				messageTextRaw = client.getMessageTXBus().take();

				if (messageTextRaw.getMessageText().equals("POISONPILL_KILLTHREAD")
						&& messageTextRaw.getMessageSenderName().equals("POISONPILL_KILLTHREAD")) {
					client.getMessageRXBus().clear();
					this.interrupt();
					break;
				} else {
					String messageLine = messageTextRaw.getMessageText();

					if (messageTextRaw.isMessageDirectedToServer()) {
						/**
						 * We have to check if we only commands the server (keepalive) or want do talk
						 * to the community
						 */

						try {
							tx(messageTextRaw);
							System.out.println("BUS: tx: " + messageTextRaw.getMessageText());

						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					} else {

						ChatMessage ownMSG = new ChatMessage();

//				ownMSG.setMessageText(
//						"MSG|" + this.client.getCategory().getCategoryNumber() + "|0|" + messageLine + "|0|");

						ownMSG.setMessageText(
								"MSG|" + this.client.getChatPreferences().getLoginChatCategory().getCategoryNumber() + "|0|"
										+ messageLine + "|0|");

						try {
							tx(ownMSG);
							System.out.println("BUS: tx: " + ownMSG.getMessageText());

						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}

				System.out.println("WritheTh: got message out of the queue: " + messageTextRaw.getMessageText());

//			this.client.getmesetChatsetServerready(true);

			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				client.getMessageTXBus().clear();
			} 

//			String messageLine = messageTextRaw.getMessageText();
//
//			if (messageTextRaw.isMessageDirectedToServer()) {
//				/**
//				 * We have to check if we only commands the server (keepalive) or want do talk
//				 * to the community
//				 */
//
//				try {
//					tx(messageTextRaw);
//					System.out.println("BUS: tx: " + messageTextRaw.getMessageText());
//
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//
//			} else {
//
//				ChatMessage ownMSG = new ChatMessage();
//
////		ownMSG.setMessageText(
////				"MSG|" + this.client.getCategory().getCategoryNumber() + "|0|" + messageLine + "|0|");
//
//				ownMSG.setMessageText(
//						"MSG|" + this.client.getChatPreferences().getLoginChatCategory().getCategoryNumber() + "|0|"
//								+ messageLine + "|0|");
//
//				try {
//					tx(ownMSG);
//					System.out.println("BUS: tx: " + ownMSG.getMessageText());
//
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}
			}
//		if (messageTextRaw.equals("/QUIT")) {
//			try {
//				this.client.getReadThread().terminateConnection();
//				this.client.getReadThread().interrupt();
//				this.client.getWriteThread().terminateConnection();
//				this.client.getWriteThread().interrupt();
//				this.interrupt();
//
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}

		
//		while (true) {
//
//		}

	}
}
