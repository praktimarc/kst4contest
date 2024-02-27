package kst4contest;

public class ApplicationConstants {
    /**
     * Name of the Application.
     */
    public static final String APPLICATION_NAME = "praktiKST";

    /**
     * Name of file to store preferences in.
     */
    public static final double APPLICATION_CURRENTVERSIONNUMBER = 1.0;

    public static final String VERSIONINFOURLFORUPDATES_KST4CONTEST = "https://do5amf.funkerportal.de/kst4ContestVersionInfo.xml";
    public static final String VERSIONINFDOWNLOADEDLOCALFILE  = "kst4ContestVersionInfo.xml";

    public static final String DISCSTRING_DISCONNECT_AND_CLOSE = "CLOSEALL";
    public static final String DISCSTRING_DISCONNECT_DUE_PAWWORDERROR = "JUSTDSICCAUSEPWWRONG";
    public static final String DISCSTRING_DISCONNECTONLY = "ONLYDISCONNECT";

    public static final String DISCONNECT_RDR_POISONPILL = "POISONPILL_KILLTHREAD"; //whereever a (blocking) udp or tcp reader in an infinite loop gets this message, it will break this loop

}
