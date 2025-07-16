package kst4contest.controller;

import javafx.beans.property.SimpleStringProperty;
import kst4contest.model.ChatMember;
import kst4contest.model.ChatPreferences;

public class DXClusterThreadPooledServerTest {

    public static void main(String[] args) {

        ChatController client = new ChatController();
        ChatPreferences testPreferences = new ChatPreferences();
        testPreferences.setStn_loginCallSign("DM5M");

        client.setChatPreferences(testPreferences);
        DXClusterThreadPooledServer dxClusterServer = new DXClusterThreadPooledServer(8000, client);

        new Thread(dxClusterServer).start();


        try {
            Thread.sleep(10 * 1000);
            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>ready.....go!");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ChatMember test = new ChatMember();
        test.setCallSign("DL5ASG");
        test.setQra("JO51HK");
        test.setFrequency(new SimpleStringProperty("144776.0"));

        dxClusterServer.broadcastSingleDXClusterEntryToLoggers(test);


//        try {
//            Thread.sleep(20 * 3333);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        System.out.println("Stopping Server");
//        server.stop();
    }
}
