package kst4contest.model;

/* 50/70 MHz..............1 <br/> 144/432 MHz............2<br/> Microwave..............3<br/> 
* EME/JT65...............4 <br/> Low Band...............5 <br/> 50 MHz IARU Region 3...6 <br/> 50
* MHz IARU Region 2...7 <br/> 144/432 MHz IARU R 2...8 <br/> 144/432 MHz IARU R 3...9 <br/> kHz
* (2000-630m).......10 <br/> Warc (30,17,12m)......11 <br/> 28 MHz................12
* <br/> 
*	TODO: LowBand and khz need to be continued later...
*/
public class ChatCategory {

	@Override
    public String toString() {
		String toStringString ="";
		
		toStringString = this.getCategoryNumber()+": " +  this.getChatCategoryName(categoryNumber);
						
		return toStringString;
	}
	
	public static final int FIFTYSEVENTYMHz = 1;
	public static final int VUHF = 2;
	public static final int MICROWAVE = 3;
	public static final int EMEJT65 = 4;
	public static final int LOWBAND = 5;
	public static final int FIFTYR3 = 6;
	public static final int FIFTYR2 = 7;
	public static final int VUHFR2 = 8;
	public static final int VUHFR3 = 9;
	public static final int KHZ2KM630M = 10;
	public static final int WARC301712 = 11;
	public static final int TENMeter = 12;

	private int categoryNumber;
	
	public int[] getPossibleCategoryNumbers() {
		int[] possibleVals = new int[12];
		possibleVals[0] = 1;
		possibleVals[1] = 2;
		possibleVals[2] = 3;
		possibleVals[3] = 4;
		possibleVals[4] = 5;
		possibleVals[5] = 6;
		possibleVals[6] = 7;
		possibleVals[7] = 8;
		possibleVals[8] = 9;
		possibleVals[9] = 10;
		possibleVals[10] = 11;
		possibleVals[11] = 12;
		
		return possibleVals;
	}
	
	public ChatCategory(int setThiscategoryNumber) {
		this.categoryNumber = setThiscategoryNumber;
	}
	
	
	
	

	public int getCategoryNumber() {
		return categoryNumber;
	}





	public void setCategoryNumber(int categoryNumber) {
		this.categoryNumber = categoryNumber;
	}





	/**
	 * Returns an Array of int with possible frequency prefixes, due to in the chat
	 * normally the following format is used (not ever): <br/>
	 * "pse listen at .325"<br/>
	 * <br/>
	 * 
	 * 50/70 MHz..............1 <br/>
	 * 144/432 MHz............2 <br/>
	 * Microwave..............3 <br/>
	 * EME/JT65...............4 <br/>
	 * Low Band...............5 <br/>
	 * 50 MHz IARU Region 3...6 <br/>
	 * 50 MHz IARU Region 2...7 <br/>
	 * 144/432 MHz IARU R 2...8 <br/>
	 * 144/432 MHz IARU R 3...9 <br/>
	 * kHz (2000-630m).......10 <br/>
	 * Warc (30,17,12m)......11 <br/>
	 * 28 MHz................12 <br/>
	 * TODO: LowBand and khz need to be continued later...
	 * 
	 * @param int chatCategory (KST ChatCategory)
	 * @return Array of int with frequency prefixes, beginning with the lowest band
	 *         (e.G. int[2] = [144, 430] for VUHF)
	 */
	public int[] getChatCategoryFrequencyPrefix(int chatCategory) {
		int[] answer = new int[1];
		answer[0] = 0;

		switch (chatCategory) {

		case ChatCategory.FIFTYSEVENTYMHz: {
			answer = new int[2];
			answer[0] = 50;
			answer[1] = 70;
			return answer;

		}

		case ChatCategory.VUHF: {
			answer = new int[2];
			answer[0] = 144;
			answer[1] = 432;
			return answer;

		}
		case ChatCategory.VUHFR2: {
			answer = new int[2];
			answer[0] = 144;
			answer[1] = 432;
			return answer;

		}
		case ChatCategory.VUHFR3: {
			answer = new int[2];
			answer[0] = 144;
			answer[1] = 432;
			return answer;

		}

		case ChatCategory.EMEJT65: { // 1296.2
			answer = new int[13];
			answer[0] = 144;
			answer[1] = 432;
			answer[2] = 1296;
			answer[3] = 3400;
			answer[4] = 5760;
			answer[5] = 10368;
			answer[6] = 24048;
			answer[7] = 47000;
			answer[8] = 77500;
			answer[9] = 122250;
			answer[10] = 134928;
			answer[11] = 241920;
			answer[12] = 453000;
			return answer;

		}

		case ChatCategory.MICROWAVE: { // 1296.2
			answer = new int[13];
			answer[0] = 144;
			answer[1] = 432;
			answer[2] = 1296;
			answer[3] = 3400;
			answer[4] = 5760;
			answer[5] = 10368;
			answer[6] = 24048;
			answer[7] = 47000;
			answer[8] = 77500;
			answer[9] = 122250;
			answer[10] = 134928;
			answer[11] = 241920;
			answer[12] = 453000;
			return answer;

		}

		case ChatCategory.TENMeter: {
			answer = new int[1];
			answer[0] = 28;
			return answer;

		}

		case ChatCategory.WARC301712: {
			answer = new int[3];
			answer[0] = 10;
			answer[1] = 18;
			answer[2] = 24;
			return answer;

		}
		default:
			return answer;
		}

	};
	
