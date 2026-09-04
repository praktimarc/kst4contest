package kst4contest.controller;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

/**
 * Determines the broadcast address used to talk to the Win-Test network.
 *
 * <p>Win-Test only reacts to broadcast packets; a unicast request to the same
 * station stays unanswered. The configured broadcast address is therefore the
 * one setting that silently disables every outgoing Win-Test feature when it is
 * wrong: sending to an address outside the local networks succeeds without an
 * error and the packet is routed away.</p>
 *
 * <p>Incoming Win-Test packets carry the information that is actually needed.
 * The source address of a received packet identifies the network the station
 * lives in, so the broadcast address of the matching local interface reaches it
 * reliably. The configured address remains the fallback and keeps working for a
 * station behind a router, where no local interface matches.</p>
 */
public class WinTestNetworkAddressResolver {

	/** Last resort when neither a station nor a usable setting is available. */
	private static final String LIMITED_BROADCAST_ADDRESS = "255.255.255.255";

	/**
	 * Resolves the local broadcast address for a remote address.
	 */
	@FunctionalInterface
	public interface LocalBroadcastLookup {

		/**
		 * @param remoteAddress address a Win-Test packet was received from
		 * @return broadcast address of the matching local interface, or
		 *         {@code null} when no local interface serves that network
		 */
		InetAddress findBroadcastFor(InetAddress remoteAddress);
	}

	private final LocalBroadcastLookup localBroadcastLookup;

	private volatile InetAddress lastStationAddress;

	private volatile String lastReportedBroadcastAddress;

	public WinTestNetworkAddressResolver() {
		this(WinTestNetworkAddressResolver::findLocalBroadcastAddress);
	}

	/**
	 * @param localBroadcastLookup interface lookup, replaceable for tests
	 */
	WinTestNetworkAddressResolver(LocalBroadcastLookup localBroadcastLookup) {
		this.localBroadcastLookup = localBroadcastLookup;
	}

	/**
	 * Remembers where Win-Test packets come from.
	 *
	 * <p>Only addresses of real Win-Test stations may be passed in. Loopback and
	 * wildcard addresses are ignored, so an internal control packet cannot
	 * redirect outgoing Win-Test traffic.</p>
	 *
	 * @param stationAddress source address of a received Win-Test packet
	 */
	public void rememberStationAddress(InetAddress stationAddress) {
		if (stationAddress == null
				|| stationAddress.isLoopbackAddress()
				|| stationAddress.isAnyLocalAddress()
				|| !(stationAddress instanceof Inet4Address)) {
			return;
		}

		this.lastStationAddress = stationAddress;
	}

	/**
	 * Determines the broadcast address for outgoing Win-Test packets.
	 *
	 * <p>Order of preference: the broadcast address of the local interface that
	 * serves the last seen Win-Test station, then the configured address, then
	 * the limited broadcast address.</p>
	 *
	 * @param configuredBroadcastAddress address from the settings, may be blank
	 * @return address to send Win-Test packets to
	 * @throws UnknownHostException if the configured address cannot be resolved
	 *                              and the limited broadcast address fails too
	 */
	public InetAddress resolveBroadcastAddress(String configuredBroadcastAddress)
			throws UnknownHostException {

		InetAddress stationAddress = this.lastStationAddress;

		if (stationAddress != null) {
			InetAddress derivedBroadcastAddress =
					localBroadcastLookup.findBroadcastFor(stationAddress);

			if (derivedBroadcastAddress != null) {
				reportDerivedAddress(derivedBroadcastAddress, configuredBroadcastAddress);
				return derivedBroadcastAddress;
			}
		}

		if (configuredBroadcastAddress != null && !configuredBroadcastAddress.isBlank()) {
			return InetAddress.getByName(configuredBroadcastAddress.trim());
		}

		return InetAddress.getByName(LIMITED_BROADCAST_ADDRESS);
	}

	/**
	 * Logs a derived address once as long as it stays the same, and points out
	 * a configured address that does not match the Win-Test network.
	 */
	private void reportDerivedAddress(
			InetAddress derivedBroadcastAddress,
			String configuredBroadcastAddress
	) {
		String derivedHostAddress = derivedBroadcastAddress.getHostAddress();

		if (derivedHostAddress.equals(lastReportedBroadcastAddress)) {
			return;
		}

		lastReportedBroadcastAddress = derivedHostAddress;

		String configuredHostAddress = configuredBroadcastAddress == null
				? "" : configuredBroadcastAddress.trim();

		if (derivedHostAddress.equals(configuredHostAddress)) {
			return;
		}

		System.out.println("[WinTest] using broadcast address " + derivedHostAddress
				+ " of the network Win-Test was heard on, configured is '"
				+ configuredHostAddress + "'");
	}

	/**
	 * Searches the local interfaces for the network a remote address belongs to.
	 *
	 * @param remoteAddress address of a Win-Test station
	 * @return broadcast address of the matching interface, or {@code null}
	 */
	static InetAddress findLocalBroadcastAddress(InetAddress remoteAddress) {
		if (!(remoteAddress instanceof Inet4Address)) {
			return null;
		}

		try {
			Enumeration<NetworkInterface> networkInterfaces =
					NetworkInterface.getNetworkInterfaces();

			while (networkInterfaces != null && networkInterfaces.hasMoreElements()) {
				NetworkInterface networkInterface = networkInterfaces.nextElement();

				for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
					InetAddress broadcastAddress = interfaceAddress.getBroadcast();

					if (broadcastAddress == null
							|| !(interfaceAddress.getAddress() instanceof Inet4Address)) {
						continue;
					}

					if (isInSameSubnet(
							interfaceAddress.getAddress(),
							remoteAddress,
							interfaceAddress.getNetworkPrefixLength())) {
						return broadcastAddress;
					}
				}
			}
		} catch (SocketException exception) {
			System.out.println("[WinTest] could not inspect local interfaces: "
					+ exception.getMessage());
		}

		return null;
	}

	/**
	 * Compares two IPv4 addresses up to the given network prefix length.
	 *
	 * @param localAddress address of a local interface
	 * @param remoteAddress address of the Win-Test station
	 * @param networkPrefixLength prefix length of the local interface
	 * @return {@code true} when both addresses share the same network
	 */
	static boolean isInSameSubnet(
			InetAddress localAddress,
			InetAddress remoteAddress,
			int networkPrefixLength
	) {
		if (localAddress == null || remoteAddress == null) {
			return false;
		}

		byte[] localBytes = localAddress.getAddress();
		byte[] remoteBytes = remoteAddress.getAddress();

		if (localBytes.length != remoteBytes.length
				|| networkPrefixLength < 0
				|| networkPrefixLength > localBytes.length * 8) {
			return false;
		}

		int remainingPrefixBits = networkPrefixLength;

		for (int byteIndex = 0; byteIndex < localBytes.length && remainingPrefixBits > 0; byteIndex++) {
			int comparedBits = Math.min(8, remainingPrefixBits);
			int mask = (0xFF << (8 - comparedBits)) & 0xFF;

			if ((localBytes[byteIndex] & mask) != (remoteBytes[byteIndex] & mask)) {
				return false;
			}

			remainingPrefixBits -= comparedBits;
		}

		return true;
	}
}
