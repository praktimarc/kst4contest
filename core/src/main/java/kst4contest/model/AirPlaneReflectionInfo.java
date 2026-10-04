package kst4contest.model;

import java.util.List;

public class AirPlaneReflectionInfo {

	String date;
	ChatMember sender, receiver;
	int airPlanesReachableCntr;
	List<AirPlane> risingAirplanes;

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public ChatMember getSender() {
		return sender;
	}

	public void setSender(ChatMember sender) {
		this.sender = sender;
	}

	public ChatMember getReceiver() {
		return receiver;
	}

	public void setReceiver(ChatMember receiver) {
		this.receiver = receiver;
	}

	public int getAirPlanesReachableCntr() {
		return airPlanesReachableCntr;
	}

	public void setAirPlanesReachableCntr(int airPlanesReachableCntr) {
		this.airPlanesReachableCntr = airPlanesReachableCntr;
	}

	public List<AirPlane> getRisingAirplanes() {
		return risingAirplanes;
	}

	public void setRisingAirplanes(List<AirPlane> risingAirplanes) {
		this.risingAirplanes = risingAirplanes;
	}

	@Override
	public String toString() {
		String toStringString = "";
		
		
		toStringString += this.sender.getCallSign() + " > " + this.getReceiver().getCallSign() + " at " + this.getDate() + " " + this.airPlanesReachableCntr + " planes: " + this.getRisingAirplanes().toString();
		
		return toStringString;
	}
	
}
