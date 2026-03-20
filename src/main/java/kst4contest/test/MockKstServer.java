package kst4contest.test;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class MockKstServer {

    private static final int PORT = 23001;
    private static final String CHAT_ID = "2"; // 2 = 144/432 MHz

    // Thread-sichere Liste aller verbundenen Clients (OutputStreams)
    private final List<PrintWriter> clients = new CopyOnWriteArrayList<>();

    // Permanente User (Ihre Test-Callsigns)
    private final Map<String, User> onlineUsers = new HashMap<>();
    // Historien müssen synchronisiert werden
    private final List<String> historyChat = Collections.synchronizedList(new ArrayList<>());
    private final List<String> historyDx = Collections.synchronizedList(new ArrayList<>());

    private boolean running = false;
    private ServerSocket serverSocket;

    public MockKstServer() {
        // Initiale Permanente User
        addUser("DK5EW", "Erwin", "JN47NX");
        addUser("DL1TEST", "TestOp", "JO50XX");
        addUser("ON4KST", "Alain", "JO20HI");
        addUser("PA9R-2", "2", "JO20HI");
        addUser("PA9R-70", "70", "JO20HI");
        addUser("PA9R", "general", "JO20HI");
    }

    // Startet den Server im Hintergrund (Non-Blocking)
    public void start() {
        if (running) return;
        running = true;

        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                System.out.println("[Server] ON4KST Simulation gestartet auf Port " + PORT);

                // Startet den Simulator für Zufallstraffic
                new Thread(this::simulationLoop).start();

                while (running) {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("[Server] Neuer Client verbunden: " + clientSocket.getInetAddress());
                    new Thread(new ClientHandler(clientSocket)).start();
                }
            } catch (IOException e) {
                if (running) e.printStackTrace();
            }
        }).start();
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addUser(String call, String name, String loc) {
        onlineUsers.put(call, new User(call, name, loc));
    }

    private void removeUser(String call) {
        onlineUsers.remove(call);
    }

    // Sendet Nachricht an ALLE verbundenen Clients (inkl. Sender)
    private void broadcast(String message) {
        if (!message.endsWith("\r\n")) message += "\r\n";
        String finalMsg = message;

        for (PrintWriter writer : clients) {
            try {
                writer.print(finalMsg);
                writer.flush(); // WICHTIG: Sofort senden!
            } catch (Exception e) {
                // Client wohl weg, wird beim nächsten Schreibversuch oder im Handler entfernt
            }
        }
    }

    // --- Innere Logik: Client Handler ---
    private class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String myCall = "MYCLIENT"; // Default, wird bei LOGIN überschrieben

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                // ISO-8859-1 ist Standard für KST/Telnet Cluster
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "ISO-8859-1"));
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "ISO-8859-1"), true);

                clients.add(out);

                String line;
                boolean loginComplete = false;

                while ((line = in.readLine()) != null) {
                    // System.out.println("[RECV] " + line); // Debugging aktivieren falls nötig
                    String[] parts = line.split("\\|");
                    String cmd = parts[0];

                    if (cmd.equals("LOGIN") || cmd.equals("LOGINC")) {
                        // Protokoll: LOGIN|callsign|password|... [cite: 21]
                        if (parts.length > 1) myCall = parts[1];

                        // 1. Login Bestätigung
                        // Format: LOGSTAT|100|chat id|client software version|session key|config|dx option|
                        send("LOGSTAT|100|" + CHAT_ID + "|JavaSim|KEY123|Config|3|");

                        // Bei LOGIN senden wir die Daten sofort
                        // Bei LOGINC warten wir eigentlich auf SDONE, senden hier aber vereinfacht direkt
                        if (cmd.equals("LOGIN")) {
                            sendInitialData();
                            loginComplete = true;
                        }
                    }
                    else if (cmd.equals("SDONE")) {
                        // Abschluss der Settings (bei LOGINC) [cite: 34]
                        sendInitialData();
                        loginComplete = true;
                    }
                    else if (cmd.equals("MSG")) {
                        // MSG|chat id|destination|command|0| [cite: 42]
                        if (parts.length >= 4) {
                            String text = parts[3];
                            // Nachricht sofort als CH Frame an alle verteilen (Echo)
                            handleChatMessage(myCall, "Me", text);
                        }
                    }
                    else if (cmd.equals("CK")) {
                        // Keepalive [cite: 20]
                        // Server muss nicht zwingend antworten, aber Connection bleibt offen
                    }
                }
            } catch (IOException e) {
                // System.out.println("Client getrennt");
            } finally {
                clients.remove(out);
                try { socket.close(); } catch (IOException e) {}
            }
        }

        private void send(String msg) {
            if (!msg.endsWith("\r\n")) msg += "\r\n";
            out.print(msg);
            out.flush();
        }

        private void sendInitialData() {
            // 1. User Liste UA0 [cite: 14]
            for (User u : onlineUsers.values()) {
                send("UA0|" + CHAT_ID + "|" + u.call + "|" + u.name + "|" + u.loc + "|0|");
            }
            // 2. Chat History CR [cite: 7]
            synchronized(historyChat) {
                for (String h : historyChat) send(h);
            }
            // 3. DX History DL [cite: 10]
            synchronized(historyDx) {
                for (String d : historyDx) send(d);
            }
            // 4. Ende User Liste UE [cite: 15]
            send("UE|" + CHAT_ID + "|" + onlineUsers.size() + "|");
        }
    }

    // --- Hilfsmethoden für Traffic ---

    private void handleChatMessage(String call, String name, String text) {
        // CH|chat id|date|callsign|firstname|destination|msg|highlight|
        String date = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String frame = String.format("CH|%s|%s|%s|%s|0|%s|0|", CHAT_ID, date, call, name, text);

        synchronized(historyChat) {
            historyChat.add(frame);
            if (historyChat.size() > 50) historyChat.remove(0);
        }
        broadcast(frame);
    }

    private void handleDxSpot(String spotter, String dx, String freq) {
        // DL|Unix time|dx utc|spotter|qrg|dx|info|spotter locator|dx locator| [cite: 10]
        long unixTime = System.currentTimeMillis() / 1000;
        String utc = new SimpleDateFormat("HHmm").format(new Date());
        // Simple Dummy Locators
        String frame = String.format("DL|%d|%s|%s|%s|%s|Simulated|JO00|JO99|",
                unixTime, utc, spotter, freq, dx);

        synchronized(historyDx) {
            historyDx.add(frame);
            if (historyDx.size() > 20) historyDx.remove(0);
        }
        broadcast(frame);
    }

    private void simulationLoop() {
        String[] randomCalls = {"PA0GUS", "F6APE", "OH8K", "OZ2M", "G4CBW"};
        String[] msgs = {"CQ 144.300", "Tnx for QSO", "Any sked?", "QRV 432.200"};
        Random rand = new Random();

        while (running) {
            try {
                Thread.sleep(3000 + rand.nextInt(5000)); // 3-8 Sek Pause

                int action = rand.nextInt(10);
                String call = randomCalls[rand.nextInt(randomCalls.length)];

                if (action < 4) { // 40% Chat
                    handleChatMessage(call, "SimOp", msgs[rand.nextInt(msgs.length)]);
                } else if (action < 7) { // 30% DX Spot
                    handleDxSpot(call, randomCalls[rand.nextInt(randomCalls.length)], "144." + rand.nextInt(400));
                } else if (action == 8) { // Login Simulation UA5
                    if (!onlineUsers.containsKey(call)) {
                        addUser(call, "SimOp", "JO11");
                        broadcast("UA5|" + CHAT_ID + "|" + call + "|SimOp|JO11|2|");
                    }
                } else if (action == 9) { // Logout Simulation UR6
                    if (onlineUsers.containsKey(call) && !call.equals("DK5EW")) { // DK5EW nicht kicken
                        removeUser(call);
                        broadcast("UR6|" + CHAT_ID + "|" + call + "|");
                    }
                }

                // Ping ab und zu
                if (rand.nextInt(5) == 0) broadcast("CK|");

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // Kleine Datenklasse
    private static class User {
        String call, name, loc;
        User(String c, String n, String l) { this.call=c; this.name=n; this.loc=l; }
    }
}