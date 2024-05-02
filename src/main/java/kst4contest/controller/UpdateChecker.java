package kst4contest.controller;

import java.io.InputStream;

import kst4contest.ApplicationConstants;

import kst4contest.model.UpdateInformation;
import kst4contest.utils.ApplicationFileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

public class UpdateChecker {

    public static void main(String[] args) {
//        new UpdateChecker(null).parseUpdateXMLFile();

        if (new UpdateChecker(null).downloadLatestVersionInfoXML()) {
//            ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME,ApplicationConstants.VERSIONINFDOWNLOADEDLOCALFILE,ApplicationConstants.VERSIONINFDOWNLOADEDLOCALFILE);
        }
            new UpdateChecker(null).parseUpdateXMLFile();
    }

    public UpdateChecker(ChatController chatController) {


        System.out.println("[Updatechecker: checking for updates...]");
//        double currentVersionNumber = ApplicationConstants.APPLICATION_CURRENTVERSIONNUMBER;

    }

    String versionInfoDownloadedFromServerFileName = ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, ApplicationConstants.VERSIONINFDOWNLOADEDLOCALFILE);
    String versionInfoXMLURLAtServer = ApplicationConstants.VERSIONINFOURLFORUPDATES_KST4CONTEST;
//    double currentVersion = ApplicationConstants.APPLICATION_CURRENTVERSIONNUMBER;
    //DOWNLOAD from URL, then parse, then do anything with it...

    /**
     * Downloads the versioninfo-xml-file from a webserver to local. Returns true if download was successful, else false
     *
     * @return true if successful
     */
    public boolean downloadLatestVersionInfoXML() {

        try {

            InputStream in = new URL(versionInfoXMLURLAtServer).openStream();
            Files.copy(in, Paths.get(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/"+ApplicationConstants.VERSIONINFDOWNLOADEDLOCALFILE)), StandardCopyOption.REPLACE_EXISTING);

            in.close();

//            System.out.println(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/"+ApplicationConstants.VERSIONINFDOWNLOADEDLOCALFILE));
//            ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME,ApplicationFileUtils.get,ApplicationConstants.VERSIONINFDOWNLOADEDLOCALFILE);

        } catch (Exception e) {
            System.out.println("ERROR DOWNLOADING!" + e);
            return false;
        }
        return true;
    }

    public UpdateInformation parseUpdateXMLFile() {

        UpdateInformation updateInfos = new UpdateInformation();


        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME,"/"+ApplicationConstants.VERSIONINFDOWNLOADEDLOCALFILE,ApplicationConstants.VERSIONINFDOWNLOADEDLOCALFILE);

//        System.out.println("[Updatecker, Info]: restoring prefs from file " + versionInfoDownloadedFromServerFileName);

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (ParserConfigurationException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        try {

            File xmlConfigFile = new File(versionInfoDownloadedFromServerFileName);

            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(xmlConfigFile);

            /**
             * latestVersion on server
             */
            NodeList list = doc.getElementsByTagName("latestVersion");
            if (list.getLength() != 0) {

                for (int temp = 0; temp < list.getLength(); temp++) {

                    Node node = list.item(temp);

                    if (node.getNodeType() == Node.ELEMENT_NODE) {

                        Element element = (Element) node;
                        updateInfos.setLatestVersionNumberOnServer(Double.parseDouble(element.getElementsByTagName("versionNumber").item(0).getTextContent()));
                        updateInfos.setAdminMessage(element.getElementsByTagName("adminMessage").item(0).getTextContent());
                        updateInfos.setMajorChanges(element.getElementsByTagName("majorChanges").item(0)
                                .getTextContent());
                        updateInfos.setLatestVersionPathOnWebserver(element.getElementsByTagName("latestVersionPathOnWebserver").item(0).getTextContent());
//                        System.out.println(updateInfos.toString());
                    }
                }
            }

/**
 * Section changeLog
 */

            list = doc.getElementsByTagName("changeLog");
            ArrayList<String[]> changeLogArrayList = new ArrayList<String[]>();

            if (list.getLength() != 0) {

                for (int temp = 0; temp < list.getLength(); temp++) {

                    Node node = list.item(temp);

                    Element element = (Element) node;
                        int childNodeCounter = 0; //need an extra counter due to childnodes are counted...no idea, how
                        String[] aChangeLogEntry = new String[7];
                        aChangeLogEntry[0] = "";
                        aChangeLogEntry[1] = "Date: ";
                        aChangeLogEntry[2] = "Desc: ";
                        aChangeLogEntry[3] = "Added: ";
                        aChangeLogEntry[4] = "Changed: ";
                        aChangeLogEntry[5] = "Fixed: ";
                        aChangeLogEntry[6] = "Removed: ";

                    for (int i = 0; i < element.getChildNodes().getLength(); i++) {

                        if (element.getChildNodes().item(i).getNodeType() == Node.ELEMENT_NODE) {
//                            System.out.println(element.getChildNodes().item(i).getTextContent() + " <<<<<<<<<<<<<<<<<< " + i + " / " + childNodeCounter);
//                            System.out.println(element.getChildNodes().item(i).getNodeName());
                            aChangeLogEntry[childNodeCounter] = aChangeLogEntry[childNodeCounter] + element.getChildNodes().item(i).getTextContent();
                            childNodeCounter++;
                        }
                    }
                    changeLogArrayList.add(aChangeLogEntry);
                }
                updateInfos.setChangeLog(changeLogArrayList);
            }

/**
 * Section Buglist
 */

            list = doc.getElementsByTagName("bug");
            ArrayList<String[]> bugFixArrayList = new ArrayList<String[]>();

            if (list.getLength() != 0) {

                for (int temp = 0; temp < list.getLength(); temp++) {

                    Node node = list.item(temp);

                    Element element = (Element) node;
                    int childNodeCounter = 0; //need an extra counter due to childnodes are counted...no idea, how
                    String[] aChangeLogEntry = new String[3];
                    aChangeLogEntry[0] = "";
                    aChangeLogEntry[1] = "State: ";


                    for (int i = 0; i < element.getChildNodes().getLength(); i++) {

                        if (element.getChildNodes().item(i).getNodeType() == Node.ELEMENT_NODE) {
                            System.out.println(element.getChildNodes().item(i).getTextContent() + " <<<<<<<<<<<<<<<<<< " + i + " / " + childNodeCounter);
//                            System.out.println(element.getChildNodes().item(i).getNodeName());
                            aChangeLogEntry[childNodeCounter] = aChangeLogEntry[childNodeCounter] + element.getChildNodes().item(i).getTextContent();
                            childNodeCounter++;
                        }
                    }
                    bugFixArrayList.add(aChangeLogEntry);
                }
                updateInfos.setBugList(bugFixArrayList);
            }


        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }

        String[] testEntry = new String[7];
        testEntry[0] = "0.99";
        testEntry[1] = "2022-09";
        testEntry[2] = "researched the Chatprotocol";
        testEntry[3] = "addednothing";
        testEntry[4] = "changedsome";
        testEntry[5] = "fixedxed";
        testEntry[6] = "removedYourMom";

        String[] testEntry2 = new String[7];
        testEntry2[0] = "0.29";
        testEntry2[1] = "2033-09";
        testEntry2[2] = "tested";
        testEntry2[3] = "addednotashing";
        testEntry2[4] = "changeasdsome";
        testEntry2[5] = "fixedxeds";
        testEntry2[6] = "removedYosssurMom";

//        changeLogArrayList.add(testEntry);
//        changeLogArrayList.add(testEntry2);








        return updateInfos;
    }

    @Override
    public String toString() {
        String toString = "";

//        toString += this.currentVersion;

        return toString;
    }
}