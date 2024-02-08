package kst4contest.model;

import java.util.ArrayList;

public class UpdateInformation {
    double latestVersionNumberOnServer;
    String adminMessage, majorChanges,latestVersionPathOnWebserver;
    ArrayList<String> needUpdateResourcesSinceLastVersion;
    ArrayList<String[]> featureRequest;
    ArrayList<String[]> bugRequests;

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
