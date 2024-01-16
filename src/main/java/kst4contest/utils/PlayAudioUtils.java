package kst4contest.utils;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

/**
 * This part of the client drives the sounds. Its a singleton instance. All audio outs are directed to this instance.<br>
 * <br>
 * Instance of this classes will use the audio-notification-settings of the ChatPreferences instance and then wheter
 * play a sound or not. Each method of this class will only cause an audio output if the settings for audio-out are
 * made. This is good for less typing work. If audio is switched off by the user, all audio out callings will do: <br/>
 * <b>nothing</b>.
 *
 */
public class PlayAudioUtils {

//    public boolean useNormalProgramSounds, useCWCallSignSounds, useVoiceCallSignSounds;

//    public void setUseNormalProgramSounds(boolean useNormalProgramSounds) {
//        this.useNormalProgramSounds = useNormalProgramSounds;
//    }
//
//    public void setUseCWCallSignSounds(boolean useCWCallSignSounds) {
//        this.useCWCallSignSounds = useCWCallSignSounds;
//    }
//
//    public void setUseVoiceCallSignSounds(boolean useVoiceCallSignSounds) {
//        this.useVoiceCallSignSounds = useVoiceCallSignSounds;
//    }

    private Queue<Media> musicList = new LinkedList<Media>();
    private MediaPlayer mediaPlayer ;

    /**
     * Plays notification sounds out of the windws 95 box by given action character<br/>
     *<br/>
     * <b>Note that the audio settings (ChatPreferences) must be switched on in order to make the sounds playing.</b><br/>
     *  case '!': Startup<br/>
     *  case 'C': CQ Window new entry<br/>
     *  case 'P': PM Window new entry<br/>
     *  case 'E': Error occured<br/>
     *  case 'N': other notification sounds<br/>
     *
     * @param actionChar
     */
    public void playNoiseLauncher(char actionChar) {

            switch (actionChar){
                case '!':
                    musicList.add(new Media(new File ("NOISESTARTUP.mp3").toURI().toString()));
                    break;
                case 'C':
                    musicList.add(new Media(new File ("NOISECQWINDOW.mp3").toURI().toString()));
                    break;
                case 'P':
                    musicList.add(new Media(new File ("NOISEPMWINDOW.mp3").toURI().toString()));
                    break;
                case 'E':
                    musicList.add(new Media(new File ("NOISEERROR.mp3").toURI().toString()));
                    break;
                case 'N':
                    musicList.add(new Media(new File ("NOISENOTIFY.mp3").toURI().toString()));
                    break;
//                case 'M':
//                    musicList.add(new Media(new File ("VOICE.mp3").toURI().toString()));
//                    break;

                default:
                    System.out.println("[KST4ContestApp, warning, letter not defined!]");

        }
        playMusic();
//		mediaPlayer.dispose();

    }


