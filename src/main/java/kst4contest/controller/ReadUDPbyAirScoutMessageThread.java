package kst4contest.controller;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Comparator;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import kst4contest.ApplicationConstants;
import kst4contest.model.AirPlane;
import kst4contest.model.AirPlaneReflectionInfo;
import kst4contest.model.ChatMember;
import kst4contest.model.ThreadStateMessage;

/**
 * This thread is responsible for reading server's input and printing it to the
 * console. It runs in an infinite loop until the client disconnects from the
 * server.
 *
 * @author www.codejava.net
 */
public class ReadUDPbyAirScoutMessageThread extends Thread {

	private final ChatController client;
	private final int localPort;
	private final ThreadStatusCallback callBackToController;

	private final String threadNickName = "AirScout msg";

	private DatagramSocket socket;

	public ReadUDPbyAirScoutMessageThread(
			int localPort,
			ChatController client,
			ThreadStatusCallback callback
	) {
		this.localPort = localPort;
		this.client = client;
		this.callBackToController = callback;
	}

	@Override
	public void interrupt() {
		super.interrupt();

		if (socket != null && !socket.isClosed()) {
			socket.close();
		}
	}

	/**
	 * Checks whether an AirScout response is addressed to the currently
	 * configured server and client identifiers.
	 *
	 * Outgoing message:
	 * ASSETPATH: "client" "server" ...
	 *
	 * Corresponding response:
	 * ASNEAREST: "server" "client" ...
	 *
	 * The comparison is deliberately case-sensitive. In a setup with several
	 * clients, KST-A and kst-a must not silently become the same destination.
	 *
	 * @param message received UDP message
	 * @return true if the response belongs to this KST4Contest instance
	 */
	private boolean isMessageForConfiguredClient(String message) {
		if (message == null || !message.startsWith("ASNEAREST:")) {
			return false;
		}

		String[] quotedParts = message.split("\"");
		if (quotedParts.length < 4) {
			return false;
		}

		String receivedServerIdentifier = quotedParts[1].trim();
		String receivedClientIdentifier = quotedParts[3].trim();

		String configuredServerIdentifier =
				client.getChatPreferences()
						.getAirScout_asServerNameString();
		String configuredClientIdentifier =
				client.getChatPreferences()
						.getAirScout_asClientNameString();

		if (configuredServerIdentifier == null
				|| configuredClientIdentifier == null) {
			return false;
		}

		return configuredServerIdentifier.equals(
				receivedServerIdentifier
		) && configuredClientIdentifier.equals(
				receivedClientIdentifier
		);
	}

	@Override
	public void run() {
		Thread.currentThread().setName(
				"ReadUDPByAirScoutThread"
		);

		try {
			socket = new DatagramSocket(null);
			socket.setReuseAddress(true);
			socket.bind(new InetSocketAddress(localPort));
			socket.setSoTimeout(3000);

			while (!Thread.currentThread().isInterrupted()) {
				if (client.isDisconnectionPerformedByUser()) {
					break;
				}

				byte[] buffer = new byte[1777];
				DatagramPacket packet = new DatagramPacket(
						buffer,
						buffer.length
				);

				try {
					socket.receive(packet);
				} catch (SocketTimeoutException timeoutException) {
					/*
					 * AirScout may remain silent for some time. Continue with a
					 * new packet instead of processing the previous packet again.
					 */
					continue;
				}

				String received = new String(
						packet.getData(),
						packet.getOffset(),
						packet.getLength()
				).trim();

				if (received.contains(
						ApplicationConstants.DISCONNECT_RDR_POISONPILL
				)) {
					System.out.println(
							"[AirScout UDP, info]: Received shutdown packet."
					);
					break;
				}

				/*
				 * The socket remains bound so that AirScout can be enabled
				 * without reconnecting. Disabled means that received data is
				 * discarded and no station state is changed.
				 */
				if (!client.getChatPreferences()
						.isAirScout_asUDPListenerEnabled()) {
					continue;
				}

				if (!isMessageForConfiguredClient(received)) {
					continue;
				}

				processAirScoutResponse(received);
			}
		} catch (SocketException exception) {
			if (!Thread.currentThread().isInterrupted()) {
				System.out.println(
						"[AirScout UDP, error]: Could not use UDP port "
								+ localPort
								+ ": "
								+ exception.getMessage()
				);
			}
		} catch (IOException exception) {
			if (!Thread.currentThread().isInterrupted()) {
				System.out.println(
						"[AirScout UDP, error]: Communication failed: "
								+ exception.getMessage()
				);
			}
		} finally {
			if (socket != null && !socket.isClosed()) {
				socket.close();
			}

			socket = null;
		}
	}

	/**
	 * Parses an AirScout response and applies it to every active category
	 * instance of the reported station.
	 *
	 * @param received received ASNEAREST message
	 */
	private void processAirScoutResponse(String received) {
		try {
			AirPlaneReflectionInfo reflectionInfo =
					processASUDPMessage(received);

			if (reflectionInfo == null
					|| reflectionInfo.getReceiver() == null) {
				return;
			}

			String receiverCallSign =
					reflectionInfo.getReceiver().getCallSignRaw();

			if (receiverCallSign == null
					|| receiverCallSign.isBlank()) {
				receiverCallSign =
						reflectionInfo.getReceiver().getCallSign();
			}

			if (receiverCallSign == null
					|| receiverCallSign.isBlank()) {
				return;
			}

			List<ChatMember> matchingMembers =
					client.findActiveChatMembersByRawCall(
							receiverCallSign
					);

			for (ChatMember matchingMember : matchingMembers) {
				matchingMember.setAirPlaneReflectInfo(
						reflectionInfo
				);
			}

			if (!matchingMembers.isEmpty()) {
				client.getScoreService().requestRecompute(
						"airscout-update"
				);
			}

			if (callBackToController != null) {
				ThreadStateMessage threadStateMessage =
						new ThreadStateMessage(
								threadNickName,
								true,
								"Received AirScout response\n"
										+ reflectionInfo,
								false
						);

				callBackToController.onThreadStatus(
						threadNickName,
						threadStateMessage
				);
			}
		} catch (RuntimeException exception) {
			System.out.println(
					"[AirScout UDP, warning]: Could not process response: "
							+ exception.getMessage()
			);
		}
	}
	public AirPlaneReflectionInfo processASUDPMessage(String udpStringToProcess) {

//		System.out.println("RDUDPAS RECV: " + udpStringToProcess);



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
		if (socket != null && !socket.isClosed()) {
			socket.close();
		}

		return true;
	}

}