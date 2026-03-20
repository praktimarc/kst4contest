package kst4contest.model;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ChatMember {


	long lastFlagsChangeEpochMs; // timestamp of the last worked/not-QRV flag change in the internal DB

	//	private final BooleanProperty workedInfoChangeFireListEventTrigger = new SimpleBooleanProperty();
	AirPlaneReflectionInfo airPlaneReflectInfo;
	String callSign;
	String qra;
	String name;
	String callSignRaw; //without -2 or -70 etc.



	boolean isInAngleAndRange; //if he tries a sked in my dir, he is in range, will process that in the messages

//	String frequency; // last known qrg of the station

	StringProperty frequency = new SimpleStringProperty();

	String password; // only used by own instance of the chatmember instance to login to the chat
	ChatCategory chatCategory; //Source category
//	ChatCategory chatCategory;//only used by own instance of the chatmember instance to login to the chat

	long activityTimeLastInEpoch; // time of last activity in epochtimesec
	Date lastActivity; // time of last activity in epochtimesec
	Date lastActualizationTimeOfThisMember; // time of last state change if that member
	Double qrb;
	int state;

	Double QTFdirection; // antenna direction in deg
	int[] workedCategories; // Chatcategory where the station is in the log, see kst4contest.model.ChatCategory

	boolean worked; // true if the callsign is logged already - for temporary worked processing
	boolean worked144;
	boolean worked432;
	boolean worked1240;
	boolean worked2300;
	boolean worked3400;
	boolean worked5600;
	boolean worked10G;
    boolean Worked50;
    boolean Worked70;
    boolean Worked24G;
    boolean Worked47G;
    boolean Worked76G;


	/**
	 * Chatmember is qrv at all band except we initialize anything other, depending to user entry
	 */
	boolean qrv144 = true;
	boolean qrv432 = true;
	boolean qrv1240 = true;
	boolean qrv2300 = true;
	boolean qrv3400 = true;
	boolean qrv5600 = true;
	boolean qrv10G = true;
	boolean qrvAny = true;

	// Stores the last known frequency per band (Context History)
	private final Map<Band, ActiveFrequencyInfo> knownActiveBands = new ConcurrentHashMap<>();


	// --- INNER CLASS FOR QRG HISTORY ---
	public class ActiveFrequencyInfo {
		public double frequency;
		public long timestampEpoch;

		public ActiveFrequencyInfo(double freq) {
			this.frequency = freq;
			this.timestampEpoch = System.currentTimeMillis();
		}
	}

	// Counter for failed calls (Penalty Logic)
	private int failedQSOAttempts = 0;

	// Calculated Score for sorting the user list
	private double currentPriorityScore = 0.0;


	public long getLastFlagsChangeEpochMs() {
		return lastFlagsChangeEpochMs;
	}

	public void setLastFlagsChangeEpochMs(long lastFlagsChangeEpochMs) {
		this.lastFlagsChangeEpochMs = lastFlagsChangeEpochMs;
	}

	public boolean isInAngleAndRange() {
		return isInAngleAndRange;
	}

	public void setInAngleAndRange(boolean inAngleAndRange) {
		isInAngleAndRange = inAngleAndRange;
	}

	public AirPlaneReflectionInfo getAirPlaneReflectInfo() {
		return airPlaneReflectInfo;
	}

	public void setAirPlaneReflectInfo(AirPlaneReflectionInfo airPlaneReflectInfo) {
		this.airPlaneReflectInfo = airPlaneReflectInfo;
	}

	public ChatCategory getChatCategory() {
		return chatCategory;
	}

	public void setChatCategory(ChatCategory chatCategory) {
		this.chatCategory = chatCategory;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public boolean isWorked144() {
		return worked144;
	}

	public void setWorked144(boolean worked144) {
		this.worked144 = worked144;
	}

	public boolean isWorked432() {
		return worked432;
	}

	public void setWorked432(boolean worked432) {
		this.worked432 = worked432;
	}

	public boolean isWorked1240() {
		return worked1240;
	}

	public void setWorked1240(boolean worked1240) {
		this.worked1240 = worked1240;
	}

	public boolean isWorked2300() {
		return worked2300;
	}

	public void setWorked2300(boolean worked2300) {
		this.worked2300 = worked2300;
	}

	public boolean isWorked3400() {
		return worked3400;
	}

	public void setWorked3400(boolean worked3400) {
		this.worked3400 = worked3400;
	}

	public boolean isWorked5600() {
		return worked5600;
	}

	public void setWorked5600(boolean worked5600) {
		this.worked5600 = worked5600;
	}

	public boolean isWorked10G() {
		return worked10G;
	}

	public void setWorked10G(boolean worked10g) {
		worked10G = worked10g;
	}

	public boolean isQrv144() {
		return qrv144;
	}

	public void setQrv144(boolean qrv144) {
		this.qrv144 = qrv144;
	}

	public boolean isQrv432() {
		return qrv432;
	}

	public void setQrv432(boolean qrv432) {
		this.qrv432 = qrv432;
	}

	public boolean isQrv1240() {
		return qrv1240;
	}

	public void setQrv1240(boolean qrv1240) {
		this.qrv1240 = qrv1240;
	}

	public boolean isQrv2300() {
		return qrv2300;
	}

	public void setQrv2300(boolean qrv2300) {
		this.qrv2300 = qrv2300;
	}

	public boolean isQrv3400() {
		return qrv3400;
	}

	public void setQrv3400(boolean qrv3400) {
		this.qrv3400 = qrv3400;
	}

	public boolean isQrv5600() {
		return qrv5600;
	}

	public void setQrv5600(boolean qrv5600) {
		this.qrv5600 = qrv5600;
	}

	public boolean isQrv10G() {
		return qrv10G;
	}

	public void setQrv10G(boolean qrv10G) {
		this.qrv10G = qrv10G;
	}

	public boolean isQrvAny() {
		return qrvAny;
	}

	public void setQrvAny(boolean qrvAny) {
		this.qrvAny = qrvAny;
	}

	public int[] getWorkedCategories() {
		return workedCategories;
	}

	public void setWorkedCategories(int[] workedCategories) {
		this.workedCategories = workedCategories;
	}

	public void setActivityTimeLastInEpoch(long activityTimeLastInEpoch) {
		this.activityTimeLastInEpoch = activityTimeLastInEpoch;
	}

	public int getState() {
		return state;
	}

	public void setState(int state) {
		this.state = state;
	}

	public Date getLastActivity() {
		return lastActivity;
	}

	public void setLastActivity(Date lastActivity) {
		this.lastActivity = lastActivity;
	}

	public Date getLastActualizationTimeOfThisMember() {
		return lastActualizationTimeOfThisMember;
	}

	public void setLastActualizationTimeOfThisMember(Date lastActualizationTimeOfThisMember) {
		this.lastActualizationTimeOfThisMember = lastActualizationTimeOfThisMember;
	}

	public Double getQrb() {
		return qrb;
	}

	public void setQrb(Double qrb) {
		this.qrb = qrb;
	}

	public Double getQTFdirection() {
		return QTFdirection;
	}

	public void setQTFdirection(Double qTFdirection) {
		QTFdirection = qTFdirection;
	}

	public String getCallSign() {
		return callSign;
	}

	/**
	 * Sets the original callsign and derives the normalized base callsign which is
	 * used as the database key. Prefixes like EA5/ and suffixes like /P or -70 are
	 * ignored for the raw-key handling.
	 *
	 * @param callSign callsign as received from chat or database
	 */
	public void setCallSign(String callSign) {

		if (callSign == null) {
			this.callSign = null;
			this.callSignRaw = null;
			return;
		}

		this.callSign = callSign.trim().toUpperCase(Locale.ROOT);
		this.callSignRaw = normalizeCallSignToBaseCallSign(this.callSign);
	}

	/**
	 * Normalizes a callsign to the base callsign which is used as the unique key in
	 * the internal database. The method removes KST suffixes like "-2", portable
	 * suffixes like "/P" and prefix additions like "EA5/".
	 *
	 * @param callSign callsign to normalize
	 * @return normalized base callsign in upper case
	 */
	public static String normalizeCallSignToBaseCallSign(String callSign) {

		if (callSign == null) {
			return null;
		}

		String normalizedCallSign = callSign.trim().toUpperCase(Locale.ROOT);

		if (normalizedCallSign.isBlank()) {
			return normalizedCallSign;
		}

		String callSignWithoutDashSuffix = normalizedCallSign.split("-", 2)[0].trim();

		if (!callSignWithoutDashSuffix.contains("/")) {
			return callSignWithoutDashSuffix;
		}

		String[] callSignParts = callSignWithoutDashSuffix.split("/");
		String bestMatchingCallsignPart = helper_selectBestCallsignPart(callSignParts);

		if (bestMatchingCallsignPart == null || bestMatchingCallsignPart.isBlank()) {
			return callSignWithoutDashSuffix;
		}

		return bestMatchingCallsignPart;
	}

	/**
	 * Selects the most plausible base callsign segment from a slash-separated
	 * callsign. In strings like "EA5/G8MBI/P" the segment "G8MBI" is preferred over
	 * prefix or portable markers.
	 *
	 * @param callSignParts slash-separated callsign parts
	 * @return best matching base callsign segment
	 */
	private static String helper_selectBestCallsignPart(String[] callSignParts) {

		String bestLikelyBaseCallsignPart = null;
		int bestLikelyBaseCallsignLength = -1;
		String bestFallbackCallsignPart = null;
		int bestFallbackCallsignLength = -1;

		for (String rawCallsignPart : callSignParts) {

			String currentCallsignPart = rawCallsignPart == null ? "" : rawCallsignPart.trim().toUpperCase(Locale.ROOT);

			if (currentCallsignPart.isBlank()) {
				continue;
			}

			if (currentCallsignPart.length() > bestFallbackCallsignLength) {
				bestFallbackCallsignPart = currentCallsignPart;
				bestFallbackCallsignLength = currentCallsignPart.length();
			}

			if (helper_isLikelyBaseCallsignSegment(currentCallsignPart)
					&& currentCallsignPart.length() > bestLikelyBaseCallsignLength) {
				bestLikelyBaseCallsignPart = currentCallsignPart;
				bestLikelyBaseCallsignLength = currentCallsignPart.length();
			}
		}

		if (bestLikelyBaseCallsignPart != null) {
			return bestLikelyBaseCallsignPart;
		}

		return bestFallbackCallsignPart;
	}

	/**
	 * Checks whether a slash-separated segment looks like a real base callsign. A
	 * normal amateur-radio callsign typically contains letters and digits and is
	 * longer than one-character postfix markers.
	 *
	 * @param callsignSegment segment to inspect
	 * @return true if the segment looks like a base callsign
	 */
	private static boolean helper_isLikelyBaseCallsignSegment(String callsignSegment) {

		boolean containsLetter = false;
		boolean containsDigit = false;

		for (int currentIndex = 0; currentIndex < callsignSegment.length(); currentIndex++) {
			char currentCharacter = callsignSegment.charAt(currentIndex);

			if (Character.isLetter(currentCharacter)) {
				containsLetter = true;
			}

			if (Character.isDigit(currentCharacter)) {
				containsDigit = true;
			}
		}

		return containsLetter && containsDigit && callsignSegment.length() >= 3;
	}

	public String getQra() {
		return qra;
	}

	public void setQra(String qra) {
		this.qra = qra;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public StringProperty getFrequency() {

		return frequency;
//		return frequency;
	}

	public void setFrequency(StringProperty frequency) {

		this.frequency = frequency;
	}

	public long getActivityTimeLastInEpoch() {
		return activityTimeLastInEpoch;
	}

	public void setActivityCounter(int activityCounter) {
		this.activityTimeLastInEpoch = activityCounter;
	}

	public boolean isWorked() {
		return worked;
	}

    public boolean isWorked50() {
        return Worked50;
    }

    public void setWorked50(boolean worked50) {
        Worked50 = worked50;
    }

    public boolean isWorked70() {
        return Worked70;
    }

    public void setWorked70(boolean worked70) {
        Worked70 = worked70;
    }

    public boolean isWorked24G() {
        return Worked24G;
    }

    public void setWorked24G(boolean worked24G) {
        Worked24G = worked24G;
    }

    public boolean isWorked47G() {
        return Worked47G;
    }

    public void setWorked47G(boolean worked47G) {
        Worked47G = worked47G;
    }

    public boolean isWorked76G() {
        return Worked76G;
    }

    public void setWorked76G(boolean worked76G) {
        Worked76G = worked76G;
    }

    public void setWorked(boolean worked) {
		this.worked = worked;



	}

	/**
	 *
	 * @return String (callsign) without -2 or -70 etc.
	 */
	public String getCallSignRaw() {


        return callSignRaw;
//			String raw = "";
//
//		try {
//			return this.getCallSign().split("-")[0]; //e.g. OK2M-70, returns only ok2m
//		} catch (Exception e) {
//			return getCallSign();
//		}
	}


	/**
	 * Sets all worked information of this object to false. Scope: GUI, Reset Button
	 * for worked info, called by appcontroller
	 */
	public void resetWorkedInformationAtAllBands() {

		this.setWorked(false);
		this.setWorked144(false);
        this.setWorked50(false);
        this.setWorked70(false);
		this.setWorked432(false);
		this.setWorked1240(false);
		this.setWorked2300(false);
		this.setWorked3400(false);
		this.setWorked5600(false);
		this.setWorked10G(false);
        this.setWorked24G(false);
        this.setWorked47G(false);
        this.setWorked76G(false);


    }

	/**
	 * Sets all worked information of this object to false. Scope: GUI, Reset Button
	 * for worked info, called by appcontroller
	 */
	public void resetQRVInformationAtAllBands() {

		this.setQrvAny(true);
		this.setQrv144(true);
		this.setQrv432(true);
		this.setQrv1240(true);
		this.setQrv2300(true);
		this.setQrv3400(true);
		this.setQrv5600(true);
		this.setQrv10G(true);
	}

	@Override
	public String toString() {
		String chatMemberSerialization = "";

		chatMemberSerialization += callSign + ";" + name + ";" + qra + ";" + frequency + "; wkd " + worked + "; wkd144 " + worked144
				+ "; wkd432" + worked432 + "; wkd1240" + worked1240 + "; wkd2300" + worked2300 + "; wkd3400" + worked3400 + "; wkd5600" + worked5600 + "; wkd10G"
				+ worked10G + " ; " + chatCategory;

		return chatMemberSerialization;
	}

	/**
	 * Finds out if a given Chatmember-instance has the same callsign as this
	 * instance. Callsign is used as a key.
	 * 
	 * @param anotherChatMember
	 * @return
	 */
	public boolean equals(ChatMember anotherChatMember) {
		if (this.getCallSign().toUpperCase().equals(anotherChatMember.getCallSign().toUpperCase())) {
			return true;
		} else
			return false;
	}

	/**
	 * Adds a new recognized frequency by band to the internal band/qrg map
	 * @param band
	 * @param freq
	 */
	public void addKnownFrequency(Band band, double freq) {
		this.knownActiveBands.put(band, new ActiveFrequencyInfo(freq));
	}

	/**
	 * represents a map of bands which are known of this chatmember
	 *
	 * @return Band
	 */
	public Map<Band, ActiveFrequencyInfo> getKnownActiveBands() {
		return knownActiveBands;
	}


	/**
	 * If a sked fails and the user tells this to the client, this counter will be increased to give the station a
	 * lower score
	 */
	public void incrementFailedAttempts() {
		this.failedQSOAttempts++;
	}

	public void resetFailedAttempts() {
		this.failedQSOAttempts = 0;
	}

	public int getFailedQSOAttempts() {
		return failedQSOAttempts;
	}

	/**
	 * Sets the working-priority score of a chatmember for the "Todo-List"
	 * @param score
	 */
	public void setCurrentPriorityScore(double score) {
		this.currentPriorityScore = score;
	}

	/**
	 * Gets the working-priority score of a chatmember for the "Todo-List"
	 *
	 */
	public double getCurrentPriorityScore() {
		return currentPriorityScore;
	}


}