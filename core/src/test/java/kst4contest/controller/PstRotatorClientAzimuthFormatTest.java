package kst4contest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Locale;

import org.junit.jupiter.api.Test;

/** The azimuth sent to PSTRotator: one decimal, dot separator, within [0, 360). */
class PstRotatorClientAzimuthFormatTest {

	@Test
	void keepsOneDecimalInsteadOfTruncating() {
		assertEquals("145.7", PstRotatorClient.formatAzimuth(145.7));
		assertEquals("145.0", PstRotatorClient.formatAzimuth(145.0));
		assertEquals("0.0", PstRotatorClient.formatAzimuth(0.0));
	}

	@Test
	void roundsToTheNearestTenth() {
		assertEquals("145.7", PstRotatorClient.formatAzimuth(145.66));
		assertEquals("145.7", PstRotatorClient.formatAzimuth(145.70000000000002));
		assertEquals("12.3", PstRotatorClient.formatAzimuth(12.34));
	}

	@Test
	void aValueRoundingUpTo360IsSentAsZero() {
		assertEquals("0.0", PstRotatorClient.formatAzimuth(359.96));
		assertEquals("359.9", PstRotatorClient.formatAzimuth(359.94));
	}

	@Test
	void wrapsAnyFiniteValueIntoOneTurn() {
		assertEquals("0.0", PstRotatorClient.formatAzimuth(720.0));
		assertEquals("10.0", PstRotatorClient.formatAzimuth(370.0));
		assertEquals("350.0", PstRotatorClient.formatAzimuth(-10.0));
		assertEquals("0.0", PstRotatorClient.formatAzimuth(-0.0));
	}

	@Test
	void refusesAValueWithoutAHeading() {
		assertThrows(IllegalArgumentException.class, () -> PstRotatorClient.formatAzimuth(Double.NaN));
		assertThrows(IllegalArgumentException.class,
				() -> PstRotatorClient.formatAzimuth(Double.POSITIVE_INFINITY));
	}

	@Test
	void usesADotEvenUnderAGermanDefaultLocale() {
		Locale previous = Locale.getDefault();
		try {
			Locale.setDefault(Locale.GERMANY);
			assertEquals("145.7", PstRotatorClient.formatAzimuth(145.7));
		} finally {
			Locale.setDefault(previous);
		}
	}
}
