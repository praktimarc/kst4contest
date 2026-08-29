package kst4contest.controller;

import javafx.application.Platform;
import kst4contest.ApplicationConstants;
import kst4contest.model.Band;
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
     * Formats a frequency from a Win-Test STATUS packet for use as MYQRG.
     *
     * <p>Win-Test transmits the frequency in units of 0.1 kHz. KST4Contest
     * displays frequencies as {@code MHz.kHz.10Hz}, for example
     * {@code 144.300.00} or {@code 10368.100.00}. The MHz part may contain
     * between one and five digits. Deriving its length from the complete value
     * avoids separate and incomplete handling for individual bands.</p>
     *
     * @param frequencyIn100Hz frequency received from Win-Test in units of
     *                         0.1 kHz, equivalent to 100 Hz
     * @return frequency formatted for MYQRG
     * @throws IllegalArgumentException if the supplied frequency is not positive
     *                                  or too small to be formatted
     * @throws ArithmeticException if the supplied value exceeds the supported
     *                             numeric range
     */
    private String helper_formatWinTestFrequency(long frequencyIn100Hz) {
        if (frequencyIn100Hz <= 0) {
            throw new IllegalArgumentException(
                    "Win-Test frequency must be greater than zero"
            );
        }

        /*
         * Multiplication by ten creates a digit sequence whose final five
         * digits represent kHz and 10-Hz groups:
         *
         * 1443210   -> 14432100   -> 144.321.00
         * 103681000 -> 1036810000 -> 10368.100.00
         */
        long frequencyIn10Hz = Math.multiplyExact(
                frequencyIn100Hz,
                10L
        );

        String frequencyDigits = Long.toString(frequencyIn10Hz);

        if (frequencyDigits.length() < 6) {
            throw new IllegalArgumentException(
                    "Win-Test frequency is too small: " + frequencyIn100Hz
            );
        }

        int mhzEndIndex = frequencyDigits.length() - 5;
        int khzEndIndex = frequencyDigits.length() - 2;

        return frequencyDigits.substring(0, mhzEndIndex)
                + "."
                + frequencyDigits.substring(mhzEndIndex, khzEndIndex)
                + "."
                + frequencyDigits.substring(khzEndIndex);
    }

    /**
     * Parses a Win-Test STATUS packet and optionally updates MYQRG.
     *
     * <p>The packet is tokenised while preserving quoted station names.
     * The configured station-name filter is applied before any frequency is
     * processed. An empty filter accepts STATUS packets from every Win-Test
     * station.</p>
     *
     * <p>The main frequency is read from token 7. If pass-frequency use is
     * enabled and token 11 contains a valid frequency, the pass frequency is
     * used instead. A missing or invalid pass frequency deliberately falls
     * back to the main frequency.</p>
     *
     * @param msg complete Win-Test STATUS packet
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
                System.out.println(
                        "[WinTest] STATUS too short: " + msg
                );
                return;
            }

            String stationName = parts.get(1);
            String stationFilter = client
                    .getChatPreferences()
                    .getLogsynch_wintestNetworkStationNameOfWintestClient1();

            if (stationFilter != null
                    && !stationFilter.isBlank()
                    && !stationName.equalsIgnoreCase(stationFilter)) {
                return;
            }

            String modeValue = parts.get(5);
            long mainFrequencyRaw = Long.parseLong(parts.get(7));
            double mainFrequencyKHz = mainFrequencyRaw / 10.0;

            String mode;

            if ("1".equals(modeValue)) {
                mode = mainFrequencyKHz > 10000.0 ? "usb" : "lsb";
            } else {
                mode = "cw";
            }

            String formattedMainQrg =
                    helper_formatWinTestFrequency(mainFrequencyRaw);

            /*
             * Token 11 may contain the pass frequency, depending on the
             * Win-Test STATUS packet. Small numeric flag values must not be
             * interpreted as frequencies.
             */
            String formattedPassQrg = null;

            if (parts.size() > 11) {
                try {
                    long passFrequencyRaw =
                            Long.parseLong(parts.get(11));
                    double passFrequencyKHz =
                            passFrequencyRaw / 10.0;

                    if (passFrequencyKHz > 100.0) {
                        formattedPassQrg =
                                helper_formatWinTestFrequency(
                                        passFrequencyRaw
                                );
                    }
                } catch (IllegalArgumentException
                         | ArithmeticException ignored) {
                    /*
                     * Token 11 does not contain a usable frequency.
                     * The main frequency remains the safe fallback.
                     */
                }
            }

            boolean usePassQrg = client
                    .getChatPreferences()
                    .isLogsynch_wintestUsePassQrg();

            final String qrgToSet =
                    usePassQrg && formattedPassQrg != null
                            ? formattedPassQrg
                            : formattedMainQrg;

            if (client
                    .getChatPreferences()
                    .isLogsynch_wintestQrgSyncEnabled()) {
                Platform.runLater(
                        () -> client
                                .getChatPreferences()
                                .getMYQRGFirstCat()
                                .set(qrgToSet)
                );
            }

            System.out.println(
                    "[WinTest STATUS] stn=" + stationName
                            + ", mode=" + mode
                            + ", qrg=" + formattedMainQrg
                            + (formattedPassQrg != null
                            ? ", passQrg=" + formattedPassQrg
                            : "")
                            + ", selectedQrg=" + qrgToSet
                            + ", syncActive="
                            + client
                            .getChatPreferences()
                            .isLogsynch_wintestQrgSyncEnabled()
            );
        } catch (Exception exception) {
            System.out.println(
                    "[WinTest] STATUS parsing error: "
                            + exception.getMessage()
            );
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
     * Resolves the project Band enum from Win-Test band IDs.
     *
     * <p>Only bands that exist in the current Band enum are returned. The existing
     * 50 MHz and 70 MHz values are resolved in the same way as the other supported
     * VHF, UHF and microwave bands.</p>
     *
     * @param bandId Win-Test band id from ADDQSO
     * @return matching Band or null
     */
    private Band helper_resolveBandFromWinTestBandId(String bandId) {
        if (bandId == null) {
            return null;
        }

        return switch (bandId.trim()) {
            case "10" -> Band.B_50;
            case "11" -> Band.B_70;
            case "12" -> Band.B_144;
            case "14" -> Band.B_432;
            case "16" -> Band.B_1296;
            case "17" -> Band.B_2320;
            case "18" -> Band.B_3400;
            case "19" -> Band.B_5760;
            case "20" -> Band.B_10G;
            case "21" -> Band.B_24G;
            default -> null;
        };
    }

    /**
     * Extracts the locator from a Win-Test ADDQSO packet.
     *
     * <p>Current parser model based on the existing split-by-quotes code:
     * <ul>
     *     <li>{@code split("\"")[7]}  = callsign</li>
     *     <li>{@code split("\"")[11]} = received exchange, e.g. 599001</li>
     *     <li>{@code split("\"")[13]} = locator, e.g. JO51UM</li>
     * </ul>
     *
     * <p>If the dedicated locator field is empty, the exchange is used as fallback.</p>
     *
     * @param msg raw ADDQSO message
     * @return normalized six-character locator or null
     */
    private String helper_resolveLocatorFromWinTestAddQso(String msg) {
        if (msg == null) {
            return null;
        }

        String[] quotedParts = msg.split("\"");

        if (quotedParts.length > 13) {
            String locator = WorkedGrossFieldCache.extractLocator6(quotedParts[13]);
            if (locator != null) {
                return locator;
            }
        }

        if (quotedParts.length > 11) {
            return WorkedGrossFieldCache.extractLocator6(quotedParts[11]);
        }

        return null;
    }


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
            String locatorFromLogger = helper_resolveLocatorFromWinTestAddQso(msg);

            ChatMember workedCall = new ChatMember();
            workedCall.setCallSign(callSignCatched);
            workedCall.setWorked(true); //its worked at this place, for sure!

            if (locatorFromLogger != null) {
                workedCall.setQra(locatorFromLogger);
            }

            ArrayList<Integer> markTheseChattersAsWorked = client.checkListForChatMemberIndexesByCallSign(workedCall);

            String bandId;
            bandId = msg.split("\"")[6].split(" ")[4].trim();

            Band workedBand = helper_resolveBandFromWinTestBandId(bandId);
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

            if (workedBand != null && locatorFromLogger != null) {
                this.client.registerWorkedGrossField(workedBand, locatorFromLogger, workedCall, "WINTEST");
            }

            if (!markTheseChattersAsWorked.isEmpty()) {
                //Worked call is part of the current chatmember list

                for (int index : markTheseChattersAsWorked) {
                    //iterate through the logged in chatmembers callsigns and set the worked markers
                    modifyThat = client.getLst_chatMemberList().get(index);

                    modifyThat.setWorked(true); //worked its for sure

                    if (locatorFromLogger != null
                            && (modifyThat.getQra() == null
                            || modifyThat.getQra().isBlank()
                            || "unknown".equalsIgnoreCase(modifyThat.getQra()))) {
                        modifyThat.setQra(locatorFromLogger);
                    }

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

                if (workedCall.getQra() == null || workedCall.getQra().isBlank()) {
                    workedCall.setQra("unknown");
                }

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
