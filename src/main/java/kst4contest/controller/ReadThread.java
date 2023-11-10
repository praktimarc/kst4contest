package kst4contest.controller;

import java.io.*;
import java.net.*;

import kst4contest.model.ChatMessage;
 
/**
 * This thread is responsible for reading telnet servers input at port 23001 and printing it
 * to the console.
 * It runs in an infinite loop until the client disconnects from the server.
 *
 * @author www.codejava.net
 */
public class ReadThread extends Thread {
    private BufferedReader reader;
    private Socket socket;
    private ChatController client;
    public boolean accidentalDisconnected;
    
    
    
    public boolean isAccidentalDisconnected() {
		return accidentalDisconnected;
	}

	public void setAccidentalDisconnected(boolean accidentalDisconnected) {
		this.accidentalDisconnected = accidentalDisconnected;
	}

	//    private boolean readingFinished = true; //kst4contest.test 4 23001
    private boolean readingFinished = true;
 
    InputStream input;
    
    public ReadThread(Socket socket, ChatController client) {
        this.socket = socket;
        this.client = client;
 
        try {
            input = socket.getInputStream();            
            reader = new BufferedReader(new InputStreamReader(input));
            
        } catch (IOException ex) {
            System.out.println("Error getting input stream: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
 
    public void run() {
    	Thread.currentThread().setName("ReadFromTelnetThread");

    	ChatMessage message; //bugfix leak, moved out of while 
    	while (true) {
        	
//    		System.out.println("rdth");
    		
            try {
            
                String response = reader.readLine();  
                message = new ChatMessage();
                message.setMessageText(response);
                
//                message.setDirectedToServer(false);
//                message.setDirectedToServer(false);
//                message.setDirectedToServer(false);
                
                
                if (response != null) {                	
                	client.getMessageRXBus().put(message);
//                	System.out.println("[RT]: read message and added it to msgrxqueue --- " + response + " ---");
                } else {
                	System.out.println("[RT]: read message responsed a nullstring, do nothing, buffersize = " + socket.getReceiveBufferSize() + ", reader ready? "
                			+ reader.ready());
//                    reader = new BufferedReader(new InputStreamReader(input));               
//                    response = reader.readLine();  
                	this.client.getSocket().close();
					this.interrupt();

                }
              
            } 
            catch (Exception sexc) {
            	System.out.println("[ReadThread, CRITICAL: ] Socket geschlossen: " + sexc.getMessage());
            	try {
					this.client.getSocket().close();
					this.interrupt();
					break;
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            	
//            	try {
//					sleep(3000);
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
            	
//            	try {
//            		System.out.println("RDTH: try new socket");
//            		this.client.getSocket().close();
//            		this.client.getSocket().close();
//            		this.client.setSocket(new Socket(this.client.getHostname(), this.client.getPort()));
//            		socket.connect(new InetSocketAddress(this.client.getHostname(), this.client.getPort()));
//            		System.out.println("[Readthread, Warning:] new socket connected? -> " + socket.isConnected());
            		
//            		input = socket.getInputStream();            
//                    reader = new BufferedReader(new InputStreamReader(input));
//            		
//					this.sleep(5000);
//				} catch (IOException | InterruptedException e2) {
//					// TODO Auto-generated catch block
//					System.out.println("fucktah");
//					e2.printStackTrace();
//				}
//            	try {
//					sleep(2000);
//				} catch (InterruptedException e1) {
//					// TODO Auto-generated catch block
//					e1.printStackTrace();
//				}
//            	try {
//            		this.client.getSocket().close();
//					this.client.setSocket(new Socket(this.client.getHostname(), this.client.getPort()));
//				} catch (UnknownHostException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
            	
            }

        }
    }
    
    public boolean terminateConnection() throws IOException {
    	this.reader.close();
    	this.input.close();
    	this.socket.close();
    	
    	return true;
    }

	public boolean isReadingFinished() {
		return readingFinished;
	}

	public void setReadingFinished(boolean readingReady) {
		this.readingFinished = readingReady;
	}
    
    
}