package kst4contest.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatMemberDirectionOpportunityTest {

    @Test
    void directionalOpportunityExpiresAfterConfiguredValidityPeriod() {
        ChatMember member = new ChatMember();

        member.setInAngleAndRange(true);
        long afterActivationEpochMs = System.currentTimeMillis();

        assertTrue(member.isInAngleAndRangeAt(afterActivationEpochMs));
        assertFalse(member.isInAngleAndRangeAt(
                afterActivationEpochMs
                        + ChatMember.DIRECTION_OPPORTUNITY_VALIDITY_MILLIS
                        + 1L));
    }

    @Test
    void directionalOpportunityCanBeClearedImmediately() {
        ChatMember member = new ChatMember();

        member.setInAngleAndRange(true);
        assertTrue(member.isInAngleAndRange());

        member.setInAngleAndRange(false);
        assertFalse(member.isInAngleAndRange());
    }
}