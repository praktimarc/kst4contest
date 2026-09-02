package kst4contest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javafx.collections.FXCollections;
import kst4contest.model.AirPlane;
import kst4contest.model.AirPlaneReflectionInfo;
import kst4contest.model.ChatMember;
import org.junit.jupiter.api.Test;

class MessageBusManagementThreadDxClusterCommentTest {

    @Test
    void keepsLocatorAndAddsCompactAirScoutInformation() {
        ChatMember sender = new ChatMember();
        sender.setQra("jo51hk");

        AirPlane firstAircraft = new AirPlane();
        firstAircraft.setArrivingDurationMinutes(1);
        firstAircraft.setPotential(100);

        AirPlane secondAircraft = new AirPlane();
        secondAircraft.setArrivingDurationMinutes(4);
        secondAircraft.setPotential(75);

        AirPlaneReflectionInfo reflectionInfo = new AirPlaneReflectionInfo();
        reflectionInfo.setRisingAirplanes(
                FXCollections.observableArrayList(
                        firstAircraft,
                        secondAircraft
                )
        );
        sender.setAirPlaneReflectInfo(reflectionInfo);

        assertEquals(
                "JO51HK AP 1m/100%;4m/75%",
                MessageBusManagementThread.buildDxClusterSpotComment(sender)
        );
    }

    @Test
    void returnsLocatorWhenAirScoutInformationIsMissing() {
        ChatMember sender = new ChatMember();
        sender.setQra("JO51HK");

        assertEquals(
                "JO51HK",
                MessageBusManagementThread.buildDxClusterSpotComment(sender)
        );
    }
}
