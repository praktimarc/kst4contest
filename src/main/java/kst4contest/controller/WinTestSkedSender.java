package kst4contest.controller;

import kst4contest.model.Band;
import kst4contest.model.ContestSked;
import kst4contest.model.ThreadStateMessage;

import java.net.*;
import java.nio.charset.StandardCharsets;

/**
 * Sends SKED entries to Win-Test via UDP broadcast.
 * <p>
 * Ported from the C# wtSked class in wtKST.
 * <p>
 * Win-Test expects a LOCKSKED / ADDSKED / UNLOCKSKED sequence
 * to safely insert a new sked into its schedule window.
 */
public class WinTestSkedSender {

    private final String stationName;
    private final InetAddress broadcastAddress;
    private final int port;
    private final ThreadStatusCallback callback;

    private static final String THREAD_NICKNAME = "WT-SkedSend";

    /**
     * @param stationName   our station name in the Win-Test network (e.g. "KST4Contest")
     * @param broadcastAddress UDP broadcast address (e.g. 255.255.255.255 or subnet broadcast)
     * @param port           Win-Test network port (default 9871)
     * @param callback       optional callback for status reporting (may be null)
     */
    public WinTestSkedSender(String stationName, InetAddress broadcastAddress, int port,
                             ThreadStatusCallback callback) {
        this.stationName = stationName;
        this.broadcastAddress = broadcastAddress;
        this.port = port;
        this.callback = callback;
    }

    /**
     * Pushes a ContestSked into Win-Test by sending the
     * LOCKSKED / ADDSKED / UNLOCKSKED sequence.
     *
     * @param sked             sked to push
     * @param targetCallsign   callsign prepared for Win-Test
     * @param frequencyKHz     operating frequency in kHz
     * @param notes            optional notes
     * @param mode             Win-Test mode ID: 0 for CW, 1 for SSB
     */
    public void pushSkedToWinTest(ContestSked sked,
                                  String targetCallsign,
                                  double frequencyKHz,
                                  String notes,
                                  int mode) {

        try {
            sendLockSked();

            sendAddSked(
                    sked,
                    targetCallsign,
                    frequencyKHz,
                    notes,
                    mode
            );

            sendUnlockSked();

            reportStatus(
                    "Sked pushed to WT: " + targetCallsign,
                    false
            );

            System.out.println(
                    "[WinTestSkedSender] Sked pushed: "
                            + targetCallsign
                            + " at "
                            + frequencyKHz
                            + " kHz, band="
                            + sked.getBand()
                            + ", mode="
                            + mode
            );

        } catch (Exception exception) {
            reportStatus(
                    "ERROR pushing sked: "
                            + exception.getMessage(),
                    true
            );

            System.out.println(
                    "[WinTestSkedSender] Error pushing sked: "
                            + exception.getMessage()
            );

            exception.printStackTrace();
        }
    }

    /**
     * Sends a LOCKSKED message to lock the Win-Test sked window.
     */
    private void sendLockSked() throws Exception {
        WinTestMessage msg = new WinTestMessage(
                WinTestMessage.MessageType.LOCKSKED,
                stationName, "",
                "\"" + stationName + "\"");
        sendUdp(msg);
    }

    /**
     * Sends an UNLOCKSKED message to unlock the Win-Test sked window.
     */
    private void sendUnlockSked() throws Exception {
        WinTestMessage msg = new WinTestMessage(
                WinTestMessage.MessageType.UNLOCKSKED,
                stationName, "",
                "\"" + stationName + "\"");
        sendUdp(msg);
    }

    /**
     * Sends an ADDSKED message with the sked details.
     *
     * <p>The wtKST implementation subtracts a reference time of
     * 1970-01-01 00:01:00 UTC and subsequently adds 60 seconds. Both
     * operations cancel each other out. The transmitted value is therefore
     * an ordinary Unix timestamp and must not receive another offset here.</p>
     */
    private void sendAddSked(ContestSked sked,
                             String targetCallsign,
                             double frequencyKHz,
                             String notes,
                             int mode) throws Exception {

        long wtTimestamp =
                sked.getSkedTimeEpoch() / 1000L;

        // Frequency in 0.1 kHz units.
        long frequencyTenthKHz =
                Math.round(frequencyKHz * 10.0);

        int bandId =
                toWinTestBandId(sked.getBand());

        /*
         * Accept only the mode IDs supported by this UI.
         * Any unexpected value falls back to SSB.
         */
        int winTestMode =
                mode == 0
                        ? 0
                        : 1;

        String data =
                wtTimestamp
                        + " " + frequencyTenthKHz
                        + " " + bandId
                        + " " + winTestMode
                        + " \"" + targetCallsign + "\""
                        + " \"" + (notes != null ? notes : "") + "\"";

        WinTestMessage message = new WinTestMessage(
                WinTestMessage.MessageType.ADDSKED,
                stationName,
                "",
                data
        );

        sendUdp(message);
    }

    /**
     * Sends a WinTestMessage via UDP broadcast.
     */
    private void sendUdp(WinTestMessage msg) throws Exception {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);
            socket.setReuseAddress(true);

            byte[] bytes = msg.toBytes();
            DatagramPacket packet = new DatagramPacket(bytes, bytes.length, broadcastAddress, port);
            socket.send(packet);

            System.out.println("[WinTestSkedSender] sent: " + msg);
        }
    }

    /**
     * Maps the kst4contest Band enum to Win-Test band IDs.
     * <p>
     * Win-Test band IDs (reverse-engineered from wtKST):
     * 10=50MHz, 11=70MHz, 12=144MHz, 14=432MHz, 16=1.2GHz,
     * 17=2.3GHz, 18=3.4GHz, 19=5.7GHz, 20=10GHz, 21=24GHz,
     * 22=47GHz, 23=76GHz
     */
    public static int toWinTestBandId(Band band) {
        if (band == null) return 12; // default to 144 MHz
        return switch (band) {
            case B_50   -> 10;
            case B_70   -> 11;
            case B_144  -> 12;
            case B_432  -> 14;
            case B_1296 -> 16;
            case B_2320 -> 17;
            case B_3400 -> 18;
            case B_5760 -> 19;
            case B_10G  -> 20;
            case B_24G  -> 21;
        };
    }

    private void reportStatus(String text, boolean isError) {
        if (callback != null) {
            callback.onThreadStatus(THREAD_NICKNAME,
                    new ThreadStateMessage(THREAD_NICKNAME, !isError, text, isError));
        }
    }
}
