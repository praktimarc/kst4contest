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
        double currentVersionNumber = ApplicationConstants.APPLICATION_CURRENTVERSIONNUMBER;

    }

    String versionInfoDownloadedFromServerFileName = ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, ApplicationConstants.VERSIONINFDOWNLOADEDLOCALFILE);
    String versionInfoXMLURLAtServer = ApplicationConstants.VERSIONINFOURLFORUPDATES_KST4CONTEST;
    double currentVersion = ApplicationConstants.APPLICATION_CURRENTVERSIONNUMBER;
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
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return updateInfos;
    }

    @Override
    public String toString() {
        String toString = "";

        toString += this.currentVersion;

        return toString;
    }
}