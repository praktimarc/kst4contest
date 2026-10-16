package kst4contest.controller;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static kst4contest.controller.RadioSlot.FIRST;
import static kst4contest.controller.RadioSlot.SECOND;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Every row of the spec's two routing tables. */
class RadioSlotRouterTest {

    @Test
    void withoutSo2rEveryRadioInfoGoesToRadioOne() {
        assertEquals(Optional.of(FIRST), RadioSlotRouter.forRadioInfo(false, "2"));
        assertEquals(Optional.of(FIRST), RadioSlotRouter.forRadioInfo(false, null));
        assertEquals(Optional.of(FIRST), RadioSlotRouter.forRadioInfo(false, "x"));
    }

    @Test
    void withSo2rTheRadioNumberDecides() {
        assertEquals(Optional.of(FIRST), RadioSlotRouter.forRadioInfo(true, "1"));
        assertEquals(Optional.of(SECOND), RadioSlotRouter.forRadioInfo(true, "2"));
        assertEquals(Optional.of(SECOND), RadioSlotRouter.forRadioInfo(true, " 2 "));
    }

    @Test
    void withSo2rAMissingRadioNumberIsRadioOne() {
        assertEquals(Optional.of(FIRST), RadioSlotRouter.forRadioInfo(true, null));
        assertEquals(Optional.of(FIRST), RadioSlotRouter.forRadioInfo(true, "  "));
    }

    @Test
    void withSo2rOtherRadioNumbersAreDropped() {
        assertEquals(Optional.empty(), RadioSlotRouter.forRadioInfo(true, "3"));
        assertEquals(Optional.empty(), RadioSlotRouter.forRadioInfo(true, "0"));
        assertEquals(Optional.empty(), RadioSlotRouter.forRadioInfo(true, "x"));
    }

    @Test
    void withoutSo2rWinTestKeepsTheOldFilter() {
        assertEquals(Optional.of(FIRST), RadioSlotRouter.forWinTestStation(false, "STN1", "stn1", "STN2"));
        assertEquals(Optional.of(FIRST), RadioSlotRouter.forWinTestStation(false, "ANY", "", "STN2"));
        assertEquals(Optional.empty(), RadioSlotRouter.forWinTestStation(false, "STN2", "STN1", "STN2"));
    }

    @Test
    void withSo2rTheSecondNameWinsThenTheFirst() {
        assertEquals(Optional.of(SECOND), RadioSlotRouter.forWinTestStation(true, "stn2", "STN1", "STN2"));
        assertEquals(Optional.of(FIRST), RadioSlotRouter.forWinTestStation(true, "STN1", "STN1", "STN2"));
        assertEquals(Optional.of(FIRST), RadioSlotRouter.forWinTestStation(true, "ANY", "", "STN2"));
        assertEquals(Optional.empty(), RadioSlotRouter.forWinTestStation(true, "STN3", "STN1", "STN2"));
    }

    @Test
    void withSo2rAnEmptySecondNameSendsNothingToRadioTwo() {
        assertEquals(Optional.of(FIRST), RadioSlotRouter.forWinTestStation(true, "STN2", "", ""));
        assertEquals(Optional.empty(), RadioSlotRouter.forWinTestStation(true, "STN2", "STN1", ""));
    }
}
