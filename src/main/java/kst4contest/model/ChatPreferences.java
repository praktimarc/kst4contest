package kst4contest.model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import javafx.beans.property.*;
import kst4contest.ApplicationConstants;
import kst4contest.utils.ApplicationFileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * refactored version if ChatPreferences
 */
public class ChatPreferences {


	// Main window: right split pane (ChatMember table / Priority list / FurtherInfo)
	// 3 items => 2 dividers => 2 positions.
	private double[] GUImainWindowRightSplitPane_dividerposition = new double[] { 0.53, 0.78 };

	// Defaults used for config upgrades / first start.
	private final double[] GUImainWindowRightSplitPane_dividerpositionDefault = new double[] { 0.53, 0.78 };


	/**
	 * Bump this when you change the XML schema written by {@link #writePreferencesToXmlFile()}.
	 * <p>
	 * Reading must stay backwards compatible: missing/unknown tags should fall back to defaults.
	 */
//	private static final int CONFIG_VERSION = 2;
	public static final int CONFIG_VERSION = 3;

	// Prefer writing tag names that mirror variable names (human readable). Keep legacy tags for compatibility.
	private static final String TAG_CONFIG_VERSION = "configVersion";

	/**
	 * Name of file to store preferences in.
	 */
	public static final String PREFERENCES_FILE = "preferences.xml";

	/**
	 * Resource with the example properties xml file.
	 */
	public static final String PREFERENCE_RESOURCE = "/praktiKSTpreferences.xml";



	/**
	 * Default constructor will set the default values (also for predefined texts
	 * and shorts) automatically at initialization
	 *
	 * TODO: delete this from the kst4contest.view/Main.java!
	 */
	public ChatPreferences() {
		ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, PREFERENCE_RESOURCE, PREFERENCES_FILE);

//        lstNotify_QSOSniffer_sniffedCallSignList.add("DF0GEB");

//		shortcuts[2] = "pse";
//		shortcuts[3] = "turn";
//		shortcuts[4] = "ant";
//		shortcuts[5] = "my";
//		shortcuts[6] = "dir";
//		shortcuts[7] = "sked";
//		shortcuts[8] = "ssb";
//		shortcuts[9] = "cw";
//		shortcuts[10] = "try";
//		shortcuts[11] = "agn";
//		shortcuts[12] = "nw";
//		shortcuts[13] = "qrg";
//		shortcuts[14] = "beaming";
//		shortcuts[15] = "calling";
//		shortcuts[16] = "lsn to";
//		shortcuts[17] = "qsb";
//		shortcuts[18] = "rpt";
//		shortcuts[19] = "nr";
//		shortcuts[20] = "ur";
//		shortcuts[21] = "I";
//		shortcuts[22] = "hear";
//		shortcuts[23] = "you";
//		shortcuts[24] = "weak";
//		shortcuts[25] = "nil, sry";
//		shortcuts[26] = "maybe";
//		shortcuts[27] = "later";
//		shortcuts[28] = "tmw";
//		shortcuts[29] = "rrr";
//		shortcuts[30] = "tnx";
//		shortcuts[31] = "qso";
//		shortcuts[32] = "73";
//		shortcuts[33] = "?";
//		shortcuts[34] = "!";
//		shortcuts[35] = ",";
//		shortcuts[36] = "MYQRG";
//
//		textSnippets[0] = "Hi OM, try sked?";
//		textSnippets[1] = "I am calling cq ur dir, pse lsn to me at ";
//		textSnippets[2] = "pse ur qrg?";
//		textSnippets[3] = "rrr, I move to your qrg nw, pse ant dir me";
//		textSnippets[4] = "Hrd you but many qrm here, pse agn";
//
//		textSnippets[5] = "I turn my ant to you";
//
//		textSnippets[6] = "Sry, strong qrm by local station there, may try ";
//		textSnippets[7] = "Sry, in qso nw, pse qrx, I will meep you";
//
//		textSnippets[8] = "Ur ant my dir nw?";
//		textSnippets[9] = "nil?";
//		textSnippets[10] = "No cw op here, could we use ssb?";
//		textSnippets[11] = "No chance in ssb, could we use cw?";
//
//		textSnippets[12] = "Nil till now, are you calling?";
//		textSnippets[13] = "Nil, I will look for an ap";
//		textSnippets[14] = "Tnx try, maybe later!";
//		textSnippets[15] = "Tnx fb qso, all ok, 73 es gl!";
	}

	/**
	 * Preferences for the preferences
	 * kst4contest@googlegroups.com
	 * praktimarc+kst4contest@gmail.com
	 *
	 */

	String programVersion = "Chat is powered by ON4KST \n\nUsage is free. You are welcome to support: \n\n- my project (donations, bugreports, good ideas are welcome), \n- ON4KST Servers, \n- AirScout developers and \n- OV3T (best AS-data provider of the world). \n\n73 de DO5AMF, Marc (DM5M / DARC X08)";
	String logsynch_storeWorkedCallSignsFileNameUDPMessageBackup = "udpReaderBackup.txt";
	String storeAndRestorePreferencesFileName = ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, PREFERENCES_FILE);
	String chatState; // working variable only for use by primarystage (title bar)
//	ObservableStringValue chatState;

	/**
	 * Station preferences
	 */



	String stn_on4kstServersDns = "www.on4kst.org";
	int stn_on4kstServersPort = 23001;

	boolean stn_pstRotatorEnabled = false;

	boolean stn_loginAFKState = false; //always start as here
	String stn_loginCallSign = "do5amf";
	String stn_loginCallSignRaw = "do5amf"; //for example: do5amf instead of logincallsign do5amf-2
	String stn_loginPassword = "";
	String stn_loginNameMainCat = "KST4Contest";
	String stn_loginNameSecondCat = "KST4ContestSHF";
	String stn_loginLocatorMainCat = "jn49fk";
	String stn_loginLocatorSecondCat = "jn49fk";

	double stn_antennaBeamWidthDeg = 50;
	double stn_maxQRBDefault = 900;
	double stn_qtfDefault = 135;

	ChatCategory loginChatCategoryMain = new ChatCategory(2);
	ChatCategory loginChatCategorySecond = new ChatCategory(3);
	boolean loginToSecondChatEnabled;
	DoubleProperty actualQTF = new SimpleDoubleProperty(360); // will be updated by user at runtime!

	boolean stn_bandActive144;
	boolean stn_bandActive432;
	boolean stn_bandActive1240;
	boolean stn_bandActive2300;
	boolean stn_bandActive3400;
	boolean stn_bandActive5600;
	boolean stn_bandActive10G;

	/**
	 * Log Synch preferences
	 */
	String logsynch_fileBasedWkdCallInterpreterFileNameReadOnly = "SimpleLogFile.txt";
	boolean logsynch_fileBasedWkdCallInterpreterEnabled = true;

	int logsynch_ucxUDPWkdCallListenerPort = 12060;
	boolean logsynch_ucxUDPWkdCallListenerEnabled = true;

	String logsynch_wintestNetworkStationNameOfKST = "KST4Contest";
	String logsynch_wintestNetworkStationNameOfWintestClient1 = "STN1";
	boolean logsynch_wintestNetworkSimulationEnabled = false;
	int logsynch_wintestNetworkStationIDOfKST = 55555;
	int logsynch_wintestNetworkPort = 9871;
	boolean logsynch_wintestNetworkListenerEnabled = true; // default true = bisheriges Verhalten
	String logsynch_wintestNetworkBroadcastAddress = "255.255.255.255"; // UDP broadcast address for sending to Win-Test
	boolean logsynch_wintestNetworkSkedPushEnabled = false; // push SKEDs to Win-Test via UDP



	/**
	 * TRX Synch prefs
	 */
	StringProperty MYQRGFirstCat = new SimpleStringProperty(); // own qrg will be set by user entry or ucxlog if trx Synch is
	// activated
	StringProperty MYQRGSecondCat = new SimpleStringProperty(); // own qrg will be set by user entry or ucxlog if trx Synch is activated

	boolean trxSynch_ucxLogUDPListenerEnabled = false;

	/**
	 * AirScout prefs
	 */
	boolean AirScout_asUDPListenerEnabled;
	String AirScout_asServerNameString, AirScout_asClientNameString, AirScout_asBandString;
	int AirScout_asCommunicationPort;

	/**
	 * Notification prefs
	 */



	//Audio section
	boolean notify_playSimpleSounds = true;
	boolean notify_playCWCallsignsOnRxedPMs = true;
	boolean notify_playVoiceCallsignsOnRxedPMs = true;

	//DXCluster section
	boolean notify_dxClusterServerEnabled = true;
	int notify_dxclusterServerPort = 8000; //default 8000 like db0sue.de
	SimpleStringProperty notify_optionalFrequencyPrefix = new SimpleStringProperty("144");

	SimpleStringProperty notify_DXCSrv_SpottersCallSign = new SimpleStringProperty("DO5AMF");

	boolean notify_DXClusterServerTriggerBearing;
	boolean notify_DXClusterServerTriggerOnQRGDetect;

	//    ObservableList<String> lstNotify_QSOSniffer_sniffedCallSignList = FXCollections.observableArrayList();
	ObservableList<String> lstNotify_QSOSniffer_sniffedWordsList = FXCollections.observableArrayList();
	ObservableList<String> lstNotify_QSOSniffer_sniffedPrefixLocList = FXCollections.observableArrayList();

	// Scoring / interaction metrics
	int notify_noReplyPenaltyMinutes = 13;          // if we ping via /cq and no response arrives within X minutes => penalty strike
	int notify_momentumWindowSeconds = 180;         // momentum window size (count inbound lines)
	String notify_positiveSignalsPatterns = "QRV;READY;RX;RGR;RR;OK;YES;TNX;TU;HEARD;LSN"; //TODO: to be continued

	boolean notify_bandUpgradeHintOnLogEnabled = true;          // show hint after log entry if station QRV on other unworked enabled band
	boolean notify_bandUpgradePriorityBoostEnabled = false;     // optional score boost to make it stand out in toplists


	/**
	 * Shortcuts and Textsnippets prefs
	 */
//	String[] shortcuts = new String[37];
//	String[] textSnippets = new String[16];
	ObservableList<String> lst_txtSnipList = FXCollections.observableArrayList();
	ObservableList<String> lst_txtShortCutBtnList = FXCollections.observableArrayList();

	/**
	 * Beacon prefs
	 */

	boolean bcn_beaconsEnabledMainCat = true;
	boolean bcn_beaconsEnabledSecondCat = false;
	int bcn_beaconIntervalInMinutesMainCat = 20;
	int bcn_beaconIntervalInMinutesSecondCat = 20;
	String bcn_beaconTextMainCat = "Hi, pse call us";
	String bcn_beaconTextSecondCat = "Hi, pse call us";
	long bcn_contestScoreSum;
	int bcn_contestQsoSum;
	String bcn_contestODXCallsignKms;

	/**
	 * Unworked station requester prefs
	 */

	boolean messageHandling_unworkedStnRequesterBeaconsEnabled;
	int messageHandling_unworkedStnRequesterBeaconsInterval;
	String messageHandling_unworkedStnRequesterBeaconsText;
	String messageHandling_beaconUnworkedstationsPrefix;

	String messageHandling_autoAnswerTextMainCat = "Hi, sry I am not qrv, just testing new features of KST4Contest " +  ApplicationConstants.APPLICATION_CURRENTVERSIONNUMBER;
	String messageHandling_autoAnswerTextSecondCat = "Hi, sry I am not qrv, just testing new features of KST4Contest " +  ApplicationConstants.APPLICATION_CURRENTVERSIONNUMBER;

	boolean messageHandling_autoAnswerEnabled = false;
	boolean messageHandling_autoAnswerEnabledSecondCat = false;

	boolean messageHandling_autoAnswerToQRGRequestEnabled;

	/*********************************************************************************
	 *
	 * GUI OPTIONS WHICH CAN BE CHANGED WILL BE SAVED HERE
	 *
	 *********************************************************************************/

	boolean guiOptions_defaultFilterNothing;
	boolean guiOptions_defaultFilterPmToMe;
	boolean guiOptions_defaultFilterPmToOther;
	boolean guiOptions_defaultFilterPublicMsgs;


	/*********************************************************************************
	 *
	 * GUI SETTING VARIABLES WILL BE BUFFERED HERE, DEFAULTS TOO
	 *
	 *********************************************************************************/

	private boolean GUI_darkModeActive = false;
	private boolean GUI_darkModeActiveByDefault = false;

	private double[] GUIscn_ChatwindowMainSceneSizeHW = new double[] {768, 1234};
	private double[] GUIclusterAndQSOMonStage_SceneSizeHW = new double[] {700, 500};
	private double[] GUIstage_updateStage_SceneSizeHW = new double[] {580, 480};
	private double[] GUIsettingsStageSceneSizeHW = new double[] {720, 768};

	private double[] GUIselectedCallSignSplitPane_dividerposition = {0.55};
	private double[] GUImainWindowLeftSplitPane_dividerposition = {0.51};
	private double[] GUImessageSectionSplitpane_dividerposition = {0.62, 0.7, 0.75, 0.9}; //3 deviders now //TODO: more should be possible?
