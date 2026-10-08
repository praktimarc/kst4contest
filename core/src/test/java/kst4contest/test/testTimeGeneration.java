package kst4contest.test;

import kst4contest.controller.Utils4KST;
import org.junit.jupiter.api.Test;

public class testTimeGeneration {

	@Test
	public static void main(String[] args) {
		
		System.out.println(new Utils4KST().time_convertEpochToReadable("1664669836"));

	}

}
