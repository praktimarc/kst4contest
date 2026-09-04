package kst4contest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.jupiter.api.Test;

class WinTestNetworkAddressResolverTest {

	private static InetAddress address(String hostAddress) throws UnknownHostException {
		return InetAddress.getByName(hostAddress);
	}

	@Test
	void stationNetworkWinsOverConfiguredAddress() throws UnknownHostException {
		WinTestNetworkAddressResolver resolver = new WinTestNetworkAddressResolver(
				remoteAddress -> {
					try {
						return address("192.168.122.255");
					} catch (UnknownHostException exception) {
						return null;
					}
				});

		resolver.rememberStationAddress(address("192.168.122.1"));

		assertEquals(address("192.168.122.255"),
				resolver.resolveBroadcastAddress("192.168.101.255"));
	}

	@Test
	void configuredAddressIsUsedWhenNoLocalInterfaceServesTheStation()
			throws UnknownHostException {
		WinTestNetworkAddressResolver resolver =
				new WinTestNetworkAddressResolver(remoteAddress -> null);

		resolver.rememberStationAddress(address("10.9.8.7"));

		assertEquals(address("192.168.101.255"),
				resolver.resolveBroadcastAddress("192.168.101.255"));
	}

	@Test
	void limitedBroadcastIsUsedWithoutStationAndWithoutSetting()
			throws UnknownHostException {
		WinTestNetworkAddressResolver resolver =
				new WinTestNetworkAddressResolver(remoteAddress -> null);

		assertEquals(address("255.255.255.255"), resolver.resolveBroadcastAddress("  "));
		assertEquals(address("255.255.255.255"), resolver.resolveBroadcastAddress(null));
	}

	@Test
	void loopbackAndWildcardSourcesAreIgnored() throws UnknownHostException {
		WinTestNetworkAddressResolver resolver = new WinTestNetworkAddressResolver(
				remoteAddress -> {
					throw new IllegalStateException("must not be asked for " + remoteAddress);
				});

		resolver.rememberStationAddress(address("127.0.0.1"));
		resolver.rememberStationAddress(address("0.0.0.0"));
		resolver.rememberStationAddress(null);

		assertEquals(address("192.168.101.255"),
				resolver.resolveBroadcastAddress("192.168.101.255"));
	}

	@Test
	void subnetComparisonHonoursThePrefixLength() throws UnknownHostException {
		assertTrue(WinTestNetworkAddressResolver.isInSameSubnet(
				address("192.168.122.1"), address("192.168.122.203"), 24));
		assertFalse(WinTestNetworkAddressResolver.isInSameSubnet(
				address("192.168.101.5"), address("192.168.122.1"), 24));
		assertTrue(WinTestNetworkAddressResolver.isInSameSubnet(
				address("172.19.0.1"), address("172.19.240.9"), 16));
		assertFalse(WinTestNetworkAddressResolver.isInSameSubnet(
				address("10.244.22.73"), address("10.244.23.1"), 24));
		assertTrue(WinTestNetworkAddressResolver.isInSameSubnet(
				address("10.244.22.73"), address("10.244.23.1"), 16));
	}
}