//	private double[] GUImainWindowRightSplitPane_dividerposition = {0.72};
	private double[] GUIpnl_directedMSGWin_dividerpositionDefault = {0.8};


	public boolean isMessageHandling_autoAnswerEnabledSecondCat() {
		return messageHandling_autoAnswerEnabledSecondCat;
	}

	public void setMessageHandling_autoAnswerEnabledSecondCat(boolean messageHandling_autoAnswerEnabledSecondCat) {
		this.messageHandling_autoAnswerEnabledSecondCat = messageHandling_autoAnswerEnabledSecondCat;
	}

	public StringProperty getMYQRGSecondCat() {
		return MYQRGSecondCat;
	}

	public void setMYQRGSecondCat(String MYQRGSecondCat) {
		this.MYQRGSecondCat.set(MYQRGSecondCat);
	}

	public boolean isMessageHandling_autoAnswerEnabled() {
		return messageHandling_autoAnswerEnabled;
	}

	public String getMessageHandling_autoAnswerTextSecondCat() {
		return messageHandling_autoAnswerTextSecondCat;
	}

	public void setMessageHandling_autoAnswerTextSecondCat(String messageHandling_autoAnswerTextSecondCat) {
		this.messageHandling_autoAnswerTextSecondCat = messageHandling_autoAnswerTextSecondCat;
	}

	public String getMessageHandling_beaconUnworkedstationsPrefix() {
		return messageHandling_beaconUnworkedstationsPrefix;
	}

	public StringProperty MYQRGFirstCatProperty() {
		return MYQRGFirstCat;
	}

	public void setMYQRGFirstCat(String MYQRGFirstCat) {
		this.MYQRGFirstCat.set(MYQRGFirstCat);
	}

	public String getStn_loginNameSecondCat() {
		return stn_loginNameSecondCat;
	}

	public void setStn_loginNameSecondCat(String stn_loginNameSecondCat) {
		this.stn_loginNameSecondCat = stn_loginNameSecondCat;
	}

	public int getStn_on4kstServersPort() {
		return stn_on4kstServersPort;
	}

	public void setStn_on4kstServersPort(int stn_on4kstServersPort) {
		this.stn_on4kstServersPort = stn_on4kstServersPort;
	}

	public String getStn_on4kstServersDns() {
		return stn_on4kstServersDns;
	}

	public void setStn_on4kstServersDns(String stn_on4kstServersDns) {
		this.stn_on4kstServersDns = stn_on4kstServersDns;
	}

	public ObservableList<String> getLstNotify_QSOSniffer_sniffedWordsList() {
		return lstNotify_QSOSniffer_sniffedWordsList;
	}

	public void setLstNotify_QSOSniffer_sniffedWordsList(ObservableList<String> lstNotify_QSOSniffer_sniffedWordsList) {
		this.lstNotify_QSOSniffer_sniffedWordsList = lstNotify_QSOSniffer_sniffedWordsList;
	}

	public ObservableList<String> getLstNotify_QSOSniffer_sniffedPrefixLocList() {
		return lstNotify_QSOSniffer_sniffedPrefixLocList;
	}

	public void setLstNotify_QSOSniffer_sniffedPrefixLocList(ObservableList<String> lstNotify_QSOSniffer_sniffedPrefixLocList) {
		this.lstNotify_QSOSniffer_sniffedPrefixLocList = lstNotify_QSOSniffer_sniffedPrefixLocList;
	}

	public String getLogsynch_wintestNetworkStationNameOfKST() {
		return logsynch_wintestNetworkStationNameOfKST;
	}

	public void setLogsynch_wintestNetworkStationNameOfKST(String logsynch_wintestNetworkStationNameOfKST) {
		this.logsynch_wintestNetworkStationNameOfKST = logsynch_wintestNetworkStationNameOfKST;
	}

	public String getLogsynch_wintestNetworkStationNameOfWintestClient1() {
		return logsynch_wintestNetworkStationNameOfWintestClient1;
	}

	public void setLogsynch_wintestNetworkStationNameOfWintestClient1(String logsynch_wintestNetworkStationNameOfWintestClient1) {
		this.logsynch_wintestNetworkStationNameOfWintestClient1 = logsynch_wintestNetworkStationNameOfWintestClient1;
	}

	public boolean isLogsynch_wintestNetworkSimulationEnabled() {
		return logsynch_wintestNetworkSimulationEnabled;
	}

	public void setLogsynch_wintestNetworkSimulationEnabled(boolean logsynch_wintestNetworkSimulationEnabled) {
		this.logsynch_wintestNetworkSimulationEnabled = logsynch_wintestNetworkSimulationEnabled;
	}

	public int getLogsynch_wintestNetworkStationIDOfKST() {
		return logsynch_wintestNetworkStationIDOfKST;
	}

	public void setLogsynch_wintestNetworkStationIDOfKST(int logsynch_wintestNetworkStationIDOfKST) {
		this.logsynch_wintestNetworkStationIDOfKST = logsynch_wintestNetworkStationIDOfKST;
	}

	public int getLogsynch_wintestNetworkPort() {
		return logsynch_wintestNetworkPort;
	}

	public void setLogsynch_wintestNetworkPort(int logsynch_wintestNetworkPort) {
		this.logsynch_wintestNetworkPort = logsynch_wintestNetworkPort;
	}

	public boolean isLogsynch_wintestNetworkListenerEnabled() {
		return logsynch_wintestNetworkListenerEnabled;
	}

	public void setLogsynch_wintestNetworkListenerEnabled(boolean logsynch_wintestNetworkListenerEnabled) {
		this.logsynch_wintestNetworkListenerEnabled = logsynch_wintestNetworkListenerEnabled;
	}

	public String getLogsynch_wintestNetworkBroadcastAddress() {
		return logsynch_wintestNetworkBroadcastAddress;
	}

	public void setLogsynch_wintestNetworkBroadcastAddress(String logsynch_wintestNetworkBroadcastAddress) {
		this.logsynch_wintestNetworkBroadcastAddress = logsynch_wintestNetworkBroadcastAddress;
	}

	public boolean isLogsynch_wintestNetworkSkedPushEnabled() {
		return logsynch_wintestNetworkSkedPushEnabled;
	}

	public void setLogsynch_wintestNetworkSkedPushEnabled(boolean logsynch_wintestNetworkSkedPushEnabled) {
		this.logsynch_wintestNetworkSkedPushEnabled = logsynch_wintestNetworkSkedPushEnabled;
	}

	public String getStn_loginLocatorSecondCat() {
		return stn_loginLocatorSecondCat;
	}

	public void setStn_loginLocatorSecondCat(String stn_loginLocatorSecondCat) {
		this.stn_loginLocatorSecondCat = stn_loginLocatorSecondCat;
	}

	public boolean isBcn_beaconsEnabledSecondCat() {
		return bcn_beaconsEnabledSecondCat;
	}

	public void setBcn_beaconsEnabledSecondCat(boolean bcn_beaconsEnabledSecondCat) {
		this.bcn_beaconsEnabledSecondCat = bcn_beaconsEnabledSecondCat;
	}

	public int getBcn_beaconIntervalInMinutesSecondCat() {
		return bcn_beaconIntervalInMinutesSecondCat;
	}

	public void setBcn_beaconIntervalInMinutesSecondCat(int bcn_beaconIntervalInMinutesSecondCat) {
		this.bcn_beaconIntervalInMinutesSecondCat = bcn_beaconIntervalInMinutesSecondCat;
	}

	public String getBcn_beaconTextSecondCat() {
		return bcn_beaconTextSecondCat;
	}

	public void setBcn_beaconTextSecondCat(String bcn_beaconTextSecondCat) {
		this.bcn_beaconTextSecondCat = bcn_beaconTextSecondCat;
	}

	public ChatCategory getLoginChatCategorySecond() {
		return loginChatCategorySecond;
	}

	public void setLoginChatCategorySecond(ChatCategory loginChatCategorySecond) {
		this.loginChatCategorySecond = loginChatCategorySecond;
	}

	public boolean isLoginToSecondChatEnabled() {
		return loginToSecondChatEnabled;
	}

	public void setLoginToSecondChatEnabled(boolean loginToSecondChatEnabled) {
		this.loginToSecondChatEnabled = loginToSecondChatEnabled;
	}

	public boolean isGuiOptions_defaultFilterNothing() {
		return guiOptions_defaultFilterNothing;
	}

	public void setGuiOptions_defaultFilterNothing(boolean guiOptions_defaultFilterNothing) {
		this.guiOptions_defaultFilterNothing = guiOptions_defaultFilterNothing;
	}

	public boolean isGuiOptions_defaultFilterPmToMe() {
		return guiOptions_defaultFilterPmToMe;
	}

	public void setGuiOptions_defaultFilterPmToMe(boolean guiOptions_defaultFilterPmToMe) {
		this.guiOptions_defaultFilterPmToMe = guiOptions_defaultFilterPmToMe;
	}

	public boolean isGuiOptions_defaultFilterPmToOther() {
		return guiOptions_defaultFilterPmToOther;
	}

	public void setGuiOptions_defaultFilterPmToOther(boolean guiOptions_defaultFilterPmToOther) {
		this.guiOptions_defaultFilterPmToOther = guiOptions_defaultFilterPmToOther;
	}

	public boolean isGuiOptions_defaultFilterPublicMsgs() {
		return guiOptions_defaultFilterPublicMsgs;
	}

	public void setGuiOptions_defaultFilterPublicMsgs(boolean guiOptions_defaultFilterPublicMsgs) {
		this.guiOptions_defaultFilterPublicMsgs = guiOptions_defaultFilterPublicMsgs;
	}

	public boolean isMessageHandling_autoAnswerToQRGRequestEnabled() {
		return messageHandling_autoAnswerToQRGRequestEnabled;
	}

	public void setMessageHandling_autoAnswerToQRGRequestEnabled(boolean messageHandling_autoAnswerToQRGRequestEnabled) {
		this.messageHandling_autoAnswerToQRGRequestEnabled = messageHandling_autoAnswerToQRGRequestEnabled;
	}

	public String getMessageHandling_autoAnswerTextMainCat() {
		return messageHandling_autoAnswerTextMainCat;
	}

	public void setMessageHandling_autoAnswerTextMainCat(String messageHandling_autoAnswerTextMainCat) {
		this.messageHandling_autoAnswerTextMainCat = messageHandling_autoAnswerTextMainCat;
	}

	public boolean isMsgHandling_autoAnswerEnabled() {
		return messageHandling_autoAnswerEnabled;
	}

	public void setMessageHandling_autoAnswerEnabled(boolean messageHandling_autoAnswerEnabled) {
		this.messageHandling_autoAnswerEnabled = messageHandling_autoAnswerEnabled;
	}

	public long getBcn_contestScoreSum() {
		return bcn_contestScoreSum;
	}

	public void setBcn_contestScoreSum(long bcn_contestScoreSum) {
		this.bcn_contestScoreSum = bcn_contestScoreSum;
	}

	public void setNotify_DXCSrv_SpottersCallSign(String notify_DXCSrv_SpottersCallSign) {
		this.notify_DXCSrv_SpottersCallSign.set(notify_DXCSrv_SpottersCallSign);
	}

	public boolean isNotify_DXClusterServerTriggerBearing() {
		return notify_DXClusterServerTriggerBearing;
	}

	public void setNotify_DXClusterServerTriggerBearing(boolean notify_DXClusterServerTriggerBearing) {
		this.notify_DXClusterServerTriggerBearing = notify_DXClusterServerTriggerBearing;
	}

	public boolean isNotify_DXClusterServerTriggerOnQRGDetect() {
		return notify_DXClusterServerTriggerOnQRGDetect;
	}

	public void setNotify_DXClusterServerTriggerOnQRGDetect(boolean notify_DXClusterServerTriggerOnQRGDetect) {
		this.notify_DXClusterServerTriggerOnQRGDetect = notify_DXClusterServerTriggerOnQRGDetect;
	}

	public boolean isNotify_dxClusterServerEnabled() {
		return notify_dxClusterServerEnabled;
	}

	public void setNotify_dxClusterServerEnabled(boolean notify_dxClusterServerEnabled) {
		this.notify_dxClusterServerEnabled = notify_dxClusterServerEnabled;
	}


	public int getNotify_noReplyPenaltyMinutes() {
		return notify_noReplyPenaltyMinutes;
	}

	public void setNotify_noReplyPenaltyMinutes(int notify_noReplyPenaltyMinutes) {
		this.notify_noReplyPenaltyMinutes = notify_noReplyPenaltyMinutes;
	}

	public int getNotify_momentumWindowSeconds() {
		return notify_momentumWindowSeconds;
	}

	public void setNotify_momentumWindowSeconds(int notify_momentumWindowSeconds) {
		this.notify_momentumWindowSeconds = notify_momentumWindowSeconds;
	}

	public String getNotify_positiveSignalsPatterns() {
		return notify_positiveSignalsPatterns;
	}

	public void setNotify_positiveSignalsPatterns(String notify_positiveSignalsPatterns) {
		this.notify_positiveSignalsPatterns = notify_positiveSignalsPatterns;
	}


	public boolean isNotify_bandUpgradePriorityBoostEnabled() {
		return notify_bandUpgradePriorityBoostEnabled;
	}

	public void setNotify_bandUpgradePriorityBoostEnabled(boolean notify_bandUpgradePriorityBoostEnabled) {
		this.notify_bandUpgradePriorityBoostEnabled = notify_bandUpgradePriorityBoostEnabled;
	}

	public boolean isNotify_bandUpgradeHintOnLogEnabled() {
		return notify_bandUpgradeHintOnLogEnabled;
	}

	public void setNotify_bandUpgradeHintOnLogEnabled(boolean notify_bandUpgradeHintOnLogEnabled) {
		this.notify_bandUpgradeHintOnLogEnabled = notify_bandUpgradeHintOnLogEnabled;
	}

	public boolean isStn_pstRotatorEnabled() {
		return stn_pstRotatorEnabled;
	}

	public void setStn_pstRotatorEnabled(boolean stn_pstRotatorEnabled) {
		this.stn_pstRotatorEnabled = stn_pstRotatorEnabled;
	}

	public SimpleStringProperty getNotify_optionalFrequencyPrefix() {
		return notify_optionalFrequencyPrefix;
	}

	public void setNotify_optionalFrequencyPrefix(SimpleStringProperty notify_optionalFrequencyPrefix) {
		this.notify_optionalFrequencyPrefix = notify_optionalFrequencyPrefix;
	}

	public int getNotify_dxclusterServerPort() {
		return notify_dxclusterServerPort;
	}

	public void setNotify_dxclusterServerPort(int notify_dxclusterServerPort) {
		this.notify_dxclusterServerPort = notify_dxclusterServerPort;
	}

	public double[] getGUIscn_ChatwindowMainSceneSizeHW() {
		return GUIscn_ChatwindowMainSceneSizeHW;
	}

	public void setGUIscn_ChatwindowMainSceneSizeHW(double[] GUIscn_ChatwindowMainSceneSizeHW) {
		this.GUIscn_ChatwindowMainSceneSizeHW = GUIscn_ChatwindowMainSceneSizeHW;
	}

	public double[] getGUIclusterAndQSOMonStage_SceneSizeHW() {
		return GUIclusterAndQSOMonStage_SceneSizeHW;
	}

	public void setGUIclusterAndQSOMonStage_SceneSizeHW(double[] GUIclusterAndQSOMonStage_SceneSizeHW) {
		this.GUIclusterAndQSOMonStage_SceneSizeHW = GUIclusterAndQSOMonStage_SceneSizeHW;
	}

	public double[] getGUIstage_updateStage_SceneSizeHW() {
		return GUIstage_updateStage_SceneSizeHW;
	}

	public void setGUIstage_updateStage_SceneSizeHW(double[] GUIstage_updateStage_SceneSizeHW) {
		this.GUIstage_updateStage_SceneSizeHW = GUIstage_updateStage_SceneSizeHW;
	}

	public double[] getGUIsettingsStageSceneSizeHW() {
		return GUIsettingsStageSceneSizeHW;
	}

	public void setGUIsettingsStageSceneSizeHW(double[] GUIsettingsStageSceneSizeHW) {
		this.GUIsettingsStageSceneSizeHW = GUIsettingsStageSceneSizeHW;
	}

	public double[] getGUIselectedCallSignSplitPane_dividerposition() {
		return GUIselectedCallSignSplitPane_dividerposition;
	}

	public void setGUIselectedCallSignSplitPane_dividerposition(double[] GUIselectedCallSignSplitPane_dividerposition) {
		this.GUIselectedCallSignSplitPane_dividerposition = GUIselectedCallSignSplitPane_dividerposition;
	}

	public double[] getGUImainWindowLeftSplitPane_dividerposition() {
		return GUImainWindowLeftSplitPane_dividerposition;
	}

	public void setGUImainWindowLeftSplitPane_dividerposition(double[] GUImainWindowLeftSplitPane_dividerposition) {
		this.GUImainWindowLeftSplitPane_dividerposition = GUImainWindowLeftSplitPane_dividerposition;
	}

	public double[] getGUImessageSectionSplitpane_dividerposition() {
		return GUImessageSectionSplitpane_dividerposition;
	}

	public double[] getGUImainWindowRightSplitPane_dividerposition() {
		return GUImainWindowRightSplitPane_dividerposition;
	}

	public void setGUImessageSectionSplitpane_dividerposition(double[] GUImessageSectionSplitpane_dividerposition) {
		this.GUImessageSectionSplitpane_dividerposition = GUImessageSectionSplitpane_dividerposition;
	}



	public void setGUImainWindowRightSplitPane_dividerposition(double[] positions) {
		this.GUImainWindowRightSplitPane_dividerposition = positions;
	}

