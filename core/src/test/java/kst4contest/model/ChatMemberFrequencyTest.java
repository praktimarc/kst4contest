package kst4contest.model;

import kst4contest.observe.SimpleValue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ChatMemberFrequencyTest {

    @Test
    void aFreshMemberHasNoFrequency() {
        ChatMember member = new ChatMember();
        assertNotNull(member.getFrequency(), "the holder itself must exist");
        assertNull(member.getFrequency().get(),
                "an unknown frequency must stay unavailable, never become an empty string or a default band");
    }

    @Test
    void theFrequencyCanBeSetAndReadBack() {
        ChatMember member = new ChatMember();
        member.setFrequency(new SimpleValue<>("144.300"));
        assertEquals("144.300", member.getFrequency().get());
    }

    @Test
    void clearingTheFrequencyReturnsToUnavailable() {
        ChatMember member = new ChatMember();
        member.setFrequency(new SimpleValue<>("144.300"));
        member.getFrequency().set(null);
        assertNull(member.getFrequency().get());
    }
}
