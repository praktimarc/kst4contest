package kst4contest.controller;

import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import kst4contest.ApplicationConstants;
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

	private ChatMessage messageToBeSend;

	public WriteThread(Socket socket, ChatController client) throws InterruptedException {
		this.socket = socket;
		this.client = client;

		try {
			output = socket.getOutputStream();

			writer = new PrintWriter(output, true, StandardCharsets.UTF_8);

		} catch (IOException ex) {
			System.out.println("Error getting output stream: " + ex.getMessage());
			ex.printStackTrace();
		}
	}

	/**
	 * This method is used to send a message to the server, raw formatted. E.g. for
	 * the keepalive message. This method sends only in the main message-Category. To send it in a category
	 * "defined by Chatmessage", use txByRxmsgCatOrigin(Chatmessage "toBeSend")
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
	 * This method is used to send a message directly to a receiver in a special chatcategory. The receivers category
	 * will be read out of the Chatmessage.getChatCategory method. <b> The message text will be modified to fit kst
	 * messageformat</b>
	 *
	 * @param messageToServer
	 * @throws InterruptedException
	 */
	public void txByRxmsgCatOrigin(ChatMessage messageToServer) throws InterruptedException {

//	   	writer.println(messageToServer.getMessage()); //kst4contest.test 4 23001
//	   	writer.flush(); //kst4contest.test 4 23001

		String originalMessageText = messageToServer.getMessageText() + "";

		String newMessageText = "";

		newMessageText = ("MSG|" + messageToServer.getChatCategory().getCategoryNumber()
				+ "|0|" + originalMessageText + "|0|"); //original before 1.26


		System.out.println(newMessageText + "< sended to the writer (DIRECTED REPLY)");
		writer.println(newMessageText);

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
		messageToBeSend = messageToServer;

		try {

			messageToBeSend = client.getMessageTXBus().take();
//			this.client.getmesetChatsetServerready(true);

		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		String messageLine = messageToBeSend.getMessageText();

		if (messageToBeSend.isMessageDirectedToServer()) {
			/**
			 * We have to check if we only commands the server (keepalive) or want do talk
			 * to the community
			 */

			try {
				tx(messageToBeSend);
				System.out.println("BUS: tx: " + messageToBeSend.getMessageText());

			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} else {

			ChatMessage ownMSG = new ChatMessage();

//		ownMSG.setMessageText(
//				"MSG|" + this.client.getCategory().getCategoryNumber() + "|0|" + messageLine + "|0|");

			ownMSG.setMessageText("MSG|" + this.client.getChatPreferences().getLoginChatCategoryMain().getCategoryNumber()
					+ "|0|" + messageLine + "|0|"); //original before 1.26

			try {
				tx(ownMSG);
				System.out.println("BUS: tx: " + ownMSG.getMessageText());

			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		if (messageToBeSend.equals("/QUIT")) {
			try {
				this.client.getReadThread().terminateConnection();
				this.client.getReadThread().interrupt();
				this.client.getWriteThread().terminateConnection();
				this.client.getWriteThread().interrupt();
				this.interrupt();

			} catch (IOException e) {
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
				messageToBeSend = client.getMessageTXBus().take();

				if (messageToBeSend.getMessageText().equals(ApplicationConstants.DISCONNECT_RDR_POISONPILL)
						&& messageToBeSend.getMessageSenderName().equals(ApplicationConstants.DISCONNECT_RDR_POISONPILL)) {
					client.getMessageRXBus().clear();
					this.interrupt();
					break;
				} else {
					String messageLine = messageToBeSend.getMessageText();

					if (messageToBeSend.isMessageDirectedToServer()) {
						/**
						 * We have to check if we only commands the server (keepalive) or want do talk
						 * to the community
						 */

						try {
							tx(messageToBeSend);
							System.out.println("BUS: tx: " + messageToBeSend.getMessageText());

						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					} else { //message is not directed to the server, it´s directed to all or to a station

						if (messageToBeSend.getChatCategory() == this.client.getChatCategoryMain() || messageToBeSend.getChatCategory() == this.client.getChatCategorySecondChat()) {

							txByRxmsgCatOrigin(messageToBeSend);

						} else { //default bhv if destination cat is not detectable


							ChatMessage ownMSG = new ChatMessage();

							ownMSG.setMessageText(
									"MSG|" + this.client.getChatPreferences().getLoginChatCategoryMain().getCategoryNumber() + "|0|"
											+ messageLine + "|0|");

							try {
								tx(ownMSG);
								System.out.println("WT: tx (raw): " + ownMSG.getMessageText());

							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					}
				}

				System.out.println("WritheTh: got message out of the queue: " + messageToBeSend.getMessageText());

//			this.client.getmesetChatsetServerready(true);

			} catch (InterruptedException e) {
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
