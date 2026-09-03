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
     * Version shown to the user and used for semantic version comparison.
     */
    public static final String APPLICATION_CURRENT_VERSION = "1.43.1";

    /**
     * Legacy numeric representation used only while older update feeds and
     * application versions still exist.
     */
    @Deprecated
    public static final double APPLICATION_CURRENTVERSIONNUMBER = 1.431;

    public static final String VERSIONINFOURLFORUPDATES_KST4CONTEST = "https://kst4contest.hamradioonline.de/kst4ContestVersionInfo.xml";
    public static final String VERSIONINFDOWNLOADEDLOCALFILE  = "kst4ContestVersionInfo.xml";

    public static final String STYLECSSFILE_DEFAULT_DAYLIGHT = "KST4ContestDefaultDay.css";
    public static final String STYLECSSFILE_DEFAULT_EVENING = "KST4ContestDefaultEvening.css";

    public static final String DISCSTRING_DISCONNECT_AND_CLOSE = "CLOSEALL";
    public static final String DISCSTRING_DISCONNECT_DUE_PAWWORDERROR = "JUSTDSICCAUSEPWWRONG";
    public static final String DISCSTRING_DISCONNECTONLY = "ONLYDISCONNECT";

//    public static final String DISCONNECT_RDR_POISONPILL = "POISONPILL_KILLTHREAD: " + sessionRuntimeUniqueId; //whereever a (blocking) udp or tcp reader in an infinite loop gets this message, it will break this loop

    public static final String DISCONNECT_RDR_POISONPILL = "UNKNOWN: KST4C KILL POISONPILL_KILLTHREAD=: " + sessionRuntimeUniqueId; //whereever a (blocking) udp or tcp reader in an infinite loop gets this message, it will break this loop

    public static final String AUTOANSWER_PREFIX = "[KST4C Automsg]";   // hard-coded marker (user cannot remove it)

    /**
     * UI message retention limits.
     *
     * The global chat message list is the backing list for several FilteredLists
     * and TableViews. It must not grow without limit during long contest runs.
     *
     * The list is kept in newest-first order:
     * index 0 = newest message
     * last index = oldest message
     */
    public static final int CHAT_MESSAGE_STORE_MAX_SIZE = 30000;
    public static final int CHAT_MESSAGE_STORE_TRIM_TO_SIZE = 25000;

    /**
     * DXCluster table retention limits.
     */
    public static final int CLUSTER_MESSAGE_STORE_MAX_SIZE = 10000;
    public static final int CLUSTER_MESSAGE_STORE_TRIM_TO_SIZE = 8000;

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
