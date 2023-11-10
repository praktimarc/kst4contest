package kst4contest.test;

import java.io.IOException;

import kst4contest.controller.UCXLogFileToHashsetParser;
import org.junit.jupiter.api.Test;

public class TestUCXLogFileToHashsetParser {

	@Test
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		UCXLogFileToHashsetParser testTheParser = new UCXLogFileToHashsetParser("C:\\UcxLog\\Logs\\DO5AMF\\DVU322_I.UCX");
		try {
			testTheParser.parse();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
