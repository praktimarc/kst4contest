package kst4contest.controller;

import java.io.*;
import java.net.*;
import java.sql.SQLException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import kst4contest.ApplicationConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import kst4contest.model.ChatMember;

/**
 * This thread is responsible for reading server's input and printing it to the
 * console. It runs in an infinite loop until the client disconnects from the
 * server.
 *
 * @author www.codejava.net
 */
public class ReadUDPbyUCXMessageThread extends Thread {
	private BufferedReader reader;
	private Socket socket;
	private ChatController client;

	public ReadUDPbyUCXMessageThread(int localPort) {

	}

	public ReadUDPbyUCXMessageThread(int localPort, ChatController client) {
		this.client = client;
	}

	@Override
	public void interrupt() {
		super.interrupt();
		try {
			if (this.socket != null) {
				System.out.println(">>>>>>>>>>>>>>ReadUdpbyUCS: closing socket");
				terminateConnection();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println("UCXUDPRDR: catched error " + e.getMessage());
		}
	}
	
	public void run() {



		Thread.currentThread().setName("ReadUDPByUCXLogThread");
		
		DatagramSocket socket = null;
		boolean running;
		byte[] buf = new byte[1777];
		DatagramPacket packet = new DatagramPacket(buf, buf.length);

		try {
			socket = new DatagramSocket(12060);
			socket.setSoTimeout(2000); //TODO try for end properly
		}
		
		catch (SocketException e) {
			//this will catch the repeating Sockettimeoutexception...nothing to do
//			e.printStackTrace();
		}

		while (true) {
			
			boolean timeOutIndicator = false;
			
//			packet = new DatagramPacket(buf, buf.length); //TODO: Changed that due to memory leak, check if all works (seems like that)
//    		 DatagramPacket packet  = new DatagramPacket(SRPDefinitions.BYTE_BUFFER_MAX_LENGTH); //TODO: Changed that due to memory leak, check if all works (seems like that)
			try {
				socket.receive(packet);

			} catch (SocketTimeoutException e2) {

				timeOutIndicator = true;
				// this will catch the repeating Sockettimeoutexception...nothing to do
//				e2.printStackTrace();
			}
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NullPointerException nE) {
				// TODO Auto-generated catch block
				nE.printStackTrace();
				System.out.println("ReadUdpByUCXTH: Socket not ready");



				try {
					socket = new DatagramSocket(client.getChatPreferences().getLogsynch_ucxUDPWkdCallListenerPort());
					socket.setSoTimeout(2000);
				} catch (SocketException e) {
					System.out.println("[ReadUDPByUCSMsgTH, Error]: socket in use or something:");
					e.printStackTrace();

					try {
						socket = new DatagramSocket(null);
						socket.setReuseAddress(true);
						socket.bind(new InetSocketAddress(client.getChatPreferences().getLogsynch_ucxUDPWkdCallListenerPort()));
						socket.receive(packet);
						socket.setSoTimeout(3000);
					} catch (Exception ex) {
						System.out.println("ReadUDPByUCXMsgTh: Could not solve that. Program Restart needed.");
						throw new RuntimeException(ex);
					}

				}

			}

			InetAddress address = packet.getAddress();
			int port = packet.getPort();
//			packet = new DatagramPacket(buf, buf.length, address, port);
			String received = new String(packet.getData(), packet.getOffset(), packet.getLength());
			received = received.trim();


//			System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<recv " + received);

			if (received.contains(ApplicationConstants.DISCONNECT_RDR_POISONPILL)) {
				System.out.println("ReadUdpByUCX, Info: got poison, now dieing....");
				socket.close();
				timeOutIndicator = true;
				break;
			}

			if (this.client.isDisconnectionPerformedByUser()) {
				break;//TODO: what if it´s not the finally closage but a band channel change?
			}

			if (!timeOutIndicator) {
				processUCXUDPMessage(received);
			} else {
				//dont process the empty message
			}

			buf = new byte[1777]; // reset buffer for future smaller packets

		}

	}

