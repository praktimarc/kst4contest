package kst4contest.view;

import kst4contest.controller.On4KstConnectionState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MacOsConnectionStateMenuTitleTest {

    @Test
    void everyStateHasTheAgreedTitle() {
        assertEquals("🟢 LINK: Connected",
                Kst4ContestApplication.macOsConnectionStateMenuTitle(On4KstConnectionState.ONLINE));
        for (On4KstConnectionState state : new On4KstConnectionState[]{
                On4KstConnectionState.CONNECTING,
                On4KstConnectionState.WAITING_FOR_LOGIN_PROMPT,
                On4KstConnectionState.AUTHENTICATING,
                On4KstConnectionState.SYNCING_MAIN_CHAT,
                On4KstConnectionState.SYNCING_SECOND_CHAT}) {
            assertEquals("🟡 LINK: Connecting…", Kst4ContestApplication.macOsConnectionStateMenuTitle(state));
        }
        assertEquals("🟡 LINK: Disconnecting…",
                Kst4ContestApplication.macOsConnectionStateMenuTitle(On4KstConnectionState.STOPPING));
        assertEquals("🔴 LINK: Reconnecting…",
                Kst4ContestApplication.macOsConnectionStateMenuTitle(On4KstConnectionState.RECONNECT_WAIT));
        assertEquals("🔴 LINK: Disconnected",
                Kst4ContestApplication.macOsConnectionStateMenuTitle(On4KstConnectionState.DISCONNECTED));
    }

    @Test
    void missingStateIsShownAsDisconnected() {
        assertEquals("🔴 LINK: Disconnected", Kst4ContestApplication.macOsConnectionStateMenuTitle(null));
    }
}
