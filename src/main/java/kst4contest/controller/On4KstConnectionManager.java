package kst4contest.controller;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import jdk.net.ExtendedSocketOptions;
import kst4contest.ApplicationConstants;
import kst4contest.model.ChatCategory;
import kst4contest.model.ChatMember;
import kst4contest.model.ChatMessage;
import kst4contest.model.ChatPreferences;

/**
 * Owns the complete lifecycle of the single ON4KST TCP session.
 *
 * <p>Every reader, writer, queue and parser belongs to an immutable session id.
 * A delayed failure from an old socket can therefore never close or consume data
 * from its replacement.</p>
 */
final class On4KstConnectionManager {
    private static final Logger LOGGER =
            Logger.getLogger(On4KstConnectionManager.class.getName());
    private static final DateTimeFormatter LIVE_MESSAGE_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    static final int CONNECT_TIMEOUT_MILLIS = 10_000; //TCP-Connect-Timeout
    static final long LOGIN_FALLBACK_MILLIS = 2_000L; //Login-Fallback
    static final long HANDSHAKE_TIMEOUT_MILLIS = 45_000L; //Handshake-Timeout
    static final long APPLICATION_HEARTBEAT_AFTER_MILLIS = 90_000L; //Application-Heartbeat
    static final long INBOUND_STALE_AFTER_MILLIS = 210_000L; //Stale-Timeout - time without rxed data
    static final List<Long> RECONNECT_DELAYS_MILLIS =
            List.of(2_000L, 5_000L, 10_000L, 20_000L, 30_000L); //Reconnect-Backoff if no connection possible

    private final ChatController controller;
    private final ScheduledExecutorService scheduler;
    private final AtomicLong generation = new AtomicLong();
    private final AtomicLong lastReceivedMessageTimestamp = new AtomicLong();

    private volatile Session activeSession;
    private volatile On4KstConnectionState state =
            On4KstConnectionState.DISCONNECTED;
    private volatile boolean stopRequested = true;
    private int reconnectAttempt;