    /**
     * Plays all chars of a given String-parameter as CW Sound out of the speaker.
     * As a workaround for delay problems at the beginning of playing, there are added 2x pause chars to the string.
     * <b>Note that the audio settings (ChatPreferences) must be switched on in order to make the sounds playing.</b><br/>
     * @param playThisChars
     */
     public void playCWLauncher(String playThisChars) {

        char[] playThisInCW = playThisChars.toUpperCase().toCharArray();


        for (char letterToPlay: playThisInCW){
            switch (letterToPlay){
                case 'A':
                    musicList.add(new Media(new File("LTTRA.mp3").toURI().toString()));
                    break;
                case 'B':
                    musicList.add(new Media(new File ("LTTRB.mp3").toURI().toString()));
                    break;
                case 'C':
                    musicList.add(new Media(new File ("LTTRC.mp3").toURI().toString()));
                    break;
                case 'D':
                    musicList.add(new Media(new File ("LTTRD.mp3").toURI().toString()));
                    break;
                case 'E':
                    musicList.add(new Media(new File ("LTTRE.mp3").toURI().toString()));
                    break;
                case 'F':
                    musicList.add(new Media(new File ("LTTRF.mp3").toURI().toString()));
                    break;
                case 'G':
                    musicList.add(new Media(new File ("LTTRG.mp3").toURI().toString()));
                    break;
                case 'H':
                    musicList.add(new Media(new File ("LTTRH.mp3").toURI().toString()));
                    break;
                case 'I':
                    musicList.add(new Media(new File ("LTTRI.mp3").toURI().toString()));
                    break;
                case 'J':
                    musicList.add(new Media(new File ("LTTRJ.mp3").toURI().toString()));
                    break;
                case 'K':
                    musicList.add(new Media(new File ("LTTRK.mp3").toURI().toString()));
                    break;
                case 'L':
                    musicList.add(new Media(new File ("LTTRL.mp3").toURI().toString()));
                    break;
                case 'M':
                    musicList.add(new Media(new File ("LTTRM.mp3").toURI().toString()));
                    break;
                case 'N':
                    musicList.add(new Media(new File ("LTTRN.mp3").toURI().toString()));
                    break;
                case 'O':
                    musicList.add(new Media(new File ("LTTRO.mp3").toURI().toString()));
                    break;
                case 'P':
                    musicList.add(new Media(new File ("LTTRP.mp3").toURI().toString()));
                    break;
                case 'Q':
                    musicList.add(new Media(new File ("LTTRQ.mp3").toURI().toString()));
                    break;
                case 'R':
                    musicList.add(new Media(new File ("LTTRR.mp3").toURI().toString()));
                    break;
                case 'S':
                    musicList.add(new Media(new File ("LTTRS.mp3").toURI().toString()));
                    break;
                case 'T':
                    musicList.add(new Media(new File ("LTTRT.mp3").toURI().toString()));
                    break;
                case 'U':
                    musicList.add(new Media(new File ("LTTRU.mp3").toURI().toString()));
                    break;
                case 'V':
                    musicList.add(new Media(new File ("LTTRV.mp3").toURI().toString()));
                    break;
                case 'W':
                    musicList.add(new Media(new File ("LTTRW.mp3").toURI().toString()));
                    break;
                case 'X':
                    musicList.add(new Media(new File ("LTTRX.mp3").toURI().toString()));
                    break;
                case 'Y':
                    musicList.add(new Media(new File ("LTTRY.mp3").toURI().toString()));
                    break;
                case 'Z':
                    musicList.add(new Media(new File ("LTTRZ.mp3").toURI().toString()));
                    break;
                case '1':
                    musicList.add(new Media(new File ("LTTR1.mp3").toURI().toString()));
                    break;
                case '2':
                    musicList.add(new Media(new File ("LTTR2.mp3").toURI().toString()));
                    break;
                case '3':
                    musicList.add(new Media(new File ("LTTR3.mp3").toURI().toString()));
                    break;
                case '4':
                    musicList.add(new Media(new File ("LTTR4.mp3").toURI().toString()));
                    break;
                case '5':
                    musicList.add(new Media(new File ("LTTR5.mp3").toURI().toString()));
                    break;
                case '6':
                    musicList.add(new Media(new File ("LTTR6.mp3").toURI().toString()));
                    break;
                case '7':
                    musicList.add(new Media(new File ("LTTR7.mp3").toURI().toString()));
                    break;
                case '8':
                    musicList.add(new Media(new File ("LTTR8.mp3").toURI().toString()));
                    break;
                case '9':
                    musicList.add(new Media(new File ("LTTR9.mp3").toURI().toString()));
                    break;
                case '0':
                    musicList.add(new Media(new File ("LTTR0.mp3").toURI().toString()));
                    break;
                case '/':
                    musicList.add(new Media(new File ("LTTRSTROKE.mp3").toURI().toString()));
                    break;
                case ' ':
                    musicList.add(new Media(new File ("LTTRSPACE.mp3").toURI().toString()));
                    break;
                default:
                    System.out.println("[KST4ContestApp, warning, letter not defined:] cwLetters = " + Arrays.toString(playThisInCW));
            }
        }
        playMusic();
//		mediaPlayer.dispose();

    }

