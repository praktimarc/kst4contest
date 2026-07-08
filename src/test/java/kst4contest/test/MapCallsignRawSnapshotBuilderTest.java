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

    private ChatMember buildStation(String callSign, String name, String locator, long activityEpoch) {
        ChatMember chatMember = new ChatMember();
        chatMember.setCallSign(callSign);
        chatMember.setName(name);
        chatMember.setQra(locator);
        chatMember.setActivityTimeLastInEpoch(activityEpoch);
        return chatMember;
    }
}
