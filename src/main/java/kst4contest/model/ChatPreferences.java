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
import org.w3c.dom.Comment;
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

	String loginCallSign = "do5amf";
	String loginPassword = "";
	String loginName = "Marc";
	String loginLocator = "jn49fk";
	ChatCategory loginChatCategory = new ChatCategory(2);
	IntegerProperty actualQTF = new SimpleIntegerProperty(360); // will be updated by user at runtime!

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
	StringProperty MYQRG = new SimpleStringProperty(); // own qrg will be set by user entry or ucxlog if trx Synch is
														// activated
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

	boolean bcn_beaconsEnabled = true;
	int bcn_beaconIntervalInMinutes = 20;
	String bcn_beaconText = "Hi, pse call us";

	/**
	 * Unworked station requester prefs
	 */

	boolean unwkd_unworkedStnRequesterBeaconsEnabled;
	int unwkd_unworkedStnRequesterBeaconsInterval;
	String unwkd_unworkedStnRequesterBeaconsText;
	String unwkd_beaconUnworkedstationsPrefix;

//	public String getMYQRG() {
//		return MYQRG;
//	}
//
//	public void setMYQRG(String mYQRG) {
//		
//		MYQRG = mYQRG;
//	}

	public String getLoginCallSign() {
		return loginCallSign;
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

	/**
	 * actualQTF, int, QTF in degrees
	 * 
	 * @param actualQTF, int, QTF in degrees
	 */

	public StringProperty getMYQRG() {
		return MYQRG;
	}

	public IntegerProperty getActualQTF() {
		return actualQTF;
	}

	public void setActualQTF(IntegerProperty actualQTF) {
		this.actualQTF = actualQTF;
	}

	public void setMYQRG(StringProperty mYQRG) {
		MYQRG = mYQRG;
	}

	public void setLoginCallSign(String loginCallSign) {
		this.loginCallSign = loginCallSign;
	}

	public String getLoginPassword() {
		return loginPassword;
	}

	public void setLoginPassword(String loginPassword) {
		this.loginPassword = loginPassword;
	}

	public String getLoginName() {
		return loginName;
	}

	public void setLoginName(String loginName) {
		this.loginName = loginName;
	}

	public String getLoginLocator() {
		return loginLocator;
	}

	public void setLoginLocator(String loginLocator) {
		this.loginLocator = loginLocator;
	}

	public ChatCategory getLoginChatCategory() {
		return loginChatCategory;
	}

	public void setLoginChatCategory(ChatCategory loginChatCategory) {
		this.loginChatCategory = loginChatCategory;
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

	public boolean isBcn_beaconsEnabled() {
		return bcn_beaconsEnabled;
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

	public void setBcn_beaconsEnabled(boolean bcn_beaconsEnabled) {
		this.bcn_beaconsEnabled = bcn_beaconsEnabled;
	}

	public int getBcn_beaconIntervalInMinutes() {
		return bcn_beaconIntervalInMinutes;
	}

	public void setBcn_beaconIntervalInMinutes(int bcn_beaconIntervalInMinutes) {
		this.bcn_beaconIntervalInMinutes = bcn_beaconIntervalInMinutes;
	}

	public String getBcn_beaconText() {
		return bcn_beaconText;
	}

	public void setBcn_beaconText(String bcn_beaconText) {

		this.bcn_beaconText = bcn_beaconText;
	}

	 
	
	public String getUnwkd_beaconUnworkedstationsPrefix() {
		return unwkd_beaconUnworkedstationsPrefix;
	}

	public void setUnwkd_beaconUnworkedstationsPrefix(String unwkd_beaconUnworkedstationsPrefix) {
		this.unwkd_beaconUnworkedstationsPrefix = unwkd_beaconUnworkedstationsPrefix;
	}

	public boolean isUnwkd_unworkedStnRequesterBeaconsEnabled() {
		return unwkd_unworkedStnRequesterBeaconsEnabled;
	}

	public void setUnwkd_unworkedStnRequesterBeaconsEnabled(boolean unwkd_unworkedStnRequesterBeaconsEnabled) {
		this.unwkd_unworkedStnRequesterBeaconsEnabled = unwkd_unworkedStnRequesterBeaconsEnabled;
	}

	public int getUnwkd_unworkedStnRequesterBeaconsInterval() {
		return unwkd_unworkedStnRequesterBeaconsInterval;
	}

	public void setUnwkd_unworkedStnRequesterBeaconsInterval(int unwkd_unworkedStnRequesterBeaconsInterval) {
		this.unwkd_unworkedStnRequesterBeaconsInterval = unwkd_unworkedStnRequesterBeaconsInterval;
	}

	public String getUnwkd_unworkedStnRequesterBeaconsText() {
		return unwkd_unworkedStnRequesterBeaconsText;
	}

	public void setUnwkd_unworkedStnRequesterBeaconsText(String unwkd_unworkedStnRequesterBeaconsText) {
		this.unwkd_unworkedStnRequesterBeaconsText = unwkd_unworkedStnRequesterBeaconsText;
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
		      LoginCallSign.setTextContent(this.getLoginCallSign());
		      station.appendChild(LoginCallSign);
		      

		      Element LoginPassword = doc.createElement("LoginPassword");
		      LoginPassword.setTextContent(this.getLoginPassword());
		      station.appendChild(LoginPassword);

		      Element LoginDisplayedName = doc.createElement("LoginDisplayedName");
		      LoginDisplayedName.setTextContent(this.getLoginName());
		      station.appendChild(LoginDisplayedName);

		      Element LoginLocator = doc.createElement("LoginLocator");
		      LoginLocator.setTextContent(this.getLoginLocator());
		      station.appendChild(LoginLocator);

		      Element ChatCategory = doc.createElement("ChatCategory");
		      ChatCategory.setTextContent(this.getLoginChatCategory().getCategoryNumber()+"");
		      station.appendChild(ChatCategory);

//		      Element salary = doc.createElement("salary");
//		      salary.setAttribute("currency", "USD");
//		      salary.setTextContent("5000");
//		      staff.appendChild(salary);

		      // add xml comment
		      Comment comment = doc.createComment(
		              "for special characters like < &, need CDATA");
//		      staff.appendChild(comment);

//		      Element bio = doc.createElement("bio");
		      // add xml CDATA
		      
//		      staff.appendChild(bio);
		      
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
		      trxSynch_defaultMYQRGValue.setTextContent(this.getMYQRG().getValue());
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
		      beaconCQText.setTextContent(this.getBcn_beaconText());
		      beaconCQ.appendChild(beaconCQText);
		      
		      Element beaconCQIntervalMinutes = doc.createElement("beaconCQIntervalMinutes");
		      beaconCQIntervalMinutes.setTextContent(this.getBcn_beaconIntervalInMinutes()+"");
		      beaconCQ.appendChild(beaconCQIntervalMinutes);
		      
		      Element beaconCQEnabled = doc.createElement("beaconCQEnabled");
		      beaconCQEnabled.setTextContent(this.isBcn_beaconsEnabled()+"");
		      beaconCQ.appendChild(beaconCQEnabled);
		      
		      
		      
		      /**
		       * Beacon Unworked Stations
		       */

		      Element beaconUnworkedstations = doc.createElement("beaconUnworkedstations");
		      rootElement.appendChild(beaconUnworkedstations);
		      
		      
		      Element beaconUnworkedstationsText = doc.createElement("beaconUnworkedstationsText");
		      beaconUnworkedstationsText.setTextContent(this.getUnwkd_unworkedStnRequesterBeaconsText());
		      beaconUnworkedstations.appendChild(beaconUnworkedstationsText);
		      
		      Element beaconUnworkedstationsIntervalMinutes = doc.createElement("beaconUnworkedstationsIntervalMinutes");
		      beaconUnworkedstationsIntervalMinutes.setTextContent(this.getUnwkd_unworkedStnRequesterBeaconsInterval()+"");
		      beaconUnworkedstations.appendChild(beaconUnworkedstationsIntervalMinutes);

		      Element beaconUnworkedstationsEnabled = doc.createElement("beaconUnworkedstationsEnabled");
		      beaconUnworkedstationsEnabled.setTextContent(this.isUnwkd_unworkedStnRequesterBeaconsEnabled()+"");
		      beaconUnworkedstations.appendChild(beaconUnworkedstationsEnabled);
		      
		      Element beaconUnworkedstationsPrefix = doc.createElement("beaconUnworkedstationsPrefix");
		      beaconUnworkedstationsPrefix.setTextContent(this.getUnwkd_beaconUnworkedstationsPrefix());
		      beaconUnworkedstations.appendChild(beaconUnworkedstationsPrefix);
		      
		      

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
						loginCallSign = call;

//						call = call.toLowerCase();
						String password = element.getElementsByTagName("LoginPassword").item(0).getTextContent();
						loginPassword = password;

						String loginDisplayedName = element.getElementsByTagName("LoginDisplayedName").item(0)
								.getTextContent();
						loginName = loginDisplayedName;

						String qra = element.getElementsByTagName("LoginLocator").item(0).getTextContent();
						loginLocator = qra;

						String category = element.getElementsByTagName("ChatCategory").item(0).getTextContent();

						if (isNumeric(category)) {
							ChatCategory chatCategory = new ChatCategory(Integer.parseInt(category));
							loginChatCategory = chatCategory;
						} else {

							loginChatCategory = new ChatCategory(2); // TODO: Set this default at another place
						}

						System.out.println("[ChatPreferences, info]: Current Element: " + node.getNodeName()
								+ " --> call: " + call + " / " + password + " / " + loginDisplayedName + " / " + qra
								+ " / " + category);

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

						this.getMYQRG().setValue(trxSynch_defaultMYQRGValue);

//						this.getMYQRG().addListener((observable, oldValue, newValue) -> {
//						    System.out.println("[Chatprefs.java, Info]: MYQRG changed from " + oldValue + " to " + newValue);
////						    this.getMYQRG().
////						    txt_ownqrg.setText(newValue);
//						});

//						
						System.out.println(
								"[ChatPreferences, info]: Set the trx qrg synch to " + trxSynch_ucxLogUDPListenerEnabled
										+ " and default value to " + this.getMYQRG().getValue());

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

							bcn_beaconsEnabled = true;
						} else {
							bcn_beaconsEnabled = false;
						}

						String beaconCQIntervalMinutes = element.getElementsByTagName("beaconCQIntervalMinutes").item(0)
								.getTextContent();

						if (isNumeric(beaconCQIntervalMinutes)) {
							bcn_beaconIntervalInMinutes = Integer.parseInt(beaconCQIntervalMinutes);
						} else {
							bcn_beaconIntervalInMinutes = 20; // Default value, TODO: Set this in default list
						}

						String beaconCQText = element.getElementsByTagName("beaconCQText").item(0).getTextContent();
						this.setBcn_beaconText(beaconCQText);

//						
						System.out.println("[ChatPreferences, info]: set the beacon text to: " + beaconCQText);

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
						unwkd_unworkedStnRequesterBeaconsText = beaconUnworkedstationsText;

						String beaconUnworkedstationsIntervalMinutes = element
								.getElementsByTagName("beaconUnworkedstationsIntervalMinutes").item(0).getTextContent();

						if (isNumeric(beaconUnworkedstationsIntervalMinutes)) {
							unwkd_unworkedStnRequesterBeaconsInterval = Integer
									.parseInt(beaconUnworkedstationsIntervalMinutes);
						} else {
							unwkd_unworkedStnRequesterBeaconsInterval = 20;
						}

						String beaconUnworkedstationsEnabled = element
								.getElementsByTagName("beaconUnworkedstationsEnabled").item(0).getTextContent();

						if (beaconUnworkedstationsEnabled.equals("true")) {
							unwkd_unworkedStnRequesterBeaconsEnabled = true;
						} else {
							unwkd_unworkedStnRequesterBeaconsEnabled = false;
						}
						
						String beaconUnworkedstationsPrefix = element
								.getElementsByTagName("beaconUnworkedstationsPrefix").item(0).getTextContent();

						unwkd_beaconUnworkedstationsPrefix = beaconUnworkedstationsPrefix;

					}
				}
				System.out.println("[ChatPreferences, info]: set the unworked stn beacon text to: "
						+ unwkd_unworkedStnRequesterBeaconsText);
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
