package kst4contest.test;

import kst4contest.controller.ReadUDPbyAirScoutMessageThread;
import org.junit.jupiter.api.Test;

public class TestReadUDPASListenerThread {

	@Test
	public static void main(String[] args) {

		ReadUDPbyAirScoutMessageThread asUDPReader = new ReadUDPbyAirScoutMessageThread(9872, null, "AS", "KST");
		asUDPReader.start();

		String testThis;

//		testThis = "ASNEAREST: \"AS\" \"KST\" \"2023-04-01 21:33:42Z,DO5AMF,JN49GL,G4CLA,IO92JL,9,VLG2PT,M,190,75,14,BAW809,M,250,50,18,BEL6CM,M,143,50,12,WZZ6719,M,148,50,11,KLM1678,M,313,75,22,TRA1B,M,271,75,20,SAS4728,M,125,75,9,RYR6TL,M,90,75,6,UAE10,S,96,50,6\"";
//		asUDPReader.processASUDPMessage(testThis);
		
		
//		ucxUDPReader.processUCXUDPMessage(testThis);

		
//		ucxUDPReader =new ReadUDPbyUCXMessageThread(12060);
//		ucxUDPReader.processUCXUDPMessage(testThis);
	}

}
