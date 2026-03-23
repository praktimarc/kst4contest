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
     * Pushes a ContestSked into Win-Test by sending the LOCKSKED / ADDSKED / UNLOCKSKED
     * sequence via UDP broadcast.
     *
     * @param sked          the sked to push
     * @param frequencyKHz  current operating frequency in kHz (e.g. 144321.0)
     * @param notes         free-text notes (e.g. "[JO62QM - 123°] sked via KST")
     */
    public void pushSkedToWinTest(ContestSked sked, double frequencyKHz, String notes) {
        try {
            sendLockSked();
            sendAddSked(sked, frequencyKHz, notes);
            sendUnlockSked();

            reportStatus("Sked pushed to WT: " + sked.getTargetCallsign(), false);
            System.out.println("[WinTestSkedSender] Sked pushed: " + sked.getTargetCallsign()
                    + " at " + frequencyKHz + " kHz, band=" + sked.getBand());
        } catch (Exception e) {
            reportStatus("ERROR pushing sked: " + e.getMessage(), true);
            System.out.println("[WinTestSkedSender] Error pushing sked: " + e.getMessage());
            e.printStackTrace();
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
     * <p>
     * Win-Test ADDSKED data format (from wtKST):
     * <pre>
     *   {epoch_seconds} {freq_in_0.1kHz} {bandId} {mode} "{callsign}" "{notes}"
     * </pre>
     * <p>
     * Win-Test uses a timestamp reference of 1970-01-01 00:01:00 UTC (60s offset from Unix epoch).
     * The C# code adds 60 seconds to compensate.
     */
    private void sendAddSked(ContestSked sked, double frequencyKHz, String notes) throws Exception {
        // Win-Test timestamp: epoch seconds with 60s offset
        long epochSeconds = sked.getSkedTimeEpoch() / 1000;
        long wtTimestamp = epochSeconds + 60;

        // Frequency in 0.1 kHz units (Win-Test convention): multiply kHz by 10
        long freqTenthKHz = Math.round(frequencyKHz * 10.0);

        // Win-Test band ID
        int bandId = toWinTestBandId(sked.getBand());

        // Mode: 0 = CW, 1 = SSB. Default to CW; detect SSB from frequency.
        int mode = (frequencyKHz > 144_000 || isInSsbSegment(frequencyKHz)) ? 0 : 0;
        // Simple heuristic: could be refined with actual mode info later

        String data = wtTimestamp
                + " " + freqTenthKHz
                + " " + bandId
                + " " + mode
                + " \"" + sked.getTargetCallsign() + "\""
                + " \"" + (notes != null ? notes : "") + "\"";

        WinTestMessage msg = new WinTestMessage(
                WinTestMessage.MessageType.ADDSKED,
                stationName, "",
                data);
        sendUdp(msg);
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

    /**
     * Very simple SSB segment heuristic.
     * A more complete implementation would check actual mode from Win-Test STATUS.
     */
    private boolean isInSsbSegment(double frequencyKHz) {
        // Example: 144.300+ is typically SSB on 2m
        if (frequencyKHz >= 144.300 && frequencyKHz <= 144.400) return true;
        if (frequencyKHz >= 432.200 && frequencyKHz <= 432.400) return true;
        return false;
    }

    private void reportStatus(String text, boolean isError) {
        if (callback != null) {
            callback.onThreadStatus(THREAD_NICKNAME,
                    new ThreadStateMessage(THREAD_NICKNAME, !isError, text, isError));
        }
    }
}
