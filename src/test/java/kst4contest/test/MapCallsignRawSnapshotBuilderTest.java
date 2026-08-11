package kst4contest.test;

import kst4contest.model.Band;
import kst4contest.model.ChatMember;
import kst4contest.view.map.MapCallsignRawSnapshot;
import kst4contest.view.map.MapCallsignRawSnapshotBuilder;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapCallsignRawSnapshotBuilderTest {

    @Test
    void explicitStationNameQrgIsShownInMapSnapshot() {
        ChatMember station =
                buildStation(
                        "G0JSB",
                        "Phil 432.357",
                        "IO91AA",
                        1_000L
                );

        MapCallsignRawSnapshot snapshot =
                new MapCallsignRawSnapshotBuilder()
                        .buildSnapshots(
                                List.of(station),
                                null,
                                EnumSet.of(Band.B_432)
                        )
                        .get(0);

        assertEquals(
                "432",
                snapshot.bandSummary()
        );

        assertEquals(
                "432.357",
                snapshot
                        .lastKnownFrequenciesByBand()
                        .get("432")
        );
    }

    @Test
    void recentDynamicQrgWinsOverStationNameQrgInMap() {
        ChatMember station =
                buildStation(
                        "G0JSB",
                        "Phil 432.357",
                        "IO91AA",
                        1_000L
                );

        station.addKnownFrequency(
                Band.B_432,
                432.335
        );

        MapCallsignRawSnapshot snapshot =
                new MapCallsignRawSnapshotBuilder()
                        .buildSnapshots(
                                List.of(station),
                                null,
                                EnumSet.of(Band.B_432)
                        )
                        .get(0);

        assertEquals(
                "432.335",
                snapshot
                        .lastKnownFrequenciesByBand()
                        .get("432")
        );
    }

    @Test
    void marksSnapshotWhenNameAdvertisesSelectedBand() {
        ChatMember station = buildStation("DL1ABC", "QRV 2-70-23", "JN58TD", 1_000L);

        MapCallsignRawSnapshotBuilder builder = new MapCallsignRawSnapshotBuilder();
        List<MapCallsignRawSnapshot> snapshots = builder.buildSnapshots(
                List.of(station),
                null,
                EnumSet.of(Band.B_1296)
        );

        assertEquals(1, snapshots.size());
        assertTrue(snapshots.get(0).offersSelectedBand());
    }

    @Test
    void keepsSnapshotUnmarkedWhenNoSelectedBandMatches() {
        ChatMember station = buildStation("DL1ABC", "QRV 2m only", "JN58TD", 1_000L);

        MapCallsignRawSnapshotBuilder builder = new MapCallsignRawSnapshotBuilder();
        List<MapCallsignRawSnapshot> snapshots = builder.buildSnapshots(
                List.of(station),
                null,
                EnumSet.of(Band.B_2320)
        );

        assertEquals(1, snapshots.size());
        assertFalse(snapshots.get(0).offersSelectedBand());
    }

    @Test
    void notQrvOverridesNameDerivedMapOpportunity() {
        ChatMember station = buildStation("DL1ABC", "QRV 2m 70cm", "JN58TD", 1_000L);
        station.setQrv432(false);

        MapCallsignRawSnapshotBuilder builder = new MapCallsignRawSnapshotBuilder();
        MapCallsignRawSnapshot snapshot = builder.buildSnapshots(
                List.of(station),
                null,
                EnumSet.of(Band.B_432)
        ).get(0);

        assertFalse(snapshot.offersSelectedBand());
        assertFalse(snapshot.bandSummary().contains("432"));
    }

    @Test
    void workedBandIsShownAsInformationButNotAsUpgradeOpportunity() {
        ChatMember station = buildStation("DL1ABC", "QRV 2m", "JN58TD", 1_000L);
        station.setWorked144(true);

        MapCallsignRawSnapshotBuilder builder = new MapCallsignRawSnapshotBuilder();
        MapCallsignRawSnapshot snapshot = builder.buildSnapshots(
                List.of(station),
                null,
                EnumSet.of(Band.B_144)
        ).get(0);

        assertTrue(snapshot.bandSummary().contains("144"));
        assertFalse(snapshot.offersSelectedBand());
    }

    private ChatMember buildStation(String callSign, String name, String locator, long activityEpoch) {
        ChatMember chatMember = new ChatMember();
        chatMember.setCallSign(callSign);
        chatMember.setName(name);
        chatMember.setQra(locator);
        chatMember.setActivityTimeLastInEpoch(activityEpoch);
        return chatMember;
    }
}