    /**
     *
     * Plays a voice file for each char in the string (only EN alphabetic and numbers) except some specials: <br/><br/>
     * <b>Note that the audio settings (ChatPreferences) must be switched on in order to make the sounds playing.</b><br/><br/>
     * 	case '!': BELL<br/>
     *  case '?': YOUGOTMAIL<br/>
     * 	case '#': HELLO<br/>
     * 	case '*': 73 bye<br/>
     * 	case '$': STROKEPORTABLE<br/>
     * @param playThisChars
     */
     public void playVoiceLauncher(String playThisChars) {

        char[] playThisInCW = playThisChars.toUpperCase().toCharArray();

        for (char letterToPlay: playThisInCW){
            switch (letterToPlay){
                case '!':
                    musicList.add(new Media(new File ("VOICEBELL.mp3").toURI().toString()));
                    break;
                case '?':
                    musicList.add(new Media(new File ("VOICEYOUGOTMAIL.mp3").toURI().toString()));
                    break;
                case '#':
                    musicList.add(new Media(new File ("VOICEHELLO.mp3").toURI().toString()));
                    break;
                case '*':
                    musicList.add(new Media(new File ("VOICE73.mp3").toURI().toString()));
                    break;
                case '$':
                    musicList.add(new Media(new File ("VOICESTROKEPORTABLE.mp3").toURI().toString()));
                    break;
                case 'A':
                    musicList.add(new Media(new File ("VOICEA.mp3").toURI().toString()));
                    break;
                case 'B':
                    musicList.add(new Media(new File ("VOICEB.mp3").toURI().toString()));
                    break;
                case 'C':
                    musicList.add(new Media(new File ("VOICEC.mp3").toURI().toString()));
                    break;
                case 'D':
                    musicList.add(new Media(new File ("VOICED.mp3").toURI().toString()));
                    break;
                case 'E':
                    musicList.add(new Media(new File ("VOICEE.mp3").toURI().toString()));
                    break;
                case 'F':
                    musicList.add(new Media(new File ("VOICEF.mp3").toURI().toString()));
                    break;
                case 'G':
                    musicList.add(new Media(new File ("VOICEG.mp3").toURI().toString()));
                    break;
                case 'H':
                    musicList.add(new Media(new File ("VOICEH.mp3").toURI().toString()));
                    break;
                case 'I':
                    musicList.add(new Media(new File ("VOICEI.mp3").toURI().toString()));
                    break;
                case 'J':
                    musicList.add(new Media(new File ("VOICEJ.mp3").toURI().toString()));
                    break;
                case 'K':
                    musicList.add(new Media(new File ("VOICEK.mp3").toURI().toString()));
                    break;
                case 'L':
                    musicList.add(new Media(new File ("VOICEL.mp3").toURI().toString()));
                    break;
                case 'M':
                    musicList.add(new Media(new File ("VOICEM.mp3").toURI().toString()));
                    break;
                case 'N':
                    musicList.add(new Media(new File ("VOICEN.mp3").toURI().toString()));
                    break;
                case 'O':
                    musicList.add(new Media(new File ("VOICEO.mp3").toURI().toString()));
                    break;
                case 'P':
                    musicList.add(new Media(new File ("VOICEP.mp3").toURI().toString()));
                    break;
                case 'Q':
                    musicList.add(new Media(new File ("VOICEQ.mp3").toURI().toString()));
                    break;
                case 'R':
                    musicList.add(new Media(new File ("VOICER.mp3").toURI().toString()));
                    break;
                case 'S':
                    musicList.add(new Media(new File ("VOICES.mp3").toURI().toString()));
                    break;
                case 'T':
                    musicList.add(new Media(new File ("VOICET.mp3").toURI().toString()));
                    break;
                case 'U':
                    musicList.add(new Media(new File ("VOICEU.mp3").toURI().toString()));
                    break;
                case 'V':
                    musicList.add(new Media(new File ("VOICEV.mp3").toURI().toString()));
                    break;
                case 'W':
                    musicList.add(new Media(new File ("VOICEW.mp3").toURI().toString()));
                    break;
                case 'X':
                    musicList.add(new Media(new File ("VOICEX.mp3").toURI().toString()));
                    break;
                case 'Y':
                    musicList.add(new Media(new File ("VOICEY.mp3").toURI().toString()));
                    break;
                case 'Z':
                    musicList.add(new Media(new File ("VOICEZ.mp3").toURI().toString()));
                    break;
                case '1':
                    musicList.add(new Media(new File ("VOICE1.mp3").toURI().toString()));
                    break;
                case '2':
                    musicList.add(new Media(new File ("VOICE2.mp3").toURI().toString()));
                    break;
                case '3':
                    musicList.add(new Media(new File ("VOICE3.mp3").toURI().toString()));
                    break;
                case '4':
                    musicList.add(new Media(new File ("VOICE4.mp3").toURI().toString()));
                    break;
                case '5':
                    musicList.add(new Media(new File ("VOICE5.mp3").toURI().toString()));
                    break;
                case '6':
                    musicList.add(new Media(new File ("VOICE6.mp3").toURI().toString()));
                    break;
                case '7':
                    musicList.add(new Media(new File ("VOICE7.mp3").toURI().toString()));
                    break;
                case '8':
                    musicList.add(new Media(new File ("VOICE8.mp3").toURI().toString()));
                    break;
                case '9':
                    musicList.add(new Media(new File ("VOICE9.mp3").toURI().toString()));
                    break;
                case '0':
                    musicList.add(new Media(new File ("VOICE0.mp3").toURI().toString()));
                    break;
                case '/':
                    musicList.add(new Media(new File ("VOICESTROKE.mp3").toURI().toString()));
                    break;
//				case ' ':
//					musicList.add(new Media(new File ("VOICESPACE.mp3").toURI().toString()));
//					break;
                default:
                    System.out.println("[KST4ContestApp, warning, letter not defined:] cwLetters = " + Arrays.toString(playThisInCW));
            }
        }
        playMusic();
//		mediaPlayer.dispose();

    }
    private void playMusic() {

//		System.out.println("Kst4ContestApplication.playMusic");
        if(musicList.peek() == null)
        {
            return;
        }
        mediaPlayer = new MediaPlayer(musicList.poll());
        mediaPlayer.setRate(1.0);

        mediaPlayer.setOnReady(() -> {
            mediaPlayer.play();
            mediaPlayer.setOnEndOfMedia(() -> {
//				mediaPlayer.dispose();
                playMusic();
                if (musicList.isEmpty()) {
//					mediaPlayer.dispose();
                }
            });
        });

    }
}
