/**
 * 
 */
package kst4contest.controller;

import java.io.IOException;

/**
 * @author mywire
 *
 */
public class StartChat {

	/**
	 * @param args
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws InterruptedException, IOException {

		System.out.println("[Startchat:] Starting new Chat instance");
		
		 ChatController client = new ChatController();
	        client.execute();
	        
	}

}