    On4KstConnectionManager(ChatController controller) {
        this.controller = controller;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "On4KstConnectionSupervisor");
            thread.setDaemon(true);
            return thread;
        });
        this.scheduler.scheduleAtFixedRate(
                this::monitorActiveSession, 5L, 5L, TimeUnit.SECONDS);
        LOGGER.fine("ON4KST connection supervisor initialized");
    }

    /**
     * Returns the last lifecycle state published by the connection supervisor.
     *
     * @return current immutable connection-state value
     */
    On4KstConnectionState getState() {
        return state;
    }

    /**
     * Verifies that a callback still belongs to the currently installed session.
     *
     * <p>Every reconnect receives a new id. Late EOF, write or parser callbacks from
     * an obsolete socket therefore become harmless instead of closing the replacement
     * connection.</p>
     *
     * @param sessionId id captured by the calling worker
     * @return {@code true} only for the current, open and non-stopped session
     */
    boolean isActiveSession(long sessionId) {
        Session session = activeSession;
        return session != null
                && session.id == sessionId
                && !session.closed
                && !stopRequested;
    }

    /**
     * Starts a non-blocking connection attempt.
     *
     * <p>Configuration is validated before a socket is opened. A duplicate Connect
     * action is ignored while another attempt or session is active. Connection work
     * runs on the supervisor executor, so an unreachable server cannot block the
     * JavaFX application thread.</p>
     */
    void start() {
        long token;
        synchronized (this) {
            if (!stopRequested && state.isConnectionAttemptActive()) {
                return;
            }

            try {
                validateConfiguration();
            } catch (IllegalArgumentException invalidConfiguration) {
                stopRequested = true;
                transition(On4KstConnectionState.DISCONNECTED,
                        "Invalid ON4KST configuration: "
                                + invalidConfiguration.getMessage(), true);
                return;
            }

            stopRequested = false;
            reconnectAttempt = 0;
            token = generation.incrementAndGet();
            transition(On4KstConnectionState.CONNECTING,
                    "Opening ON4KST connection", false);
        }

        scheduler.execute(() -> openConnection(token));
    }

    /**
     * Stops the current session and invalidates every scheduled callback or reconnect
     * belonging to it.
     */
    void stopByUser() {
        Session oldSession;
        synchronized (this) {
            stopRequested = true;
            generation.incrementAndGet();
            transition(On4KstConnectionState.STOPPING,
                    "Disconnecting from ON4KST", false);
            oldSession = activeSession;
            activeSession = null;
        }

        closeSession(oldSession);
        controller.onOn4KstConnectionLost();
        transition(On4KstConnectionState.DISCONNECTED,
                "Disconnected by user", false);
    }

    /**
     * Records one received protocol line as proof of application-level liveness.
     *
     * <p>TCP's {@code isConnected()} only states that a connection once succeeded.
     * It does not prove that the peer is still reachable. Updating the inbound
     * timestamp here gives the monitor a meaningful end-to-end signal.</p>
     *
     * @param sessionId immutable source-session id
     * @param line complete protocol line received from ON4KST
     */
    void onInboundActivity(long sessionId, String line) {
        Session session = activeSession;
        if (session == null || session.id != sessionId || session.closed) {
            return;
        }

        long now = System.currentTimeMillis();
        session.lastInboundMillis.set(now);
        session.lastProgressMillis.set(now);

        String opcode = opcode(line);
        if ("CK".equals(opcode)) {
            sendHeartbeat(session);
        }

        if (!session.loginSent
                && line != null
                && line.toLowerCase(Locale.ROOT).contains("login")) {
            scheduler.execute(() -> sendLogin(sessionId));
        }

        if ("CH".equals(opcode) || "CR".equals(opcode)) {
            recordHistoryTimestamp(line);
        }
    }

    void onLogstat(long sessionId, String[] fields) {
        String[] copy = fields == null ? new String[0] : fields.clone();
        scheduler.execute(() -> handleLogstat(sessionId, copy));
    }

    void stageInitialChatMember(long sessionId, ChatMember member) {
        Session session = activeSession;
        if (session == null || session.id != sessionId || member == null
                || member.getChatCategory() == null || member.getCallSign() == null) {
            return;
        }

        int category = member.getChatCategory().getCategoryNumber();
        session.initialMembers
                .computeIfAbsent(category, ignored -> new ConcurrentHashMap<>())
                .put(member.getCallSign().trim().toUpperCase(Locale.ROOT), member);
        session.lastProgressMillis.set(System.currentTimeMillis());
    }

    void onInitialUserListCompleted(long sessionId, ChatCategory category) {
        if (category == null) {
            return;
        }
        scheduler.execute(() -> completeInitialUserList(
                sessionId, category.getCategoryNumber()));
    }

    private void openConnection(long token) {
        if (!mayOpen(token)) {
            return;
        }

        LOGGER.log(Level.INFO,
                "Opening ON4KST TCP session {0}", token);
        Socket socket = new Socket();
        try {
            ChatPreferences preferences = controller.getChatPreferences();
            socket.connect(new InetSocketAddress(
                            preferences.getStn_on4kstServersDns(),
                            preferences.getStn_on4kstServersPort()),
                    CONNECT_TIMEOUT_MILLIS);
            configureSocket(socket);
            LOGGER.log(Level.INFO,
                    "ON4KST TCP session {0} connected to {1}",
                    new Object[] {token, socket.getRemoteSocketAddress()});

            LinkedBlockingQueue<ChatMessage> receiveQueue =
                    new LinkedBlockingQueue<>();
            LinkedBlockingQueue<ChatMessage> transmitQueue =
                    new LinkedBlockingQueue<>();
            Session session = new Session(token, socket, receiveQueue, transmitQueue);

            ReadThread readThread = new ReadThread(
                    token, socket, receiveQueue, this::isActiveSession,
                    line -> onInboundActivity(token, line),
                    failure -> onConnectionFailure(token, failure));
            WriteThread writeThread = new WriteThread(
                    token, socket, transmitQueue,
                    controller.getChatPreferences().getLoginChatCategoryMain()
                            .getCategoryNumber(),
                    this::isActiveSession,
                    failure -> onConnectionFailure(token, failure),
                    controller::onOn4KstOutboundFrameRejected);
            MessageBusManagementThread messageProcessor =
                    new MessageBusManagementThread(
                            controller, controller, token, receiveQueue,
                            this::isActiveSession);

            session.readThread = readThread;
            session.writeThread = writeThread;
            session.messageProcessor = messageProcessor;

            synchronized (this) {
                if (!mayOpen(token)) {
                    closeSession(session);
                    return;
                }
                activeSession = session;
                controller.installOn4KstSession(
                        token, socket, receiveQueue, transmitQueue,
                        readThread, writeThread, messageProcessor);
                transition(On4KstConnectionState.WAITING_FOR_LOGIN_PROMPT,
                        "TCP connected; waiting for ON4KST login prompt", false);
            }

            messageProcessor.start();
            writeThread.start();
            readThread.start();
            scheduler.schedule(
                    () -> sendLogin(token), LOGIN_FALLBACK_MILLIS,
                    TimeUnit.MILLISECONDS);
        } catch (Throwable exception) {
            // Errors must be caught as well: an Error escaping here would be
            // swallowed by the scheduler and leave the state machine stuck in
            // CONNECTING without any reconnect attempt or user visible failure.
            try {
                socket.close();
            } catch (IOException ignored) {
                // The original connection exception is more useful.
            }
            scheduler.execute(() -> handleOpenFailure(token, exception));
        }
    }

    private boolean mayOpen(long token) {
        return !stopRequested && generation.get() == token;
    }

    private void sendLogin(long sessionId) {
        Session session = activeSession;
        if (session == null || session.id != sessionId || session.loginSent
                || session.closed || stopRequested) {
            return;
        }

        try {
            ChatPreferences preferences = controller.getChatPreferences();
            int mainCategory = preferences.getLoginChatCategoryMain()
                    .getCategoryNumber();
            long historyFrom = Math.max(
                    0L, lastReceivedMessageTimestamp.get() - 1L);
            String login = On4KstProtocol.login(
                    preferences.getStn_loginCallSign(),
                    preferences.getStn_loginPassword(),
                    mainCategory,
                    "KST4Contest v" + ApplicationConstants.APPLICATION_CURRENT_VERSION,
                    historyFrom);

            session.loginSent = true;
            session.lastProgressMillis.set(System.currentTimeMillis());
            LOGGER.log(Level.INFO,
                    "Sending ON4KST login for session {0}, main category {1}",
                    new Object[] {sessionId, mainCategory});
            transition(On4KstConnectionState.AUTHENTICATING,
                    "ON4KST login sent", false);
            sendControl(session, login);
        } catch (IllegalArgumentException invalidConfiguration) {
            failPermanently(session,
                    "Invalid ON4KST login configuration: "
                            + invalidConfiguration.getMessage());
        }
    }

    private void handleLogstat(long sessionId, String[] fields) {
        Session session = activeSession;
        if (session == null || session.id != sessionId || session.closed) {
            return;
        }

        String code = fields.length > 1 ? fields[1] : "";
        if (!"100".equals(code)) {
            String serverText = fields.length > 2 ? fields[2] : "Login rejected";
            failPermanently(session,
                    "ON4KST login rejected (" + code + "): " + serverText);
            return;
        }

        if (session.authenticated) {
            return;
        }
        session.authenticated = true;
        session.lastProgressMillis.set(System.currentTimeMillis());
        LOGGER.log(Level.INFO,
                "ON4KST login accepted for session {0}", sessionId);

        int mainCategory = controller.getChatPreferences()
                .getLoginChatCategoryMain().getCategoryNumber();
        transition(On4KstConnectionState.SYNCING_MAIN_CHAT,
                "Login accepted; loading main chat", false);
        sendControl(session, On4KstProtocol.settingsDone(mainCategory));
    }

    /**
     * Publishes the initial user snapshot for one chat category exactly once.
     *
     * <p>ON4KST can send further {@code UE} frames after live user updates or
     * after commands such as {@code SETNAME} and {@code BACK}. Those frames do
     * not announce a new, empty snapshot. Treating them as another initial-list
     * completion would remove the already published members because the staging
     * map was consumed by the first {@code UE} frame.</p>
     *
     * <p>The completed-category set is updated before the staging map is removed.
     * This makes the operation idempotent even if completion callbacks should
     * later be invoked from more than one thread. A genuinely empty initial list
     * remains valid: the first {@code UE} for a category is always processed,
     * even when no preceding valid {@code UA0} frame was staged.</p>
     *
     * @param sessionId immutable id of the socket session that received the frame
     * @param categoryNumber numeric ON4KST category terminated by {@code UE}
     */
    private void completeInitialUserList(long sessionId, int categoryNumber) {
        Session session = activeSession;
        if (session == null || session.id != sessionId || session.closed) {
            return;
        }

        if (!session.completedInitialUserLists.add(categoryNumber)) {
            LOGGER.log(Level.FINE,
                    "ON4KST session {0}: ignoring duplicate user-list end "
                            + "marker for category {1}; the initial snapshot "
                            + "has already been published",
                    new Object[] {sessionId, categoryNumber});
            return;
        }

        Map<String, ChatMember> staged =
                session.initialMembers.remove(categoryNumber);
        Collection<ChatMember> completeMembers = staged == null
                ? List.of()
                : new ArrayList<>(staged.values());

        LOGGER.log(Level.INFO,
                "ON4KST session {0}: complete user list for category {1} "
                        + "contains {2} valid users",
                new Object[] {
                        sessionId,
                        categoryNumber,
                        completeMembers.size()
                });

        controller.replaceActiveChatMembersForCategory(
                sessionId,
                new ChatCategory(categoryNumber),
                completeMembers);

        ChatPreferences preferences = controller.getChatPreferences();
        int mainCategory =
                preferences.getLoginChatCategoryMain().getCategoryNumber();

        if (categoryNumber == mainCategory && !session.mainListComplete) {
            session.mainListComplete = true;
            configureMainChat(session);

            if (hasDistinctSecondChat(preferences)) {
                int secondCategory =
                        preferences.getLoginChatCategorySecond()
                                .getCategoryNumber();

                transition(
                        On4KstConnectionState.SYNCING_SECOND_CHAT,
                        "Main chat ready; loading second chat",
                        false);

                sendControl(
                        session,
                        On4KstProtocol.addChat(
                                secondCategory,
                                Math.max(
                                        0L,
                                        lastReceivedMessageTimestamp.get() - 1L)));
            } else {
                markOnline(session);
            }
            return;
        }

        if (hasDistinctSecondChat(preferences)
                && categoryNumber
                == preferences.getLoginChatCategorySecond()
                .getCategoryNumber()
                && !session.secondListComplete) {
            session.secondListComplete = true;
            configureSecondChat(session);
            markOnline(session);
        }
    }

    private void configureMainChat(Session session) {
        ChatPreferences preferences = controller.getChatPreferences();
        int category = preferences.getLoginChatCategoryMain().getCategoryNumber();
        sendControl(session, On4KstProtocol.setLocator(
                category, preferences.getStn_loginLocatorMainCat()));
        if (preferences.getStn_loginNameMainCat() != null
                && !preferences.getStn_loginNameMainCat().isBlank()) {
            sendControl(session, On4KstProtocol.setName(
                    category, preferences.getStn_loginNameMainCat()));
        }
        sendControl(session, On4KstProtocol.back(category));

        String secondLocator = preferences.getStn_loginLocatorSecondCat();
        String mainLocator = preferences.getStn_loginLocatorMainCat();
        if (preferences.isLoginToSecondChatEnabled()
                && secondLocator != null && !secondLocator.isBlank()
                && !secondLocator.equalsIgnoreCase(mainLocator)) {
            controller.onOn4KstConnectionWarning(
                    "ON4KST uses one locator per TCP session. The second-chat locator '"
                            + secondLocator + "' is ignored; using '" + mainLocator + "'.");
        }
    }

    private void configureSecondChat(Session session) {
        ChatPreferences preferences = controller.getChatPreferences();
        int category = preferences.getLoginChatCategorySecond().getCategoryNumber();
        if (preferences.getStn_loginNameSecondCat() != null
                && !preferences.getStn_loginNameSecondCat().isBlank()) {
            sendControl(session, On4KstProtocol.setName(
                    category, preferences.getStn_loginNameSecondCat()));
        }
        sendControl(session, On4KstProtocol.back(category));
    }

    private void markOnline(Session session) {
        if (!isActiveSession(session.id)) {
            return;
        }
        reconnectAttempt = 0;
        session.online = true;
        session.lastProgressMillis.set(System.currentTimeMillis());
        LOGGER.log(Level.INFO,
                "ON4KST session {0} is authenticated and synchronized",
                session.id);
        transition(On4KstConnectionState.ONLINE,
                "ON4KST session is authenticated and synchronized", false);
        controller.onOn4KstConnectionOnline();
    }

    private void sendControl(Session session, String frame) {
        if (session == null || !isActiveSession(session.id)) {
            return;
        }
        ChatMessage message = new ChatMessage();
        message.setMessageDirectedToServer(true);
        message.setMessageText(frame);
        session.transmitQueue.offer(message);
    }

    private void sendHeartbeat(Session session) {
        if (session == null || !isActiveSession(session.id)) {
            return;
        }
        long now = System.currentTimeMillis();
        session.lastHeartbeatMillis.set(now);
        LOGGER.log(Level.FINE,
                "Sending application heartbeat for ON4KST session {0}",
                session.id);
        ChatMessage heartbeat = new ChatMessage();
        heartbeat.setMessageDirectedToServer(true);
        heartbeat.setMessageText("");
        session.transmitQueue.offer(heartbeat);
    }

    private void onConnectionFailure(long sessionId, Throwable failure) {
        scheduler.execute(() -> failSession(sessionId, failure));
    }

    private void failSession(long sessionId, Throwable failure) {
        Session failedSession;
        synchronized (this) {
            failedSession = activeSession;
            if (failedSession == null || failedSession.id != sessionId
                    || failedSession.closed) {
                return;
            }
            activeSession = null;
            failedSession.closed = true;
        }

        closeSession(failedSession);
        controller.onOn4KstConnectionLost();
        if (stopRequested) {
            transition(On4KstConnectionState.DISCONNECTED,
                    "ON4KST connection stopped", false);
            return;
        }

        scheduleReconnect(failure);
    }

    private void handleOpenFailure(long token, Throwable failure) {
        if (!mayOpen(token)) {
            return;
        }
        controller.onOn4KstConnectionLost();
        scheduleReconnect(failure);
    }

    private void scheduleReconnect(Throwable failure) {
        if (stopRequested) {
            transition(On4KstConnectionState.DISCONNECTED,
                    "ON4KST connection stopped", false);
            return;
        }

        String reason = describeFailure(failure);
        LOGGER.log(Level.WARNING,
                "ON4KST connection lost; automatic reconnect scheduled", failure);
        long delay = RECONNECT_DELAYS_MILLIS.get(Math.min(
                reconnectAttempt, RECONNECT_DELAYS_MILLIS.size() - 1));
        reconnectAttempt++;
        transition(On4KstConnectionState.RECONNECT_WAIT,
                "Connection lost (" + reason + "); reconnecting in "
                        + Duration.ofMillis(delay).toSeconds() + " s", true);

        long nextToken = generation.incrementAndGet();
        scheduler.schedule(() -> {
            if (!mayOpen(nextToken)) {
                return;
            }
            transition(On4KstConnectionState.CONNECTING,
                    "Reconnecting to ON4KST", false);
            openConnection(nextToken);
        }, delay, TimeUnit.MILLISECONDS);
    }

    private void failPermanently(Session session, String reason) {
        if (session == null || !isActiveSession(session.id)) {
            return;
        }
        stopRequested = true;
        generation.incrementAndGet();
        activeSession = null;
        session.closed = true;
        closeSession(session);
        controller.onOn4KstConnectionLost();
        LOGGER.log(Level.WARNING, reason);
        transition(On4KstConnectionState.DISCONNECTED, reason, true);
    }

    private void monitorActiveSession() {
        try {
            Session session = activeSession;
            if (session == null || session.closed || stopRequested) {
                return;
            }

            if (session.socket.isClosed()) {
                failSession(session.id,
                        new SocketException("Socket is closed"));
                return;
            }

            long now = System.currentTimeMillis();
            if (!session.online
                    && now - session.lastProgressMillis.get()
                    > HANDSHAKE_TIMEOUT_MILLIS) {
                failSession(session.id,
                        new SocketException("ON4KST handshake timed out"));
                return;
            }

            long inboundIdle = now - session.lastInboundMillis.get();
            if (inboundIdle > INBOUND_STALE_AFTER_MILLIS) {
                failSession(session.id,
                        new SocketException("No ON4KST data received for "
                                + inboundIdle / 1_000L + " seconds"));
                return;
            }

            if (inboundIdle > APPLICATION_HEARTBEAT_AFTER_MILLIS
                    && session.lastHeartbeatMillis.get()
                    < session.lastInboundMillis.get()) {
                sendHeartbeat(session);
            }
        } catch (RuntimeException exception) {
            LOGGER.log(Level.WARNING,
                    "ON4KST connection monitor failed", exception);
        }
    }

    private void validateConfiguration() {
        ChatPreferences preferences = controller.getChatPreferences();
        On4KstProtocol.login(
                preferences.getStn_loginCallSign(),
                preferences.getStn_loginPassword(),
                preferences.getLoginChatCategoryMain().getCategoryNumber(),
                "KST4Contest v" + ApplicationConstants.APPLICATION_CURRENT_VERSION,
                0L);
        On4KstProtocol.locator(preferences.getStn_loginLocatorMainCat());
        if (preferences.getStn_loginNameMainCat() != null
                && !preferences.getStn_loginNameMainCat().isBlank()) {
            On4KstProtocol.field(
                    preferences.getStn_loginNameMainCat(), "main chat name");
        }

        if (preferences.isLoginToSecondChatEnabled()) {
            if (preferences.getLoginChatCategorySecond() == null) {
                throw new IllegalArgumentException("Second chat has no category");
            }
            On4KstProtocol.category(
                    preferences.getLoginChatCategorySecond().getCategoryNumber());
            if (preferences.getLoginChatCategorySecond().getCategoryNumber()
                    == preferences.getLoginChatCategoryMain().getCategoryNumber()) {
                controller.onOn4KstConnectionWarning(
                        "Second ON4KST chat equals the main chat and will not be added twice.");
            }
            if (preferences.getStn_loginNameSecondCat() != null
                    && !preferences.getStn_loginNameSecondCat().isBlank()) {
                On4KstProtocol.field(
                        preferences.getStn_loginNameSecondCat(), "second chat name");
            }
        }
    }

    private boolean hasDistinctSecondChat(ChatPreferences preferences) {
        return preferences.isLoginToSecondChatEnabled()
                && preferences.getLoginChatCategorySecond() != null
                && preferences.getLoginChatCategorySecond().getCategoryNumber()
                != preferences.getLoginChatCategoryMain().getCategoryNumber();
    }

    private void configureSocket(Socket socket) throws IOException {
        socket.setTcpNoDelay(true);
        socket.setKeepAlive(true);

        try {
            socket.setOption(ExtendedSocketOptions.TCP_KEEPIDLE, 45);
            socket.setOption(ExtendedSocketOptions.TCP_KEEPINTERVAL, 15);
            socket.setOption(ExtendedSocketOptions.TCP_KEEPCOUNT, 3);
        } catch (UnsupportedOperationException | IOException | LinkageError exception) {
            // LinkageError covers runtime images built without the jdk.net module;
            // the connection stays usable, only kernel side keepalive is missing.
            LOGGER.log(Level.INFO,
                    "Platform does not support configurable TCP keepalive; "
                            + "application heartbeat remains active", exception);
        }
    }

    private void closeSession(Session session) {
        if (session == null) {
            return;
        }
        LOGGER.log(Level.FINE,
                "Closing ON4KST session {0}", session.id);
        session.closed = true;
        if (session.readThread != null) {
            session.readThread.interrupt();
        }
        if (session.writeThread != null) {
            session.writeThread.interrupt();
        }
        if (session.messageProcessor != null) {
            session.messageProcessor.interrupt();
        }
        try {
            session.socket.close();
        } catch (IOException exception) {
            LOGGER.log(Level.FINE, "Error closing obsolete ON4KST socket", exception);
        }
    }

    private void transition(
            On4KstConnectionState newState,
            String detail,
            boolean critical
    ) {
        On4KstConnectionState previousState = state;
        state = newState;
        Level level = critical
                || newState == On4KstConnectionState.DISCONNECTED
                || newState == On4KstConnectionState.RECONNECT_WAIT
                ? Level.WARNING : Level.INFO;
        LOGGER.log(level,
                "ON4KST state {0} -> {1}; detail: {2}",
                new Object[] {previousState, newState, detail});
        controller.updateOn4KstConnectionState(newState, detail, critical);
    }

    private void recordHistoryTimestamp(String line) {
        long timestamp = parseMessageTimestamp(line);
        if (timestamp > 0L) {
            lastReceivedMessageTimestamp.accumulateAndGet(timestamp, Math::max);
        }
    }

    static long parseMessageTimestamp(String line) {
        String[] fields = line == null ? new String[0] : line.split("\\|", -1);
        if (fields.length < 3) {
            return 0L;
        }

        try {
            long numeric = Long.parseLong(fields[2]);
            long now = System.currentTimeMillis() / 1_000L;
            if (numeric > 0L && numeric <= now + 86_400L) {
                return numeric;
            }
        } catch (NumberFormatException ignored) {
            return 0L;
        }

        try {
            return LocalDateTime.parse(fields[2], LIVE_MESSAGE_TIMESTAMP)
                    .toEpochSecond(ZoneOffset.UTC);
        } catch (DateTimeParseException ignored) {
            return 0L;
        }
    }

    private String opcode(String line) {
        if (line == null) {
            return "";
        }
        int separator = line.indexOf('|');
        return (separator < 0 ? line : line.substring(0, separator))
                .trim().toUpperCase(Locale.ROOT);
    }

    private String describeFailure(Throwable failure) {
        if (failure == null) {
            return "unknown error";
        }
        String message = failure.getMessage();
        return message == null || message.isBlank()
                ? failure.getClass().getSimpleName() : message;
    }

    private static final class Session {
        private final Set<Integer> completedInitialUserLists =
                ConcurrentHashMap.newKeySet();

        private final long id;
        private final Socket socket;
        private final LinkedBlockingQueue<ChatMessage> receiveQueue;
        private final LinkedBlockingQueue<ChatMessage> transmitQueue;
        private final long connectedMillis = System.currentTimeMillis();
        private final AtomicLong lastInboundMillis =
                new AtomicLong(connectedMillis);
        private final AtomicLong lastProgressMillis =
                new AtomicLong(connectedMillis);
        private final AtomicLong lastHeartbeatMillis = new AtomicLong();
        private final Map<Integer, Map<String, ChatMember>> initialMembers =
                new ConcurrentHashMap<>();

        private volatile ReadThread readThread;
        private volatile WriteThread writeThread;
        private volatile MessageBusManagementThread messageProcessor;
        private volatile boolean loginSent;
        private volatile boolean authenticated;
        private volatile boolean mainListComplete;
        private volatile boolean secondListComplete;
        private volatile boolean online;
        private volatile boolean closed;

        private Session(
                long id,
                Socket socket,
                LinkedBlockingQueue<ChatMessage> receiveQueue,
                LinkedBlockingQueue<ChatMessage> transmitQueue
        ) {
            this.id = id;
            this.socket = socket;
            this.receiveQueue = receiveQueue;
            this.transmitQueue = transmitQueue;
        }
    }
}