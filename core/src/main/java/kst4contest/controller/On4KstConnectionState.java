package kst4contest.controller;

/**
 * Observable lifecycle of the ON4KST TCP session.
 *
 * <p>A connected TCP socket is deliberately not synonymous with an authenticated
 * chat session.  The intermediate states make that distinction visible to the UI
 * and prevent application messages from being sent in the wrong protocol context.</p>
 */
public enum On4KstConnectionState {
    DISCONNECTED,
    CONNECTING,
    WAITING_FOR_LOGIN_PROMPT,
    AUTHENTICATING,
    SYNCING_MAIN_CHAT,
    SYNCING_SECOND_CHAT,
    ONLINE,
    RECONNECT_WAIT,
    STOPPING;

    /**
     * Returns whether the complete application-level ON4KST handshake has finished.
     *
     * @return {@code true} only after authentication and all requested user lists
     *         have been synchronized
     */
    public boolean isOnline() {
        return this == ONLINE;
    }

    /**
     * Returns whether a connection attempt or usable session is currently owned by
     * the connection manager.
     *
     * <p>This is intentionally broader than {@link #isOnline()}. The UI uses it to
     * prevent a second Connect action while authentication, synchronization or a
     * scheduled reconnect is already in progress.</p>
     *
     * @return {@code true} while connecting, synchronizing, online or waiting for
     *         an automatic reconnect
     */
    public boolean isConnectionAttemptActive() {
        return switch (this) {
            case CONNECTING, WAITING_FOR_LOGIN_PROMPT, AUTHENTICATING,
                 SYNCING_MAIN_CHAT, SYNCING_SECOND_CHAT, ONLINE,
                 RECONNECT_WAIT -> true;
            default -> false;
        };
    }
}