package kst4contest.test;

import org.junit.jupiter.api.Test;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPAirScoutSendMessages {

	@Test
	public static void main(String args[]) {
	    try {
//	      String host = "www.java2s.com";
//	      byte[] message = "ASWATCHLIST: \"KST\" \"AS\" 1440000,DO5AMF,JN49GL,OK1MZM,JN89IW ".getBytes();
	      
	      

	      // Get the internet address of the specified host
//	      InetAddress address = InetAddress.getByName(host);^
//	      byte[] byteAddress = "127.0.0.1".getBytes();
//	      System.out.println(byteAddress.length);
	    	String host = "255.255.255.255";
	    	int port = 9872;
	    	
	    	byte[] message = "ASSETPATH: \"KST\" \"AS\" 1440000,DO5AMF,JN49GL,OK1MZM,JN89IW ".getBytes();
	      InetAddress address = InetAddress.getByName("255.255.255.255");
	      
	      // Initialize a datagram packet with data and address
	      DatagramPacket packet = new DatagramPacket(message, message.length,
	          address, port);
	    

	      // Create a datagram socket, send the packet through it, close it.
	      DatagramSocket dsocket = new DatagramSocket();
	      dsocket.setBroadcast(true);
	      dsocket.send(packet);
	      dsocket.close();
	      
	    } catch (Exception e) {
	      System.err.println(e);
	    }
	  }
	}