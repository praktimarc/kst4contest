package kst4contest.test;

import kst4contest.logic.BandOpportunityResolver;
import kst4contest.model.Band;
import kst4contest.model.ChatMember;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BandOpportunityResolverTest {

    @Test
    void resolvesCommonShorthandBandsFromStationName() {
        EnumSet<Band> bands = BandOpportunityResolver.detectBandsFromStationName(
                "Peter QRV 2-70-23/13/9/6/3"
        );

        assertEquals(
                EnumSet.of(
                        Band.B_144,
                        Band.B_432,
                        Band.B_1296,
                        Band.B_2320,
                        Band.B_3400,
                        Band.B_5760,
                        Band.B_10G
                ),
                bands
        );
    }

    @Test
    void doesNotMistakeOnePointTwoCentimetersForTwoMeters() {
        EnumSet<Band> bands = BandOpportunityResolver.detectBandsFromStationName(
                "David 23/3/1.2"
        );

        assertEquals(EnumSet.of(Band.B_1296, Band.B_10G, Band.B_24G), bands);
        assertFalse(bands.contains(Band.B_144));
    }

    @Test
    void keepsOnlyRecentDynamicBandEvidence() {
        long now = 1_000_000L;
        ChatMember station = new ChatMember();
        station.addKnownFrequency(Band.B_144, 144.210);
        station.addKnownFrequency(Band.B_432, 432.210);
        station.getKnownActiveBands().get(Band.B_144).timestampEpoch = now - 5_000L;
        station.getKnownActiveBands().get(Band.B_432).timestampEpoch =
                now - BandOpportunityResolver.RECENT_DYNAMIC_EVIDENCE_MAX_AGE_MS - 1L;

        BandOpportunityResolver.Resolution resolution =
                BandOpportunityResolver.resolve(List.of(station), now);

        assertEquals(EnumSet.of(Band.B_144), resolution.getOfferedBands());
    }

    @Test
    void notQrvOverridesNameAndFrequencyEvidenceAcrossVariants() {
        long now = 1_000_000L;

        ChatMember categoryTwo = new ChatMember();
        categoryTwo.setName("QRV 2m 70cm");
        categoryTwo.addKnownFrequency(Band.B_432, 432.210);
        categoryTwo.getKnownActiveBands().get(Band.B_432).timestampEpoch = now - 1_000L;

        ChatMember categoryThree = new ChatMember();
        categoryThree.setQrv432(false);

        BandOpportunityResolver.Resolution resolution =
                BandOpportunityResolver.resolve(List.of(categoryTwo, categoryThree), now);

        assertTrue(resolution.getOfferedBands().contains(Band.B_432));
        assertFalse(resolution.getAvailableBands().contains(Band.B_432));
        assertTrue(resolution.getAvailableBands().contains(Band.B_144));
    }

    @Test
    void opportunityRequiresAvailableEnabledAndUnworkedBand() {
        ChatMember station = new ChatMember();
        station.setName("2m 70cm");
        station.setWorked144(true);

        BandOpportunityResolver.Resolution resolution =
                BandOpportunityResolver.resolve(List.of(station), System.currentTimeMillis());

        assertEquals(
                EnumSet.of(Band.B_432),
                resolution.getUnworkedEnabledBands(EnumSet.of(Band.B_144, Band.B_432))
        );
    }
}
