package kst4contest.controller;

import java.io.*;
import java.net.*;
import java.util.Comparator;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import kst4contest.ApplicationConstants;
import kst4contest.model.AirPlane;
import kst4contest.model.AirPlaneReflectionInfo;
import kst4contest.model.ChatMember;

/**
 * This thread is responsible for reading server's input and printing it to the
 * console. It runs in an infinite loop until the client disconnects from the
 * server.
 *
 * @author www.codejava.net
 */
public class ReadUDPbyAirScoutMessageThread extends Thread {
	private BufferedReader reader;
	private Socket socket;
	private ChatController client;
	private int localPort;
	private String ASIdentificator, ChatClientIdentificator;

	public ReadUDPbyAirScoutMessageThread(int localPort) {
		this.localPort = localPort;
	}

	public ReadUDPbyAirScoutMessageThread(int localPort, ChatController client, String ASIdentificator,
			String ChatClientIdentificator) {


		this.localPort = localPort;
		this.client = client;
		this.ASIdentificator = ASIdentificator;
		this.ChatClientIdentificator = ChatClientIdentificator;
	}

	@Override
	public void interrupt() {
		System.out.println("ReadUDP");
		super.interrupt();
		try {
			if (this.socket != null) {
				
				this.socket.close();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	
	public void run() {
		Thread.currentThread().setName("ReadUDPByAirScoutThread");

		DatagramSocket socket = null;
		boolean running;
		byte[] buf = new byte[1777];
		DatagramPacket packet;
//		DatagramPacket packet = new DatagramPacket(buf, buf.length); //changed due to save memory
		packet = new DatagramPacket(buf, buf.length);

		try {

			socket = new DatagramSocket(null);
			socket.setReuseAddress(true);
			socket.bind(new InetSocketAddress(localPort));
			socket.receive(packet);
			socket.setSoTimeout(3000);

		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		} catch (IOException e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
		} 
		

		while (true) {
//			packet = new DatagramPacket(buf, buf.length);
//    		 DatagramPacket packet  = new DatagramPacket(SRPDefinitions.BYTE_BUFFER_MAX_LENGTH);
			try {
				if (this.client.isDisconnectionPerformedByUser()) {
					break;//TODO: what if it´s not the finally closage but a band channel change?
				}
				
				socket.receive(packet);






			} catch (SocketTimeoutException e2) {
				// this will catch the repeating Sockettimeoutexception...nothing to do
//				e2.printStackTrace();
			} 
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			InetAddress address = packet.getAddress();
			int port = packet.getPort();
//			packet = new DatagramPacket(buf, buf.length, address, port);
			String received = new String(packet.getData(), packet.getOffset(), packet.getLength());
			received = received.trim();

			if (received.contains(ApplicationConstants.DISCONNECT_RDR_POISONPILL)) {
				System.out.println("ReadUdpByASMsgTh, Info: got poison, now dieing....");
				try {
					terminateConnection();
				} catch (Exception e) {
					System.out.println("ASUDPRDR: catched error " + e.getMessage());
				}
				break;
			}


			if (received.contains("ASSETPATH") || received.contains("ASWATCHLIST")) {
				// do nothing, that is your own message
			} else if (received.contains("ASNEAREST:")) { //answer by airscout
				processASUDPMessage(received);

//				System.out.println("[ReadUSPASTh, info:] received AS String " + received);

				AirPlaneReflectionInfo apReflectInfoForChatMember;

				apReflectInfoForChatMember = processASUDPMessage(received);
				if (this.client.getLst_chatMemberList().size() != 0) {

					try {

//					if (this.client.checkListForChatMemberIndexByCallSign(apReflectInfoForChatMember.getReceiver()) != -1) {

						this.client.getLst_chatMemberList()
								.get(this.client.checkListForChatMemberIndexByCallSign(
										apReflectInfoForChatMember.getReceiver()))
								.setAirPlaneReflectInfo(apReflectInfoForChatMember); // TODO: here we set the ap info at
																						// the central instance of
																						// chatmember list .... -1 is a
																						// problem!
						/**
						 * CK| MSGBUS BGFX Listactualizer Exception in thread "Thread-10"
						 * java.util.ConcurrentModificationException at
						 * java.base/java.util.AbstractList$Itr.checkForComodification(AbstractList.java:399)
						 * at java.base/java.util.AbstractList$Itr.next(AbstractList.java:368) at
						 * kst4contest.controller.ChatController.checkListForChatMemberIndexByCallSign(ChatController.java:173)
						 * at
						 * kst4contest.controller.ReadUDPbyAirScoutMessageThread.run(ReadUDPbyAirScoutMessageThread.java:93)
						 * 
						 */
//					}
					} catch (Exception e) {

						System.out.println("ReadUdpByAsMsgTh, Warning:"
								+ apReflectInfoForChatMember.getReceiver().getCallSign()
								+ " is not in the Chatmemberlist or the Chatmemberlist is modified by another Thread");
						// TODO: handle exception
					}

				}

			}
//			packet = null; //reset packet
			buf = new byte[1777]; // reset buffer for future smaller packets

		}

	}

	public AirPlaneReflectionInfo processASUDPMessage(String udpStringToProcess) {

//		System.out.println("RDUDPAS RECV: " + udpStringToProcess);

		// TODO: filter messages which are directed to another client

		/*
		 * Example mesage: ASNEAREST: "AS" "KST"
		 * "2023-04-01 21:33:42Z,DO5AMF,JN49GL,G4CLA,IO92JL,9,VLG2PT,M,190,75,14,BAW809,M,250,50,18,BEL6CM,M,143,50,12,WZZ6719,M,148,50,11,KLM1678,M,313,75,22,TRA1B,M,271,75,20,SAS4728,M,125,75,9,RYR6TL,M,90,75,6,UAE10,S,96,50,6"
		 * Syntax: ASNEAREST: "AS" "KST" "2023-03-09
		 * 23:21:50Z,DO5AMF,JN49GL,DM5M,JO51IJ,3, SWT8TB,M,121,75,16, ^kleines Ding
		 * ^^^Distanz km ^^Potenzial 0-100% ^^Dauer bis ankunft minutes
		 */
		String[] fullStringSplitter;
		String[] apStringSplitter;
		AirPlaneReflectionInfo apInfo = new AirPlaneReflectionInfo();
		ObservableList<AirPlane> airplaneList = FXCollections.observableArrayList();

		if (udpStringToProcess.contains("ASNEAREST: ")) {
			udpStringToProcess = udpStringToProcess.replace("ASNEAREST: ", "");
			udpStringToProcess = udpStringToProcess.replace(" ", "");
			fullStringSplitter = udpStringToProcess.split("\"");

//			for (int i = 0; i < fullStringSplitter.length; i++) {
//				 System.out.println(i + " " + fullStringSplitter[i]);
//			}

			String APInfoString = fullStringSplitter[5];
			apStringSplitter = APInfoString.split(",");
			String[] allAPInfos = new String[apStringSplitter.length - 6]; // new String shold only provide aps, nothing
																			// other

			for (int i = 0; i < apStringSplitter.length; i++) {

				if (i >= 6) {
					allAPInfos[i - 6] = apStringSplitter[i];
				}

//				System.out.println(i + ": " + apStringSplitter[i]);

//				if (i>=6) {
//					allAPInfos[i-6] = apStringSplitter[i];
//					System.out.println(i-5 + " > " + apStringSplitter[i]);
//					
//					
//					
//				}

			}
			AirPlane airPlane = new AirPlane();

			for (int i = 0; i < allAPInfos.length; i++) {
				if (((i) % 5) == 0) {
					airPlane = new AirPlane();
//					airPlane = new AirPlane();

					airPlane.setApCallSign(allAPInfos[i]);

//					System.out.println(i + " AP: " + allAPInfos[i]);
				} else if (((i) % 5) == 1) {

					airPlane.setApSizeCategory(allAPInfos[i]);
//					System.out.println(i + " cat: " + allAPInfos[i]);
				} else if (((i) % 5) == 2) {

					airPlane.setDistanceKm(Integer.parseInt(allAPInfos[i]));
//					System.out.println(i + " dist: " + allAPInfos[i]);
				} else if (((i) % 5) == 3) {

					airPlane.setPotential(Integer.parseInt(allAPInfos[i]));
//					System.out.println(i + " potential: " + allAPInfos[i]);

				}
				if (((i) % 5) == 4) {

//					System.out.println(i + " duration: " + allAPInfos[i]);
					airPlane.setArrivingDurationMinutes(Integer.parseInt(allAPInfos[i]));
					airplaneList.add(airPlane);
				}
			}

			apInfo.setDate(apStringSplitter[0]);
			ChatMember apStartCallSign = new ChatMember();
			apStartCallSign.setCallSign(apStringSplitter[1]);
			apStartCallSign.setQra(apStringSplitter[2]);
			apInfo.setSender(apStartCallSign); // usally its the callsign of own chatmember object, may check this

			ChatMember apDestCallSign = new ChatMember();
			apDestCallSign.setCallSign(apStringSplitter[3]);
			apDestCallSign.setQra(apStringSplitter[4]);
			apInfo.setReceiver(apDestCallSign);

			apInfo.setAirPlanesReachableCntr(Integer.parseInt(apStringSplitter[5]));
			apInfo.setRisingAirplanes(airplaneList);

//			System.out.println("total airplanes for rx stn "  + apInfo.getReceiver().getCallSign() + ": "  + airplaneList.size() + " " + apInfo.toString());

			airplaneList.sort(Comparator.comparing(AirPlane::getPotential).reversed()
					.thenComparing(AirPlane::getArrivingDurationMinutes));

		}

		return apInfo;
	}

	public boolean terminateConnection() {

		try {
			this.socket.close();
		} catch (Exception e) {
			System.out.println("udpbyas: catched " + e.getMessage());
		}

		return true;
	}

}