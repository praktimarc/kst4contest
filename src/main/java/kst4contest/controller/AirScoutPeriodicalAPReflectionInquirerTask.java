package kst4contest.controller;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import kst4contest.model.ChatMember;

/**
 * Sends periodical path requests and an AirScout watchlist for the currently
 * active ON4KST stations.
 */
public class AirScoutPeriodicalAPReflectionInquirerTask extends TimerTask {

	private static final Logger LOGGER = Logger.getLogger(
			AirScoutPeriodicalAPReflectionInquirerTask.class.getName()
	);

	private static final String BROADCAST_ADDRESS = "255.255.255.255";

	private final ChatController client;

	public AirScoutPeriodicalAPReflectionInquirerTask(
			ChatController client
	) {
		this.client = client;
	}

	@Override
	public void run() {
		Thread.currentThread().setName(
				"AirscoutPeriodicalReflectionInquirerTask"
		);

		/*
		 * Keep the scheduled task installed so that AirScout can be enabled at
		 * runtime, but do not send anything while the integration is disabled.
		 */
		if (!client.getChatPreferences().isAirScout_asUDPListenerEnabled()) {
			return;
		}

		String clientIdentifier =
				client.getChatPreferences().getAirScout_asClientNameString();
		String serverIdentifier =
				client.getChatPreferences().getAirScout_asServerNameString();
		String bandValue =
				client.getChatPreferences().getAirScout_asBandString();

		String ownCallSign = normalizeOwnCallSign(
				client.getChatPreferences().getStn_loginCallSign()
		);
		String ownLocator =
				client.getChatPreferences().getStn_loginLocatorMainCat();

		if (ownCallSign == null
				|| ownCallSign.isBlank()
				|| ownLocator == null
				|| ownLocator.isBlank()) {
			LOGGER.warning(
					"AirScout queries were skipped because the own callsign "
							+ "or locator is missing."
			);
			return;
		}

		String setPathPrefix =
				"ASSETPATH: \"" + clientIdentifier
						+ "\" \"" + serverIdentifier + "\" ";

		String watchListPrefix =
				"ASWATCHLIST: \"" + clientIdentifier
						+ "\" \"" + serverIdentifier + "\" ";

		String ownStation = ownCallSign + "," + ownLocator;
		StringBuilder watchListMessage = new StringBuilder(
				watchListPrefix
						+ bandValue
						+ ","
						+ ownStation
		);

		List<ChatMember> activeMembers = client.snapshotChatMembers();
		int port = client.getChatPreferences()
				.getAirScout_asCommunicationPort();

		try (
				DatagramSocket socket = new DatagramSocket()
		) {
			socket.setBroadcast(true);
			InetAddress broadcastAddress =
					InetAddress.getByName(BROADCAST_ADDRESS);

			for (ChatMember member : activeMembers) {
				if (!isUsableAirScoutTarget(member)) {
					continue;
				}

				if (member.getQrb()
						>= client.getChatPreferences().getStn_maxQRBDefault()) {
					continue;
				}

				String targetStation =
						member.getCallSign() + "," + member.getQra();

				String pathQuery =
						setPathPrefix
								+ bandValue
								+ ","
								+ ownStation
								+ ","
								+ targetStation
								+ " ";

				sendPacket(
						socket,
						broadcastAddress,
						port,
						pathQuery
				);

				watchListMessage
						.append(",")
						.append(targetStation);
			}

			watchListMessage.append(" ");

			sendPacket(
					socket,
					broadcastAddress,
					port,
					watchListMessage.toString()
			);
		} catch (IOException exception) {
			LOGGER.log(
					Level.WARNING,
					"Could not send the periodical AirScout queries.",
					exception
			);
		}
	}

	/**
	 * Removes the ON4KST login suffix because AirScout expects the actual
	 * station callsign, for example 9A1W instead of 9A1W-2.
	 *
	 * @param callSign configured ON4KST login callsign
	 * @return callsign without an ON4KST login suffix
	 */
	private String normalizeOwnCallSign(String callSign) {
		if (callSign == null) {
			return null;
		}

		String normalizedCallSign = callSign.trim();
		int suffixSeparator = normalizedCallSign.indexOf("-");

		if (suffixSeparator > 0) {
			return normalizedCallSign.substring(0, suffixSeparator);
		}

		return normalizedCallSign;
	}

	private boolean isUsableAirScoutTarget(ChatMember member) {
		return member != null
				&& member.getCallSign() != null
				&& !member.getCallSign().isBlank()
				&& member.getQra() != null
				&& !member.getQra().isBlank();
	}

	private void sendPacket(
			DatagramSocket socket,
			InetAddress address,
			int port,
			String message
	) throws IOException {
		byte[] payload = message.getBytes(StandardCharsets.UTF_8);

		DatagramPacket packet = new DatagramPacket(
				payload,
				payload.length,
				address,
				port
		);

		socket.send(packet);
	}
}