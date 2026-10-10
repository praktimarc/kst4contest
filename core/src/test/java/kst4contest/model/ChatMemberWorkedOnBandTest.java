package kst4contest.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ChatMemberWorkedOnBandTest {

    @ParameterizedTest
    @EnumSource(Band.class)
    void noFlagSetMeansNotWorkedOnAnyBand(Band band) {
        ChatMember member = new ChatMember();

        assertFalse(member.isWorkedOnBand(band));
    }

    @Test
    void nullBandIsNeverWorked() {
        ChatMember member = new ChatMember();
        member.setWorked144(true);

        assertFalse(member.isWorkedOnBand(null));
    }

    @Test
    void globalWorkedFlagDoesNotCountAsBandFlag() {
        ChatMember member = new ChatMember();
        member.setWorked(true);

        for (Band band : Band.values()) {
            assertFalse(member.isWorkedOnBand(band), band.name());
        }
    }

    @Test
    void worked50MapsToBand50() {
        ChatMember member = new ChatMember();
        member.setWorked50(true);

        assertOnlyBandWorked(member, Band.B_50);
    }

    @Test
    void worked70MapsToBand70() {
        ChatMember member = new ChatMember();
        member.setWorked70(true);

        assertOnlyBandWorked(member, Band.B_70);
    }

    @Test
    void worked144MapsToBand144() {
        ChatMember member = new ChatMember();
        member.setWorked144(true);

        assertOnlyBandWorked(member, Band.B_144);
    }

    @Test
    void worked432MapsToBand432() {
        ChatMember member = new ChatMember();
        member.setWorked432(true);

        assertOnlyBandWorked(member, Band.B_432);
    }

    @Test
    void worked1240MapsToBand1296() {
        ChatMember member = new ChatMember();
        member.setWorked1240(true);

        assertOnlyBandWorked(member, Band.B_1296);
    }

    @Test
    void worked2300MapsToBand2320() {
        ChatMember member = new ChatMember();
        member.setWorked2300(true);

        assertOnlyBandWorked(member, Band.B_2320);
    }

    @Test
    void worked3400MapsToBand3400() {
        ChatMember member = new ChatMember();
        member.setWorked3400(true);

        assertOnlyBandWorked(member, Band.B_3400);
    }

    @Test
    void worked5600MapsToBand5760() {
        ChatMember member = new ChatMember();
        member.setWorked5600(true);

        assertOnlyBandWorked(member, Band.B_5760);
    }

    @Test
    void worked10GMapsToBand10G() {
        ChatMember member = new ChatMember();
        member.setWorked10G(true);

        assertOnlyBandWorked(member, Band.B_10G);
    }

    @Test
    void worked24GMapsToBand24G() {
        ChatMember member = new ChatMember();
        member.setWorked24G(true);

        assertOnlyBandWorked(member, Band.B_24G);
    }

    @Test
    void hasAnyBandWorkedFlagIsFalseWithoutFlags() {
        ChatMember member = new ChatMember();

        assertFalse(member.hasAnyBandWorkedFlag());
    }

    @Test
    void hasAnyBandWorkedFlagIgnoresGlobalWorkedFlag() {
        ChatMember member = new ChatMember();
        member.setWorked(true);

        assertFalse(member.hasAnyBandWorkedFlag());
    }

    @Test
    void hasAnyBandWorkedFlagDetectsEachBandFlag() {
        assertTrue(memberWith(m -> m.setWorked50(true)).hasAnyBandWorkedFlag());
        assertTrue(memberWith(m -> m.setWorked70(true)).hasAnyBandWorkedFlag());
        assertTrue(memberWith(m -> m.setWorked144(true)).hasAnyBandWorkedFlag());
        assertTrue(memberWith(m -> m.setWorked432(true)).hasAnyBandWorkedFlag());
        assertTrue(memberWith(m -> m.setWorked1240(true)).hasAnyBandWorkedFlag());
        assertTrue(memberWith(m -> m.setWorked2300(true)).hasAnyBandWorkedFlag());
        assertTrue(memberWith(m -> m.setWorked3400(true)).hasAnyBandWorkedFlag());
        assertTrue(memberWith(m -> m.setWorked5600(true)).hasAnyBandWorkedFlag());
        assertTrue(memberWith(m -> m.setWorked10G(true)).hasAnyBandWorkedFlag());
        assertTrue(memberWith(m -> m.setWorked24G(true)).hasAnyBandWorkedFlag());
        assertTrue(memberWith(m -> m.setWorked47G(true)).hasAnyBandWorkedFlag());
        assertTrue(memberWith(m -> m.setWorked76G(true)).hasAnyBandWorkedFlag());
    }

    @Test
    void hasAnyBandWorkedFlagIsFalseAfterReset() {
        ChatMember member = new ChatMember();
        member.setWorked144(true);
        member.setWorked76G(true);
        member.resetWorkedInformationAtAllBands();

        assertFalse(member.hasAnyBandWorkedFlag());
    }

    private static void assertOnlyBandWorked(ChatMember member, Band expected) {
        for (Band band : Band.values()) {
            if (band == expected) {
                assertTrue(member.isWorkedOnBand(band), band.name());
            } else {
                assertFalse(member.isWorkedOnBand(band), band.name());
            }
        }
    }

    private static ChatMember memberWith(java.util.function.Consumer<ChatMember> setup) {
        ChatMember member = new ChatMember();
        setup.accept(member);
        return member;
    }
}
