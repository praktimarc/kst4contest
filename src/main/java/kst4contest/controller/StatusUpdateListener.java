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

}
