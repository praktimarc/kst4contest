package kst4contest.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import kst4contest.controller.MessageVariableResolver;
import kst4contest.model.AirPlane;
import kst4contest.model.AirPlaneReflectionInfo;
import kst4contest.model.ChatMember;
import kst4contest.model.ChatPreferences;

class MessageVariableResolverTest {

    private ChatPreferences chatPreferences;
    private MessageVariableResolver resolver;

    @BeforeEach
    void setUp() {
        chatPreferences = mock(ChatPreferences.class);

        when(chatPreferences.getMYQRGFirstCat()).thenReturn(new SimpleStringProperty("144.388.03"));
        when(chatPreferences.getMYQRGSecondCat()).thenReturn(new SimpleStringProperty("432.088.00"));
        when(chatPreferences.getStn_loginLocatorMainCat()).thenReturn("JO51IJ");
        when(chatPreferences.getStn_loginCallSign()).thenReturn("DO5AMF");
        when(chatPreferences.getActualQTF()).thenReturn(new SimpleDoubleProperty(135.0));

        resolver = new MessageVariableResolver(chatPreferences);
    }

    @Test
    void resolvesAllGlobalVariablesInEveryMessageContext() {
        String template = "MYCALL MYQRGSHORT MYQRG SECONDQRG MYLOCATORSHORT MYLOCATOR MYQTF";

        assertEquals(
                "DO5AMF 144.388 144.388.03 432.088.00 JO51 JO51IJ 135",
                resolver.resolveGlobalVariables(template)
        );
    }

    @Test
    void shortValuesRemainSafeWhenTheConfiguredValueIsShorterThanExpected() {
        when(chatPreferences.getMYQRGFirstCat()).thenReturn(new SimpleStringProperty("144"));
        when(chatPreferences.getStn_loginLocatorMainCat()).thenReturn("JO5");

        assertEquals("144 JO5", resolver.resolveGlobalVariables("MYQRGSHORT MYLOCATORSHORT"));
    }

    @Test
    void resolvesSelectedStationNameAndTheFirstTwoAirPlanes() {
        ChatMember selectedStation = new ChatMember();
        selectedStation.setCallSign("DL0TEST");
        selectedStation.setName("Test Operator");

        AirPlane firstAirPlane = new AirPlane();
        firstAirPlane.setPotential(100);
        firstAirPlane.setArrivingDurationMinutes(1);

        AirPlane secondAirPlane = new AirPlane();
        secondAirPlane.setPotential(75);
        secondAirPlane.setArrivingDurationMinutes(9);

        AirPlaneReflectionInfo reflectionInfo = new AirPlaneReflectionInfo();
        reflectionInfo.setRisingAirplanes(FXCollections.observableArrayList(firstAirPlane, secondAirPlane));
        selectedStation.setAirPlaneReflectInfo(reflectionInfo);

        assertEquals(
                "Hi Test Operator, a very big AP in 1 min; Next big AP in 9 min",
                resolver.resolveForSelectedStation(
                        "Hi QRZNAME, FIRSTAP; SECONDAP",
                        selectedStation
                )
        );
    }

    @Test
    void usesCallsignWhenTheSelectedStationHasNoName() {
        ChatMember selectedStation = new ChatMember();
        selectedStation.setCallSign("DL0TEST");
        selectedStation.setName(" ");

        assertEquals(
                "Hi DL0TEST",
                resolver.resolveForSelectedStation("Hi QRZNAME", selectedStation)
        );
    }

    @Test
    void keepsStationVariablesVisibleWhenNoStationIsSelected() {
        assertEquals(
                "QRZNAME FIRSTAP SECONDAP",
                resolver.resolveForSelectedStation("QRZNAME FIRSTAP SECONDAP", null)
        );
    }

    @Test
    void returnsUsefulFallbacksWhenNoAirPlaneIsAvailable() {
        ChatMember selectedStation = new ChatMember();
        selectedStation.setCallSign("DL0TEST");

        assertEquals(
                "no ap available ",
                resolver.resolveForSelectedStation("FIRSTAP SECONDAP", selectedStation)
        );
    }
}