//	public void setGUImainWindowRightSplitPane_dividerposition(double[] GUImainWindowRightSplitPane_dividerposition) {
//		this.GUImainWindowRightSplitPane_dividerposition = GUImainWindowRightSplitPane_dividerposition;
//	}

	public double[] getGUIpnl_directedMSGWin_dividerpositionDefault() {
		return GUIpnl_directedMSGWin_dividerpositionDefault;
	}

	public void setGUIpnl_directedMSGWin_dividerpositionDefault(double[] GUIpnl_directedMSGWin_dividerpositionDefault) {
		this.GUIpnl_directedMSGWin_dividerpositionDefault = GUIpnl_directedMSGWin_dividerpositionDefault;
	}

	public double getStn_antennaBeamWidthDeg() {
		return stn_antennaBeamWidthDeg;
	}

	public void setStn_antennaBeamWidthDeg(double stn_antennaBeamWidthDeg) {
		this.stn_antennaBeamWidthDeg = stn_antennaBeamWidthDeg;
	}

	public double getStn_maxQRBDefault() {
		return stn_maxQRBDefault;
	}

	public void setStn_maxQRBDefault(double stn_maxQRBDefault) {
		this.stn_maxQRBDefault = stn_maxQRBDefault;
	}

	public double getStn_qtfDefault() {
		return stn_qtfDefault;
	}

	public void setStn_qtfDefault(double stn_qtfDefault) {
		this.stn_qtfDefault = stn_qtfDefault;
	}

	public boolean isStn_loginAFKState() {
		return stn_loginAFKState;
	}

	public void setStn_loginAFKState(boolean stn_loginAFKState) {
		this.stn_loginAFKState = stn_loginAFKState;
	}

	public String getStn_loginCallSign() {
		return stn_loginCallSign;
	}

	public String getStn_loginCallSignRaw() {
		return stn_loginCallSignRaw;
	}

	public String getAirScout_asBandString() {
		return AirScout_asBandString;
	}

	public void setAirScout_asBandString(String airScout_asBandString) {
		AirScout_asBandString = airScout_asBandString;
	}

	public String getAirScout_asServerNameString() {
		return AirScout_asServerNameString;
	}

	public void setAirScout_asServerNameString(String airScout_asServerNameString) {
		AirScout_asServerNameString = airScout_asServerNameString;
	}

	public String getAirScout_asClientNameString() {
		return AirScout_asClientNameString;
	}

	public void setAirScout_asClientNameString(String airScout_asClientNameString) {
		AirScout_asClientNameString = airScout_asClientNameString;
	}

	public int getAirScout_asCommunicationPort() {
		return AirScout_asCommunicationPort;
	}

	public void setAirScout_asCommunicationPort(int airScout_asCommunicationPort) {
		AirScout_asCommunicationPort = airScout_asCommunicationPort;
	}

	public boolean isAirScout_asUDPListenerEnabled() {
		return AirScout_asUDPListenerEnabled;
	}

	public void setAirScout_asUDPListenerEnabled(boolean airScout_asUDPListenerEnabled) {
		AirScout_asUDPListenerEnabled = airScout_asUDPListenerEnabled;
	}

	public String getChatState() {
		return chatState;
	}

	public void setChatState(String chatState) {
		this.chatState = chatState;
	}

	public String getProgramVersion() {
		return programVersion;
	}

	public void setProgramVersion(String programVersion) {
		this.programVersion = programVersion;
	}

	public String getLogsynch_storeWorkedCallSignsFileNameUDPMessageBackup() {
		return logsynch_storeWorkedCallSignsFileNameUDPMessageBackup;
	}

	public void setLogsynch_storeWorkedCallSignsFileNameUDPMessageBackup(
			String logsynch_storeWorkedCallSignsFileNameUDPMessageBackup) {
		this.logsynch_storeWorkedCallSignsFileNameUDPMessageBackup = logsynch_storeWorkedCallSignsFileNameUDPMessageBackup;
	}

	public boolean isNotify_playSimpleSounds() {
		return notify_playSimpleSounds;
	}

	public boolean isNotify_playCWCallsignsOnRxedPMs() {
		return notify_playCWCallsignsOnRxedPMs;
	}

	public boolean isNotify_playVoiceCallsignsOnRxedPMs() {
		return notify_playVoiceCallsignsOnRxedPMs;
	}

	public void setNotify_playSimpleSounds(boolean notify_playSimpleSounds) {
		this.notify_playSimpleSounds = notify_playSimpleSounds;
	}

	public void setNotify_playCWCallsignsOnRxedPMs(boolean notify_playCWCallsignsOnRxedPMs) {
		this.notify_playCWCallsignsOnRxedPMs = notify_playCWCallsignsOnRxedPMs;
	}

	public void setNotify_playVoiceCallsignsOnRxedPMs(boolean notify_playVoiceCallsignsOnRxedPMs) {
		this.notify_playVoiceCallsignsOnRxedPMs = notify_playVoiceCallsignsOnRxedPMs;
	}

	/**
	 * actualQTF, int, QTF in degrees
	 *
	 */

	public StringProperty getMYQRGFirstCat() {
		return MYQRGFirstCat;
	}

	public DoubleProperty getActualQTF() {
		return actualQTF;
	}

	public void setActualQTF(DoubleProperty actualQTF) {
		this.actualQTF = actualQTF;
	}

	public void setMYQRGFirstCat(StringProperty mYQRG) {
		MYQRGFirstCat = mYQRG;
	}

	public void setStn_loginCallSign(String stn_loginCallSign) {

		this.stn_loginCallSign = stn_loginCallSign;
		this.stn_loginCallSignRaw = stn_loginCallSign;

		if (stn_loginCallSign.contains("-")) {

			this.stn_loginCallSignRaw = stn_loginCallSign.split("-")[0];

		} else if (stn_loginCallSign.contains("/")) {

			this.stn_loginCallSignRaw = stn_loginCallSign.split("/")[0];

		}

		if ((stn_loginCallSign.split("-").length > 2 ) || stn_loginCallSign.split("/").length > 2) {
			System.out.println("ChatPreferences, WARNING! Logincallsign is not plausible"); //strange login like do5-amf-2
		}

	}

	public String getStn_loginPassword() {
		return stn_loginPassword;
	}

	public void setStn_loginPassword(String stn_loginPassword) {
		this.stn_loginPassword = stn_loginPassword;
	}

	public String getStn_loginNameMainCat() {
		return stn_loginNameMainCat;
	}

	public void setStn_loginNameMainCat(String stn_loginNameMainCat) {
		this.stn_loginNameMainCat = stn_loginNameMainCat;
	}

	public String getStn_loginLocatorMainCat() {
		return stn_loginLocatorMainCat;
	}

	public void setStn_loginLocatorMainCat(String stn_loginLocatorMainCat) {
		this.stn_loginLocatorMainCat = stn_loginLocatorMainCat;
	}

	public ChatCategory getLoginChatCategoryMain() {
		return loginChatCategoryMain;
	}

	public void setLoginChatCategoryMain(ChatCategory loginChatCategoryMain) {
		this.loginChatCategoryMain = loginChatCategoryMain;
	}

	public String getLogsynch_fileBasedWkdCallInterpreterFileNameReadOnly() {
		return logsynch_fileBasedWkdCallInterpreterFileNameReadOnly;
	}

	public void setLogsynch_fileBasedWkdCallInterpreterFileNameReadOnly(
			String logsynch_fileBasedWkdCallInterpreterFileNameReadOnly) {
		this.logsynch_fileBasedWkdCallInterpreterFileNameReadOnly = logsynch_fileBasedWkdCallInterpreterFileNameReadOnly;
	}

	public boolean isLogsynch_fileBasedWkdCallInterpreterEnabled() {
		return logsynch_fileBasedWkdCallInterpreterEnabled;
	}

	public void setLogsynch_fileBasedWkdCallInterpreterEnabled(boolean logsynch_fileBasedWkdCallInterpreterEnabled) {
		this.logsynch_fileBasedWkdCallInterpreterEnabled = logsynch_fileBasedWkdCallInterpreterEnabled;
	}

	public int getLogsynch_ucxUDPWkdCallListenerPort() {
		return logsynch_ucxUDPWkdCallListenerPort;
	}

	public void setLogsynch_ucxUDPWkdCallListenerPort(int logsynch_ucxUDPWkdCallListenerPort) {
		this.logsynch_ucxUDPWkdCallListenerPort = logsynch_ucxUDPWkdCallListenerPort;
	}

	public boolean isLogsynch_ucxUDPWkdCallListenerEnabled() {
		return logsynch_ucxUDPWkdCallListenerEnabled;
	}

	public void setLogsynch_ucxUDPWkdCallListenerEnabled(boolean logsynch_ucxUDPWkdCallListenerEnabled) {
		this.logsynch_ucxUDPWkdCallListenerEnabled = logsynch_ucxUDPWkdCallListenerEnabled;
	}

	public boolean isTrxSynch_ucxLogUDPListenerEnabled() {
		return trxSynch_ucxLogUDPListenerEnabled;
	}

	public void setTrxSynch_ucxLogUDPListenerEnabled(boolean trxSynch_ucxLogUDPListenerEnabled) {
		this.trxSynch_ucxLogUDPListenerEnabled = trxSynch_ucxLogUDPListenerEnabled;
	}

	public SimpleStringProperty notify_optionalFrequencyPrefixProperty() {
		return notify_optionalFrequencyPrefix;
	}

	public void setNotify_optionalFrequencyPrefix(String notify_optionalFrequencyPrefix) {
		this.notify_optionalFrequencyPrefix.set(notify_optionalFrequencyPrefix);
	}

	public SimpleStringProperty getNotify_DXCSrv_SpottersCallSign() {
		return notify_DXCSrv_SpottersCallSign;
	}

	public SimpleStringProperty notify_DXCSrv_SpottersCallSignProperty() {
		return notify_DXCSrv_SpottersCallSign;
	}

	public void setNotify_DXCSrv_SpottersCallSign(SimpleStringProperty notify_DXCSrv_SpottersCallSign) {
		this.notify_DXCSrv_SpottersCallSign.setValue(notify_DXCSrv_SpottersCallSign.getValue());
	}

	//	public String[] getShortcuts() {
//		return shortcuts;
//	}
//
//	public void setShortcuts(String[] shortcuts) {
//		this.shortcuts = shortcuts;
//	}

