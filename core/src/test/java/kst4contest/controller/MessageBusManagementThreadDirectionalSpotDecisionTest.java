package kst4contest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import kst4contest.controller.MessageBusManagementThread.DirectionalOpportunity;
import kst4contest.controller.MessageBusManagementThread.DxClusterSpotDecision;
import kst4contest.model.ChatMember;
import kst4contest.observe.SimpleValue;

/**
 * Unit tests for the directional-opportunity and DX Cluster spot decisions
 * extracted from the message branch for traffic between two other stations.
 * No mocks, no thread, no preferences.
 */
class MessageBusManagementThreadDirectionalSpotDecisionTest {

    // Local station between a western sender and an eastern receiver, all on one latitude.
    private static final String OWN = "JO50LL";
    private static final String WEST = "JO30LL";
    private static final String EAST = "JO70LL";
    private static final String FAR_WEST = "IO70LL";
    private static final double MAX_QRB_KM = 900.0;
    private static final double BEAM_WIDTH_DEG = 50.0;

    // --- classifyDirectionalOpportunity -------------------------------------

    @Test
    void senderBeamingAcrossTheLocalStationIsAnOpportunity() {
        assertEquals(DirectionalOpportunity.IN_ANGLE_AND_RANGE,
                MessageBusManagementThread.classifyDirectionalOpportunity(
                        OWN, WEST, EAST, MAX_QRB_KM, BEAM_WIDTH_DEG));
    }

    @Test
    void senderBeamingAwayFromTheLocalStationIsNoOpportunity() {
        assertEquals(DirectionalOpportunity.OUT_OF_ANGLE_OR_RANGE,
                MessageBusManagementThread.classifyDirectionalOpportunity(
                        OWN, WEST, FAR_WEST, MAX_QRB_KM, BEAM_WIDTH_DEG));
    }

    @Test
    void senderBeyondTheMaximumDistanceIsNoOpportunity() {
        assertEquals(DirectionalOpportunity.OUT_OF_ANGLE_OR_RANGE,
                MessageBusManagementThread.classifyDirectionalOpportunity(
                        OWN, WEST, EAST, 50.0, BEAM_WIDTH_DEG));
    }

    @ParameterizedTest
    @CsvSource(value = {
            "NULL,   JO70LL",
            "'',     JO70LL",
            "'   ',  JO70LL",
            "JO30LL, NULL",
            "JO30LL, ''",
            "NULL,   NULL"
    }, nullValues = "NULL")
    void missingOrBlankLocatorSkipsTheGeometry(String senderLocator, String receiverLocator) {
        // The own locator is invalid on purpose: it must not be evaluated at all.
        assertEquals(DirectionalOpportunity.MISSING_LOCATOR,
                MessageBusManagementThread.classifyDirectionalOpportunity(
                        null, senderLocator, receiverLocator, MAX_QRB_KM, BEAM_WIDTH_DEG));
    }

    // --- shouldPlayDirectionalTick ------------------------------------------

    @Test
    void tickOnlyOnTheTransitionIntoAnOpportunityAndOnlyWithSounds() {
        assertTrue(MessageBusManagementThread.shouldPlayDirectionalTick(true, false));
        assertFalse(MessageBusManagementThread.shouldPlayDirectionalTick(true, true));
        assertFalse(MessageBusManagementThread.shouldPlayDirectionalTick(false, false));
        assertFalse(MessageBusManagementThread.shouldPlayDirectionalTick(false, true));
    }

    // --- decideDxClusterSpot ------------------------------------------------

    @Test
    void knownFrequencyAndNotWorkedIsSpotted() {
        assertEquals(DxClusterSpotDecision.SPOT,
                MessageBusManagementThread.decideDxClusterSpot(senderOn("144.300")));
    }

    @Test
    void workedOnTheBandOfTheFrequencyIsSuppressed() {
        ChatMember sender = senderOn("144.300");
        sender.setWorked144(true);

        assertEquals(DxClusterSpotDecision.ALREADY_WORKED,
                MessageBusManagementThread.decideDxClusterSpot(sender));
    }

    @Test
    void workedOnlyOnAnotherBandIsSpotted() {
        ChatMember sender = senderOn("432.200");
        sender.setWorked144(true);

        assertEquals(DxClusterSpotDecision.SPOT,
                MessageBusManagementThread.decideDxClusterSpot(sender));
    }

    @Test
    void missingFrequencyIsNeitherSpottedNorReportedAsWorked() {
        ChatMember noWrapper = new ChatMember();
        noWrapper.setCallSign("DL1ABC");
        noWrapper.setFrequency(null);
        noWrapper.setWorked(true);

        ChatMember noValue = new ChatMember();
        noValue.setCallSign("DL1ABC");
        noValue.setFrequency(new SimpleValue<>());
        noValue.setWorked(true);

        assertEquals(DxClusterSpotDecision.NO_FREQUENCY,
                MessageBusManagementThread.decideDxClusterSpot(noWrapper));
        assertEquals(DxClusterSpotDecision.NO_FREQUENCY,
                MessageBusManagementThread.decideDxClusterSpot(noValue));
        assertEquals(DxClusterSpotDecision.NO_FREQUENCY,
                MessageBusManagementThread.decideDxClusterSpot(senderOn("  ")));
        assertEquals(DxClusterSpotDecision.NO_FREQUENCY,
                MessageBusManagementThread.decideDxClusterSpot(null));
    }

    /*
     * Pinned oddity: the frequency text is only checked for being non-blank. A value
     * that names no supported band is spotted, even for a station flagged as worked.
     */
    @Test
    void pinsThatAnUnparseableFrequencyIsStillSpotted() {
        ChatMember sender = senderOn("999.9");
        sender.setWorked144(true);

        assertEquals(DxClusterSpotDecision.SPOT,
                MessageBusManagementThread.decideDxClusterSpot(sender));
    }

    // --- buildDxClusterSpotEntry --------------------------------------------

    @Test
    void spotEntryCarriesCallsignFrequencyAndCommentInTheLocatorField() {
        ChatMember sender = senderOn("144.300");
        sender.setQra("jo51hk");

        ChatMember entry = MessageBusManagementThread.buildDxClusterSpotEntry(sender);

        assertEquals("DL1ABC", entry.getCallSign());
        assertSame(sender.getFrequency(), entry.getFrequency());
        assertEquals("JO51HK", entry.getQra());
    }

    private static ChatMember senderOn(String frequencyMHz) {
        ChatMember sender = new ChatMember();
        sender.setCallSign("DL1ABC");
        sender.setFrequency(new SimpleValue<>(frequencyMHz));
        return sender;
    }
}
