package kst4contest.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import kst4contest.model.ChatPreferences;
import org.junit.jupiter.api.Test;

/**
 * Loopback tests for the UCXLog/DXLog UDP listener. Every socket is bound to
 * port 0 on the loopback interface, so the tests neither collide with a running
 * logger nor send anything to a broadcast address.
 */
class ReadUDPbyUCXMessageThreadLoopbackTest {

	private final ChatController controller = mock(ChatController.class);
	private final ChatPreferences preferences = mock(ChatPreferences.class);
	private final ThreadStatusCallback statusCallback = (threadName, message) -> { };

	@Test
	void interruptEndsTheReceiveLoopWithinTwoSeconds() throws Exception {
		when(controller.getChatPreferences()).thenReturn(preferences);
		ReadUDPbyUCXMessageThread listener = new ReadUDPbyUCXMessageThread(0, controller, statusCallback);
		listener.setDaemon(true);
		listener.start();
		// Let the thread bind its socket and block in receive() before interrupting it.
		Thread.sleep(200);

		listener.interrupt();
		listener.join(2_000);

		assertFalse(listener.isAlive(), "listener must stop once interrupted");
	}
}
