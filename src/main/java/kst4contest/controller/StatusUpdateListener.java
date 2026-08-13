package kst4contest.controller;

import kst4contest.model.ThreadStateMessage;

public interface StatusUpdateListener {

    /**
     * Thread (key) will send update status (value) to the view via this interface.
     *
     */
    void onThreadStatusChanged(String key, ThreadStateMessage threadStateMessage);


    /**
     * Called on change if the userlist to update the UI (sort the chatmembers list)
     */
    void onUserListUpdated(String reason);
    // new: userlist-update

    /**
     * Called whenever the authoritative ON4KST session changes lifecycle state.
     *
     * <p>The callback may originate from a background connection supervisor. A UI
     * implementation must marshal control changes onto its application thread.</p>
     *
     * @param state new connection, authentication or synchronization state
     * @param detail human-readable progress or failure reason
     */
    default void onConnectionStateChanged(
            On4KstConnectionState state,
            String detail
    ) {
        // Optional for non-UI listeners.
    }

}