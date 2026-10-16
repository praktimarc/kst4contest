package kst4contest.controller;

import kst4contest.model.ChatPreferences;
import kst4contest.observe.MutableValue;

/**
 * One of the two radios of an SO2R station: the place where its frequency and its
 * antenna heading are shown. Without SO2R only {@link #FIRST} is used.
 */
public enum RadioSlot {
    FIRST,
    SECOND;

    /** The frequency field of this radio: MYQRG for radio 1, SECONDQRG for radio 2. */
    public MutableValue<String> qrgIn(ChatPreferences prefs) {
        return this == FIRST ? prefs.getMYQRGFirstCat() : prefs.getMYQRGSecondCat();
    }

    /** The heading field of this radio: QTF for radio 1, QTF2 for radio 2. */
    public MutableValue<Double> qtfIn(ChatPreferences prefs) {
        return this == FIRST ? prefs.getActualQTF() : prefs.getActualQTF2();
    }
}