	/**
	 * Returns an Array of int with possible frequency prefixes, due to in the chat
	 * normally the following format is used (not ever): <br/>
	 * "pse listen at .325"<br/>
	 * <br/>
	 * 
	 * 50/70 MHz..............1 <br/>
	 * 144/432 MHz............2 <br/>
	 * Microwave..............3 <br/>
	 * EME/JT65...............4 <br/>
	 * Low Band...............5 <br/>
	 * 50 MHz IARU Region 3...6 <br/>
	 * 50 MHz IARU Region 2...7 <br/>
	 * 144/432 MHz IARU R 2...8 <br/>
	 * 144/432 MHz IARU R 3...9 <br/>
	 * kHz (2000-630m).......10 <br/>
	 * Warc (30,17,12m)......11 <br/>
	 * 28 MHz................12 <br/>
	 * TODO: LowBand and khz need to be continued later...
	 * 
	 * @param int chatCategory (KST ChatCategory)
	 * @return Array of int with frequency prefixes, beginning with the lowest band
	 *         (e.G. int[2] = [144, 430] for VUHF)
	 */
	public String getChatCategoryName(int chatCategory) {
		
		
		switch (chatCategory) {

		case ChatCategory.FIFTYSEVENTYMHz: {
			
			return "50/70 MHz";

		}
		
		case ChatCategory.FIFTYR2: {
			return "50 MHz IARU Region 2";
		}
		
		case ChatCategory.FIFTYR3: {
			return "50 MHz IARU Region 3";
		}
		
		
		case ChatCategory.LOWBAND: {
			return "Low Band";
		}
		
		case ChatCategory.KHZ2KM630M: {
			return "kHz (2000-630m)";
		}
		

		case ChatCategory.VUHF: {
			
			return "144/432 MHz";

		}
		case ChatCategory.VUHFR2: {
			return "144/432 MHz IARU R 2";

		}
		case ChatCategory.VUHFR3: {
			return "144/432 MHz IARU R 3";

		}

		case ChatCategory.EMEJT65: { // 1296.2
			
			return "EME/JT65";

		}

		case ChatCategory.MICROWAVE: { // 1296.2
			return "Microwave";

		}

		case ChatCategory.TENMeter: {
			return "28 MHz";

		}

		case ChatCategory.WARC301712: {
			return "Warc (30,17,12m)";

		}
		default:
			return "ERRROR: unknown";
		}

	};

}