	public String processUCXUDPMessage(String udpPacketToProcess) {

		File logUDPMessageToThisFile;

		String udpMsg = udpPacketToProcess;

		ChatMember modifyThat = null;

//		System.out.println(udpMsg);

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		} catch (ParserConfigurationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(new InputSource(new StringReader(udpMsg)));

			/**
			 * case Log-QSO-Packet in ucxlog
			 * 
			 */
			NodeList list = doc.getElementsByTagName("contactinfo");
			if (list.getLength() != 0) {

				for (int temp = 0; temp < list.getLength(); temp++) {

					Node node = list.item(temp);

					if (node.getNodeType() == Node.ELEMENT_NODE) {

						Element element = (Element) node;

						String call = element.getElementsByTagName("call").item(0).getTextContent();
//						call = call.toLowerCase();
						String band = element.getElementsByTagName("band").item(0).getTextContent();

						System.out.println("[Readudp, info ]: received Current Element :" + node.getNodeName()
								+ "call: " + call + " / " + band);

						ChatMember workedCall = new ChatMember();
						workedCall.setCallSign(call);
						workedCall.setWorked(true);

						switch (band) {
						case "144": {
							workedCall.setWorked144(true);
							break;
						}

						case "432": {
							workedCall.setWorked432(true);
							break;
						}

						case "1240": {
							workedCall.setWorked1240(true);
							break;
						}

						case "2300": {
							workedCall.setWorked2300(true);
							break;
						}

						case "3400": {
							workedCall.setWorked3400(true);
							break;
						}

						case "5600": {
							workedCall.setWorked5600(true);
							break;
						}

						case "10G": {
							workedCall.setWorked10G(true);

						}

						default:
							System.out.println("[ReadUDPFromUCX, Error:] unexpected band value: \"" + band + "\"");
							break;
						}

//						if (!client.getMap_ucxLogInfoWorkedCalls().containsKey("call")) {
//							client.getMap_ucxLogInfoWorkedCalls().put(call, workedCall);

//						} else 
						{
							/**
							 * That means, the station is worked already but maybe at another band. So we
							 * have to get the worked ChatMember out of the list and to modify the worked
							 * options.
							 */

//							modifyThat = (ChatMember) client.getMap_ucxLogInfoWorkedCalls().get(call);

							int indexOfChatMemberInTable = -1;
							indexOfChatMemberInTable = client.checkListForChatMemberIndexByCallSign(workedCall);

							if (indexOfChatMemberInTable == -1) {
								// do nothing
							} else {
								modifyThat = client.getLst_chatMemberList().get(indexOfChatMemberInTable);
//							modifyThat.setWorked(true);
								
								client.getLst_chatMemberList()
										.get(client.checkListForChatMemberIndexByCallSign(modifyThat)).setWorked(true);

								if (workedCall.isWorked144()) {
									modifyThat.setWorked144(true);
									client.getLst_chatMemberList()
											.get(client.checkListForChatMemberIndexByCallSign(modifyThat))
											.setWorked144(true);

								} else if (workedCall.isWorked432()) {
									modifyThat.setWorked432(true);
									client.getLst_chatMemberList()
											.get(client.checkListForChatMemberIndexByCallSign(modifyThat))
											.setWorked432(true);

								} else if (workedCall.isWorked1240()) {
									modifyThat.setWorked1240(true);
									client.getLst_chatMemberList()
											.get(client.checkListForChatMemberIndexByCallSign(modifyThat))
											.setWorked1240(true);

								} else if (workedCall.isWorked2300()) {
									modifyThat.setWorked2300(true);
									client.getLst_chatMemberList()
											.get(client.checkListForChatMemberIndexByCallSign(modifyThat))
											.setWorked2300(true);

								} else if (workedCall.isWorked3400()) {
									modifyThat.setWorked3400(true);
									client.getLst_chatMemberList()
											.get(client.checkListForChatMemberIndexByCallSign(modifyThat))
											.setWorked3400(true);

								} else if (workedCall.isWorked5600()) {
									modifyThat.setWorked5600(true);
									client.getLst_chatMemberList()
											.get(client.checkListForChatMemberIndexByCallSign(modifyThat))
											.setWorked5600(true);

								} else if (workedCall.isWorked10G()) {
									modifyThat.setWorked10G(true);
									client.getLst_chatMemberList()
											.get(client.checkListForChatMemberIndexByCallSign(modifyThat))
											.setWorked10G(true);
								}

							}
						}

						boolean isInChat = this.client.getDbHandler().updateWkdInfoOnChatMember(workedCall);
						// This will update the worked info on a worked chatmember. DBHandler will
						// check, if an entry at the db had been modified. If not, then the worked
						// station had not been stored. DBHandler will store the informations then.
						if (!isInChat) {
							
							workedCall.setName("unknown");
							workedCall.setQra("unknown");
							workedCall.setLastActivity(new Utils4KST().time_generateActualTimeInDateFormat());
							this.client.getDbHandler().storeChatMember(workedCall);
						}
						

						logUDPMessageToThisFile = new File(this.client.getChatPreferences()
								.getLogSynch_storeWorkedCallSignsFileNameUDPMessageBackup());

						FileWriter fileWriterPersistUDPToFile = null;
						BufferedWriter bufwrtrRawMSGOut;

						try {
							fileWriterPersistUDPToFile = new FileWriter(logUDPMessageToThisFile, true);

						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}

						bufwrtrRawMSGOut = new BufferedWriter(fileWriterPersistUDPToFile);

						if (modifyThat != null) {
							bufwrtrRawMSGOut.write("\n" + modifyThat.toString());
							bufwrtrRawMSGOut.flush();
							bufwrtrRawMSGOut.close();

						} else {
							bufwrtrRawMSGOut.write("\n" + workedCall.toString());
							bufwrtrRawMSGOut.flush();
							bufwrtrRawMSGOut.close();

						}

					}
				}
			} else {
				list = doc.getElementsByTagName("RadioInfo");

				for (int temp = 0; temp < list.getLength(); temp++) {

					Node node = list.item(temp);

					if (node.getNodeType() == Node.ELEMENT_NODE) {

						String formattedQRG;

						Element element = (Element) node;

						String qrg = element.getElementsByTagName("Freq").item(0).getTextContent();
						String mode = element.getElementsByTagName("Mode").item(0).getTextContent();

//						System.out.println("QRG Length: " + qrg.length() + " // " + qrg);

						/**
						 * The following if statement is only for formatting the frequency input for
						 * good readability to avoid values like 129601000 and set it to something
						 * readable like 1296.010.00
						 * 
						 */
						if (qrg.length() == 6) {
							// 701000 KHz
							formattedQRG = qrg.format("%s.%s.%s", qrg.substring(0, 1), qrg.substring(2, 5),
									qrg.substring(5, 6));

						} else if (qrg.length() == 7) {
							// 700000 KHz
							formattedQRG = qrg.format("%s.%s.%s", qrg.substring(0, 2), qrg.substring(2, 5),
									qrg.substring(5, 7));
						} else if (qrg.length() == 8) {
							// 144.123.22 KHz
							formattedQRG = qrg.format("%s.%s.%s", qrg.substring(0, 3), qrg.substring(3, 6),
									qrg.substring(6, 8));
						} else if (qrg.length() == 9) {
							// 1296.010.00
							formattedQRG = qrg.format("%s.%s.%s", qrg.substring(0, 4), qrg.substring(4, 7),
									qrg.substring(7, 9));
						} else if (qrg.length() == 10) {
							// 10000.010.00
							formattedQRG = qrg.format("%s.%s.%s", qrg.substring(0, 5), qrg.substring(5, 8),
									qrg.substring(8, 10));
						}

						else {
							formattedQRG = qrg;
						}

//						System.out.println("Current Element :" + node.getNodeName());
//						System.out.println("Radio QRG : " + qrg);
//						System.out.println("Radio Mode: " + mode);
//						System.out.println("[ReadUDPFromUCX, Info:] Setted QRG pref to: \"" + qrg + "\"" );

						this.client.getChatPreferences().getMYQRG().set(formattedQRG);
						
						System.out.println("[ReadUDPbyUCXTh: ] Radioinfo processed: " + formattedQRG);
					}
				}

			}

		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
			System.out.println(e.getCause());
			System.out.println(e.getMessage());
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

//		System.out.println("[ReadUDPbyUCXTh: ] worked size = " + this.client.getMap_ucxLogInfoWorkedCalls().size());
//		System.out.println("[ReadUDPbyUCXTh: ] worked size = removeThisActions" );

		return "";
	}

	public boolean terminateConnection() throws IOException {

		this.socket.close();

		return true;
	}

}