//package kst4contest.controller;


//
//import kst4contest.model.ChatMember;
//import kst4contest.model.ChatMessage;
//
//import java.io.*;
//import java.net.ServerSocket;
//import java.net.Socket;
//import java.nio.channels.ServerSocketChannel;
//import java.nio.channels.SocketChannel;
//import java.time.Instant;
//
///**
// * This thread is responsible for providing DXCluster messages for a connected log program.
// *
// *
// */
////public class DXClusterController extends Thread {
//	PrintWriter outTelnet;
//	BufferedReader inTelnet;
//	private Socket socket;
//	private ChatController client;
////	private OutputStream output;
////	private InputStream input;
//
//	private ChatMessage messageTextRaw;
//
//		private static final int PORT = 23;
//		private static final String USERNAME = "user";
//		private static final String PASSWORD = "pass";
//		private Socket clientSocket;
//
//
//
//
////	public DXClusterController(Socket clientSocket, ChatController client) throws InterruptedException {
////
////		this.client = client;
////
////        try {
////            outTelnet = new PrintWriter(clientSocket.getOutputStream(), true);
////			inTelnet = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
////        } catch (IOException e) {
////            throw new RuntimeException(e);
////        }
////        System.out.println("defcons");
////		this.clientSocket = clientSocket;
////
////	}
//
//	public DXClusterController(Socket clientSocket, ChatController chatController) {
//
//		try {
//			socket = clientSocket;
//		} catch (Exception e) {
//			throw new RuntimeException(e);
//		}
//		this.client = chatController;
//
//		try {
//			outTelnet = new PrintWriter(socket.getOutputStream(), true);
//			inTelnet = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}
//		System.out.println("[DXCCtrl, info:] DXCluster Controller created!");
//		this.clientSocket = socket;
//	}
//
//	public DXClusterController(Socket clientSocket, ObjectOutputStream objectout, ChatController chatController) {
//
//		try {
//            socket = clientSocket;
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//        this.client = chatController;
//
//		try {
//			outTelnet = new PrintWriter(socket.getOutputStream(), true);
//			inTelnet = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}
//		System.out.println("[DXCCtrl, info:] DXCluster Controller created!");
//		this.clientSocket = socket;
//	}
//
////	public DXClusterController(ServerSocket clientSocket, ChatController client) throws InterruptedException {
////		//TODO: GOT FROM https://stackoverflow.com/questions/15541804/creating-the-serversocket-in-a-separate-thread
////        try {
////            socket = clientSocket.accept(2);
////        } catch (IOException e) {
////            throw new RuntimeException(e);
////        }
////        this.client = client;
////
////		try {
////			outTelnet = new PrintWriter(socket.getOutputStream(), true);
////			inTelnet = new BufferedReader(new InputStreamReader(socket.getInputStream()));
////		} catch (IOException e) {
////			throw new RuntimeException(e);
////		}
////		System.out.println("defcons");
////		this.clientSocket = socket;
////
////	}
//
////	public DXClusterController(ServerSocketChannel serverSocketChannel, ChatController client) throws InterruptedException {
////
////		this.client = client;
////
//////		clientSocketChannel.ac
////
////		try {
////
////			serverSocketChannel.accept();
////			serverSocketChannel.rea
////
////			outTelnet = new PrintWriter(clientSocket.getOutputStream(), true);
////			inTelnet = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
////		} catch (IOException e) {
////			throw new RuntimeException(e);
////		}
////		System.out.println("defcons");
////		this.clientSocket = clientSocket;
////
////	}
//
//	public boolean terminateConnection() throws IOException {
//
////		this.output.close();
//		this.socket.close();
//
//		return true;
//	}
//
//	public void sendLocalClusterMessage() {
//
//	}
//
//	public void run() {
////		try (
////				PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
////			 BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream())))
////		{
//
////			out.println("Welcome to the Telnet Server");
//			outTelnet.print("login: ");
//			outTelnet.flush();
//        try {
//            String user = inTelnet.readLine();
//        } catch (IOException ex) {
//            throw new RuntimeException(ex);
//        }
////		finally {
////			try {
////				clientSocket.close();
////			} catch (Exception e) {
////				System.out.println("Error closing client socket: " + e.getMessage());
////			}
////		}
//
//
////        for (int i = 0; i < 10; i++) {
////
////				outTelnet.println("DX de DM5M:     144222.0  DO5AMF       JN49FL                           2250Z\n");
////			}
//
//
//
//	}
//
//	/**
//	 * Sends a DX cluster message to the connected log programs via telnet, returns true if sent
//	 *
//	 * @param aChatMember
//	 * @return
//	 */
//	public boolean propagateSingleDXClusterEntry(ChatMember aChatMember) {
//
//		String singleDXClusterMessage = "DX de ";
//
//		singleDXClusterMessage += client.getChatPreferences().getLoginCallSign() + " ";
//		singleDXClusterMessage += aChatMember.getFrequency().getValue() + " ";
//		singleDXClusterMessage += aChatMember.getCallSign().toUpperCase() + " ";
//		singleDXClusterMessage += aChatMember.getQra().toUpperCase() + " ";
//		singleDXClusterMessage += new Utils4KST().time_generateCurrenthhmmZTimeStringForClusterMessage() + "\n";
//
//		outTelnet.println(singleDXClusterMessage);
//		outTelnet.flush();
//		return true;
//	}
//}
