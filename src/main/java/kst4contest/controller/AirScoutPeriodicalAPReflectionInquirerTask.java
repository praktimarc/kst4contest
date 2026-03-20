package kst4contest.controller;


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.TimerTask;

import javafx.collections.ObservableList;
import kst4contest.locatorUtils.Location;
import kst4contest.model.ChatMember;


public class AirScoutPeriodicalAPReflectionInquirerTask extends TimerTask {

	private ChatController client;

	public AirScoutPeriodicalAPReflectionInquirerTask(ChatController client) {

		this.client = client;

	}

	@Override
	public void run() {
		
		Thread.currentThread().setName("AirscoutPeriodicalReflectionInquirierTask");

		String KSTClientsNameForQuery = this.client.getChatPreferences().getAirScout_asClientNameString();
		String ASServerNameStringForAnswer = this.client.getChatPreferences().getAirScout_asServerNameString();

		//TODO: Manage prefixes kst and as via preferences file and instance
		//TODO: Check if locator is changeable via the preferences object, need to be correct if it changes
		DatagramSocket dsocket;
		
//		String prefix_asSetpath ="ASSETPATH: \"KST\" \"AS\" "; //working original
//		String prefix_asWatchList  = "ASWATCHLIST: \"KST\" \"AS\" "; //working original

		String prefix_asSetpath ="ASSETPATH: \"" + this.client.getChatPreferences().getAirScout_asClientNameString() + "\" \"" + this.client.getChatPreferences().getAirScout_asServerNameString() + "\" ";
		String prefix_asWatchList  = "ASWATCHLIST: \""+ this.client.getChatPreferences().getAirScout_asClientNameString()+ "\" \"" + this.client.getChatPreferences().getAirScout_asServerNameString() + "\" ";

		String bandString = "1440000"; //TODO: this must variable in case of higher bands! ... default: 1440000
//		String myCallAndMyLocString = this.client.getChatPreferences().getStn_loginCallSign() + "," + this.client.getChatPreferences().getStn_loginLocatorMainCat(); //before fix 1.266


        String ownCallSign = this.client.getChatPreferences().getStn_loginCallSign();
        try {
            if (this.client.getChatPreferences().getStn_loginCallSign().contains("-")) {
                ownCallSign = this.client.getChatPreferences().getStn_loginCallSign().split("-")[0];
            } else {
                ownCallSign = this.client.getChatPreferences().getStn_loginCallSign();
            }
        } catch (Exception e) {
            System.out.println("[ASPERIODICAL, Error]: " + e.getMessage());
        }
        String myCallAndMyLocString = ownCallSign + "," + this.client.getChatPreferences().getStn_loginLocatorMainCat(); //bugfix, Airscout do not process 9A1W-2 but 9A1W like formatted calls


		String suffix = ""; //"FOREIGNCALL,FOREIGNLOC " -- dont forget the space at the end!!!
		String asWatchListString = prefix_asWatchList + bandString + "," + myCallAndMyLocString;
		String asWatchListStringSuffix = asWatchListString;

		String host = "255.255.255.255";
//		int port = 9872;

		int port = client.getChatPreferences().getAirScout_asCommunicationPort();
//		byte[] message = "ASSETPATH: \"KST\" \"AS\" 1440000,DO5AMF,JN49GL,OK1MZM,JN89IW ".getBytes(); Original, ging
		InetAddress address;
		

		/**
		 * Iterate over chatmemberlist and asking airscout for plane reflection information		
		 * To avoid a concurrentmodifyexception, we have to convert the original list to an array at first
		 * since the iterator brakes if the list changing during the iteration time
		 */
		ObservableList<ChatMember> praktiKSTActiveUserList = this.client.getLst_chatMemberList();
		
		
		ChatMember[] ary_threadSafeChatMemberArray = new ChatMember[praktiKSTActiveUserList.size()]; 
		praktiKSTActiveUserList.toArray(ary_threadSafeChatMemberArray);
		
		for (ChatMember i : ary_threadSafeChatMemberArray) {

			if (i.getQrb() < this.client.getChatPreferences().getStn_maxQRBDefault())
			//Here: check if maximum distance to the chatmember is reached, only ask AS if distance is lower!
                //this counts for AS request and Aswatchlist
			{
					suffix = i.getCallSign() + "," + i.getQra() + " ";

				String queryStringToAirScout = "";

				queryStringToAirScout += prefix_asSetpath + bandString + "," + myCallAndMyLocString + "," + suffix;

				byte[] queryStringToAirScoutMSG = queryStringToAirScout.getBytes();

				try {
					address = InetAddress.getByName("255.255.255.255");
					DatagramPacket packet = new DatagramPacket(queryStringToAirScoutMSG, queryStringToAirScoutMSG.length, address, port);
					dsocket = new DatagramSocket();
					dsocket.setBroadcast(true);
					dsocket.send(packet);
					dsocket.close();
				} catch (UnknownHostException e1) {
					e1.printStackTrace();
				} catch (NoRouteToHostException e) {
					e.printStackTrace();
				} catch (SocketException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				//			System.out.println("[ASUDPTask, info:] sent query " + queryStringToAirScout);

				asWatchListStringSuffix += "," + i.getCallSign() + "," + i.getQra();
			}
		}

		/**
		 * As next we will set the ASWatchlist. All stations in chat will be watched by airscout causing following code.\n\n
		 * ASWATCHLIST: "KST" "AS" 4320000,DO5AMF,JN49GL,DF9QX,JO42HD,DG2KBC,JN58MI,DJ0PY,JO32MF,DL1YDI,JO42FA,DL6BF,JO32QI,F1NZC,JN15MR,F4TXU,JN23CX,F5GHP,IN96LE,F6HTJ,JN12KQ,G0GGG,IO81VE,G0JCC,IO82MA,G0JDL,JO02SI,G0MBL,JO01QH,G4AEP,IO91MB,G4CLA,IO92JL,G4DCV,IO91OF,G4LOH,IO70JC,G4MKF,IO91HJ,G4TRA,IO81WN,G8GXP,IO93FQ,G8VHI,IO92FM,GW0RHC,IO71UN,HA4ND,JN97MJ,I5/HB9SJV/P,JN52JS,IW2DAL,JN45NN,OK1FPR,JO80CE,OK6M,JN99CR,OV3T,JO46CM,OZ2M,JO65FR,PA0V,JO33II,PA2RU,JO32LT,PA3DOL,JO22MT,PA9R,JO22JK,PE1EVX,JO22MP,S51AT,JN75GW,SM7KOJ,JO66ND,SP9TTG,JO90KW�
		 * The watchlist-String is bult by the for loop which builds the AP queries
		 */
		asWatchListStringSuffix += " ";
		
		byte[] queryStringToAirScoutMSG = asWatchListStringSuffix.getBytes();

		try {
			address = InetAddress.getByName("255.255.255.255");
			DatagramPacket packet = new DatagramPacket(queryStringToAirScoutMSG, queryStringToAirScoutMSG.length, address, port);
			dsocket = new DatagramSocket();
			dsocket.setBroadcast(true);
			dsocket.send(packet);
			dsocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
//		System.out.println("[ASUDPTask, info:] set watchlist: " + asWatchListStringSuffix);

		
	}

}
