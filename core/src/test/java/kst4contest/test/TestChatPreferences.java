package kst4contest.test;

import kst4contest.model.ChatPreferences;
import org.junit.jupiter.api.Test;

public class TestChatPreferences {

	@Test
	public static void main(String[] args) {

		ChatPreferences prefs = new ChatPreferences();
		prefs.readPreferencesFromXmlFile(); //works
		
		prefs.writePreferencesToXmlFile(); //works

	}

}
