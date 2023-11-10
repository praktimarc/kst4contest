package kst4contest.model;

//import java.time.OffsetDateTime;

/**
 * boolean directedToServer is important
 * @author mywire
 *
 */
public class ChatMessage {
	
	ChatMember sender;
	String messageText;
	ChatMember receiver; //the callsign to whom the message is directed
	String  messageGeneratedTime; //generated time in epochtimemillis/1000, as used by the chatserver
	
	ChatCategory chatCategory;
	String messageSenderName;
	
	boolean messageByServer; //whether the chat message is directed do the server
	boolean messageDirectedToCommunity; //wheter the message is directed to anyone or to all users
	boolean messageDirectedToMe; //wheter message is directed to the praktiKST User
	boolean messageDirectedToServer;
	
	
	
	public boolean isMessageDirectedToServer() {
		return messageDirectedToServer;
	}
	public void setMessageDirectedToServer(boolean messageDirectedToServer) {
		this.messageDirectedToServer = messageDirectedToServer;
	}
	public boolean isMessageDirectedToCommunity() {
		return messageDirectedToCommunity;
	}
	public void setMessageDirectedToCommunity(boolean messageDirectedToCommunity) {
		this.messageDirectedToCommunity = messageDirectedToCommunity;
	}
	public boolean isMessageDirectedToMe() {
		return messageDirectedToMe;
	}
	public void setMessageDirectedToMe(boolean messageDirectedToMe) {
		this.messageDirectedToMe = messageDirectedToMe;
	}
	public String getMessageSenderName() {
		return messageSenderName;
	}
	public void setMessageSenderName(String messageSenderName) {
		this.messageSenderName = messageSenderName;
	}
	public ChatCategory getChatCategory() {
		return chatCategory;
	}
	public void setChatCategory(ChatCategory chatCategory) {
		this.chatCategory = chatCategory;
	}
	public boolean isMessageByServer() {
		return messageByServer;
	}
	public void setMessageByServer(boolean isMessageByServer) {
		this.messageByServer = isMessageByServer;
	}
	public void setMessageText(String messageText) {
		this.messageText = messageText;
	}
	public String getMessageGeneratedTime() {
		return messageGeneratedTime;
	}
	public void setMessageGeneratedTime(String messageGeneratedTime) {
		this.messageGeneratedTime = messageGeneratedTime;
	}
	public ChatMember getSender() {
		return sender;
	}
	public void setSender(ChatMember sender) {
		this.sender = sender;
	}
	public String getMessageText() {
		return messageText;
	}
	public ChatMember getReceiver() {
		return receiver;
	}
	public void setReceiver(ChatMember receiver) {
		this.receiver = receiver;
	}
	
	
	
}
