package kst4contest.test;

import javafx.application.Application;
import javafx.stage.Stage;
import kst4contest.utils.PlayAudioUtils;

public class TestAudioPlayerUtils extends Application {

    public static void main(String[] args) {

    }

    @Override
    public void start(Stage stage) throws Exception {

        PlayAudioUtils testAudio = new PlayAudioUtils();
        testAudio.playCWLauncher("DO5AMF");
    }
}
