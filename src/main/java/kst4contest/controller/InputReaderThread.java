package kst4contest.controller;

import java.io.*;
import java.net.*;

import kst4contest.model.ChatMessage;
 
/**
 *	Consolereader, reads a full line and adds it to the tx-queue after formatting it like: <br> <br> 
 *
 *	MSG|2|0|/cq DO5AMF kst4contest.test|0| <br/><br/>
 *
 *	No need for it as it´s not longer a console application
 */
public class InputReaderThread extends Thread {
    private PrintWriter writer;
    private Socket socket;
    private ChatController client;
 
    public InputReaderThread(ChatController client) throws InterruptedException {
        this.client = client;
        
    }
 
    
    public void run() {
         
    	
 
        while (true) {
        	
        	BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        	ChatMessage ownMSG = new ChatMessage();
//        	ownMSG.setDirectedToServer(true);
        	
        	
        	String sendThisMessage23001 = "";
        	
        	try {
        		sendThisMessage23001 = reader.readLine();
        	} catch (IOException e) {
        		// TODO Auto-generated catch block
        		e.printStackTrace();
        	}

        	ownMSG.setMessageText("MSG|" + this.client.getCategory().getCategoryNumber() + "|0|" + sendThisMessage23001 + "|0|");
        	
//        	System.out.println("inreader " + ownMSG.getMessage() + client.getMessageTXBus().size());
        	
//        	client.getMessageTXBus().add(ownMSG);
        	client.getMessageTXBus().add(ownMSG);
        	
        	try {
				this.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	
        }
   
    }
}
