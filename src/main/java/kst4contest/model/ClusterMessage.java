package kst4contest.model;

public class ClusterMessage {

	String timeGenerated;
	ChatMember sender;
	ChatMember receiver;
	String qrgSpotted;
	String qraOfSender;
	String qraOfReceiver;
	String messageInhibited;
	boolean receiverWkd;
	
	
	
	public boolean isReceiverWkd() {
		return receiverWkd;
	}
	public void setReceiverWkd(boolean receiverWkd) {
		this.receiverWkd = receiverWkd;
	}
	public String getTimeGenerated() {
		return timeGenerated;
	}
	public void setTimeGenerated(String timeGenerated) {
		this.timeGenerated = timeGenerated;
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
	public String getQrgSpotted() {
		return qrgSpotted;
	}
	public void setQrgSpotted(String qrgSpotted) {
		this.qrgSpotted = qrgSpotted;
	}
	public String getQraOfSender() {
		return qraOfSender;
	}
	public void setQraOfSender(String qraOfSender) {
		this.qraOfSender = qraOfSender;
	}
	public String getQraOfReceiver() {
		return qraOfReceiver;
	}
	public void setQraOfReceiver(String qraOfReceiver) {
		this.qraOfReceiver = qraOfReceiver;
	}
	public String getMessageInhibited() {
		return messageInhibited;
	}
	public void setMessageInhibited(String messageInhibited) {
		this.messageInhibited = messageInhibited;
	}
	
	
	
}
