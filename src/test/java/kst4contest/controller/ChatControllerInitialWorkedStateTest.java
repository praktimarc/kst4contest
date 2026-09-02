package kst4contest.controller;

import kst4contest.model.ChatCategory;
import kst4contest.model.ChatMember;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatControllerInitialWorkedStateTest {

    @Test
    void loadsEachCompletedInitialListOnceAndAppliesStateToEveryVariant()
            throws SQLException {
        DBController database = mock(DBController.class);
        ChatMember stored = member("9A0BB", 2);
        stored.setWorked(true);
        stored.setWorked144(true);
        stored.setWorked10G(true);
        stored.setQrv432(false);

        HashMap<String, ChatMember> databaseSnapshot = new HashMap<>();
        databaseSnapshot.put(stored.getCallSignRaw(), stored);
        when(database.fetchChatMemberWkdDataFromDB())
                .thenReturn(databaseSnapshot);

        ChatController controller = new ChatController();
        controller.setDbHandler(database);

        ChatMember mainVariant = member("9A0BB-2", 2);
        ChatMember secondVariant = member("9A0BB-70", 3);
        ChatMember reconnectMainVariant = member("9A0BB-144", 2);
        ChatMember reconnectSecondVariant = member("9A0BB-432", 3);

        controller.loadWorkedStateForInitialUserList(List.of(mainVariant));
        controller.loadWorkedStateForInitialUserList(List.of(secondVariant));
        controller.loadWorkedStateForInitialUserList(
                List.of(reconnectMainVariant));
        controller.loadWorkedStateForInitialUserList(
                List.of(reconnectSecondVariant));

        verify(database, times(4)).fetchChatMemberWkdDataFromDB();
        for (ChatMember variant : List.of(
                mainVariant,
                secondVariant,
                reconnectMainVariant,
                reconnectSecondVariant
        )) {
            assertTrue(variant.isWorked());
            assertTrue(variant.isWorked144());
            assertTrue(variant.isWorked10G());
            assertFalse(variant.isQrv432());
        }
    }

    @Test
    void keepsAmbiguousStationNameFromReplacingCompatibilityFrequency() {
        ChatController controller = new ChatController();
        ChatMember member = member("DL1ABC", 2);
        member.setName("144307 and 432100");

        controller.initializeFrequencyFromStationNameIfUnambiguous(member);

        assertTrue(
                member.getFrequency() == null
                        || member.getFrequency().get() == null
                        || member.getFrequency().get().isBlank()
        );
    }

    private static ChatMember member(String callSign, int categoryNumber) {
        ChatMember member = new ChatMember();
        member.setCallSign(callSign);
        member.setChatCategory(new ChatCategory(categoryNumber));
        return member;
    }
}
