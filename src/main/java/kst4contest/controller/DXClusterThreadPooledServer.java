package kst4contest.controller;

import kst4contest.model.ChatMember;
import kst4contest.model.ChatPreferences;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DXClusterThreadPooledServer implements Runnable{

    private List<Socket> clientSockets = Collections.synchronizedList(new ArrayList<>()); //list of all connected clients

    ChatController chatController = null;
    protected int          serverPort   = 8080;
    protected ServerSocket serverSocket = null;
    protected boolean      isStopped    = false;
    protected Thread       runningThread= null;
    protected ExecutorService threadPool =
            Executors.newFixedThreadPool(10);
    Socket clientSocket;

    public DXClusterThreadPooledServer(int port, ChatController chatController){
        this.serverPort = port;
        this.chatController = chatController;
    }

    public void run(){

        synchronized(this){
            this.runningThread = Thread.currentThread();
            runningThread.setName("DXCluster-thread-pooled-server");
        }
        openServerSocket();
        while(! isStopped()){
            clientSocket = null;
            try {
                clientSocket = this.serverSocket.accept();

                synchronized(clientSockets) {
                    clientSockets.add(clientSocket);  // add dx cluster client to the "clients list" for broadcasting
                }

            } catch (IOException e) {
                if(isStopped()) {
                    System.out.println("Server Stopped.") ;
                    break;
                }
                throw new RuntimeException(
                        "Error accepting client connection", e);
            }

            DXClusterServerWorkerRunnable worker = new DXClusterServerWorkerRunnable(clientSocket, "Thread Pooled DXCluster Server ", chatController, clientSockets);

            this.threadPool.execute(worker);

        }
        this.threadPool.shutdown();
        System.out.println("Server Stopped.") ;
    }

    private synchronized boolean isStopped() {
        return this.isStopped;
    }

    public synchronized void stop(){
        this.isStopped = true;
        try {
            this.serverSocket.close();
            synchronized(clientSockets) {
                for (Socket socket : clientSockets) {
                    socket.close();  // close all client connections
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("DXCCSERVER Error closing server", e);
        }
    }

    private void openServerSocket() {
        try {
            this.serverSocket = new ServerSocket(this.serverPort);
        } catch (IOException e) {
            throw new RuntimeException("DXCCSERVER Cannot open port ", e);
        }
    }

    /**
     * Sends a DX cluster message to ALL connected log programs via telnet, returns true if sent
     *
     * @param aChatMember
     * @return boolean true if message had been sent
     */
    public boolean broadcastSingleDXClusterEntryToLoggers(ChatMember aChatMember) {
        synchronized(clientSockets) {

            System.out.println("DXClusterSrvr: broadcasting message to clients: " + clientSockets.size());

            try {

                System.out.println("-------------> ORIGINALEE VAL: " + aChatMember.getFrequency().getValue());
                System.out.println("-------------> NORMALIZED VAL: " + Utils4KST.normalizeFrequencyString(aChatMember.getFrequency().getValue(), chatController.getChatPreferences().getNotify_optionalFrequencyPrefix()) + " ");
            } catch (Exception e) {
                System.out.println("DXCThPooledServer: Error accessing value in chatmember object: " + e.getMessage());
//                e.printStackTrace();
            }

            for (Socket socket : clientSockets) {

                try {
                OutputStream output = socket.getOutputStream();

                    String singleDXClusterMessage = "DX de ";

//                    singleDXClusterMessage += chatController.getChatPreferences().getLoginCallSign() + ":   ";




                    singleDXClusterMessage += this.chatController.getChatPreferences().getNotify_DXCSrv_SpottersCallSign().getValue() + ":   ";
                    singleDXClusterMessage += Utils4KST.normalizeFrequencyString(aChatMember.getFrequency().getValue(), chatController.getChatPreferences().getNotify_optionalFrequencyPrefix()) + "  ";
                    singleDXClusterMessage += aChatMember.getCallSign().toUpperCase() + "             "; //we need such an amount of spaces for n1mm to work, otherwise bullshit happens
                    singleDXClusterMessage += aChatMember.getQra().toUpperCase() + "  ";
                    singleDXClusterMessage += new Utils4KST().time_generateCurrenthhmmZTimeStringForClusterMessage() + ((char)7) + ((char)7) + "\r\n";

//                    singleDXClusterMessage += chatController.getChatPreferences().getLoginCallSign() + ":   ";
//                    singleDXClusterMessage += Utils4KST.normalizeFrequencyString(aChatMember.getFrequency().getValue(), chatController.getChatPreferences().getNotify_optionalFrequencyPrefix()) + "  ";
//                    singleDXClusterMessage += aChatMember.getCallSign().toUpperCase() + " ";
//                    singleDXClusterMessage += aChatMember.getQra().toUpperCase() + "  ";
//                    singleDXClusterMessage += new Utils4KST().time_generateCurrenthhmmZTimeStringForClusterMessage() + ((char)7) + ((char)7) + "\r\n";

                    output.write((singleDXClusterMessage).getBytes());

                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("[DXClusterSrvr, Error:] broadcasting DXC-message to clients went wrong!");
                    return false;
                }
            }
        }
        return true; //if message had been sent, return true for "ok"
    }

}

class DXClusterServerWorkerRunnable implements Runnable{

    protected Socket clientSocket = null;
    protected String serverText = null;
    private ChatController client = null;
    private List<Socket> dxClusterClientSocketsConnectedList;

    public DXClusterServerWorkerRunnable(Socket clientSocket, String serverText, ChatController chatController, List<Socket> clientSockets) {
        this.clientSocket = clientSocket;
        this.serverText = serverText;
        this.client = chatController;
        this.dxClusterClientSocketsConnectedList = clientSockets;
    }

    public void run() {
        try {
            OutputStream output = clientSocket.getOutputStream();
            dxClusterClientSocketsConnectedList.add(clientSocket);

            Timer dXCkeepAliveTimer = new Timer();
            dXCkeepAliveTimer.schedule(new TimerTask() {

                @Override
                public void run() {

                    for (Socket socket : dxClusterClientSocketsConnectedList) {

                        try {
                            OutputStream output = socket.getOutputStream();
                            output.write(("\r\n").getBytes());

                        } catch (IOException e) {
                            e.printStackTrace();
                            System.out.println("[DXClusterSrvr, Error:] broadcasting DXC-message to clients went wrong!");
                            dXCkeepAliveTimer.purge();

                            try {
                                socket.close();
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                            finally {
                                this.cancel();
                            }
                            dxClusterClientSocketsConnectedList.remove(socket); //if socket is closed by client, remove it from the broadcast list and close it
                        }
                    }

                }
            }, 30000, 30000);


            output.write(("login: ").getBytes()); //say hello to the client, it will answer with a callsign
            System.out.println("[DXClusterThreadPooledServer, Info:] New cluster client connected! "); //TODO: maybe integrate non blocking reader for client identification

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            synchronized(dxClusterClientSocketsConnectedList) {
                dxClusterClientSocketsConnectedList.remove(clientSocket); // Entferne den Client nach Verarbeitung
            }
        }
    }



}
