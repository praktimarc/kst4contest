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

import kst4contest.ApplicationConstants;
import kst4contest.utils.ApplicationFileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class ChatPreferences {

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

	boolean stn_loginAFKState = false; //always start as here
	String stn_loginCallSign = "do5amf";
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
	IntegerProperty actualQTF = new SimpleIntegerProperty(360); // will be updated by user at runtime!

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

	private double[] GUIscn_ChatwindowMainSceneSizeHW = new double[] {768, 1234};
	private double[] GUIclusterAndQSOMonStage_SceneSizeHW = new double[] {700, 500};
	private double[] GUIstage_updateStage_SceneSizeHW = new double[] {640, 480};
	private double[] GUIsettingsStageSceneSizeHW = new double[] {720, 768};

	private double[] GUIselectedCallSignSplitPane_dividerposition = {0.55};
	private double[] GUImainWindowLeftSplitPane_dividerposition = {0.51};
	private double[] GUImessageSectionSplitpane_dividerposition = {0.62, 0.7, 0.75}; //3 deviders now //TODO: more should be possible?
	private double[] GUImainWindowRightSplitPane_dividerposition = {0.72};
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

	public void setGUImessageSectionSplitpane_dividerposition(double[] GUImessageSectionSplitpane_dividerposition) {
		this.GUImessageSectionSplitpane_dividerposition = GUImessageSectionSplitpane_dividerposition;
	}

	public double[] getGUImainWindowRightSplitPane_dividerposition() {
		return GUImainWindowRightSplitPane_dividerposition;
	}

	public void setGUImainWindowRightSplitPane_dividerposition(double[] GUImainWindowRightSplitPane_dividerposition) {
		this.GUImainWindowRightSplitPane_dividerposition = GUImainWindowRightSplitPane_dividerposition;
	}

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

	public IntegerProperty getActualQTF() {
		return actualQTF;
	}

	public void setActualQTF(IntegerProperty actualQTF) {
		this.actualQTF = actualQTF;
	}

	public void setMYQRGFirstCat(StringProperty mYQRG) {
		MYQRGFirstCat = mYQRG;
	}

	public void setStn_loginCallSign(String stn_loginCallSign) {
		this.stn_loginCallSign = stn_loginCallSign;
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


		      Element station = doc.createElement("station");
		      rootElement.appendChild(station);
		      
		      Element LoginCallSign = doc.createElement("LoginCallSign");
		      LoginCallSign.setTextContent(this.getStn_loginCallSign());
		      station.appendChild(LoginCallSign);
		      

		      Element LoginPassword = doc.createElement("LoginPassword");
		      LoginPassword.setTextContent(this.getStn_loginPassword());
		      station.appendChild(LoginPassword);

		      Element LoginDisplayedName = doc.createElement("LoginDisplayedName");
		      LoginDisplayedName.setTextContent(this.getStn_loginNameMainCat());
		      station.appendChild(LoginDisplayedName);

			Element stn_loginNameSecondCat = doc.createElement("stn_loginNameSecondCat");
			stn_loginNameSecondCat.setTextContent(this.getStn_loginNameSecondCat());
			station.appendChild(stn_loginNameSecondCat);

		      Element LoginLocator = doc.createElement("LoginLocator");
		      LoginLocator.setTextContent(this.getStn_loginLocatorMainCat());
		      station.appendChild(LoginLocator);

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
		      logsynch_ucxUDPWkdCallListenerEnabled.setTextContent(this.isTrxSynch_ucxLogUDPListenerEnabled()+"");
		      logsynch.appendChild(logsynch_ucxUDPWkdCallListenerEnabled);
		      
		      
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
		      
		      Element beaconCQIntervalMinutes = doc.createElement("beaconCQIntervalMinutes");
		      beaconCQIntervalMinutes.setTextContent(this.getBcn_beaconIntervalInMinutesMainCat()+"");
		      beaconCQ.appendChild(beaconCQIntervalMinutes);
		      
		      Element beaconCQEnabled = doc.createElement("beaconCQEnabled");
		      beaconCQEnabled.setTextContent(this.isBcn_beaconsEnabledMainCat()+"");
		      beaconCQ.appendChild(beaconCQEnabled);


			Element beaconCQTextSecondText = doc.createElement("beaconCQTextSecondText");
			beaconCQTextSecondText.setTextContent(this.getBcn_beaconTextSecondCat());
			beaconCQ.appendChild(beaconCQTextSecondText);

			Element beaconCQIntervalMinutesSecondCat = doc.createElement("beaconCQIntervalMinutesSecondCat");
			beaconCQIntervalMinutesSecondCat.setTextContent(this.getBcn_beaconIntervalInMinutesSecondCat()+"");
			beaconCQ.appendChild(beaconCQIntervalMinutesSecondCat);

			Element beaconCQEnabledSecondCat = doc.createElement("beaconCQEnabledSecondCat");
			beaconCQEnabledSecondCat.setTextContent(this.isBcn_beaconsEnabledSecondCat()+"");
			beaconCQ.appendChild(beaconCQEnabledSecondCat);

		      /**
		       * Messagehandling section / ex Beacon Unworked Stations
		       */

		      Element beaconUnworkedstations = doc.createElement("beaconUnworkedstations");
		      rootElement.appendChild(beaconUnworkedstations);

		      Element beaconUnworkedstationsText = doc.createElement("beaconUnworkedstationsText");
		      beaconUnworkedstationsText.setTextContent(this.getMessageHandling_unworkedStnRequesterBeaconsText());
		      beaconUnworkedstations.appendChild(beaconUnworkedstationsText);
		      
		      Element beaconUnworkedstationsIntervalMinutes = doc.createElement("beaconUnworkedstationsIntervalMinutes");
		      beaconUnworkedstationsIntervalMinutes.setTextContent(this.getMessageHandling_unworkedStnRequesterBeaconsInterval()+"");
		      beaconUnworkedstations.appendChild(beaconUnworkedstationsIntervalMinutes);

		      Element beaconUnworkedstationsEnabled = doc.createElement("beaconUnworkedstationsEnabled");
		      beaconUnworkedstationsEnabled.setTextContent(this.isMessageHandling_unworkedStnRequesterBeaconsEnabled()+"");
		      beaconUnworkedstations.appendChild(beaconUnworkedstationsEnabled);
		      
		      Element beaconUnworkedstationsPrefix = doc.createElement("beaconUnworkedstationsPrefix");
		      beaconUnworkedstationsPrefix.setTextContent(this.messageHandling_beaconUnworkedstationsPrefix());
		      beaconUnworkedstations.appendChild(beaconUnworkedstationsPrefix);

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


			/**
			 * case station settings
			 * 
			 */
			NodeList list = doc.getElementsByTagName("station");
			if (list.getLength() != 0) {

				for (int temp = 0; temp < list.getLength(); temp++) {

					Node node = list.item(temp);

					if (node.getNodeType() == Node.ELEMENT_NODE) {

						Element element = (Element) node;

						String call = element.getElementsByTagName("LoginCallSign").item(0).getTextContent();
						stn_loginCallSign = call;

//						call = call.toLowerCase();
						String password = element.getElementsByTagName("LoginPassword").item(0).getTextContent();
						stn_loginPassword = password;

						String loginDisplayedName = element.getElementsByTagName("LoginDisplayedName").item(0)
								.getTextContent();
						stn_loginNameMainCat = loginDisplayedName;

						try {
							String loginDisplayedNameSecondCat = element.getElementsByTagName("stn_loginNameSecondCat").item(0)
									.getTextContent();
							stn_loginNameSecondCat = loginDisplayedNameSecondCat;
						} catch (Exception previousVersionExc) {
							stn_loginNameSecondCat = "KST4Contest2nd";
						}

						String qra = element.getElementsByTagName("LoginLocator").item(0).getTextContent();
						stn_loginLocatorMainCat = qra;

						String category = element.getElementsByTagName("ChatCategory").item(0).getTextContent();

						if (isNumeric(category)) {
							ChatCategory chatCategory = new ChatCategory(Integer.parseInt(category));
							loginChatCategoryMain = chatCategory;
						} else {

							loginChatCategoryMain = new ChatCategory(2); // TODO: Set this default at another place
						}

						try {

							String ChatCategorySecond = element.getElementsByTagName("ChatCategorySecond").item(0).getTextContent();
							if (isNumeric(ChatCategorySecond)) {
								ChatCategory chatCategory2 = new ChatCategory(Integer.parseInt(ChatCategorySecond));
								loginChatCategorySecond = chatCategory2;
							} else {
								loginChatCategorySecond = new ChatCategory(3); // TODO: Set this default at another place
							}

							String secondCatEnabledOrNot = element
									.getElementsByTagName("stn_secondCatEnabled").item(0)
									.getTextContent();

							if (secondCatEnabledOrNot.equals("true")) {

								loginToSecondChatEnabled = true;
							} else {
								loginToSecondChatEnabled = false;
							}
						} catch (Exception prevVersionExc){
							loginToSecondChatEnabled = false; //default if setting not found
						}



						double antennaBeamWidthDeg = Double.parseDouble(element.getElementsByTagName("stn_antennaBeamWidthDeg").item(0).getTextContent());
						stn_antennaBeamWidthDeg = antennaBeamWidthDeg;
						double maxQRBDefault = Double.parseDouble(element.getElementsByTagName("stn_maxQRBDefault").item(0).getTextContent());
						stn_maxQRBDefault = maxQRBDefault;
						double qtfDefault = Double.parseDouble(element.getElementsByTagName("stn_qtfDefault").item(0).getTextContent());
						stn_qtfDefault = qtfDefault;

						try {

							String stnUses144 = element
									.getElementsByTagName("stn_bandActive144").item(0)
									.getTextContent();

							if (stnUses144.equals("true")) {

								stn_bandActive144 = true;
							} else {
								stn_bandActive144 = false;
							}

							String stnUses432 = element
									.getElementsByTagName("stn_bandActive432").item(0)
									.getTextContent();

							if (stnUses432.equals("true")) {

								stn_bandActive432 = true;
							} else {
								stn_bandActive432 = false;
							}

							String stnUses1240 = element
									.getElementsByTagName("stn_bandActive1240").item(0)
									.getTextContent();

							if (stnUses1240.equals("true")) {

								stn_bandActive1240 = true;
							} else {
								stn_bandActive1240 = false;
							}

							String stnUses2300 = element
									.getElementsByTagName("stn_bandActive2300").item(0)
									.getTextContent();

							if (stnUses2300.equals("true")) {

								stn_bandActive2300 = true;
							} else {
								stn_bandActive2300 = false;
							}

							String stnUses3400 = element
									.getElementsByTagName("stn_bandActive3400").item(0)
									.getTextContent();

							if (stnUses3400.equals("true")) {

								stn_bandActive3400 = true;
							} else {
								stn_bandActive3400 = false;
							}

							String stnUses5600 = element
									.getElementsByTagName("stn_bandActive5600").item(0)
									.getTextContent();

							if (stnUses5600.equals("true")) {

								stn_bandActive5600 = true;
							} else {
								stn_bandActive5600 = false;
							}

							String stnUses10G = element
									.getElementsByTagName("stn_bandActive10G").item(0)
									.getTextContent();

							if (stnUses10G.equals("true")) {

								stn_bandActive10G = true;
							} else {
								stn_bandActive10G = false;
							}

						} catch (NullPointerException tooOldConfigFileOrFormatError) {
							/**
							 * In program version 1 there had not been these settings in the xml and not founding em
							 * would cause an exception and dumb values for the preferences. So we have to initialize
							 * these variables and later write a proper configfile which can be used correctly then.
							 */
							stn_bandActive144 = true;
							stn_bandActive432 = true;
							stn_bandActive1240 = true;
							stn_bandActive2300 = true;
							stn_bandActive3400 = true;
							stn_bandActive5600 = true;
							stn_bandActive10G = true;
						}


						System.out.println("[ChatPreferences, info]: Current Element: " + node.getNodeName()
								+ " --> call: " + call + " / " + password + " / " + loginDisplayedName + " / " + qra
								+ " / " + category + " / " + antennaBeamWidthDeg + " / " + maxQRBDefault + " / " + qtfDefault + " qrv144: " + stn_bandActive144);

					}
				}
			}

			/**
			 * Case log synchronizatrion
			 */

			list = doc.getElementsByTagName("logsynch");
			if (list.getLength() != 0) {

				for (int temp = 0; temp < list.getLength(); temp++) {

					Node node = list.item(temp);

					if (node.getNodeType() == Node.ELEMENT_NODE) {

						Element element = (Element) node;

						String logsynchReadFile = element
								.getElementsByTagName("logsynch_fileBasedWkdCallInterpreterFileNameReadOnly").item(0)
								.getTextContent();

						logsynch_fileBasedWkdCallInterpreterFileNameReadOnly = logsynchReadFile;

						String UDPMessageBackupFileName = element
								.getElementsByTagName("logsynch_storeWorkedCallSignsFileNameUDPMessageBackup").item(0)
								.getTextContent();

						logsynch_storeWorkedCallSignsFileNameUDPMessageBackup = UDPMessageBackupFileName;

//						call = call.toLowerCase();
						String fileBasedLogSynchEnabled = element
								.getElementsByTagName("logsynch_fileBasedWkdCallInterpreterEnabled").item(0)
								.getTextContent();

						if (fileBasedLogSynchEnabled.equals("true")) {

							logsynch_fileBasedWkdCallInterpreterEnabled = true;
						} else {
							logsynch_fileBasedWkdCallInterpreterEnabled = false;
						}

						String ucxUDPLogSynchListenerPort = element
								.getElementsByTagName("logsynch_ucxUDPWkdCallListenerPort").item(0).getTextContent();

						if (isNumeric(ucxUDPLogSynchListenerPort)) {
							logsynch_ucxUDPWkdCallListenerPort = Integer.parseInt(ucxUDPLogSynchListenerPort);
						} else {
							logsynch_ucxUDPWkdCallListenerPort = 12060; // TODO: Set default at another place or with
																		// STATIC VAR
						}

						String ucxUDPLogSynchListenerEnabled = element
								.getElementsByTagName("logsynch_ucxUDPWkdCallListenerEnabled").item(0).getTextContent();

						if (ucxUDPLogSynchListenerEnabled.equals("true")) {
							logsynch_ucxUDPWkdCallListenerEnabled = true;
						} else {
							logsynch_ucxUDPWkdCallListenerEnabled = false;
						}

						System.out.println(
								"[ChatPreferences, info]: Set the Universal file based worked-Call Interpreter to : "
										+ logsynch_fileBasedWkdCallInterpreterEnabled);

						System.out.println("[ChatPreferences, info]: Set the UCX UDP Worked Call Listener to : "
								+ logsynch_ucxUDPWkdCallListenerEnabled);

					}
				}
			}

			/**
			 * Case trx synchronizatrion
			 */

			list = doc.getElementsByTagName("trxSynchUCX");
			if (list.getLength() != 0) {

				for (int temp = 0; temp < list.getLength(); temp++) {

					Node node = list.item(temp);

					if (node.getNodeType() == Node.ELEMENT_NODE) {

						Element element = (Element) node;

						String trxSynchUCX = element.getElementsByTagName("trxSynch_ucxLogUDPListenerEnabled").item(0)
								.getTextContent();

						if (trxSynchUCX.equals("true")) {
							trxSynch_ucxLogUDPListenerEnabled = true;
						} else {
							trxSynch_ucxLogUDPListenerEnabled = false;
						}

						String trxSynch_defaultMYQRGValue = element.getElementsByTagName("trxSynch_defaultMYQRGValue")
								.item(0).getTextContent();

						this.getMYQRGFirstCat().setValue(trxSynch_defaultMYQRGValue);

						String trxSynch_defaultMYQRG2Value;
						try{
							trxSynch_defaultMYQRG2Value = element.getElementsByTagName("trxSynch_defaultMYQRG2Value")
									.item(0).getTextContent();

						} catch (Exception notFoundExc) {
							trxSynch_defaultMYQRG2Value = "1296.123.00"; //v1.26, new setting
						}

						this.getMYQRGSecondCat().setValue(trxSynch_defaultMYQRG2Value);

						System.out.println(
								"[ChatPreferences, info]: Set the trx qrg synch to " + trxSynch_ucxLogUDPListenerEnabled
										+ " and default value to " + this.getMYQRGFirstCat().getValue() + " // " + this.getMYQRGSecondCat().getValue());

					}
				}
			}

			/**
			 * Case notifications
			 */

			list = doc.getElementsByTagName("notifications");
			if (list.getLength() != 0) {

				for (int temp = 0; temp < list.getLength(); temp++) {

					Node node = list.item(temp);

					if (node.getNodeType() == Node.ELEMENT_NODE) {

						Element element = (Element) node;

						String notify_simpleAudioNotificationsEnabled = element.getElementsByTagName("notify_SimpleAudioNotificationsEnabled").item(0)
								.getTextContent();

						if (notify_simpleAudioNotificationsEnabled.equals("true")) {
							notify_playSimpleSounds = true;
						} else {
							notify_playSimpleSounds = false;
						}

						String notify_cwAudioNotificationsEnabled = element.getElementsByTagName("notify_CWCallsignAudioNotificationsEnabled").item(0)
								.getTextContent();

						if (notify_cwAudioNotificationsEnabled.equals("true")) {
							notify_playCWCallsignsOnRxedPMs = true;
						} else {
							notify_playCWCallsignsOnRxedPMs = false;
						}

						String notify_voiceAudioNotificationsEnabled = element.getElementsByTagName("notify_VoiceCallsignAudioNotificationsEnabled").item(0)
								.getTextContent();

						if (notify_voiceAudioNotificationsEnabled.equals("true")) {
							notify_playVoiceCallsignsOnRxedPMs = true;
						} else {
							notify_playVoiceCallsignsOnRxedPMs = false;
						}

						try { //try catch block since Version 1.23 due to new prefs to save and read

							String notify_dxClusterServerEnabledFromFile = element.getElementsByTagName("notify_dxClusterServerEnabled").item(0)
									.getTextContent();

							if (notify_dxClusterServerEnabledFromFile.equals("true")) {
								notify_dxClusterServerEnabled = true;
							} else {
								notify_dxClusterServerEnabled = false;
							}

							String notify_DXClusterServerTriggerBearingFromFile = element.getElementsByTagName("notify_DXClusterServerTriggerBearing").item(0)
									.getTextContent();

							if (notify_DXClusterServerTriggerBearingFromFile.equals("true")) {
								notify_DXClusterServerTriggerBearing = true;
							} else {
								notify_DXClusterServerTriggerBearing = false;
							}

							String notify_DXClusterServerTriggerOnQRGDetectFromFile = element.getElementsByTagName("notify_DXClusterServerTriggerOnQRGDetect").item(0)
									.getTextContent();

							if (notify_DXClusterServerTriggerOnQRGDetectFromFile.equals("true")) {
								notify_DXClusterServerTriggerOnQRGDetect = true;
							} else {
								notify_DXClusterServerTriggerOnQRGDetect = false;
							}

							String notify_dxclusterServerPortFromFile = element
									.getElementsByTagName("notify_dxclusterServerPort").item(0).getTextContent();

							if (isNumeric(notify_dxclusterServerPortFromFile)) {
								notify_dxclusterServerPort = Integer.parseInt(notify_dxclusterServerPortFromFile);
							} else {
//								notify_dxclusterServerPort = 8000; Default setted on very top of file
							}

							String notify_DXCSrv_SpottersCallSignFromFile = element.getElementsByTagName("notify_DXCSrv_SpottersCallSign").item(0).getTextContent();
							notify_DXCSrv_SpottersCallSign.set(notify_DXCSrv_SpottersCallSignFromFile);

							String notify_optionalFrequencyPrefixFromFile = element.getElementsByTagName("notify_optionalFrequencyPrefix").item(0).getTextContent();
							notify_optionalFrequencyPrefix.set(notify_optionalFrequencyPrefixFromFile);


						} catch (NullPointerException e) {
							e.printStackTrace();
							System.out.println("[ChatPreferences, Warning:] some monitoring preferences could not be found in "+ storeAndRestorePreferencesFileName +". Using defaults.");
						}

						System.out.println(
								"[ChatPreferences, info]: Set the audionotifications simple: " + notify_playSimpleSounds + ", CW: " + notify_playCWCallsignsOnRxedPMs + ", Voice: " + notify_playVoiceCallsignsOnRxedPMs);

					}
				}
			}


			/**
			 * Case AirScout querier
			 */

			list = doc.getElementsByTagName("AirScoutQuerier");
			if (list.getLength() != 0) {

				for (int temp = 0; temp < list.getLength(); temp++) {

					Node node = list.item(temp);

					if (node.getNodeType() == Node.ELEMENT_NODE) {

						Element element = (Element) node;

						String asQuerierUDPEnabled = element.getElementsByTagName("asQry_airScoutCommunicationEnabled")
								.item(0).getTextContent();

						if (asQuerierUDPEnabled.equals("true")) {
							AirScout_asUDPListenerEnabled = true;
						} else {
							AirScout_asUDPListenerEnabled = false;
						}

						this.setAirScout_asServerNameString(
								element.getElementsByTagName("asQry_airScoutServerName").item(0).getTextContent());
						this.setAirScout_asClientNameString(
								element.getElementsByTagName("asQry_airScoutClientName").item(0).getTextContent());
						this.setAirScout_asCommunicationPort(Integer.parseInt(
								element.getElementsByTagName("asQry_airScoutUDPPort").item(0).getTextContent()));
						this.setAirScout_asBandString(
								element.getElementsByTagName("asQry_airScoutBandValue").item(0).getTextContent());
//						this.getMYQRG().addListener((observable, oldValue, newValue) -> {
//						    System.out.println("[Chatprefs.java, Info]: MYQRG changed from " + oldValue + " to " + newValue);
////						    this.getMYQRG().
////						    txt_ownqrg.setText(newValue);
//						});

//						
						System.out.println("[ChatPreferences, info]: Set the Airscout Querier to " + asQuerierUDPEnabled
								+ " for qrg " + AirScout_asBandString);

					}
				}
			}

			/**
			 * Case shortCuts
			 */

			list = doc.getElementsByTagName("shortCuts");
			String[] shortCutsExtractedOutOfXML = new String[0];

			if (list.getLength() != 0) {

				ArrayList<String> textShorts = new ArrayList<String>();

				for (int temp = 0; temp < list.getLength(); temp++) {

					Node node = list.item(temp);

					Element element = (Element) node;

					for (int i = 0; i < element.getChildNodes().getLength(); i++) {
						if (element.getChildNodes().item(i).getNodeType() == Node.ELEMENT_NODE) {
							textShorts.add(element.getChildNodes().item(i).getTextContent());
							shortCutsExtractedOutOfXML = textShorts.toArray(String[]::new); // String[]::new = API
																							// Collection.toArray(IntFunction<T[]>
																							// generator)
//								System.out.println(element.getChildNodes().item(i).getNodeType() + ": " + element.getChildNodes().item(i).getNodeName() + " - " + element.getChildNodes().item(i).getTextContent());
						}
					}

				}

				// if praktiKST found Shortcuts in the configfile, set it to the preferences
//				shortcuts = shortCutsExtractedOutOfXML;
				// else use defaults (as the initialization vars had been)
				for (int i = 0; i < shortCutsExtractedOutOfXML.length; i++) {
					lst_txtShortCutBtnList.add(shortCutsExtractedOutOfXML[i]);
					System.out.println("[Chatpreferences, Info]: Setting Short " + i + " \""
							+ shortCutsExtractedOutOfXML[i] + "\"");
				}
			}

			/**
			 * Case textSnippets
			 */

			list = doc.getElementsByTagName("textSnippets");
			String[] textSnippetsExtractedOutOfXML = new String[0];

			if (list.getLength() != 0) {

				ArrayList<String> textShorts = new ArrayList<String>();

				for (int temp = 0; temp < list.getLength(); temp++) {

					Node node = list.item(temp);

					Element element = (Element) node;

					for (int i = 0; i < element.getChildNodes().getLength(); i++) {
						if (element.getChildNodes().item(i).getNodeType() == Node.ELEMENT_NODE) {
							textShorts.add(element.getChildNodes().item(i).getTextContent());
							textSnippetsExtractedOutOfXML = textShorts.toArray(String[]::new); // String[]::new = API
																								// Collection.toArray(IntFunction<T[]>
																								// generator)
//								System.out.println(element.getChildNodes().item(i).getNodeType() + ": " + element.getChildNodes().item(i).getNodeName() + " - " + element.getChildNodes().item(i).getTextContent());
						}
					}

				}

				// if praktiKST found Shortcuts in the configfile, set it to the preferences
//				textSnippets = textSnippetsExtractedOutOfXML;
				// else use defaults (as the initialization vars had been)
				for (int i = 0; i < textSnippetsExtractedOutOfXML.length; i++) {
					lst_txtSnipList.add(textSnippetsExtractedOutOfXML[i]);
					System.out.println("[Chatpreferences, Info]: Setting Snip " + i + " \""
							+ textSnippetsExtractedOutOfXML[i] + "\"");
				}
			}

			/**
			 * Case beaconCQ
			 */

			list = doc.getElementsByTagName("beaconCQ");
			if (list.getLength() != 0) {

				for (int temp = 0; temp < list.getLength(); temp++) {

					Node node = list.item(temp);

					if (node.getNodeType() == Node.ELEMENT_NODE) {

						Element element = (Element) node;

						String beaconCQEnabled = element.getElementsByTagName("beaconCQEnabled").item(0)
								.getTextContent();

						if (beaconCQEnabled.equals("true")) {

							bcn_beaconsEnabledMainCat = true;
						} else {
							bcn_beaconsEnabledMainCat = false;
						}

						String beaconCQIntervalMinutes = element.getElementsByTagName("beaconCQIntervalMinutes").item(0)
								.getTextContent();

						if (isNumeric(beaconCQIntervalMinutes)) {
							bcn_beaconIntervalInMinutesMainCat = Integer.parseInt(beaconCQIntervalMinutes);
						} else {
							bcn_beaconIntervalInMinutesMainCat = 20; // Default value, TODO: Set this in default list
						}

						String beaconCQText = element.getElementsByTagName("beaconCQText").item(0).getTextContent();
						this.setBcn_beaconTextMainCat(beaconCQText);

						String beaconCQEnabledSecondCat;
						try {
							beaconCQEnabledSecondCat = element.getElementsByTagName("beaconCQEnabledSecondCat").item(0)
									.getTextContent();

							if (beaconCQEnabledSecondCat.equals("true")) {

								bcn_beaconsEnabledSecondCat = true;
							} else {
								bcn_beaconsEnabledSecondCat = false;
							}

							String beaconCQIntervalMinutesSecondCat = element.getElementsByTagName("beaconCQIntervalMinutesSecondCat").item(0)
									.getTextContent();

							if (isNumeric(beaconCQIntervalMinutesSecondCat)) {
								bcn_beaconIntervalInMinutesSecondCat = Integer.parseInt(beaconCQIntervalMinutesSecondCat);
							} else {
								bcn_beaconIntervalInMinutesSecondCat = 3; // Default value, TODO: Set this in default list
							}

							String beaconCQTextSecondText = element.getElementsByTagName("beaconCQTextSecondText").item(0).getTextContent();
							this.setBcn_beaconTextSecondCat(beaconCQTextSecondText);

						} catch (Exception previousVersionException) {
							bcn_beaconsEnabledSecondCat = false;
							bcn_beaconIntervalInMinutesSecondCat = 3;
							this.setBcn_beaconTextSecondCat(this.getStn_loginCallSign() + " is QRV, pse sked for contact");

						}
//
						System.out.println("[ChatPreferences, info]: set the beacon text to: " + beaconCQText  + " and " + this.getBcn_beaconTextSecondCat());

					}
				}
			}

			/**
			 * Case beaconUnworkedstations
			 * 
			 */

			list = doc.getElementsByTagName("beaconUnworkedstations");
			if (list.getLength() != 0) {

				for (int temp = 0; temp < list.getLength(); temp++) {

					Node node = list.item(temp);

					if (node.getNodeType() == Node.ELEMENT_NODE) {

						Element element = (Element) node;
//
						String beaconUnworkedstationsText = element.getElementsByTagName("beaconUnworkedstationsText")
								.item(0).getTextContent();
						messageHandling_unworkedStnRequesterBeaconsText = beaconUnworkedstationsText;

						String beaconUnworkedstationsIntervalMinutes = element
								.getElementsByTagName("beaconUnworkedstationsIntervalMinutes").item(0).getTextContent();

						if (isNumeric(beaconUnworkedstationsIntervalMinutes)) {
							messageHandling_unworkedStnRequesterBeaconsInterval = Integer
									.parseInt(beaconUnworkedstationsIntervalMinutes);
						} else {
							messageHandling_unworkedStnRequesterBeaconsInterval = 20;
						}

						String beaconUnworkedstationsEnabled = element
								.getElementsByTagName("beaconUnworkedstationsEnabled").item(0).getTextContent();

						if (beaconUnworkedstationsEnabled.equals("true")) {
							messageHandling_unworkedStnRequesterBeaconsEnabled = true;
						} else {
							messageHandling_unworkedStnRequesterBeaconsEnabled = false;
						}
						
						String beaconUnworkedstationsPrefix = element
								.getElementsByTagName("beaconUnworkedstationsPrefix").item(0).getTextContent();

						messageHandling_beaconUnworkedstationsPrefix = beaconUnworkedstationsPrefix;

					}
				}
				System.out.println("[ChatPreferences, info]: set the unworked stn beacon text to: "
						+ messageHandling_unworkedStnRequesterBeaconsText);
			}



			/***********************************************
			 *
			 * case messageHandling
			 *
			 ***********************************************/
			list = doc.getElementsByTagName("messageHandling");
			if (list.getLength() != 0) {

				for (int temp = 0; temp < list.getLength(); temp++) {

					Node node = list.item(temp);

					if (node.getNodeType() == Node.ELEMENT_NODE) {

						Element element = (Element) node;

						try{

							String autoAnswerText = element.getElementsByTagName("autoAnswerText").item(0)
									.getTextContent();

							this.setMessageHandling_autoAnswerTextMainCat(autoAnswerText);

							String autoAnswerEnabled = element.getElementsByTagName("autoAnswerEnabled").item(0)
									.getTextContent();

							if (autoAnswerEnabled.equals("true")) {
								this.setMessageHandling_autoAnswerEnabled(true);
							} else {
								this.setMessageHandling_autoAnswerEnabled(false);
							}

							String autoAnswerToQrgRequestEnabled = element.getElementsByTagName("autoAnswerToQrgRequestEnabled").item(0)
									.getTextContent();

							if (autoAnswerToQrgRequestEnabled.equals("true")) {
								this.setMessageHandling_autoAnswerToQRGRequestEnabled(true);
							} else {
								this.setMessageHandling_autoAnswerToQRGRequestEnabled(false);
							}


						}

						catch (NullPointerException tooOldConfigFileOrFormatError) {
							/**
							 * In program version 1.24 there had not been these settings in the xml and not founding em
							 * would cause an exception and dumb values for the preferences. So we have to initialize
							 * these variables and later write a proper configfile which can be used correctly then.
							 *
							 * So THESE ARE DEFAULTS for the new variables
							 */

							tooOldConfigFileOrFormatError.printStackTrace();
							this.setMessageHandling_autoAnswerTextMainCat("Hi, sry I am not qrv, just testing new features of KST4Contest " +  ApplicationConstants.APPLICATION_CURRENTVERSIONNUMBER);
							this.setMessageHandling_autoAnswerEnabled(false);
							this.setMessageHandling_autoAnswerToQRGRequestEnabled(true);
						}

						try {
							String autoAnswerTextSecondCat = element.getElementsByTagName("autoAnswerTextSecondCat").item(0)
									.getTextContent();

							this.setMessageHandling_autoAnswerTextSecondCat(autoAnswerTextSecondCat);


							String autoAnswerEnabledSecondCat = element.getElementsByTagName("autoAnswerEnabledSecondCat").item(0)
									.getTextContent();

							if (autoAnswerEnabledSecondCat.equals("true")) {
								this.setMessageHandling_autoAnswerEnabledSecondCat(true);
							} else {
								this.setMessageHandling_autoAnswerEnabledSecondCat(false);
							}
						} catch (Exception prevVersionExc) {

							String autoAnswerTextSecondCat = "[KST4Contest autoreply] change me ... ";
							this.setMessageHandling_autoAnswerEnabledSecondCat(false);

						}


					}
				}
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

						try{

							String GUIscn_ChatwindowMainSceneSizeHW = element.getElementsByTagName("GUIscn_ChatwindowMainSceneSizeHW").item(0)
									.getTextContent();

							for (int i = 0; i < (GUIscn_ChatwindowMainSceneSizeHW.split(";").length); i++) {
								this.getGUIscn_ChatwindowMainSceneSizeHW()[i] =
										Double.parseDouble(GUIscn_ChatwindowMainSceneSizeHW.split(";")[i]);
							}

							System.out.println(
									"[ChatPreferences, info]: Set the GUIscn_ChatwindowMainSceneSizeHW size to " + GUIclusterAndQSOMonStage_SceneSizeHW);


							String GUIclusterAndQSOMonStage_SceneSizeHW = element.getElementsByTagName("GUIclusterAndQSOMonStage_SceneSizeHW").item(0)
									.getTextContent();

							for (int i = 0; i < (GUIclusterAndQSOMonStage_SceneSizeHW.split(";").length); i++) {
								this.getGUIclusterAndQSOMonStage_SceneSizeHW()[i] =
										Double.parseDouble(GUIclusterAndQSOMonStage_SceneSizeHW.split(";")[i]);
							}

							String GUIselectedCallSignSplitPane_dividerposition = element.getElementsByTagName("GUIselectedCallSignSplitPane_dividerposition").item(0)
									.getTextContent();
							this.setGUIselectedCallSignSplitPane_dividerposition(csvStringToDoubleArray(GUIselectedCallSignSplitPane_dividerposition));

							String GUImainWindowLeftSplitPane_dividerposition = element.getElementsByTagName("GUImainWindowLeftSplitPane_dividerposition").item(0)
									.getTextContent();
							this.setGUImainWindowLeftSplitPane_dividerposition(csvStringToDoubleArray(GUImainWindowLeftSplitPane_dividerposition));

							String GUImessageSectionSplitpane_dividerposition = element.getElementsByTagName("GUImessageSectionSplitpane_dividerposition").item(0)
									.getTextContent();
							this.setGUImessageSectionSplitpane_dividerposition(csvStringToDoubleArray(GUImessageSectionSplitpane_dividerposition));

							String GUImainWindowRightSplitPane_dividerposition = element.getElementsByTagName("GUImainWindowRightSplitPane_dividerposition").item(0)
									.getTextContent();
							this.setGUImainWindowRightSplitPane_dividerposition(csvStringToDoubleArray(GUImainWindowRightSplitPane_dividerposition));

							String GUIpnl_directedMSGWin_dividerpositionDefault = element.getElementsByTagName("GUIpnl_directedMSGWin_dividerpositionDefault").item(0)
									.getTextContent();
							this.setGUIpnl_directedMSGWin_dividerpositionDefault(csvStringToDoubleArray(GUIpnl_directedMSGWin_dividerpositionDefault));




//							System.out.println(
//									"[ChatPreferences, info]: Set the GUIclusterAndQSOMonStage_SceneSizeHW size to " + GUIclusterAndQSOMonStage_SceneSizeHW);

						}

						catch (NullPointerException tooOldConfigFileOrFormatError) {
							/**
							 * In program version 1.2 there had not been these settings in the xml and not founding em
							 * would cause an exception and dumb values for the preferences. So we have to initialize
							 * these variables and later write a proper configfile which can be used correctly then.
							 *
							 * So THESE ARE DEFAULTS
							 */

							tooOldConfigFileOrFormatError.printStackTrace();
							GUIscn_ChatwindowMainSceneSizeHW = new double[] {768, 1234};
							GUIclusterAndQSOMonStage_SceneSizeHW = new double[] {700, 500};
							GUIstage_updateStage_SceneSizeHW = new double[] {640, 480};
							GUIsettingsStageSceneSizeHW = new double[] {720, 768};

							GUIselectedCallSignSplitPane_dividerposition = new double[]{0.9};
							setGUImainWindowLeftSplitPane_dividerposition(new double[]{0.7});
							GUImessageSectionSplitpane_dividerposition = new double[]{0.5};
							GUImainWindowRightSplitPane_dividerposition = new double[]{0.8};
							GUIpnl_directedMSGWin_dividerpositionDefault = new double[]{0.8};
//							GUImainWindowLeftSplitPane_dividerposition
						}
					}
				}
			}

			/***********************************************
			 *
			 * case read guiSaveableOptions
			 *
			 ***********************************************/
			list = doc.getElementsByTagName("guiSaveableOptions");
			if (list.getLength() != 0) {

				for (int temp = 0; temp < list.getLength(); temp++) {

					Node node = list.item(temp);

					if (node.getNodeType() == Node.ELEMENT_NODE) {

						Element element = (Element) node;

						try{

							String guiOptions_defaultFilterNothing = element.getElementsByTagName("guiOptions_defaultFilterNothing").item(0)
									.getTextContent();

							if (guiOptions_defaultFilterNothing.equals("true")) {
								this.setGuiOptions_defaultFilterNothing(true);
							} else {
								this.setGuiOptions_defaultFilterNothing(false);
							}

							String guiOptions_defaultFilterPmToMe = element.getElementsByTagName("guiOptions_defaultFilterPmToMe").item(0)
									.getTextContent();

							if (guiOptions_defaultFilterPmToMe.equals("true")) {
								this.setGuiOptions_defaultFilterPmToMe(true);
							} else {
								this.setGuiOptions_defaultFilterNothing(false);
							}

							String guiOptions_defaultFilterPmToOther = element.getElementsByTagName("guiOptions_defaultFilterPmToOther").item(0)
									.getTextContent();

							if (guiOptions_defaultFilterPmToOther.equals("true")) {
								this.setGuiOptions_defaultFilterPmToOther(true);
							} else {
								this.setGuiOptions_defaultFilterPmToOther(false);
							}

							String guiOptions_defaultFilterPublicMsgs = element.getElementsByTagName("guiOptions_defaultFilterPublicMsgs").item(0)
									.getTextContent();

							if (guiOptions_defaultFilterPublicMsgs.equals("true")) {
								this.setGuiOptions_defaultFilterPublicMsgs(true);
							} else {
								this.setGuiOptions_defaultFilterPublicMsgs(false);
							}


						}





						catch (NullPointerException tooOldConfigFileOrFormatError) {
							/**
							 * In program version 1.24 there had not been these settings in the xml and not founding em
							 * would cause an exception and dumb values for the preferences. So we have to initialize
							 * these variables and later write a proper configfile which can be used correctly then.
							 *
							 * So THESE ARE DEFAULTS for the new variables
							 */

							tooOldConfigFileOrFormatError.printStackTrace();
							this.setGuiOptions_defaultFilterPmToMe(true);
						}
					}
				}
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

	/**
	 * 
	 * If the file-reading goes wrong, set the defaults
	 */
	public void setPreferencesDefaults() {
		System.out.println("[ChatPreferences, Info]: restoring prefs from defaults");

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

}
