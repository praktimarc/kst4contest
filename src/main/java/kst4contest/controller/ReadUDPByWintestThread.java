package kst4contest.controller;

import javafx.application.Platform;
import kst4contest.ApplicationConstants;
import kst4contest.model.ChatMember;
import kst4contest.model.ThreadStateMessage;
import kst4contest.view.GuiUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReadUDPByWintestThread extends Thread {

    private static final Pattern STATUS_TOKEN_PATTERN = Pattern.compile("\"([^\"]*)\"|(\\S+)");

    private DatagramSocket socket;
    private ChatController client;

    private volatile boolean running = true;

    private int PORT = 9871; //default

    private static final int BUFFER_SIZE = 4096;

    private final Map<Integer, String> receivedQsos = new ConcurrentHashMap<>();
    private long lastPacketTime = 0;

    private String myStation = "DO5AMF";

    private String targetStation = "";
    private String stationID = "";
    private int lastKnownQso = 0;

    private ThreadStatusCallback callBackToController;
    private String ThreadNickName = "Wintest-msg";


    public ReadUDPByWintestThread(ChatController client, ThreadStatusCallback callback) {

        this.callBackToController = callback;
        this.client = client;
        this.myStation = client.getChatPreferences().getStn_loginCallSignRaw(); //callsign of the logging stn
        this.PORT =  client.getChatPreferences().getLogsynch_wintestNetworkPort();

    }

    @Override
    public void interrupt() {
        running = false;
        if (socket != null && !socket.isClosed()) socket.close();
        super.interrupt();
    }

    @Override
    public void run() {

        ThreadStateMessage threadStateMessage = new ThreadStateMessage(this.ThreadNickName, true, "initialized", false);
        callBackToController.onThreadStatus(ThreadNickName,threadStateMessage);
        Thread.currentThread().setName("ReadUDPByWintestThread");

        byte[] buffer = new byte[BUFFER_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        try {
            socket = new DatagramSocket(null); //first init with null, then make ready for reuse
            socket.setReuseAddress(true);
//            socket = new DatagramSocket(PORT);
            int boundPort = client.getChatPreferences().getLogsynch_wintestNetworkPort();
            socket.bind(new InetSocketAddress(boundPort));
            socket.setSoTimeout(3000);
            System.out.println("[WinTest UDP listener] started at port: " + boundPort);
        } catch (SocketException e) {
            e.printStackTrace();
            return;
        }

        while (running) {
            try {
                socket.receive(packet);
                String msg = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.US_ASCII).trim();
                processWinTestMessage(msg);
            } catch (SocketTimeoutException e) {
//                checkForMissingQsos();
            } catch (IOException e) {
                //TODO: here is something to catch
            }
        }
    }

    private void processWinTestMessage(String msg) {
//        System.out.println("Wintest-Message received: " + msg);

        lastPacketTime = System.currentTimeMillis();

        if (msg.startsWith("HELLO:")) { //Client Signon of wintest
            parseHello(msg);
            try {
//                send_needqso();
            }catch (Exception e) {
                System.out.println("Error: ");
                e.printStackTrace();
            }


        } else if (msg.startsWith("ADDQSO:")) { //adding qso to wintest log
            try {

                parseAddQso(msg);
            } catch (Exception e) {
                ThreadStateMessage threadStateMessage = new ThreadStateMessage(this.ThreadNickName, true, "Parsing ERROR: " + Arrays.toString(e.getStackTrace()), true);
                callBackToController.onThreadStatus(ThreadNickName,threadStateMessage);
            }

        } else if (msg.startsWith("STATUS")) {
            parseStatus(msg);

        } else if (msg.startsWith("IHAVE:")) { //periodical message of wintest, which qsos are in the log
//            parseIHave(msg); //TODO
        }

        else if (msg.contains(ApplicationConstants.DISCONNECT_RDR_POISONPILL)) {
            System.out.println("ReadUdpByWintest, Info: got poison, now dieing....");
            socket.close();
            running = false;

        }

        ThreadStateMessage threadStateMessage = new ThreadStateMessage(this.ThreadNickName, true, "message received\n" + msg, false);
        callBackToController.onThreadStatus(ThreadNickName,threadStateMessage);
    }

    /**
     * parsing of the hello message of wintest:
     * "HELLO: "STN1" "" 6667 130 "SLAVE" 1 0 1762201985"
     * @param msg
     */
    private void parseHello(String msg) {
        try {
            String[] tokens = msg.split("\"");
            if (tokens.length >= 2) {
                targetStation = tokens[1];
                System.out.println("[WinTest rcv: found logger instance: " + targetStation);
            }
        } catch (Exception e) {
            System.out.println("[WinTest] ERROR on HELLO-Parsing: " + e.getMessage());
        }
    }

    private byte util_calculateChecksum(byte[] bytes) {
        int sum = 0;
        for (byte b : bytes) sum += b;
        return (byte) ((sum | 0x80) & 0xFF);
    }

    /**
     * Parse Win-Test STATUS packets and update own QRG from WT station.
     *
     * Parsing model (tokenized with quotes preserved):
     * parts[0] = "STATUS"
     * parts[1] = station name (example: "STN1")
     * parts[5] = val2 (used to derive mode: 1 => SSB, else CW)
     * parts[7] = frequency in 0.1 kHz units (example: 1443210 => 144321.0)
     */
    private void parseStatus(String msg) {
        try {
            ArrayList<String> parts = new ArrayList<>();
            Matcher matcher = STATUS_TOKEN_PATTERN.matcher(msg);
            while (matcher.find()) {
                if (matcher.group(1) != null) {
                    parts.add(matcher.group(1));
                } else {
                    parts.add(matcher.group(2));
                }
            }

            if (parts.size() < 8) {
                System.out.println("[WinTest] STATUS too short: " + msg);
                return;
            }

            String stn = parts.get(1);
            String stationFilter = client.getChatPreferences().getLogsynch_wintestNetworkStationNameOfWintestClient1();
            if (stationFilter != null && !stationFilter.isBlank() && !stn.equalsIgnoreCase(stationFilter)) {
                return;
            }

            String val2 = parts.get(5);
            String freqRaw = parts.get(7);
            double freqFloat = Integer.parseInt(freqRaw) / 10.0;

            String mode;
            if ("1".equals(val2)) {
                mode = freqFloat > 10000.0 ? "usb" : "lsb";
            } else {
                mode = "cw";
            }

            // Format as MMM.KKK.HH display format (e.g. 144.300.00) consistent with UCX thread
            // freqFloat is in kHz (e.g. 144300.0), convert to Hz-string for formatting
            long freqHzTimes100 = Math.round(freqFloat * 100.0); // e.g. 14430000
            String hzStr = String.valueOf(freqHzTimes100);
            String formattedQRG;
            if (hzStr.length() == 8) {
                // 144MHz range: 14430000 -> 144.300.00
                formattedQRG = String.format("%s.%s.%s", hzStr.substring(0, 3), hzStr.substring(3, 6), hzStr.substring(6, 8));
            } else if (hzStr.length() == 9) {
                // 1296MHz range: 129600000 -> 1296.000.00
                formattedQRG = String.format("%s.%s.%s", hzStr.substring(0, 4), hzStr.substring(4, 7), hzStr.substring(7, 9));
            } else if (hzStr.length() == 7) {
                // 70MHz range: 7010000 -> 70.100.00
                formattedQRG = String.format("%s.%s.%s", hzStr.substring(0, 2), hzStr.substring(2, 5), hzStr.substring(5, 7));
            } else if (hzStr.length() == 6) {
                // 50MHz range: 5030000 but 6 digits: 503000 -> 5.030.00
                formattedQRG = String.format("%s.%s.%s", hzStr.substring(0, 1), hzStr.substring(1, 4), hzStr.substring(4, 6));
            } else {
                formattedQRG = String.format(Locale.US, "%.1f", freqFloat); // fallback
            }
            // Parse pass frequency from parts[11] if available (WT STATUS format)
            String formattedPassQRG = null;
            if (parts.size() > 11) {
                try {
                    String passFreqRaw = parts.get(11);
                    double passFreqFloat = Integer.parseInt(passFreqRaw) / 10.0;
                    if (passFreqFloat > 100) { // Must be a valid radio frequency (> 100 kHz), protects against parsing boolean flag tokens
                        long passFreqHzTimes100 = Math.round(passFreqFloat * 100.0);
                        String passHzStr = String.valueOf(passFreqHzTimes100);
                        if (passHzStr.length() == 8) {
                            formattedPassQRG = String.format("%s.%s.%s", passHzStr.substring(0, 3), passHzStr.substring(3, 6), passHzStr.substring(6, 8));
                        } else if (passHzStr.length() == 9) {
                            formattedPassQRG = String.format("%s.%s.%s", passHzStr.substring(0, 4), passHzStr.substring(4, 7), passHzStr.substring(7, 9));
                        } else if (passHzStr.length() == 7) {
                            formattedPassQRG = String.format("%s.%s.%s", passHzStr.substring(0, 2), passHzStr.substring(2, 5), passHzStr.substring(5, 7));
                        } else if (passHzStr.length() == 6) {
                            formattedPassQRG = String.format("%s.%s.%s", passHzStr.substring(0, 1), passHzStr.substring(1, 4), passHzStr.substring(4, 6));
                        } else {
                            formattedPassQRG = String.format(Locale.US, "%.1f", passFreqFloat);
                        }
                    }
                } catch (Exception ignored) {
                    // parts[11] not a valid frequency, leave formattedPassQRG as null
                }
            }

            if (this.client.getChatPreferences().isLogsynch_wintestQrgSyncEnabled()) {
                final String qrgToSet = (this.client.getChatPreferences().isLogsynch_wintestUsePassQrg() && formattedPassQRG != null)
                        ? formattedPassQRG
                        : formattedQRG;
                // JavaFX StringProperty must be updated on the FX Application Thread
                Platform.runLater(() -> this.client.getChatPreferences().getMYQRGFirstCat().set(qrgToSet));
            }

            System.out.println("[WinTest STATUS] stn=" + stn + ", mode=" + mode + ", qrg=" + formattedQRG
                    + (formattedPassQRG != null ? ", passQrg=" + formattedPassQRG : "")
                    + ", syncActive=" + this.client.getChatPreferences().isLogsynch_wintestQrgSyncEnabled());
        } catch (Exception e) {
            System.out.println("[WinTest] STATUS parsing error: " + e.getMessage());
        }
    }

//    private void send_needqso() throws IOException {
//        String payload = String.format("NEEDQSO:\"%s\" \"%s\" \"%s\" %d %d?\0",
//                "DO5AMF", "STN1", stationID, 1, 9999);
//        InetAddress broadcast = InetAddress.getByName("255.255.255.255");
//        byte[] bytes = payload.getBytes(StandardCharsets.US_ASCII);
//        bytes[bytes.length - 2] = util_calculateChecksum((bytes));
//        socket.send(new DatagramPacket(bytes, bytes.length, broadcast, 9871));
//    }

//    private void send_hello() throws IOException {
//        String payload = String.format("HELLO:\"%s\" \"%s\" \"%s\" %d %d?\0",
//                "DO5AMF", "", stationID, "SLAVE", 1, 14);
//        InetAddress broadcast = InetAddress.getByName("255.255.255.255");
//        byte[] bytes = payload.getBytes(StandardCharsets.US_ASCII);
//        bytes[bytes.length - 2] = util_calculateChecksum((bytes));
//        socket.send(new DatagramPacket(bytes, bytes.length, broadcast, 9871));
//    }

    /**
     * Catches add-qso messages of wintest if a new qso gets into the log<br/>
     *
     * String is like this:<br/><br/>
     *ADDQSO: "STN1" "" "STN1" 1762202297 1440000 0 12 0 0 0 2 2 "DM2RN" "599" "599001" "JO51UM" "" "" 0 "" "" "" 44510
     *
     *          ^^^^sentby<br/>
     *                          ^^^^^^^^^^time<br/>
     *                                      ^^^^^^qrg<br/>
     *                                               ^^band<br/>
     *                                                             ^^^^^callsign logged<br/>
     *                                                                                                     stn-id   ^^^^
     * @param msg
     */
    private void parseAddQso(String msg) {


        ChatMember modifyThat = null;

        try {
//            int qsoNumber = extractQsoNumber(msg);
//            receivedQsos.put(qsoNumber, msg);
//            lastKnownQso = Math.max(lastKnownQso, qsoNumber);
            String callSignCatched = msg.split("\"") [7];

            ChatMember workedCall = new ChatMember();
            workedCall.setCallSign(callSignCatched);
            workedCall.setWorked(true); //its worked at this place, for sure!

            ArrayList<Integer> markTheseChattersAsWorked = client.checkListForChatMemberIndexesByCallSign(workedCall);

            String bandId;
            bandId = msg.split("\"")[6].split(" ")[4].trim();

            switch (bandId) {
                case "10" -> workedCall.setWorked50(true);
                case "11" -> workedCall.setWorked70(true);
                case "12" -> workedCall.setWorked144(true);
                case "14" -> workedCall.setWorked432(true);
                case "16" -> workedCall.setWorked1240(true);
                case "17" -> workedCall.setWorked2300(true);
                case "18" -> workedCall.setWorked3400(true);
                case "19" -> workedCall.setWorked5600(true);
                case "20" -> workedCall.setWorked10G(true);
                case "21" -> workedCall.setWorked24G(true);
                case "22" -> workedCall.setWorked47G(true);
                case "23" -> workedCall.setWorked76G(true);
                default -> System.out.println("[WinTestUDPRcvr: warning] Unbekannte Band-ID: " + bandId);
            }

            if (!markTheseChattersAsWorked.isEmpty()) {
                //Worked call is part of the current chatmember list

                for (int index : markTheseChattersAsWorked) {
                    //iterate through the logged in chatmembers callsigns and set the worked markers
                    modifyThat = client.getLst_chatMemberList().get(index);

                    modifyThat.setWorked(true); //worked its for sure

                    if (workedCall.isWorked50()) {
                        modifyThat.setWorked50(true);
                    } else if (workedCall.isWorked70()) {
                        modifyThat.setWorked70(true);
                    } else if (workedCall.isWorked144()) {
                        modifyThat.setWorked144(true);
                    } else if (workedCall.isWorked432()) {
                        modifyThat.setWorked432(true);
                    } else if (workedCall.isWorked1240()) {
                        modifyThat.setWorked1240(true);
                    } else if (workedCall.isWorked2300()) {
                        modifyThat.setWorked2300(true);
                    } else if (workedCall.isWorked3400()) {
                        modifyThat.setWorked3400(true);
                    } else if (workedCall.isWorked5600()) {
                        modifyThat.setWorked5600(true);
                    } else if (workedCall.isWorked10G()) {
                        modifyThat.setWorked10G(true);
                    } else if (workedCall.isWorked24G()) {
                        modifyThat.setWorked24G(true);
                    } else if (workedCall.isWorked47G()) {
                        modifyThat.setWorked47G(true);
                    } else if (workedCall.isWorked76G()) {
                        modifyThat.setWorked76G(true);
                    }   else {
                        System.out.println("[WinTestUDPRcvr: warning] found no new worked-flag for this band: " + workedCall.getCallSignRaw() + bandId);
                    }
                }

                try {

                    GuiUtils.triggerGUIFilteredChatMemberListChange(client); //not clean at all

                    // trigger band-upgrade hint after log entry (Win-Test)
                    try {
                        client.onExternalLogEntryReceived(workedCall.getCallSignRaw());
                    } catch (Exception e) {
                        System.out.println("[WinTestUDPRcvr, warning]: band-upgrade hint failed: " + e.getMessage());
                    }

                } catch (Exception IllegalStateException) {
                    //do nothing, as it works...
                }
            }


            boolean isInChat = this.client.getDbHandler().updateWkdInfoOnChatMember(workedCall);
            // This will update the worked info on a worked chatmember. DBHandler will
            // check, if an entry at the db had been modified. If not, then the worked
            // station had not been stored. DBHandler will store the information then.
            if (!isInChat) {

                workedCall.setName("unknown");
                workedCall.setQra("unknown");
                workedCall.setLastActivity(new Utils4KST().time_generateActualTimeInDateFormat());
                this.client.getDbHandler().storeChatMember(workedCall);
            }

            File logUDPMessageToThisFile = new File(this.client.getChatPreferences()
                    .getLogSynch_storeWorkedCallSignsFileNameUDPMessageBackup());

            FileWriter fileWriterPersistUDPToFile = null;
            BufferedWriter bufwrtrRawMSGOut;

            try {
                fileWriterPersistUDPToFile = new FileWriter(logUDPMessageToThisFile, true);

            } catch (IOException e1) {
                e1.printStackTrace();
            }

            bufwrtrRawMSGOut = new BufferedWriter(fileWriterPersistUDPToFile);

            if (modifyThat != null) {
                bufwrtrRawMSGOut.write("\n" + modifyThat.toString());
                bufwrtrRawMSGOut.flush();
                bufwrtrRawMSGOut.close();

            } else {
                bufwrtrRawMSGOut.write("\n" + workedCall.toString());
                bufwrtrRawMSGOut.flush();
                bufwrtrRawMSGOut.close();

            }


            System.out.println("[WinTest, Info: Marking Chatmember as worked: " + workedCall.toString());

//            markChatMemberAsWorked(call, band); //TODO

        } catch (Exception e) {
            System.out.println("[WinTest] Fehler beim ADDQSO-Parsing: " + e.getMessage());
        }
    }

}
