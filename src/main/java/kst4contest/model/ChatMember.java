package kst4contest.model;

import java.util.Date;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ChatMember {

	AirPlaneReflectionInfo airPlaneReflectInfo;
	String callSign;
	String qra;
	String name;

	boolean isInAngleAndRange; //if he tries a sked in my dir, he is in range, will process that in the messages

//	String frequency; // last known qrg of the station

	StringProperty frequency = new SimpleStringProperty();

	String password; // only used by own instance of the chatmember instance to login to the chat
	ChatCategory chatCategory; // only used by own instance of the chatmember instance to login to the chat
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

	public void setCallSign(String callSign) {
		this.callSign = callSign;
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

	public void setWorked(boolean worked) {
		this.worked = worked;
	}

	/**
	 * Sets all worked information of this object to false. Scope: GUI, Reset Button
	 * for worked info, called by appcontroller
	 */
	public void resetWorkedInformationAtAllBands() {

		this.setWorked(false);
		this.setWorked144(false);
		this.setWorked432(false);
		this.setWorked1240(false);
		this.setWorked2300(false);
		this.setWorked3400(false);
		this.setWorked5600(false);
		this.setWorked10G(false);
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

		chatMemberSerialization += callSign + ";" + name + ";" + qra + ";" + frequency + ";" + worked + ";" + worked144
				+ ";" + worked432 + ";" + worked1240 + ";" + worked2300 + ";" + worked3400 + ";" + worked5600 + ";"
				+ worked10G;

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

}