package kst4contest;

import java.util.Random;

public class ApplicationConstants {

    /**
     * default constructor generates runtime id
     */
    ApplicationConstants() {
        sessionRuntimeUniqueId = generateRuntimeId();
    };

    public static int sessionRuntimeUniqueId = generateRuntimeId();
    /**
     * Name of the Application.
     */
    public static final String APPLICATION_NAME = "praktiKST";

    /**
     * Name of file to store preferences in.
     */
    public static final double APPLICATION_CURRENTVERSIONNUMBER = 1.40;

    public static final String VERSIONINFOURLFORUPDATES_KST4CONTEST = "https://do5amf.funkerportal.de/kst4ContestVersionInfo.xml";
    public static final String VERSIONINFDOWNLOADEDLOCALFILE  = "kst4ContestVersionInfo.xml";

    public static final String STYLECSSFILE_DEFAULT_DAYLIGHT = "KST4ContestDefaultDay.css";
    public static final String STYLECSSFILE_DEFAULT_EVENING = "KST4ContestDefaultEvening.css";

    public static final String DISCSTRING_DISCONNECT_AND_CLOSE = "CLOSEALL";
    public static final String DISCSTRING_DISCONNECT_DUE_PAWWORDERROR = "JUSTDSICCAUSEPWWRONG";
    public static final String DISCSTRING_DISCONNECTONLY = "ONLYDISCONNECT";

//    public static final String DISCONNECT_RDR_POISONPILL = "POISONPILL_KILLTHREAD: " + sessionRuntimeUniqueId; //whereever a (blocking) udp or tcp reader in an infinite loop gets this message, it will break this loop

    public static final String DISCONNECT_RDR_POISONPILL = "UNKNOWN: KST4C KILL POISONPILL_KILLTHREAD=: " + sessionRuntimeUniqueId; //whereever a (blocking) udp or tcp reader in an infinite loop gets this message, it will break this loop

    public static final String AUTOANSWER_PREFIX = "[KST4C Automsg] ";   // hard-coded marker (user can't remove it)



    /**
     * generates a unique runtime id per session. Its used to feed the poison pill in order to kill only this one and
     * only instance if the program and not multiple instances
     * @return
     */
    public static int generateRuntimeId() {

        Random ran = new Random();

        return ran.nextInt(6) + 100;
    }
}
