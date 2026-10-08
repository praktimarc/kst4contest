package kst4contest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import kst4contest.model.ChatMember;
import kst4contest.model.ClusterMessage;
import org.junit.jupiter.api.Test;

class ChatControllerSimpleLogTest {

	@Test
	void marksEveryActiveVariantOfWorkedBaseCallsign() {
		ChatMember categoryTwoVariant = member("9A0BB-2");
		ChatMember categoryThreeVariant = member("9A0BB-70");
		ChatMember unrelated = member("DL1ABC");

		int changed = ChatController.markSimpleLogWorkedMembers(
				List.of(categoryTwoVariant, categoryThreeVariant, unrelated),
				Set.of("9A0BB"));

		assertEquals(2, changed);
		assertTrue(categoryTwoVariant.isWorked());
		assertTrue(categoryThreeVariant.isWorked());
		assertFalse(unrelated.isWorked());
	}

	@Test
	void marksClusterReceiverByBaseCallsignAndHandlesIncompleteMessages() {
		ClusterMessage matching = new ClusterMessage();
		matching.setReceiver(member("9A0BB-70"));
		ClusterMessage incomplete = new ClusterMessage();

		int changed = ChatController.markSimpleLogWorkedClusterMessages(
				List.of(matching, incomplete), Set.of("9A0BB"));

		assertEquals(1, changed);
		assertTrue(matching.isReceiverWkd());
		assertFalse(incomplete.isReceiverWkd());
	}

	private static ChatMember member(String callSign) {
		ChatMember member = new ChatMember();
		member.setCallSign(callSign);
		return member;
	}
}
