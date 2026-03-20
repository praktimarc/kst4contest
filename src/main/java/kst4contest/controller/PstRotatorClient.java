package kst4contest.controller;

import kst4contest.controller.interfaces.PstRotatorEventListener;
import kst4contest.model.ThreadStateMessage;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PstRotatorClient implements Runnable {

    private ThreadStatusCallback callBackToController;
    private String ThreadNickName = "PSTRotator";

    private static final Logger LOGGER = Logger.getLogger(PstRotatorClient.class.getName());
    private static final int BUFFER_SIZE = 1024;

    // Konfiguration
    private final String host;
    private final int remotePort; // Port, auf dem PSTRotator hört (z.B. 12060)
    private final int localPort;  // Port, auf dem wir hören (z.B. 12061)

    private DatagramSocket socket;
    private volatile boolean running = false;
    private PstRotatorEventListener listener;

    // Executor für Polling (Status-Abfrage)
    private ScheduledExecutorService poller;

    /**
     * Konstruktor
     * @param host IP Adresse von PSTRotator (meist "127.0.0.1")
     * @param remotePort Der Port, der in PSTRotator eingestellt ist (User-Wunsch: 12060)
     * @param listener Callback für den Chatcontroller
     */
    public PstRotatorClient(String host, int remotePort, PstRotatorEventListener listener, ThreadStatusCallback callBack) {
        this.callBackToController = callBack;
        this.host = host;
        this.remotePort = remotePort;
        // Laut Manual antwortet PSTRotator oft auf Port+1.
        // Wir binden uns also standardmäßig auf remotePort + 1.
        this.localPort = remotePort + 1;
        this.listener = listener;
    }

    /**
     * alternative constructor for seting the remote port explicitely
     */
    public PstRotatorClient(String host, int remotePort, int localPort, PstRotatorEventListener listener) {
        this.host = host;
        this.remotePort = remotePort;
        this.localPort = localPort;
        this.listener = listener;
    }

    /**
     * Startet den Empfangs-Thread und das Polling
     */
    public void start() {
        try {

            // Socket binden
//            socket = new DatagramSocket(null);
//            socket.setReuseAddress(true);
//            socket = new DatagramSocket(localPort);
//
            socket = new DatagramSocket(null);
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(localPort)); //bind to port

            running = true;

            // 1. Empfangs-Thread starten (dieses Runnable)
            Thread thread = new Thread(this, "PSTRotator-Listener-" + remotePort);
            thread.start();

            // 2. Polling starten (z.B. alle 2 Sekunden Status abfragen)
            poller = Executors.newSingleThreadScheduledExecutor();
            poller.scheduleAtFixedRate(this::pollStatus, 1, 2, TimeUnit.SECONDS);

            ThreadStateMessage threadStateMessage = new ThreadStateMessage(this.ThreadNickName, running, "initialized", false);
            callBackToController.onThreadStatus(ThreadNickName,threadStateMessage);
            LOGGER.info("PstRotatorClient started. Remote: " + remotePort + ", Local: " + localPort);

        } catch (SocketException e) {
            LOGGER.log(Level.SEVERE, "Fehler beim Öffnen des UDP Sockets", e);
        }
    }

    /**
     * Stopping threads and closing sockets of pstRotator communicator
     */
    public void stop() {
        running = false;
        if (poller != null && !poller.isShutdown()) {
            poller.shutdownNow();
        }
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    /**
     * Main loop in thread which listens fpr PSTrotator packets
     */
    @Override
    public void run() {
        byte[] buffer = new byte[BUFFER_SIZE];

        while (running && !socket.isClosed()) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet); // Blockiert bis Daten kommen

                String received = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.US_ASCII).trim();

                ThreadStateMessage threadStateMessage = new ThreadStateMessage(this.ThreadNickName, true, "received line\n" + received, false);
                callBackToController.onThreadStatus(ThreadNickName,threadStateMessage);

                parseResponse(received);

            } catch (IOException e) {
                if (running) {
                    LOGGER.log(Level.WARNING, "Fehler beim Empfangen des Pakets", e);
                }
            }
        }
    }


    /**
     * parses a pst rotatpr message to fit the PST listener interface
     * @param msg
     */

    private void parseResponse(String msg) {
        // Debug
        if (listener != null) listener.onMessageReceived(msg);

        // Example answer: "AZ:145.0<CR>", "EL:010.0<CR>", "MODE:1<CR>"
        msg = msg.replace("<CR>", "").trim();

        try {
            if (msg.startsWith("AZ:")) {
                String val = msg.substring(3);
                if (listener != null) listener.onAzimuthUpdate(Double.parseDouble(val));
            }
            else if (msg.startsWith("EL:")) {
                String val = msg.substring(3);
                if (listener != null) listener.onElevationUpdate(Double.parseDouble(val));
            }
            else if (msg.startsWith("MODE:")) {
                // MODE:1 = Tracking, MODE:0 = Manual
                String val = msg.substring(5);
                boolean tracking = "1".equals(val);
                if (listener != null) listener.onModeUpdate(tracking);
            }
            else if (msg.startsWith("OK:")) {
                // Bestätigung von Befehlen, z.B. OK:STOP:1
                LOGGER.fine("Befehl bestätigt: " + msg);
            }
        } catch (NumberFormatException e) {
            LOGGER.warning("Konnte Wert nicht parsen: " + msg);
        }
    }

    // --- Sende Methoden (API für den Chatcontroller) ---

    private void sendUdp(String message) {
        if (socket == null || socket.isClosed()) return;

        try {
            byte[] data = message.getBytes(StandardCharsets.US_ASCII);
            InetAddress address = InetAddress.getByName(host);
            DatagramPacket packet = new DatagramPacket(data, data.length, address, remotePort);
            socket.send(packet);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Fehler beim Senden an PstRotator", e);
        }
    }

    /**
     * Sendet den generischen XML Befehl.
     * Bsp: <PST><AZIMUTH>85</AZIMUTH></PST>
     */
    private void sendCommand(String tag, String value) {
        String xml = String.format("<PST><%s>%s</%s></PST>", tag, value, tag);
        System.out.println("PSTRotatorClient: sent: " +  xml);
        sendUdp(xml);
    }

    // Öffentliche Steuermethoden

    public void setAzimuth(double degrees) {
        // Formatierung ohne unnötige Nachkommastellen, falls nötig
        sendCommand("AZIMUTH", String.valueOf((int) degrees));
    }

    public void setElevation(double degrees) {
        sendCommand("ELEVATION", String.valueOf(degrees));
    }

    public void stopRotor() {
        sendCommand("STOP", "1");
    }

    public void park() {
        sendCommand("PARK", "1");
    }

    public void setTrackingMode(boolean enable) {
        sendCommand("TRACK", enable ? "1" : "0");
    }

    /**
     * Method for polling rotators status via PSTRotator software. Asks only for AZ value!<br/>
     * Scheduled in a fixed time by executor
     */
    public void pollStatus() {
        // PSTRotator Dokumentation:
        // <PST>AZ?</PST>
        // <PST>EL?</PST>
        // <PST>MODE?</PST>

        // Man kann mehrere Befehle in einem Paket senden
        String query = "<PST><AZ?></AZ?><EL?></EL?><MODE?></MODE?></PST>";
        // HINWEIS: Laut Doku ist die Syntax für Abfragen etwas anders: <PST>AZ?</PST>
        // Daher bauen wir den String manuell, da sendCommand Tags schließt.

        sendUdp("<PST>AZ?</PST>");
//        sendUdp("<PST>EL?</PST>");
        sendUdp("<PST>MODE?</PST>");
    }
}