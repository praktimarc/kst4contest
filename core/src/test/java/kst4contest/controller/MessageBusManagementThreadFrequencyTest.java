package kst4contest.controller;

import kst4contest.model.Band;
import kst4contest.model.ChatMember;
import kst4contest.model.ChatMessage;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.LinkedBlockingQueue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MessageBusManagementThreadFrequencyTest {

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void detectsCompactMicrowaveFrequencyInPublicAndDirectedMessages(
            boolean directedMessage
    ) {
        ChatController controller = mock(ChatController.class);
        ThreadStatusCallback callback = mock(ThreadStatusCallback.class);
        MessageBusManagementThread messageBus =
                new MessageBusManagementThread(
                        controller,
                        callback,
                        1L,
                        new LinkedBlockingQueue<>(),
                        ignored -> true
                );

        ChatMember sender = member("DL1ABC");
        ChatMessage message = new ChatMessage();
        message.setSender(sender);
        message.setReceiver(member(directedMessage ? "DL2XYZ" : "ALL"));
        message.setMessageText("pse try 10368100");

        messageBus.smartFrequencyExtraction(message, null);

        verify(controller).applyDetectedFrequencyToActiveMembers(
                sender,
                Band.B_10G,
                10368.100
        );
    }

    private static ChatMember member(String callSign) {
        ChatMember member = new ChatMember();
        member.setCallSign(callSign);
        return member;
    }
}
