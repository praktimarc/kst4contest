package kst4contest.controller;

import kst4contest.model.ThreadStateMessage;

public interface ThreadStatusCallback {
    void onThreadStatus(String threadName, ThreadStateMessage threadStateMessage);

}