//	public String[] getTextSnippets() {
//		return textSnippets;
//	}
//
//	public void setTextSnippets(String[] textSnippets) {
//		this.textSnippets = textSnippets;
//	}

	public boolean isBcn_beaconsEnabledMainCat() {
		return bcn_beaconsEnabledMainCat;
	}

	public ObservableList<String> getLst_txtShortCutBtnList() {
		return lst_txtShortCutBtnList;
	}

	public void setLst_txtShortCutBtnList(ObservableList<String> lst_txtShortCutBtnList) {
		this.lst_txtShortCutBtnList = lst_txtShortCutBtnList;
	}

	public ObservableList<String> getLst_txtSnipList() {
		return lst_txtSnipList;
	}

	public void setLst_txtSnipList(ObservableList<String> lst_txtSnipList) {
		this.lst_txtSnipList = lst_txtSnipList;
	}

	public void setBcn_beaconsEnabledMainCat(boolean bcn_beaconsEnabledMainCat) {
		this.bcn_beaconsEnabledMainCat = bcn_beaconsEnabledMainCat;
	}

	public int getBcn_beaconIntervalInMinutesMainCat() {
		return bcn_beaconIntervalInMinutesMainCat;
	}

	public void setBcn_beaconIntervalInMinutesMainCat(int bcn_beaconIntervalInMinutesMainCat) {
		this.bcn_beaconIntervalInMinutesMainCat = bcn_beaconIntervalInMinutesMainCat;
	}

	public String getBcn_beaconTextMainCat() {
		return bcn_beaconTextMainCat;
	}

	public void setBcn_beaconTextMainCat(String bcn_beaconTextMainCat) {

		this.bcn_beaconTextMainCat = bcn_beaconTextMainCat;
	}



	public String messageHandling_beaconUnworkedstationsPrefix() {
		return messageHandling_beaconUnworkedstationsPrefix;
	}

	public void setMessageHandling_beaconUnworkedstationsPrefix(String messageHandling_beaconUnworkedstationsPrefix) {
		this.messageHandling_beaconUnworkedstationsPrefix = messageHandling_beaconUnworkedstationsPrefix;
	}

	public boolean isMessageHandling_unworkedStnRequesterBeaconsEnabled() {
		return messageHandling_unworkedStnRequesterBeaconsEnabled;
	}

	public void setMessageHandling_unworkedStnRequesterBeaconsEnabled(boolean messageHandling_unworkedStnRequesterBeaconsEnabled) {
		this.messageHandling_unworkedStnRequesterBeaconsEnabled = messageHandling_unworkedStnRequesterBeaconsEnabled;
	}

	public int getMessageHandling_unworkedStnRequesterBeaconsInterval() {
		return messageHandling_unworkedStnRequesterBeaconsInterval;
	}

	public void setMessageHandling_unworkedStnRequesterBeaconsInterval(int messageHandling_unworkedStnRequesterBeaconsInterval) {
		this.messageHandling_unworkedStnRequesterBeaconsInterval = messageHandling_unworkedStnRequesterBeaconsInterval;
	}

	public String getMessageHandling_unworkedStnRequesterBeaconsText() {
		return messageHandling_unworkedStnRequesterBeaconsText;
	}

	public void setMessageHandling_unworkedStnRequesterBeaconsText(String messageHandling_unworkedStnRequesterBeaconsText) {
		this.messageHandling_unworkedStnRequesterBeaconsText = messageHandling_unworkedStnRequesterBeaconsText;
	}

	public String getLogSynch_storeWorkedCallSignsFileNameUDPMessageBackup() {
		return logsynch_storeWorkedCallSignsFileNameUDPMessageBackup;
	}

	public void setLogSynch_storeWorkedCallSignsFileNameUDPMessageBackup(
			String logSynch_storeWorkedCallSignsFileNameUDPMessageBackup) {
		this.logsynch_storeWorkedCallSignsFileNameUDPMessageBackup = logSynch_storeWorkedCallSignsFileNameUDPMessageBackup;
	}

	public String getStoreAndRestorePreferencesFileName() {
		return storeAndRestorePreferencesFileName;
	}

	public void setStoreAndRestorePreferencesFileName(String storeAndRestorePreferencesFileName) {
		this.storeAndRestorePreferencesFileName = storeAndRestorePreferencesFileName;
	}



	/**
	 *
	 * @return true if the file writing was successful, else false
	 */
	public boolean writePreferencesToXmlFile() {

		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		try {



			// root elements
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElement("praktiKST");
			doc.appendChild(rootElement);

			// Schema version
			Element configVersion = doc.createElement("configVersion");
			configVersion.setTextContent(String.valueOf(CONFIG_VERSION));
			rootElement.appendChild(configVersion);


			Element station = doc.createElement("station");
			rootElement.appendChild(station);

			Element LoginCallSign = doc.createElement("LoginCallSign");
			LoginCallSign.setTextContent(this.getStn_loginCallSign());
			station.appendChild(LoginCallSign);

			// New preferred tag names (closer to variable names). Keep legacy tags above for old versions.
			Element stn_loginCallSign = doc.createElement("stn_loginCallSign");
			stn_loginCallSign.setTextContent(this.getStn_loginCallSign());
			station.appendChild(stn_loginCallSign);


			Element LoginPassword = doc.createElement("LoginPassword");
			LoginPassword.setTextContent(this.getStn_loginPassword());
			station.appendChild(LoginPassword);

			Element stn_loginPassword = doc.createElement("stn_loginPassword");
			stn_loginPassword.setTextContent(this.getStn_loginPassword());
			station.appendChild(stn_loginPassword);

			Element LoginDisplayedName = doc.createElement("LoginDisplayedName");
			LoginDisplayedName.setTextContent(this.getStn_loginNameMainCat());
			station.appendChild(LoginDisplayedName);

			Element stn_loginNameMainCat = doc.createElement("stn_loginNameMainCat");
			stn_loginNameMainCat.setTextContent(this.getStn_loginNameMainCat());
			station.appendChild(stn_loginNameMainCat);

			Element stn_loginNameSecondCat = doc.createElement("stn_loginNameSecondCat");
			stn_loginNameSecondCat.setTextContent(this.getStn_loginNameSecondCat());
			station.appendChild(stn_loginNameSecondCat);

			Element LoginLocator = doc.createElement("LoginLocator");
			LoginLocator.setTextContent(this.getStn_loginLocatorMainCat());
			station.appendChild(LoginLocator);

			Element stn_loginLocatorMainCat = doc.createElement("stn_loginLocatorMainCat");
			stn_loginLocatorMainCat.setTextContent(this.getStn_loginLocatorMainCat());
			station.appendChild(stn_loginLocatorMainCat);

			Element ChatCategory = doc.createElement("ChatCategory");
			ChatCategory.setTextContent(this.getLoginChatCategoryMain().getCategoryNumber()+"");
			station.appendChild(ChatCategory);

			Element ChatCategorySecond = doc.createElement("ChatCategorySecond");
			ChatCategorySecond.setTextContent(this.getLoginChatCategorySecond().getCategoryNumber()+"");
			station.appendChild(ChatCategorySecond);

			Element stn_secondCatEnabled = doc.createElement("stn_secondCatEnabled");
			stn_secondCatEnabled.setTextContent(this.loginToSecondChatEnabled+"");
			station.appendChild(stn_secondCatEnabled);

			Element stn_antennaBeamWidthDeg = doc.createElement("stn_antennaBeamWidthDeg");
			stn_antennaBeamWidthDeg.setTextContent(this.stn_antennaBeamWidthDeg+"");
			station.appendChild(stn_antennaBeamWidthDeg);

			Element stn_maxQRBDefault = doc.createElement("stn_maxQRBDefault");
			stn_maxQRBDefault.setTextContent(this.stn_maxQRBDefault+"");
			station.appendChild(stn_maxQRBDefault);

			Element stn_qtfDefault = doc.createElement("stn_qtfDefault");
			stn_qtfDefault.setTextContent(this.stn_qtfDefault+"");
			station.appendChild(stn_qtfDefault);

			Element stn_bandActive144 = doc.createElement("stn_bandActive144");
			stn_bandActive144.setTextContent(this.stn_bandActive144+"");
			station.appendChild(stn_bandActive144);

			Element stn_bandActive432 = doc.createElement("stn_bandActive432");
			stn_bandActive432.setTextContent(this.stn_bandActive432+"");
			station.appendChild(stn_bandActive432);

			Element stn_bandActive1240 = doc.createElement("stn_bandActive1240");
			stn_bandActive1240.setTextContent(this.stn_bandActive1240+"");
			station.appendChild(stn_bandActive1240);

			Element stn_bandActive2300 = doc.createElement("stn_bandActive2300");
			stn_bandActive2300.setTextContent(this.stn_bandActive2300+"");
			station.appendChild(stn_bandActive2300);

			Element stn_bandActive3400 = doc.createElement("stn_bandActive3400");
			stn_bandActive3400.setTextContent(this.stn_bandActive3400+"");
			station.appendChild(stn_bandActive3400);

			Element stn_bandActive5600 = doc.createElement("stn_bandActive5600");
			stn_bandActive5600.setTextContent(this.stn_bandActive5600+"");
			station.appendChild(stn_bandActive5600);

			Element stn_bandActive10G = doc.createElement("stn_bandActive10G");
			stn_bandActive10G.setTextContent(this.stn_bandActive10G+"");
			station.appendChild(stn_bandActive10G);

			Element stn_on4kstServersDns = doc.createElement("stn_on4kstServersDns");
			stn_on4kstServersDns.setTextContent(this.stn_on4kstServersDns);
			station.appendChild(stn_on4kstServersDns);

			Element stn_on4kstServersPort = doc.createElement("stn_on4kstServersPort");
			stn_on4kstServersPort.setTextContent(this.stn_on4kstServersPort + "");
			station.appendChild(stn_on4kstServersPort);

			Element stn_loginAFKState = doc.createElement("stn_loginAFKState");
			stn_loginAFKState.setTextContent(this.stn_loginAFKState + "");
			station.appendChild(stn_loginAFKState);

			Element stn_pstRotatorEnabled = doc.createElement("stn_pstRotatorEnabled");
			stn_pstRotatorEnabled.setTextContent(this.stn_pstRotatorEnabled + "");
			station.appendChild(stn_pstRotatorEnabled);



			/**
			 * LOGSYNCH
			 */

			Element logsynch = doc.createElement("logsynch");
			rootElement.appendChild(logsynch);

			Element logsynch_fileBasedWkdCallInterpreterFileNameReadOnly = doc.createElement("logsynch_fileBasedWkdCallInterpreterFileNameReadOnly");
			logsynch_fileBasedWkdCallInterpreterFileNameReadOnly.setTextContent(this.getLogsynch_fileBasedWkdCallInterpreterFileNameReadOnly());
			logsynch.appendChild(logsynch_fileBasedWkdCallInterpreterFileNameReadOnly);

			Element logsynch_storeWorkedCallSignsFileNameUDPMessageBackup = doc.createElement("logsynch_storeWorkedCallSignsFileNameUDPMessageBackup");
			logsynch_storeWorkedCallSignsFileNameUDPMessageBackup.setTextContent(this.getLogsynch_storeWorkedCallSignsFileNameUDPMessageBackup());
			logsynch.appendChild(logsynch_storeWorkedCallSignsFileNameUDPMessageBackup);

			Element logsynch_fileBasedWkdCallInterpreterEnabled = doc.createElement("logsynch_fileBasedWkdCallInterpreterEnabled");
			logsynch_fileBasedWkdCallInterpreterEnabled.setTextContent(this.isLogsynch_fileBasedWkdCallInterpreterEnabled()+"");
			logsynch.appendChild(logsynch_fileBasedWkdCallInterpreterEnabled);

			Element logsynch_ucxUDPWkdCallListenerPort = doc.createElement("logsynch_ucxUDPWkdCallListenerPort");
			logsynch_ucxUDPWkdCallListenerPort.setTextContent(this.getLogsynch_ucxUDPWkdCallListenerPort()+"");
			logsynch.appendChild(logsynch_ucxUDPWkdCallListenerPort);

			Element logsynch_ucxUDPWkdCallListenerEnabled = doc.createElement("logsynch_ucxUDPWkdCallListenerEnabled");
			// BUGFIX: this is the log-synch listener flag, not the TRX-synch flag (copy&paste error in older versions)
			logsynch_ucxUDPWkdCallListenerEnabled.setTextContent(this.isLogsynch_ucxUDPWkdCallListenerEnabled()+"");
			logsynch.appendChild(logsynch_ucxUDPWkdCallListenerEnabled);

			// WinTest Settings
			Element logsynch_wintestNetworkStationNameOfKST = doc.createElement("logsynch_wintestNetworkStationNameOfKST");
			logsynch_wintestNetworkStationNameOfKST.setTextContent(this.logsynch_wintestNetworkStationNameOfKST);
			logsynch.appendChild(logsynch_wintestNetworkStationNameOfKST);

			Element logsynch_wintestNetworkStationNameOfWintestClient1 = doc.createElement("logsynch_wintestNetworkStationNameOfWintestClient1");
			logsynch_wintestNetworkStationNameOfWintestClient1.setTextContent(this.logsynch_wintestNetworkStationNameOfWintestClient1);
			logsynch.appendChild(logsynch_wintestNetworkStationNameOfWintestClient1);

			Element logsynch_wintestNetworkSimulationEnabled = doc.createElement("logsynch_wintestNetworkSimulationEnabled");
			logsynch_wintestNetworkSimulationEnabled.setTextContent(this.logsynch_wintestNetworkSimulationEnabled + "");
			logsynch.appendChild(logsynch_wintestNetworkSimulationEnabled);

			Element logsynch_wintestNetworkStationIDOfKST = doc.createElement("logsynch_wintestNetworkStationIDOfKST");
			logsynch_wintestNetworkStationIDOfKST.setTextContent(this.logsynch_wintestNetworkStationIDOfKST + "");
			logsynch.appendChild(logsynch_wintestNetworkStationIDOfKST);

			Element logsynch_wintestNetworkPort = doc.createElement("logsynch_wintestNetworkPort");
			logsynch_wintestNetworkPort.setTextContent(this.logsynch_wintestNetworkPort + "");
			logsynch.appendChild(logsynch_wintestNetworkPort);

			Element logsynch_wintestNetworkListenerEnabled = doc.createElement("logsynch_wintestNetworkListenerEnabled");
			logsynch_wintestNetworkListenerEnabled.setTextContent(this.logsynch_wintestNetworkListenerEnabled + "");
			logsynch.appendChild(logsynch_wintestNetworkListenerEnabled);

			Element logsynch_wintestNetworkBroadcastAddress = doc.createElement("logsynch_wintestNetworkBroadcastAddress");
			logsynch_wintestNetworkBroadcastAddress.setTextContent(this.logsynch_wintestNetworkBroadcastAddress);
			logsynch.appendChild(logsynch_wintestNetworkBroadcastAddress);

			Element logsynch_wintestNetworkSkedPushEnabled = doc.createElement("logsynch_wintestNetworkSkedPushEnabled");
			logsynch_wintestNetworkSkedPushEnabled.setTextContent(this.logsynch_wintestNetworkSkedPushEnabled + "");
			logsynch.appendChild(logsynch_wintestNetworkSkedPushEnabled);


			/**
			 * trxSynchUCX
			 */

			Element trxSynchUCX = doc.createElement("trxSynchUCX");
			rootElement.appendChild(trxSynchUCX);

			Element trxSynch_ucxLogUDPListenerEnabled = doc.createElement("trxSynch_ucxLogUDPListenerEnabled");
			trxSynch_ucxLogUDPListenerEnabled.setTextContent(this.isTrxSynch_ucxLogUDPListenerEnabled()+"");
			trxSynchUCX.appendChild(trxSynch_ucxLogUDPListenerEnabled);

			Element trxSynch_defaultMYQRGValue = doc.createElement("trxSynch_defaultMYQRGValue");
			trxSynch_defaultMYQRGValue.setTextContent(this.getMYQRGFirstCat().getValue());
			trxSynchUCX.appendChild(trxSynch_defaultMYQRGValue);

			Element trxSynch_defaultMYQRG2Value = doc.createElement("trxSynch_defaultMYQRG2Value");
			// Safe null check falls Property noch nicht initialisiert ist
			trxSynch_defaultMYQRG2Value.setTextContent(this.getMYQRGSecondCat().getValue() != null ? this.getMYQRGSecondCat().getValue() : "1296.200.00");
			trxSynchUCX.appendChild(trxSynch_defaultMYQRG2Value);


			/**
			 * AirScout
			 */

			Element AirScoutQuerier = doc.createElement("AirScoutQuerier");
			rootElement.appendChild(AirScoutQuerier);


			Element asQry_airScoutCommunicationEnabled = doc.createElement("asQry_airScoutCommunicationEnabled");
			asQry_airScoutCommunicationEnabled.setTextContent(this.isAirScout_asUDPListenerEnabled()+"");
			AirScoutQuerier.appendChild(asQry_airScoutCommunicationEnabled);

			Element asQry_airScoutServerName = doc.createElement("asQry_airScoutServerName");
			asQry_airScoutServerName.setTextContent(this.getAirScout_asServerNameString());
			AirScoutQuerier.appendChild(asQry_airScoutServerName);

			Element asQry_airScoutClientName = doc.createElement("asQry_airScoutClientName");
			asQry_airScoutClientName.setTextContent(this.getAirScout_asClientNameString());
			AirScoutQuerier.appendChild(asQry_airScoutClientName);

			Element asQry_airScoutUDPPort = doc.createElement("asQry_airScoutUDPPort");
			asQry_airScoutUDPPort.setTextContent(this.getAirScout_asCommunicationPort()+"");
			AirScoutQuerier.appendChild(asQry_airScoutUDPPort);

			Element asQry_airScoutBandValue = doc.createElement("asQry_airScoutBandValue");
			asQry_airScoutBandValue.setTextContent(this.getAirScout_asBandString());
			AirScoutQuerier.appendChild(asQry_airScoutBandValue);


			/**
			 * Notifications
			 */

			Element notifications = doc.createElement("notifications");
			rootElement.appendChild(notifications);

			Element notify_SimpleAudioNotificationsEnabled = doc.createElement("notify_SimpleAudioNotificationsEnabled");
			notify_SimpleAudioNotificationsEnabled.setTextContent(this.isNotify_playSimpleSounds()+"");
			notifications.appendChild(notify_SimpleAudioNotificationsEnabled);

			Element notify_CWCallSignAudioNotificationsEnabled = doc.createElement("notify_CWCallsignAudioNotificationsEnabled");
			notify_CWCallSignAudioNotificationsEnabled.setTextContent(this.isNotify_playCWCallsignsOnRxedPMs()+"");
			notifications.appendChild(notify_CWCallSignAudioNotificationsEnabled);

			Element notify_VoiceCallSignAudioNotificationsEnabled = doc.createElement("notify_VoiceCallsignAudioNotificationsEnabled");
			notify_VoiceCallSignAudioNotificationsEnabled.setTextContent(this.isNotify_playVoiceCallsignsOnRxedPMs()+"");
			notifications.appendChild(notify_VoiceCallSignAudioNotificationsEnabled);

			Element notify_dxClusterServerEnabledToFile = doc.createElement("notify_dxClusterServerEnabled");
			notify_dxClusterServerEnabledToFile.setTextContent(this.isNotify_dxClusterServerEnabled() + "");
			notifications.appendChild(notify_dxClusterServerEnabledToFile);

			Element notify_DXClusterServerTriggerBearingToFile = doc.createElement("notify_DXClusterServerTriggerBearing");
			notify_DXClusterServerTriggerBearingToFile.setTextContent(this.isNotify_DXClusterServerTriggerBearing() + "");
			notifications.appendChild(notify_DXClusterServerTriggerBearingToFile);

			Element notify_DXClusterServerTriggerOnQRGDetectToFile = doc.createElement("notify_DXClusterServerTriggerOnQRGDetect");
			notify_DXClusterServerTriggerOnQRGDetectToFile.setTextContent(this.isNotify_DXClusterServerTriggerOnQRGDetect() + "");
			notifications.appendChild(notify_DXClusterServerTriggerOnQRGDetectToFile);

			Element notify_dxclusterServerPortToFile = doc.createElement("notify_dxclusterServerPort");
			notify_dxclusterServerPortToFile.setTextContent(this.getNotify_dxclusterServerPort()+ "");
			notifications.appendChild(notify_dxclusterServerPortToFile);

			Element notify_optionalFrequencyPrefixToFile = doc.createElement("notify_optionalFrequencyPrefix");
			notify_optionalFrequencyPrefixToFile.setTextContent(this.getNotify_optionalFrequencyPrefix().get());
			notifications.appendChild(notify_optionalFrequencyPrefixToFile);

			Element notify_DXCSrv_SpottersCallSignToFile = doc.createElement("notify_DXCSrv_SpottersCallSign");
			notify_DXCSrv_SpottersCallSignToFile.setTextContent(this.getNotify_DXCSrv_SpottersCallSign().get());
			notifications.appendChild(notify_DXCSrv_SpottersCallSignToFile);

			Element notify_noReplyPenaltyMinutes = doc.createElement("notify_noReplyPenaltyMinutes");
			notify_noReplyPenaltyMinutes.setTextContent(this.getNotify_noReplyPenaltyMinutes() + "");
			notifications.appendChild(notify_noReplyPenaltyMinutes);

			Element notify_momentumWindowSeconds = doc.createElement("notify_momentumWindowSeconds");
			notify_momentumWindowSeconds.setTextContent(this.getNotify_momentumWindowSeconds() + "");
			notifications.appendChild(notify_momentumWindowSeconds);

			Element notify_positiveSignalsPatterns = doc.createElement("notify_positiveSignalsPatterns");
			notify_positiveSignalsPatterns.setTextContent(this.getNotify_positiveSignalsPatterns());
			notifications.appendChild(notify_positiveSignalsPatterns);

			Element notify_bandUpgradeHintOnLogEnabled = doc.createElement("notify_bandUpgradeHintOnLogEnabled");
			notify_bandUpgradeHintOnLogEnabled.setTextContent(this.notify_bandUpgradeHintOnLogEnabled + "");
			notifications.appendChild(notify_bandUpgradeHintOnLogEnabled);

			Element notify_bandUpgradePriorityBoostEnabled = doc.createElement("notify_bandUpgradePriorityBoostEnabled");
			notify_bandUpgradePriorityBoostEnabled.setTextContent(this.notify_bandUpgradePriorityBoostEnabled + "");
			notifications.appendChild(notify_bandUpgradePriorityBoostEnabled);


			/**
			 * Shortcuts
			 */

			Element shortCuts = doc.createElement("shortCuts");
			rootElement.appendChild(shortCuts);

			for (Iterator iterator = lst_txtShortCutBtnList.iterator(); iterator.hasNext();) {
				String string = (String) iterator.next();
				Element temp = doc.createElement("t");
				temp.setTextContent(string);
				shortCuts.appendChild(temp);
			}

			/**
			 * QSO Sniffer Lists
			 */
			Element snifferWords = doc.createElement("snifferWords");
			rootElement.appendChild(snifferWords);

			for (String word : lstNotify_QSOSniffer_sniffedWordsList) {
				Element temp = doc.createElement("w");
				temp.setTextContent(word);
				snifferWords.appendChild(temp);
			}

			Element snifferPrefixes = doc.createElement("snifferPrefixes");
			rootElement.appendChild(snifferPrefixes);

			for (String prefix : lstNotify_QSOSniffer_sniffedPrefixLocList) {
				Element temp = doc.createElement("p");
				temp.setTextContent(prefix);
				snifferPrefixes.appendChild(temp);
			}

			/**
			 * Textsnippets (right click menu)
			 */

			Element textSnippets = doc.createElement("textSnippets");
			rootElement.appendChild(textSnippets);

			for (Iterator iterator = lst_txtSnipList.iterator(); iterator.hasNext();) {
				String string = (String) iterator.next();
				Element temp = doc.createElement("t");
				temp.setTextContent(string);
				textSnippets.appendChild(temp);
			}


			/**
			 * BeaconCQ
			 */

			Element beaconCQ = doc.createElement("beaconCQ");
			rootElement.appendChild(beaconCQ);

			Element beaconCQText = doc.createElement("beaconCQText");
			beaconCQText.setTextContent(this.getBcn_beaconTextMainCat());
			beaconCQ.appendChild(beaconCQText);

			// Preferred tag name (close to variable name)
			Element bcn_beaconTextMainCat = doc.createElement("bcn_beaconTextMainCat");
			bcn_beaconTextMainCat.setTextContent(this.getBcn_beaconTextMainCat());
			beaconCQ.appendChild(bcn_beaconTextMainCat);

			Element beaconCQIntervalMinutes = doc.createElement("beaconCQIntervalMinutes");
			beaconCQIntervalMinutes.setTextContent(this.getBcn_beaconIntervalInMinutesMainCat()+"");
			beaconCQ.appendChild(beaconCQIntervalMinutes);

			Element bcn_beaconIntervalInMinutesMainCat = doc.createElement("bcn_beaconIntervalInMinutesMainCat");
			bcn_beaconIntervalInMinutesMainCat.setTextContent(this.getBcn_beaconIntervalInMinutesMainCat()+"");
			beaconCQ.appendChild(bcn_beaconIntervalInMinutesMainCat);

			Element beaconCQEnabled = doc.createElement("beaconCQEnabled");
			beaconCQEnabled.setTextContent(this.isBcn_beaconsEnabledMainCat()+"");
			beaconCQ.appendChild(beaconCQEnabled);

			Element bcn_beaconsEnabledMainCat = doc.createElement("bcn_beaconsEnabledMainCat");
			bcn_beaconsEnabledMainCat.setTextContent(this.isBcn_beaconsEnabledMainCat()+"");
			beaconCQ.appendChild(bcn_beaconsEnabledMainCat);


			Element beaconCQTextSecondText = doc.createElement("beaconCQTextSecondText");
			beaconCQTextSecondText.setTextContent(this.getBcn_beaconTextSecondCat());
			beaconCQ.appendChild(beaconCQTextSecondText);

			Element bcn_beaconTextSecondCat = doc.createElement("bcn_beaconTextSecondCat");
			bcn_beaconTextSecondCat.setTextContent(this.getBcn_beaconTextSecondCat());
			beaconCQ.appendChild(bcn_beaconTextSecondCat);

			Element beaconCQIntervalMinutesSecondCat = doc.createElement("beaconCQIntervalMinutesSecondCat");
			beaconCQIntervalMinutesSecondCat.setTextContent(this.getBcn_beaconIntervalInMinutesSecondCat()+"");
			beaconCQ.appendChild(beaconCQIntervalMinutesSecondCat);

			Element bcn_beaconIntervalInMinutesSecondCat = doc.createElement("bcn_beaconIntervalInMinutesSecondCat");
			bcn_beaconIntervalInMinutesSecondCat.setTextContent(this.getBcn_beaconIntervalInMinutesSecondCat()+"");
			beaconCQ.appendChild(bcn_beaconIntervalInMinutesSecondCat);

			Element beaconCQEnabledSecondCat = doc.createElement("beaconCQEnabledSecondCat");
			beaconCQEnabledSecondCat.setTextContent(this.isBcn_beaconsEnabledSecondCat()+"");
			beaconCQ.appendChild(beaconCQEnabledSecondCat);

			Element bcn_beaconsEnabledSecondCat = doc.createElement("bcn_beaconsEnabledSecondCat");
			bcn_beaconsEnabledSecondCat.setTextContent(this.isBcn_beaconsEnabledSecondCat()+"");
			beaconCQ.appendChild(bcn_beaconsEnabledSecondCat);

			/**
			 * Messagehandling section / ex Beacon Unworked Stations
			 */

			Element beaconUnworkedstations = doc.createElement("beaconUnworkedstations");
			rootElement.appendChild(beaconUnworkedstations);

			Element beaconUnworkedstationsText = doc.createElement("beaconUnworkedstationsText");
			beaconUnworkedstationsText.setTextContent(this.getMessageHandling_unworkedStnRequesterBeaconsText());
			beaconUnworkedstations.appendChild(beaconUnworkedstationsText);

			Element messageHandling_unworkedStnRequesterBeaconsText = doc.createElement("messageHandling_unworkedStnRequesterBeaconsText");
			messageHandling_unworkedStnRequesterBeaconsText.setTextContent(this.getMessageHandling_unworkedStnRequesterBeaconsText());
			beaconUnworkedstations.appendChild(messageHandling_unworkedStnRequesterBeaconsText);

			Element beaconUnworkedstationsIntervalMinutes = doc.createElement("beaconUnworkedstationsIntervalMinutes");
			beaconUnworkedstationsIntervalMinutes.setTextContent(this.getMessageHandling_unworkedStnRequesterBeaconsInterval()+"");
			beaconUnworkedstations.appendChild(beaconUnworkedstationsIntervalMinutes);

			Element messageHandling_unworkedStnRequesterBeaconsInterval = doc.createElement("messageHandling_unworkedStnRequesterBeaconsInterval");
			messageHandling_unworkedStnRequesterBeaconsInterval.setTextContent(this.getMessageHandling_unworkedStnRequesterBeaconsInterval()+"");
			beaconUnworkedstations.appendChild(messageHandling_unworkedStnRequesterBeaconsInterval);

			Element beaconUnworkedstationsEnabled = doc.createElement("beaconUnworkedstationsEnabled");
			beaconUnworkedstationsEnabled.setTextContent(this.isMessageHandling_unworkedStnRequesterBeaconsEnabled()+"");
			beaconUnworkedstations.appendChild(beaconUnworkedstationsEnabled);

			Element messageHandling_unworkedStnRequesterBeaconsEnabled = doc.createElement("messageHandling_unworkedStnRequesterBeaconsEnabled");
			messageHandling_unworkedStnRequesterBeaconsEnabled.setTextContent(this.isMessageHandling_unworkedStnRequesterBeaconsEnabled()+"");
			beaconUnworkedstations.appendChild(messageHandling_unworkedStnRequesterBeaconsEnabled);

			Element beaconUnworkedstationsPrefix = doc.createElement("beaconUnworkedstationsPrefix");
			beaconUnworkedstationsPrefix.setTextContent(this.messageHandling_beaconUnworkedstationsPrefix());
			beaconUnworkedstations.appendChild(beaconUnworkedstationsPrefix);

			Element messageHandling_beaconUnworkedstationsPrefix = doc.createElement("messageHandling_beaconUnworkedstationsPrefix");
			messageHandling_beaconUnworkedstationsPrefix.setTextContent(this.messageHandling_beaconUnworkedstationsPrefix());
			beaconUnworkedstations.appendChild(messageHandling_beaconUnworkedstationsPrefix);

			/*****************************************************************
			 * MESSAGEHANDLING NEW  .... BEACONUNWORKED HAVE TO BE REPLACED
			 ****************************************************************/

			Element messageHandling = doc.createElement("messageHandling");
			rootElement.appendChild(messageHandling);

			Element autoAnswerText = doc.createElement("autoAnswerText");
			autoAnswerText.setTextContent(this.getMessageHandling_autoAnswerTextMainCat());
			messageHandling.appendChild(autoAnswerText);

			Element autoAnswerEnabled = doc.createElement("autoAnswerEnabled");
			autoAnswerEnabled.setTextContent(this.isMsgHandling_autoAnswerEnabled()+"");
			messageHandling.appendChild(autoAnswerEnabled);

			Element autoAnswerTextSecondCat = doc.createElement("autoAnswerTextSecondCat");
			autoAnswerTextSecondCat.setTextContent(this.getMessageHandling_autoAnswerTextSecondCat());
			messageHandling.appendChild(autoAnswerTextSecondCat);

			Element autoAnswerEnabledSecondCat = doc.createElement("autoAnswerEnabledSecondCat");
			autoAnswerEnabledSecondCat.setTextContent(this.isMessageHandling_autoAnswerEnabledSecondCat()+"");
			messageHandling.appendChild(autoAnswerEnabledSecondCat);

			Element autoAnswerToQrgRequestEnabled = doc.createElement("autoAnswerToQrgRequestEnabled");
			autoAnswerToQrgRequestEnabled.setTextContent(isMessageHandling_autoAnswerToQRGRequestEnabled()+"");
			messageHandling.appendChild(autoAnswerToQrgRequestEnabled);

			/****************************
			 * GUI BEHAVIOUR
			 ***************************/

			Element guiSaveableOptions = doc.createElement("guiSaveableOptions");
			rootElement.appendChild(guiSaveableOptions);

			Element guiOptions_defaultFilterNothing = doc.createElement("guiOptions_defaultFilterNothing");
			guiOptions_defaultFilterNothing.setTextContent(this.isGuiOptions_defaultFilterNothing()+"");
			guiSaveableOptions.appendChild(guiOptions_defaultFilterNothing);

			Element guiOptions_defaultFilterPmToMe = doc.createElement("guiOptions_defaultFilterPmToMe");
			guiOptions_defaultFilterPmToMe.setTextContent(this.isGuiOptions_defaultFilterPmToMe()+"");
			guiSaveableOptions.appendChild(guiOptions_defaultFilterPmToMe);

			Element guiOptions_defaultFilterPmToOther = doc.createElement("guiOptions_defaultFilterPmToOther");
			guiOptions_defaultFilterPmToOther.setTextContent(this.isGuiOptions_defaultFilterPmToOther()+"");
			guiSaveableOptions.appendChild(guiOptions_defaultFilterPmToOther);

			Element guiOptions_defaultFilterPublicMsgs = doc.createElement("guiOptions_defaultFilterPublicMsgs");
			guiOptions_defaultFilterPublicMsgs.setTextContent(this.isGuiOptions_defaultFilterPublicMsgs()+"");
			guiSaveableOptions.appendChild(guiOptions_defaultFilterPublicMsgs);

			Element guiOptions_darkModeActive = doc.createElement("guiOptions_darkModeActive");
			guiOptions_darkModeActive.setTextContent(this.GUI_darkModeActive + "");
			guiSaveableOptions.appendChild(guiOptions_darkModeActive);

			Element guiOptions_darkModeActiveByDefault = doc.createElement("guiOptions_darkModeActiveByDefault");
			guiOptions_darkModeActiveByDefault.setTextContent(this.GUI_darkModeActiveByDefault + "");
			guiSaveableOptions.appendChild(guiOptions_darkModeActiveByDefault);

			// --- GUImainWindowRightSplitPane_dividerposition (2 dividers => 2 values) --- NEW
			ensureMainWindowRightSplitPaneDividerPositions(2);

			Element eDiv0 = doc.createElement("GUImainWindowRightSplitPane_dividerposition0");
			eDiv0.setTextContent(String.valueOf(GUImainWindowRightSplitPane_dividerposition[0]));
			guiSaveableOptions.appendChild(eDiv0);

			Element eDiv1 = doc.createElement("GUImainWindowRightSplitPane_dividerposition1");
			eDiv1.setTextContent(String.valueOf(GUImainWindowRightSplitPane_dividerposition[1]));
			guiSaveableOptions.appendChild(eDiv1);

			/**
			 * window sizes
			 */
			Element guiOptions = doc.createElement("guiOptions");
			rootElement.appendChild(guiOptions);

			Element GUIscn_ChatwindowMainSceneSizeHW = doc.createElement("GUIscn_ChatwindowMainSceneSizeHW");
			GUIscn_ChatwindowMainSceneSizeHW.setTextContent(this.getGUIscn_ChatwindowMainSceneSizeHW()[0]+";"+this.getGUIscn_ChatwindowMainSceneSizeHW()[1]);
			guiOptions.appendChild(GUIscn_ChatwindowMainSceneSizeHW);

			Element GUIclusterAndQSOMonStage_SceneSizeHW = doc.createElement("GUIclusterAndQSOMonStage_SceneSizeHW");
			GUIclusterAndQSOMonStage_SceneSizeHW.setTextContent(this.getGUIclusterAndQSOMonStage_SceneSizeHW()[0]+";"+this.getGUIclusterAndQSOMonStage_SceneSizeHW()[1]);
			guiOptions.appendChild(GUIclusterAndQSOMonStage_SceneSizeHW);

			Element GUIstage_updateStage_SceneSizeHW = doc.createElement("GUIstage_updateStage_SceneSizeHW");
			GUIstage_updateStage_SceneSizeHW.setTextContent(this.getGUIstage_updateStage_SceneSizeHW()[0]+";"+this.getGUIstage_updateStage_SceneSizeHW()[1]);
			guiOptions.appendChild(GUIstage_updateStage_SceneSizeHW);

			Element GUIsettingsStageSceneSizeHW = doc.createElement("GUIsettingsStageSceneSizeHW");
			GUIsettingsStageSceneSizeHW.setTextContent(this.getGUIsettingsStageSceneSizeHW()[0]+";"+this.getGUIsettingsStageSceneSizeHW()[1]);
			guiOptions.appendChild(GUIsettingsStageSceneSizeHW);

			/************************************
			 * save splitpanel divider positions
			 ************************************/

			Element GUIselectedCallSignSplitPane_dividerposition = doc.createElement("GUIselectedCallSignSplitPane_dividerposition");
			GUIselectedCallSignSplitPane_dividerposition.setTextContent(doubleArrayToCSVString(getGUIselectedCallSignSplitPane_dividerposition()));
			guiOptions.appendChild(GUIselectedCallSignSplitPane_dividerposition);

			Element GUImainWindowLeftSplitPane_dividerposition = doc.createElement("GUImainWindowLeftSplitPane_dividerposition");
			GUImainWindowLeftSplitPane_dividerposition.setTextContent(doubleArrayToCSVString(getGUImainWindowLeftSplitPane_dividerposition()));
			guiOptions.appendChild(GUImainWindowLeftSplitPane_dividerposition);

			Element GUImessageSectionSplitpane_dividerposition = doc.createElement("GUImessageSectionSplitpane_dividerposition");
			GUImessageSectionSplitpane_dividerposition.setTextContent(doubleArrayToCSVString(getGUImessageSectionSplitpane_dividerposition()));
			guiOptions.appendChild(GUImessageSectionSplitpane_dividerposition);

			Element GUImainWindowRightSplitPane_dividerposition = doc.createElement("GUImainWindowRightSplitPane_dividerposition");
			GUImainWindowRightSplitPane_dividerposition.setTextContent(doubleArrayToCSVString(getGUImainWindowRightSplitPane_dividerposition()));
			guiOptions.appendChild(GUImainWindowRightSplitPane_dividerposition);

			Element GUIpnl_directedMSGWin_dividerpositionDefault = doc.createElement("GUIpnl_directedMSGWin_dividerpositionDefault");
			GUIpnl_directedMSGWin_dividerpositionDefault.setTextContent(doubleArrayToCSVString(getGUIpnl_directedMSGWin_dividerpositionDefault()));
			guiOptions.appendChild(GUIpnl_directedMSGWin_dividerpositionDefault);

			/****************************************************************************************
			 ****************************** now write this XML! *************************************
			 ****************************************************************************************/

			writeXml(doc, System.out);

			// write dom document to a file
			try (FileOutputStream output =
						 new FileOutputStream(storeAndRestorePreferencesFileName)) {
				writeXml(doc, output);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (TransformerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}


		} catch (ParserConfigurationException | TransformerException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return true;

		// root elements

		//...create XML elements, and others...

		// write dom document to a file

	}

	// write doc to output stream
	private static void writeXml(Document doc, OutputStream output) throws TransformerException {

		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(output);

		transformer.setOutputProperty(OutputKeys.INDENT, "yes");

		transformer.transform(source, result);

	}

	/**
	 *
	 * @return true if the file reading was successful, else false
	 */
	public boolean readPreferencesFromXmlFile() {

		System.out.println("[ChatPreferences, Info]: restoring prefs from file " + storeAndRestorePreferencesFileName);

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		} catch (ParserConfigurationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try {

			File xmlConfigFile = new File(storeAndRestorePreferencesFileName);

			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(xmlConfigFile);
			NodeList list;

			// --- Schema version (optional) ---
			// Missing in older files -> assume version 1.
			int xmlVersion = getIntFromDoc(doc, CONFIG_VERSION, "configVersion", "ConfigVersion");
			// Currently we only need the version for debugging / future migrations.
			if (xmlVersion > CONFIG_VERSION) {
				System.out.println("[ChatPreferences, Info]: preferences.xml version (" + xmlVersion + ") is newer than this application (" + CONFIG_VERSION + "). Trying best-effort load.");
			}


			/**
			 * case station settings
			 *
			 */
			Element stationEl = getFirstElement(doc, "station");
			if (stationEl != null) {
				// Prefer tag names close to variable names, but accept legacy tags for backwards compatibility.
				stn_loginCallSign = getText(stationEl, stn_loginCallSign, "stn_loginCallSign", "LoginCallSign");
				stn_loginPassword = getText(stationEl, stn_loginPassword, "stn_loginPassword", "LoginPassword");
				stn_loginNameMainCat = getText(stationEl, stn_loginNameMainCat, "stn_loginNameMainCat", "LoginDisplayedName");
				stn_loginNameSecondCat = getText(stationEl, stn_loginNameSecondCat, "stn_loginNameSecondCat");
				stn_loginLocatorMainCat = getText(stationEl, stn_loginLocatorMainCat, "stn_loginLocatorMainCat", "LoginLocator");

				stn_on4kstServersDns = getText(stationEl, stn_on4kstServersDns, "stn_on4kstServersDns");
				stn_on4kstServersPort = getInt(stationEl, stn_on4kstServersPort, "stn_on4kstServersPort");
				stn_loginAFKState = getBoolean(stationEl, stn_loginAFKState, "stn_loginAFKState");

				String category = getText(stationEl, null, "loginChatCategoryMain", "ChatCategory");
				if (isNumeric(category)) {
					loginChatCategoryMain = new ChatCategory(Integer.parseInt(category));
				} else {
					loginChatCategoryMain = new ChatCategory(2);
				}

				String categorySecond = getText(stationEl, null, "loginChatCategorySecond", "ChatCategorySecond");
				if (isNumeric(categorySecond)) {
					loginChatCategorySecond = new ChatCategory(Integer.parseInt(categorySecond));
				} else {
					loginChatCategorySecond = new ChatCategory(3);
				}

				loginToSecondChatEnabled = getBoolean(stationEl, loginToSecondChatEnabled, "stn_secondCatEnabled");

				stn_antennaBeamWidthDeg = getDouble(stationEl, stn_antennaBeamWidthDeg, "stn_antennaBeamWidthDeg");
				stn_maxQRBDefault = getDouble(stationEl, stn_maxQRBDefault, "stn_maxQRBDefault");
				stn_qtfDefault = getDouble(stationEl, stn_qtfDefault, "stn_qtfDefault");

				// Band activity flags (introduced later; if missing -> keep defaults)
				stn_bandActive144 = getBoolean(stationEl, stn_bandActive144, "stn_bandActive144");
				stn_bandActive432 = getBoolean(stationEl, stn_bandActive432, "stn_bandActive432");
				stn_bandActive1240 = getBoolean(stationEl, stn_bandActive1240, "stn_bandActive1240");
				stn_bandActive2300 = getBoolean(stationEl, stn_bandActive2300, "stn_bandActive2300");
				stn_bandActive3400 = getBoolean(stationEl, stn_bandActive3400, "stn_bandActive3400");
				stn_bandActive5600 = getBoolean(stationEl, stn_bandActive5600, "stn_bandActive5600");
				stn_bandActive10G = getBoolean(stationEl, stn_bandActive10G, "stn_bandActive10G");

				stn_pstRotatorEnabled = getBoolean(stationEl, stn_pstRotatorEnabled, "stn_pstRotatorEnabled");

			}

			/**
			 * Case log synchronizatrion
			 */

			Element logsynchEl = getFirstElement(doc, "logsynch");
			if (logsynchEl != null) {
				logsynch_fileBasedWkdCallInterpreterFileNameReadOnly = getText(
						logsynchEl,
						logsynch_fileBasedWkdCallInterpreterFileNameReadOnly,
						"logsynch_fileBasedWkdCallInterpreterFileNameReadOnly");
				logsynch_storeWorkedCallSignsFileNameUDPMessageBackup = getText(
						logsynchEl,
						logsynch_storeWorkedCallSignsFileNameUDPMessageBackup,
						"logsynch_storeWorkedCallSignsFileNameUDPMessageBackup");
				logsynch_fileBasedWkdCallInterpreterEnabled = getBoolean(
						logsynchEl,
						logsynch_fileBasedWkdCallInterpreterEnabled,
						"logsynch_fileBasedWkdCallInterpreterEnabled");
				logsynch_ucxUDPWkdCallListenerPort = getInt(
						logsynchEl,
						logsynch_ucxUDPWkdCallListenerPort,
						"logsynch_ucxUDPWkdCallListenerPort");
				logsynch_ucxUDPWkdCallListenerEnabled = getBoolean(
						logsynchEl,
						logsynch_ucxUDPWkdCallListenerEnabled,
						"logsynch_ucxUDPWkdCallListenerEnabled");

				// Optional Win-Test network settings
				logsynch_wintestNetworkStationNameOfKST = getText(
						logsynchEl,
						logsynch_wintestNetworkStationNameOfKST,
						"logsynch_wintestNetworkStationNameOfKST");
				logsynch_wintestNetworkStationNameOfWintestClient1 = getText(
						logsynchEl,
						logsynch_wintestNetworkStationNameOfWintestClient1,
						"logsynch_wintestNetworkStationNameOfWintestClient1");
				logsynch_wintestNetworkSimulationEnabled = getBoolean(
						logsynchEl,
						logsynch_wintestNetworkSimulationEnabled,
						"logsynch_wintestNetworkSimulationEnabled");
				logsynch_wintestNetworkStationIDOfKST = getInt(
						logsynchEl,
						logsynch_wintestNetworkStationIDOfKST,
						"logsynch_wintestNetworkStationIDOfKST");
				logsynch_wintestNetworkPort = getInt(
						logsynchEl,
						logsynch_wintestNetworkPort,
						"logsynch_wintestNetworkPort");

				logsynch_wintestNetworkListenerEnabled = getBoolean(
						logsynchEl,
						logsynch_wintestNetworkListenerEnabled,
						"logsynch_wintestNetworkListenerEnabled");

				logsynch_wintestNetworkBroadcastAddress = getText(
						logsynchEl,
						logsynch_wintestNetworkBroadcastAddress,
						"logsynch_wintestNetworkBroadcastAddress");

				logsynch_wintestNetworkSkedPushEnabled = getBoolean(
						logsynchEl,
						logsynch_wintestNetworkSkedPushEnabled,
						"logsynch_wintestNetworkSkedPushEnabled");

				System.out.println(
						"[ChatPreferences, info]: file based worked-call interpreter: " + logsynch_fileBasedWkdCallInterpreterEnabled);
				System.out.println(
						"[ChatPreferences, info]: UCX UDP worked-call listener: " + logsynch_ucxUDPWkdCallListenerEnabled);
			}

			/**
			 * Case trx synchronizatrion
			 */

			Element trxSynchEl = getFirstElement(doc, "trxSynchUCX");
			if (trxSynchEl != null) {
				trxSynch_ucxLogUDPListenerEnabled = getBoolean(
						trxSynchEl,
						trxSynch_ucxLogUDPListenerEnabled,
						"trxSynch_ucxLogUDPListenerEnabled");

				String qrg1 = getText(trxSynchEl, null, "trxSynch_defaultMYQRGValue");
				if (qrg1 != null) {
					this.getMYQRGFirstCat().setValue(qrg1);
				}
				String qrg2 = getText(trxSynchEl, "1296.123.00", "trxSynch_defaultMYQRG2Value");
				this.getMYQRGSecondCat().setValue(qrg2);

				System.out.println(
						"[ChatPreferences, info]: trx qrg synch=" + trxSynch_ucxLogUDPListenerEnabled
								+ ", default=" + this.getMYQRGFirstCat().getValue() + " // " + this.getMYQRGSecondCat().getValue());
			}

			/**
			 * Case notifications
			 */

			Element notificationsEl = getFirstElement(doc, "notifications");
			if (notificationsEl != null) {
				notify_playSimpleSounds = getBoolean(notificationsEl, notify_playSimpleSounds, "notify_SimpleAudioNotificationsEnabled");
				notify_playCWCallsignsOnRxedPMs = getBoolean(notificationsEl, notify_playCWCallsignsOnRxedPMs, "notify_CWCallsignAudioNotificationsEnabled");
				notify_playVoiceCallsignsOnRxedPMs = getBoolean(notificationsEl, notify_playVoiceCallsignsOnRxedPMs, "notify_VoiceCallsignAudioNotificationsEnabled");

				// DXCluster / Monitoring (introduced later) -> keep defaults if absent
				notify_dxClusterServerEnabled = getBoolean(notificationsEl, notify_dxClusterServerEnabled, "notify_dxClusterServerEnabled");
				notify_DXClusterServerTriggerBearing = getBoolean(notificationsEl, notify_DXClusterServerTriggerBearing, "notify_DXClusterServerTriggerBearing");
				notify_DXClusterServerTriggerOnQRGDetect = getBoolean(notificationsEl, notify_DXClusterServerTriggerOnQRGDetect, "notify_DXClusterServerTriggerOnQRGDetect");
				notify_dxclusterServerPort = getInt(notificationsEl, notify_dxclusterServerPort, "notify_dxclusterServerPort");

				String spotter = getText(notificationsEl, null, "notify_DXCSrv_SpottersCallSign");
				if (spotter != null) {
					notify_DXCSrv_SpottersCallSign.set(spotter);
				}
				String prefix = getText(notificationsEl, null, "notify_optionalFrequencyPrefix");
				if (prefix != null) {
					notify_optionalFrequencyPrefix.set(prefix);
				}

				Integer noReply = getInt(notificationsEl, 13, "notify_noReplyPenaltyMinutes");
				if (noReply != null) {
					notify_noReplyPenaltyMinutes = noReply;
				}

				Integer momentum = getInt(notificationsEl, 666, "notify_momentumWindowSeconds");
				if (momentum != null) {
					notify_momentumWindowSeconds = momentum;
				}

				String pos = getText(notificationsEl, null, "notify_positiveSignalsPatterns");
				if (pos != null) {
					notify_positiveSignalsPatterns = pos;
				}

				notify_bandUpgradeHintOnLogEnabled = getBoolean(
						notificationsEl,
						notify_bandUpgradeHintOnLogEnabled,
						"notify_bandUpgradeHintOnLogEnabled"
				);
				notify_bandUpgradePriorityBoostEnabled = getBoolean(
						notificationsEl,
						notify_bandUpgradePriorityBoostEnabled,
						"notify_bandUpgradePriorityBoostEnabled"
				);


				System.out.println(
						"[ChatPreferences, info]: audio notifications simple=" + notify_playSimpleSounds
								+ ", CW=" + notify_playCWCallsignsOnRxedPMs
								+ ", Voice=" + notify_playVoiceCallsignsOnRxedPMs);
			}


			/**
			 * Case AirScout querier
			 */

			Element airScoutEl = getFirstElement(doc, "AirScoutQuerier");
			if (airScoutEl != null) {
				AirScout_asUDPListenerEnabled = getBoolean(airScoutEl, AirScout_asUDPListenerEnabled, "asQry_airScoutCommunicationEnabled");
				AirScout_asServerNameString = getText(airScoutEl, AirScout_asServerNameString, "asQry_airScoutServerName");
				AirScout_asClientNameString = getText(airScoutEl, AirScout_asClientNameString, "asQry_airScoutClientName");
				AirScout_asCommunicationPort = getInt(airScoutEl, AirScout_asCommunicationPort, "asQry_airScoutUDPPort");
				AirScout_asBandString = getText(airScoutEl, AirScout_asBandString, "asQry_airScoutBandValue");

				System.out.println(
						"[ChatPreferences, info]: AirScout querier enabled=" + AirScout_asUDPListenerEnabled
								+ ", band=" + AirScout_asBandString);
			}

			/**
			 * Case shortCuts
			 */

			Element shortCutsEl = getFirstElement(doc, "shortCuts");
			if (shortCutsEl != null) {
				lst_txtShortCutBtnList.clear();
				NodeList children = shortCutsEl.getChildNodes();
				for (int i = 0; i < children.getLength(); i++) {
					Node child = children.item(i);
					if (child.getNodeType() == Node.ELEMENT_NODE) {
						String v = child.getTextContent();
						if (v != null) {
							v = v.trim();
						}
						if (v != null && !v.isEmpty()) {
							lst_txtShortCutBtnList.add(v);
						}
					}
				}
			}

			/**
			 * Case textSnippets
			 */

			Element textSnippetsEl = getFirstElement(doc, "textSnippets");
			if (textSnippetsEl != null) {
				lst_txtSnipList.clear();
				NodeList children = textSnippetsEl.getChildNodes();
				for (int i = 0; i < children.getLength(); i++) {
					Node child = children.item(i);
					if (child.getNodeType() == Node.ELEMENT_NODE) {
						String v = child.getTextContent();
						if (v != null) {
							v = v.trim();
						}
						if (v != null && !v.isEmpty()) {
							lst_txtSnipList.add(v);
						}
					}
				}
			}

			/**
			 * Case QSO-sniffer lists (added later; older configs won't have them)
			 */
			list = doc.getElementsByTagName("snifferWords");
			if (list != null && list.getLength() != 0) {
				// reset to avoid duplicates when reloading
				lstNotify_QSOSniffer_sniffedWordsList.clear();
				for (int temp = 0; temp < list.getLength(); temp++) {
					Node node = list.item(temp);
					if (node.getNodeType() == Node.ELEMENT_NODE) {
						Element element = (Element) node;
						NodeList children = element.getChildNodes();
						for (int i = 0; i < children.getLength(); i++) {
							Node child = children.item(i);
							if (child.getNodeType() == Node.ELEMENT_NODE) {
								String word = child.getTextContent();
								if (word != null) {
									word = word.trim();
								}
								if (word != null && !word.isEmpty()) {
									lstNotify_QSOSniffer_sniffedWordsList.add(word);
								}
							}
						}
					}
				}
			}

			list = doc.getElementsByTagName("snifferPrefixes");
			if (list != null && list.getLength() != 0) {
				lstNotify_QSOSniffer_sniffedPrefixLocList.clear();
				for (int temp = 0; temp < list.getLength(); temp++) {
					Node node = list.item(temp);
					if (node.getNodeType() == Node.ELEMENT_NODE) {
						Element element = (Element) node;
						NodeList children = element.getChildNodes();
						for (int i = 0; i < children.getLength(); i++) {
							Node child = children.item(i);
							if (child.getNodeType() == Node.ELEMENT_NODE) {
								String prefix = child.getTextContent();
								if (prefix != null) {
									prefix = prefix.trim();
								}
								if (prefix != null && !prefix.isEmpty()) {
									lstNotify_QSOSniffer_sniffedPrefixLocList.add(prefix);
								}
							}
						}
					}
				}
			}

			/**
			 * Case beaconCQ
			 */

			Element beaconCQEl = getFirstElement(doc, "beaconCQ");
			if (beaconCQEl != null) {
				bcn_beaconsEnabledMainCat = getBoolean(beaconCQEl, bcn_beaconsEnabledMainCat, "bcn_beaconsEnabledMainCat", "beaconCQEnabled");
				bcn_beaconIntervalInMinutesMainCat = getInt(beaconCQEl, bcn_beaconIntervalInMinutesMainCat, "bcn_beaconIntervalInMinutesMainCat", "beaconCQIntervalMinutes");
				bcn_beaconTextMainCat = getText(beaconCQEl, bcn_beaconTextMainCat, "bcn_beaconTextMainCat", "beaconCQText");

				bcn_beaconsEnabledSecondCat = getBoolean(beaconCQEl, bcn_beaconsEnabledSecondCat, "bcn_beaconsEnabledSecondCat", "beaconCQEnabledSecondCat");
				bcn_beaconIntervalInMinutesSecondCat = getInt(beaconCQEl, bcn_beaconIntervalInMinutesSecondCat, "bcn_beaconIntervalInMinutesSecondCat", "beaconCQIntervalMinutesSecondCat");
				bcn_beaconTextSecondCat = getText(beaconCQEl, bcn_beaconTextSecondCat, "bcn_beaconTextSecondCat", "beaconCQTextSecondText");

				System.out.println("[ChatPreferences, info]: beaconCQ main='" + bcn_beaconTextMainCat + "', second='" + bcn_beaconTextSecondCat + "'");
			}

			/**
			 * Case beaconUnworkedstations
			 *
			 */

			Element beaconUnworkedEl = getFirstElement(doc, "beaconUnworkedstations");
			if (beaconUnworkedEl != null) {
				messageHandling_unworkedStnRequesterBeaconsText = getText(
						beaconUnworkedEl,
						messageHandling_unworkedStnRequesterBeaconsText,
						"messageHandling_unworkedStnRequesterBeaconsText",
						"beaconUnworkedstationsText");
				messageHandling_unworkedStnRequesterBeaconsInterval = getInt(
						beaconUnworkedEl,
						messageHandling_unworkedStnRequesterBeaconsInterval,
						"messageHandling_unworkedStnRequesterBeaconsInterval",
						"beaconUnworkedstationsIntervalMinutes");
				messageHandling_unworkedStnRequesterBeaconsEnabled = getBoolean(
						beaconUnworkedEl,
						messageHandling_unworkedStnRequesterBeaconsEnabled,
						"messageHandling_unworkedStnRequesterBeaconsEnabled",
						"beaconUnworkedstationsEnabled");
				messageHandling_beaconUnworkedstationsPrefix = getText(
						beaconUnworkedEl,
						messageHandling_beaconUnworkedstationsPrefix,
						"messageHandling_beaconUnworkedstationsPrefix",
						"beaconUnworkedstationsPrefix");
				System.out.println("[ChatPreferences, info]: unworked-stations beacon text='" + messageHandling_unworkedStnRequesterBeaconsText + "'");
			}



			/***********************************************
			 *
			 * case messageHandling
			 *
			 ***********************************************/
			Element messageHandlingEl = getFirstElement(doc, "messageHandling");
			if (messageHandlingEl != null) {
				messageHandling_autoAnswerTextMainCat = getText(
						messageHandlingEl,
						messageHandling_autoAnswerTextMainCat,
						"messageHandling_autoAnswerTextMainCat",
						"autoAnswerText");
				messageHandling_autoAnswerEnabled = getBoolean(
						messageHandlingEl,
						messageHandling_autoAnswerEnabled,
						"messageHandling_autoAnswerEnabled",
						"autoAnswerEnabled");
				messageHandling_autoAnswerToQRGRequestEnabled = getBoolean(
						messageHandlingEl,
						messageHandling_autoAnswerToQRGRequestEnabled,
						"messageHandling_autoAnswerToQRGRequestEnabled",
						"autoAnswerToQrgRequestEnabled",
						"autoAnswerToQRGRequestEnabled");

				messageHandling_autoAnswerTextSecondCat = getText(
						messageHandlingEl,
						messageHandling_autoAnswerTextSecondCat,
						"messageHandling_autoAnswerTextSecondCat",
						"autoAnswerTextSecondCat");
				messageHandling_autoAnswerEnabledSecondCat = getBoolean(
						messageHandlingEl,
						messageHandling_autoAnswerEnabledSecondCat,
						"messageHandling_autoAnswerEnabledSecondCat",
						"autoAnswerEnabledSecondCat");
			}









			/***********************************************
			 *
			 * case read GUI options
			 *
			 ***********************************************/
			list = doc.getElementsByTagName("guiOptions");
			if (list.getLength() != 0) {

				for (int temp = 0; temp < list.getLength(); temp++) {

					Node node = list.item(temp);

					if (node.getNodeType() == Node.ELEMENT_NODE) {

						Element element = (Element) node;

						// Scene sizes (semicolon separated), defaults are already in the arrays.
						parseSemicolonDoublesInto(getText(element, null, "GUIscn_ChatwindowMainSceneSizeHW"), this.getGUIscn_ChatwindowMainSceneSizeHW());
						parseSemicolonDoublesInto(getText(element, null, "GUIclusterAndQSOMonStage_SceneSizeHW"), this.getGUIclusterAndQSOMonStage_SceneSizeHW());
						parseSemicolonDoublesInto(getText(element, null, "GUIstage_updateStage_SceneSizeHW"), this.getGUIstage_updateStage_SceneSizeHW());
						parseSemicolonDoublesInto(getText(element, null, "GUIsettingsStageSceneSizeHW"), this.getGUIsettingsStageSceneSizeHW());

						// Splitpane divider positions
						String s1 = getText(element, null, "GUIselectedCallSignSplitPane_dividerposition");
						if (s1 != null) {
							this.setGUIselectedCallSignSplitPane_dividerposition(csvStringToDoubleArray(s1));
						}
						String s2 = getText(element, null, "GUImainWindowLeftSplitPane_dividerposition");
						if (s2 != null) {
							this.setGUImainWindowLeftSplitPane_dividerposition(csvStringToDoubleArray(s2));
						}
						String s3 = getText(element, null, "GUImessageSectionSplitpane_dividerposition");
						if (s3 != null) {
							double[] parsed = csvStringToDoubleArray(s3);
							// Config files older than ~1.40 had fewer panes.
							if (parsed.length >= 4) {
								this.setGUImessageSectionSplitpane_dividerposition(parsed);
							}
						}
						// GUImainWindowRightSplitPane divider positions (2 dividers => 2 values)
// Backward compatible: old configs stored a single CSV tag.
						String rightLegacy = getText(element, null, "GUImainWindowRightSplitPane_dividerposition");
						String right0 = getText(element, null, "GUImainWindowRightSplitPane_dividerposition0");
						String right1 = getText(element, null, "GUImainWindowRightSplitPane_dividerposition1");

						if (right0 != null || right1 != null) {

							// New format: two dedicated tags
							double p0 = (right0 != null) ? parseDoubleOrDefault(right0, this.GUImainWindowRightSplitPane_dividerpositionDefault[0])
									: this.GUImainWindowRightSplitPane_dividerpositionDefault[0];

							double p1 = (right1 != null) ? parseDoubleOrDefault(right1, this.GUImainWindowRightSplitPane_dividerpositionDefault[1])
									: this.GUImainWindowRightSplitPane_dividerpositionDefault[1];

							this.setGUImainWindowRightSplitPane_dividerposition(new double[] { p0, p1 });

						} else if (rightLegacy != null) {

							// Old format: CSV array (often length 1)
							double[] parsed = csvStringToDoubleArray(rightLegacy);

							// Upgrade older config files gracefully
							if (parsed.length >= 2) {
								this.setGUImainWindowRightSplitPane_dividerposition(new double[] { parsed[0], parsed[1] });
							} else if (parsed.length == 1) {
								this.setGUImainWindowRightSplitPane_dividerposition(new double[] {
										parsed[0],
										this.GUImainWindowRightSplitPane_dividerpositionDefault[1]
								});
							}
						}

						// Ensure correct length no matter what was in the config (prevents AIOOBE)
						ensureMainWindowRightSplitPaneDividerPositions(2);


						String s5 = getText(element, null, "GUIpnl_directedMSGWin_dividerpositionDefault");
						if (s5 != null) {
							this.setGUIpnl_directedMSGWin_dividerpositionDefault(csvStringToDoubleArray(s5));
						}
					}
				}
			}

			/***********************************************
			 *
			 * case read guiSaveableOptions
			 *
			 ***********************************************/
			Element guiSaveableOptionsEl = getFirstElement(doc, "guiSaveableOptions");
			if (guiSaveableOptionsEl != null) {
				this.setGuiOptions_defaultFilterNothing(getBoolean(guiSaveableOptionsEl, this.isGuiOptions_defaultFilterNothing(), "guiOptions_defaultFilterNothing"));
				this.setGuiOptions_defaultFilterPmToMe(getBoolean(guiSaveableOptionsEl, this.isGuiOptions_defaultFilterPmToMe(), "guiOptions_defaultFilterPmToMe"));
				this.setGuiOptions_defaultFilterPmToOther(getBoolean(guiSaveableOptionsEl, this.isGuiOptions_defaultFilterPmToOther(), "guiOptions_defaultFilterPmToOther"));
				this.setGuiOptions_defaultFilterPublicMsgs(getBoolean(guiSaveableOptionsEl, this.isGuiOptions_defaultFilterPublicMsgs(), "guiOptions_defaultFilterPublicMsgs"));

				// Added in later versions: dark mode flags
				this.GUI_darkModeActive = getBoolean(guiSaveableOptionsEl, this.GUI_darkModeActive, "guiOptions_darkModeActive");
				this.GUI_darkModeActiveByDefault = getBoolean(guiSaveableOptionsEl, this.GUI_darkModeActiveByDefault, "guiOptions_darkModeActiveByDefault");
			}



		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
			System.out.println(e.getCause());
			System.out.println(e.getMessage());
		}

		return true;

	}

	/**
	 *
	 * @return double array with separated values for each doublevalue, seperated by ";", empty array if none
	 */
	private double [] csvStringToDoubleArray(String csvStringWithDoubles){

		String separator = ";";
		double[] result = new double[csvStringWithDoubles.split(separator).length];

		for (int i = 0; i < (csvStringWithDoubles.split(separator).length); i++) {
			result[i] =
					Double.parseDouble(csvStringWithDoubles.split(separator)[i]);
//			System.out.println("EXTRACTED " + result[i]);
		}

		return result;
	}

	private String doubleArrayToCSVString(double[] doubleArrayToCSVConvert){

		String separator = ";";
		String result = "";

		for (int i = 0; i < (doubleArrayToCSVConvert.length); i++) {
			result += doubleArrayToCSVConvert[i];

			if (i+1<doubleArrayToCSVConvert.length) {
				result += separator;
			}

		}

		return result;
	}

	public boolean isStn_bandActive144() {
		return stn_bandActive144;
	}

	public void setStn_bandActive144(boolean stn_bandActive144) {
		this.stn_bandActive144 = stn_bandActive144;
	}

	public boolean isStn_bandActive432() {
		return stn_bandActive432;
	}

	public void setStn_bandActive432(boolean stn_bandActive432) {
		this.stn_bandActive432 = stn_bandActive432;
	}

	public boolean isStn_bandActive1240() {
		return stn_bandActive1240;
	}

	public void setStn_bandActive1240(boolean stn_bandActive1240) {
		this.stn_bandActive1240 = stn_bandActive1240;
	}

	public boolean isStn_bandActive2300() {
		return stn_bandActive2300;
	}

	public void setStn_bandActive2300(boolean stn_bandActive2300) {
		this.stn_bandActive2300 = stn_bandActive2300;
	}

	public boolean isStn_bandActive3400() {
		return stn_bandActive3400;
	}

	public void setStn_bandActive3400(boolean stn_bandActive3400) {
		this.stn_bandActive3400 = stn_bandActive3400;
	}

	public boolean isStn_bandActive5600() {
		return stn_bandActive5600;
	}

	public void setStn_bandActive5600(boolean stn_bandActive5600) {
		this.stn_bandActive5600 = stn_bandActive5600;
	}

	public boolean isStn_bandActive10G() {
		return stn_bandActive10G;
	}

	public void setStn_bandActive10G(boolean stn_bandActive10G) {
		this.stn_bandActive10G = stn_bandActive10G;
	}

	public boolean isGUI_darkModeActive() {
		return GUI_darkModeActive;
	}

	public void setGUI_darkModeActive(boolean GUI_darkModeActive) {
		this.GUI_darkModeActive = GUI_darkModeActive;
	}

	public boolean isGUI_darkModeActiveByDefault() {
		return GUI_darkModeActiveByDefault;
	}

	public void setGUI_darkModeActiveByDefault(boolean GUI_darkModeActiveByDefault) {
		this.GUI_darkModeActiveByDefault = GUI_darkModeActiveByDefault;
	}

	/**
	 *
	 * If the file-reading goes wrong, set the defaults
	 */
	public void setPreferencesDefaults() {
		System.out.println("[ChatPreferences, Info]: restoring prefs from defaults");

	}

	// ---------------------------------------------------------------------
	// XML helper methods
	// ---------------------------------------------------------------------

	private static Element getFirstElement(Document doc, String tagName) {
		NodeList nl = doc.getElementsByTagName(tagName);
		if (nl == null || nl.getLength() == 0) {
			return null;
		}
		Node n = nl.item(0);
		return (n instanceof Element) ? (Element) n : null;
	}

	/**
	 * Returns the text content of the first matching child tag (directly under {@code parent})
	 * or {@code defaultValue} if the tag does not exist or is empty.
	 */
	private static String getText(Element parent, String defaultValue, String... tagNames) {
		if (parent == null || tagNames == null) {
			return defaultValue;
		}
		for (String t : tagNames) {
			if (t == null || t.isEmpty()) {
				continue;
			}
			NodeList nl = parent.getElementsByTagName(t);
			if (nl == null || nl.getLength() == 0) {
				continue;
			}
			Node n = nl.item(0);
			if (n == null) {
				continue;
			}
			String v = n.getTextContent();
			if (v != null) {
				v = v.trim();
			}
			if (v != null && !v.isEmpty()) {
				return v;
			}
		}
		return defaultValue;
	}

	private static boolean getBoolean(Element parent, boolean defaultValue, String... tagNames) {
		String v = getText(parent, null, tagNames);
		if (v == null) {
			return defaultValue;
		}
		return "true".equalsIgnoreCase(v) || "1".equals(v) || "yes".equalsIgnoreCase(v);
	}

	private static int getInt(Element parent, int defaultValue, String... tagNames) {
		String v = getText(parent, null, tagNames);
		if (v == null) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(v.trim());
		} catch (NumberFormatException ignore) {
			return defaultValue;
		}
	}

	private static double getDouble(Element parent, double defaultValue, String... tagNames) {
		String v = getText(parent, null, tagNames);
		if (v == null) {
			return defaultValue;
		}
		try {
			return Double.parseDouble(v.trim());
		} catch (NumberFormatException ignore) {
			return defaultValue;
		}
	}

	private static int getIntFromDoc(Document doc, int defaultValue, String... tagNames) {
		if (doc == null) {
			return defaultValue;
		}
		for (String t : tagNames) {
			if (t == null || t.isEmpty()) {
				continue;
			}
			NodeList nl = doc.getElementsByTagName(t);
			if (nl == null || nl.getLength() == 0) {
				continue;
			}
			Node n = nl.item(0);
			if (n == null) {
				continue;
			}
			String v = n.getTextContent();
			if (v == null) {
				continue;
			}
			try {
				return Integer.parseInt(v.trim());
			} catch (NumberFormatException ignore) {
				// try next
			}
		}
		return defaultValue;
	}

	private static void parseSemicolonDoublesInto(String input, double[] target) {
		if (input == null || target == null) {
			return;
		}
		String[] parts = input.trim().split(";");
		int len = Math.min(parts.length, target.length);
		for (int i = 0; i < len; i++) {
			try {
				target[i] = Double.parseDouble(parts[i].trim());
			} catch (NumberFormatException ignore) {
				// keep existing value in target[i]
			}
		}
	}

	/**
	 * Checks wheter the input value of the String is numeric or not, true if yes
	 * TODO: Move to a utils class for checking input values by user...
	 *
	 * @param str
	 * @return
	 */
	private static boolean isNumeric(String str) {
		return str != null && str.matches("[0-9.]+");
	}

//	public ObservableList<String> getTxtSnippetsAsObservableList(){
//		ObservableList<String> lst_txtSnipList = FXCollections.observableArrayList();
//
//		for (int i = 0; i < textSnippets.length; i++) {
//			lst_txtSnipList.add(textSnippets[i]);
//			System.out.println(textSnippets[i]);
//		}
//
//		return lst_txtSnipList;
//
//	}


	/**
	 * Ensures that the divider position array matches the current number of dividers.
	 * This upgrades older XML configs (e.g. only 1 divider stored) without crashing.
	 */
	public void ensureMainWindowRightSplitPaneDividerPositions(int requiredDividerCount) {
		if (requiredDividerCount < 0) return;

		if (GUImainWindowRightSplitPane_dividerposition != null
				&& GUImainWindowRightSplitPane_dividerposition.length == requiredDividerCount) {
			return;
		}

		double[] upgraded = new double[requiredDividerCount];

		for (int i = 0; i < requiredDividerCount; i++) {

			// Prefer existing stored values if present
			if (GUImainWindowRightSplitPane_dividerposition != null
					&& i < GUImainWindowRightSplitPane_dividerposition.length) {
				upgraded[i] = GUImainWindowRightSplitPane_dividerposition[i];
				continue;
			}

			// Otherwise use defaults, fallback to even spacing
			if (i < GUImainWindowRightSplitPane_dividerpositionDefault.length) {
				upgraded[i] = GUImainWindowRightSplitPane_dividerpositionDefault[i];
			} else {
				upgraded[i] = (i + 1.0) / (requiredDividerCount + 1.0);
			}
		}

		GUImainWindowRightSplitPane_dividerposition = upgraded;
	}

	private static double parseDoubleOrDefault(String value, double defaultValue) {
		if (value == null) return defaultValue;
		String v = value.trim();
		if (v.isEmpty()) return defaultValue;
		try {
			return Double.parseDouble(v);
		} catch (Exception ignore) {
			return defaultValue;
		}
	}

}



