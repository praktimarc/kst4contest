package kst4contest.controller;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import kst4contest.model.Band;
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

	/*
	 * ASWATCHLIST is sent as one common list. Remember one syntactically valid
	 * AirScout band value so an empty list can still be sent on a later cycle
	 * to remove stations which are no longer active.
	 */
	private String lastWatchListBandValue;

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
		List<ChatMember> activeMembers = client.snapshotChatMembers();
		List<String> watchListTargets = new ArrayList<>();
		Set<String> processedCallsigns = new LinkedHashSet<>();
		String watchListBandValue = null;
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

				if (member.getQrb() == null
						|| member.getQrb()
						>= client.getChatPreferences().getStn_maxQRBDefault()) {
					continue;
				}

				String callsignKey = member.getCallSignRaw();
				if (callsignKey == null || callsignKey.isBlank()) {
					callsignKey = member.getCallSign();
				}
				if (callsignKey == null
						|| !processedCallsigns.add(
						callsignKey.trim().toUpperCase(Locale.ROOT)
				)) {
					continue;
				}

				/*
				 * The resolver may deliberately return an exact QRG. AirScout must
				 * only see a canonical protocol band value such as 4320000.
				 */
				String bandValue = canonicalizeAirScoutBandValue(
						client.resolveAirScoutBandValue(member)
				);
				if (bandValue == null) {
					continue;
				}

				if (watchListBandValue == null) {
					watchListBandValue = bandValue;
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

				watchListTargets.add(targetStation);
			}

			/*
			 * AirScout keeps one watchlist per client/server pair. Do not send
			 * separate lists for the individual station bands because a later
			 * list would replace stations from an earlier one.
			 *
			 * If there are no targets in this cycle, reuse the last valid band
			 * token and send an empty list so AirScout can clear stale entries.
			 */
			if (watchListBandValue == null) {
				watchListBandValue = lastWatchListBandValue;
			}

			if (watchListBandValue != null) {
				StringBuilder watchListMessage = new StringBuilder(
						watchListPrefix
								+ watchListBandValue
								+ ","
								+ ownStation
				);

				for (String targetStation : watchListTargets) {
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

				lastWatchListBandValue = watchListBandValue;
			}

		} catch (IOException exception) {
			LOGGER.log(
					Level.WARNING,
					"Could not send the periodical AirScout queries.",
					exception
			);
		}
	}



	/**
	 * Converts a frequency-like value returned by the station resolver into the
	 * canonical band token expected by the AirScout UDP protocol.
	 *
	 * <p>The internal resolver may keep an exact working frequency for path
	 * analysis. This method removes that precision only at the AirScout protocol
	 * boundary. For example, {@code 4321740} is sent to AirScout as
	 * {@code 4320000}.</p>
	 *
	 * @param resolvedValue frequency-like AirScout value produced by the resolver
	 * @return canonical AirScout band value, or {@code null} if unsupported
	 */
	private String canonicalizeAirScoutBandValue(String resolvedValue) {
		if (resolvedValue == null || resolvedValue.isBlank()) {
			return null;
		}

		String normalizedValue = resolvedValue.trim();

		if ("off".equalsIgnoreCase(normalizedValue)
				|| "auto".equalsIgnoreCase(normalizedValue)) {
			return null;
		}

		final long numericValue;
		try {
			numericValue = Long.parseLong(normalizedValue);
		} catch (NumberFormatException exception) {
			LOGGER.log(
					Level.WARNING,
					"Unsupported AirScout band value: " + resolvedValue,
					exception
			);
			return null;
		}

		double frequencyMHz = numericValue / 10_000.0;
		Band band = Band.fromFrequency(frequencyMHz);

		if (band == null) {
			LOGGER.warning(
					"AirScout query skipped because frequency "
							+ frequencyMHz
							+ " MHz does not belong to a supported band."
			);
			return null;
		}

		return band.getPrefix() + "0000";
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