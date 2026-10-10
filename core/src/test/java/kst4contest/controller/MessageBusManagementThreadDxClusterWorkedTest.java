package kst4contest.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import kst4contest.model.ChatMember;
import kst4contest.observe.SimpleValue;
import org.junit.jupiter.api.Test;

/**
 * Covers the worked-state check that suppresses automatic DX Cluster spots
 * (issue #91). Manual spots do not use this predicate.
 */
class MessageBusManagementThreadDxClusterWorkedTest {

    @Test
    void suppressesSpotWhenWorkedOnDetectedBand() {
        ChatMember sender = senderOn("144.3");
        sender.setWorked144(true);

        assertTrue(MessageBusManagementThread.isAlreadyWorkedForDxClusterSpot(sender));
    }

    @Test
    void allowsSpotWhenWorkedOnlyOnAnotherBand() {
        ChatMember sender = senderOn("432.2");
        sender.setWorked144(true);

        assertFalse(MessageBusManagementThread.isAlreadyWorkedForDxClusterSpot(sender));
    }

    @Test
    void mapsBand1296ToWorked1240Flag() {
        ChatMember sender = senderOn("1296.2");
        sender.setWorked1240(true);

        assertTrue(MessageBusManagementThread.isAlreadyWorkedForDxClusterSpot(sender));
    }

    @Test
    void allowsSpotWhenNothingIsWorked() {
        ChatMember sender = senderOn("144.3");

        assertFalse(MessageBusManagementThread.isAlreadyWorkedForDxClusterSpot(sender));
    }

    @Test
    void globalWorkedFlagWithoutBandInformationSuppressesSpot() {
        // Simplelogfile source: only the global flag is known, no per-band flags.
        ChatMember sender = senderOn("144.3");
        sender.setWorked(true);

        assertTrue(MessageBusManagementThread.isAlreadyWorkedForDxClusterSpot(sender));
    }

    @Test
    void globalWorkedFlagWithOtherBandInformationAllowsSpot() {
        // Per-band information exists and says 432 only, so 144 is still open.
        ChatMember sender = senderOn("144.3");
        sender.setWorked(true);
        sender.setWorked432(true);

        assertFalse(MessageBusManagementThread.isAlreadyWorkedForDxClusterSpot(sender));
    }

    @Test
    void allowsSpotWhenFrequencyWrapperIsNull() {
        ChatMember sender = new ChatMember();
        sender.setFrequency(null);
        sender.setWorked(true);
        sender.setWorked144(true);

        assertFalse(MessageBusManagementThread.isAlreadyWorkedForDxClusterSpot(sender));
    }

    @Test
    void allowsSpotWhenFrequencyValueIsNull() {
        ChatMember sender = new ChatMember();
        sender.setFrequency(new SimpleValue<>());
        sender.setWorked(true);
        sender.setWorked144(true);

        assertFalse(MessageBusManagementThread.isAlreadyWorkedForDxClusterSpot(sender));
    }

    @Test
    void allowsSpotWhenFrequencyIsEmpty() {
        ChatMember sender = senderOn("");
        sender.setWorked(true);
        sender.setWorked144(true);

        assertFalse(MessageBusManagementThread.isAlreadyWorkedForDxClusterSpot(sender));
    }

    @Test
    void allowsSpotWhenFrequencyIsOutsideKnownBands() {
        ChatMember sender = senderOn("999.9");
        sender.setWorked(true);
        sender.setWorked144(true);

        assertFalse(MessageBusManagementThread.isAlreadyWorkedForDxClusterSpot(sender));
    }

    @Test
    void allowsSpotWhenSenderIsNull() {
        assertFalse(MessageBusManagementThread.isAlreadyWorkedForDxClusterSpot(null));
    }

    private static ChatMember senderOn(String frequencyMHz) {
        ChatMember sender = new ChatMember();
        sender.setCallSign("DL1ABC");
        sender.setFrequency(new SimpleValue<>(frequencyMHz));
        return sender;
    }
}
