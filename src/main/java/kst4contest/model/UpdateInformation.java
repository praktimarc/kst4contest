package kst4contest.model;

import java.util.ArrayList;

public class UpdateInformation {
    double latestVersionNumberOnServer = 100; //dummy value to prevent nullpointerexc
    String adminMessage ="";
        String majorChanges ="";
        String latestVersionPathOnWebserver="";
    ArrayList<String> needUpdateResourcesSinceLastVersion = new ArrayList<String>();
    ArrayList<String[]> featureRequest = new ArrayList<String[]>();
    ArrayList<String[]> bugRequests = new ArrayList<String[]>();
    ArrayList<String[]> changeLog = new ArrayList<String[]>();
    ArrayList<String[]> bugList = new ArrayList<String[]>();

    public ArrayList<String[]> getBugList() {
        return bugList;
    }

    public void setBugList(ArrayList<String[]> bugList) {
        this.bugList = bugList;
    }

    public ArrayList<String[]> getChangeLog() {
        return changeLog;
    }

    public void setChangeLog(ArrayList<String[]> changeLog) {
        this.changeLog = changeLog;
    }

    public double getLatestVersionNumberOnServer() {
        return latestVersionNumberOnServer;
    }

    public void setLatestVersionNumberOnServer(double latestVersionNumberOnServer) {
        this.latestVersionNumberOnServer = latestVersionNumberOnServer;
    }

    public String getAdminMessage() {
        return adminMessage;
    }

    public void setAdminMessage(String adminMessage) {
        this.adminMessage = adminMessage;
    }

    public String getMajorChanges() {
        return majorChanges;
    }

    public void setMajorChanges(String majorChanges) {
        this.majorChanges = majorChanges;
    }

    public String getLatestVersionPathOnWebserver() {
        return latestVersionPathOnWebserver;
    }

    public void setLatestVersionPathOnWebserver(String latestVersionPathOnWebserver) {
        this.latestVersionPathOnWebserver = latestVersionPathOnWebserver;
    }

    public ArrayList<String> getNeedUpdateResourcesSinceLastVersion() {
        return needUpdateResourcesSinceLastVersion;
    }

    public void setNeedUpdateResourcesSinceLastVersion(ArrayList<String> needUpdateResourcesSinceLastVersion) {
        this.needUpdateResourcesSinceLastVersion = needUpdateResourcesSinceLastVersion;
    }

    public ArrayList<String[]> getFeatureRequest() {
        return featureRequest;
    }

    public void setFeatureRequest(ArrayList<String[]> featureRequest) {
        this.featureRequest = featureRequest;
    }

    public ArrayList<String[]> getBugRequests() {
        return bugRequests;
    }

    public void setBugRequests(ArrayList<String[]> bugRequests) {
        this.bugRequests = bugRequests;
    }
}
