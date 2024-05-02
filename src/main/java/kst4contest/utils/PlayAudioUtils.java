package kst4contest.utils;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import kst4contest.ApplicationConstants;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

/**
 * This part of the client drives the sounds. Its a singleton instance. All audio outs are directed to this instance.<br>
 * <br>
 * */
public class PlayAudioUtils {

    /**
     * Default constructor initializes the sound files and copies it to the project home folder
     */
    public PlayAudioUtils() {


        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/NOISESTARTUP.mp3", "NOISESTARTUP.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/NOISECQWINDOW.mp3", "NOISECQWINDOW.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/NOISEPMWINDOW.mp3", "NOISEPMWINDOW.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/NOISEERROR.mp3", "NOISEERROR.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/NOISENOTIFY.mp3", "NOISENOTIFY.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/tick.mp3", "tick.mp3");

        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/LTTRA.mp3", "LTTRA.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/LTTRB.mp3", "LTTRB.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/LTTRC.mp3", "LTTRC.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/LTTRD.mp3", "LTTRD.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/LTTRE.mp3", "LTTRE.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/LTTRF.mp3", "LTTRF.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/LTTRG.mp3", "LTTRG.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/LTTRH.mp3", "LTTRH.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/LTTRI.mp3", "LTTRI.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/LTTRJ.mp3", "LTTRJ.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/LTTRK.mp3", "LTTRK.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/LTTRL.mp3", "LTTRL.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/LTTRM.mp3", "LTTRM.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/LTTRN.mp3", "LTTRN.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/LTTRO.mp3", "LTTRO.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/LTTRP.mp3", "LTTRP.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/LTTRQ.mp3", "LTTRQ.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/LTTRR.mp3", "LTTRR.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/LTTRS.mp3", "LTTRS.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/LTTRT.mp3", "LTTRT.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/LTTRU.mp3", "LTTRU.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/LTTRV.mp3", "LTTRV.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/LTTRW.mp3", "LTTRW.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/LTTRX.mp3", "LTTRX.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/LTTRY.mp3", "LTTRY.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/LTTRZ.mp3", "LTTRZ.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/LTTR0.mp3", "LTTR0.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/LTTR1.mp3", "LTTR1.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/LTTR2.mp3", "LTTR2.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/LTTR3.mp3", "LTTR3.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/LTTR4.mp3", "LTTR4.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/LTTR5.mp3", "LTTR5.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/LTTR6.mp3", "LTTR6.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/LTTR7.mp3", "LTTR7.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/LTTR8.mp3", "LTTR8.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/LTTR9.mp3", "LTTR9.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/LTTRSTROKE.mp3", "LTTRSTROKE.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/LTTRSPACE.mp3", "LTTRSPACE.mp3");

        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/VOICEA.mp3", "VOICEA.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/VOICEB.mp3", "VOICEB.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/VOICEC.mp3", "VOICEC.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/VOICED.mp3", "VOICED.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/VOICEE.mp3", "VOICEE.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/VOICEF.mp3", "VOICEF.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/VOICEG.mp3", "VOICEG.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/VOICEH.mp3", "VOICEH.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/VOICEI.mp3", "VOICEI.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/VOICEJ.mp3", "VOICEJ.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/VOICEK.mp3", "VOICEK.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/VOICEL.mp3", "VOICEL.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/VOICEM.mp3", "VOICEM.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/VOICEN.mp3", "VOICEN.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/VOICEO.mp3", "VOICEO.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/VOICEP.mp3", "VOICEP.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/VOICEQ.mp3", "VOICEQ.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/VOICER.mp3", "VOICER.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/VOICES.mp3", "VOICES.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/VOICET.mp3", "VOICET.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/VOICEU.mp3", "VOICEU.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/VOICEV.mp3", "VOICEV.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/VOICEW.mp3", "VOICEW.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/VOICEX.mp3", "VOICEX.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/VOICEY.mp3", "VOICEY.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/VOICEZ.mp3", "VOICEZ.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/VOICE0.mp3", "VOICE0.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/VOICE1.mp3", "VOICE1.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/VOICE2.mp3", "VOICE2.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/VOICE3.mp3", "VOICE3.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/VOICE4.mp3", "VOICE4.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/VOICE5.mp3", "VOICE5.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/VOICE6.mp3", "VOICE6.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/VOICE7.mp3", "VOICE7.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/VOICE8.mp3", "VOICE8.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/VOICE9.mp3", "VOICE9.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/VOICESTROKE.mp3", "VOICESTROKE.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/VOICEBELL.mp3", "VOICEBELL.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/VOICEYOUGOTMAIL.mp3", "VOICEYOUGOTMAIL.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/VOICEHELLO.mp3", "VOICEHELLO.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/VOICE73.mp3", "VOICE73.mp3");
        ApplicationFileUtils.copyResourceIfRequired(ApplicationConstants.APPLICATION_NAME, "/VOICESTROKEPORTABLE.mp3", "VOICESTROKEPORTABLE.mp3");


    }

    private Queue<Media> musicList = new LinkedList<Media>();
    private MediaPlayer mediaPlayer ;

    /**
     * Plays notification sounds out of the windws 95 box by given action character<br/>
     *<br/>
     *
     *  case '!': Startup<br/>
     *  case '-': tick<br/>
     *  case 'C': CQ Window new entry<br/>
     *  case 'P': PM Window new entry<br/>
     *  case 'E': Error occured<br/>
     *  case 'N': other notification sounds<br/>
     *
     * @param actionChar
     */
    public void playNoiseLauncher(char actionChar) {



//        ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/NOISESTARTUP.mp3");


            switch (actionChar){
                case '-':
                    musicList.add(new Media(new File (ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/tick.mp3")).toURI().toString()));
                    break;
                case '!':
                    musicList.add(new Media(new File (ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/NOISESTARTUP.mp3")).toURI().toString()));
                    break;
                case 'C':
                    musicList.add(new Media(new File (ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/NOISECQWINDOW.mp3")).toURI().toString()));
                    break;
                case 'P':
                    musicList.add(new Media(new File (ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/NOISEPMWINDOW.mp3")).toURI().toString()));
                    break;
                case 'E':
                    musicList.add(new Media(new File (ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/NOISEERROR.mp3")).toURI().toString()));
                    break;
                case 'N':
                    musicList.add(new Media(new File (ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/NOISENOTIFY.mp3")).toURI().toString()));
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
     *
     * @param playThisChars
     */
     public void playCWLauncher(String playThisChars) {

        char[] playThisInCW = playThisChars.toUpperCase().toCharArray();


        for (char letterToPlay: playThisInCW){
            switch (letterToPlay){
                case 'A':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/LTTRA.mp3")).toURI().toString()));
                    break;
                case 'B':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/LTTRB.mp3")).toURI().toString()));
                    break;
                case 'C':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/LTTRC.mp3")).toURI().toString()));
                    break;
                case 'D':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/LTTRD.mp3")).toURI().toString()));
                    break;
                case 'E':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/LTTRE.mp3")).toURI().toString()));
                    break;
                case 'F':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/LTTRF.mp3")).toURI().toString()));
                    break;
                case 'G':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/LTTRG.mp3")).toURI().toString()));
                    break;
                case 'H':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/LTTRH.mp3")).toURI().toString()));
                    break;
                case 'I':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/LTTRI.mp3")).toURI().toString()));
                    break;
                case 'J':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/LTTRJ.mp3")).toURI().toString()));
                    break;
                case 'K':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/LTTRK.mp3")).toURI().toString()));
                    break;
                case 'L':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/LTTRL.mp3")).toURI().toString()));
                    break;
                case 'M':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/LTTRM.mp3")).toURI().toString()));
                    break;
                case 'N':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/LTTRN.mp3")).toURI().toString()));
                    break;
                case 'O':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/LTTRO.mp3")).toURI().toString()));
                    break;
                case 'P':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/LTTRP.mp3")).toURI().toString()));
                    break;
                case 'Q':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/LTTRQ.mp3")).toURI().toString()));
                    break;
                case 'R':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/LTTRR.mp3")).toURI().toString()));
                    break;
                case 'S':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/LTTRS.mp3")).toURI().toString()));
                    break;
                case 'T':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/LTTRT.mp3")).toURI().toString()));
                    break;
                case 'U':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/LTTRU.mp3")).toURI().toString()));
                    break;
                case 'V':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/LTTRV.mp3")).toURI().toString()));
                    break;
                case 'W':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/LTTRW.mp3")).toURI().toString()));
                    break;
                case 'X':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/LTTRX.mp3")).toURI().toString()));
                    break;
                case 'Y':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/LTTRY.mp3")).toURI().toString()));
                    break;
                case 'Z':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/LTTRZ.mp3")).toURI().toString()));
                    break;
                case '1':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/LTTR1.mp3")).toURI().toString()));
                    break;
                case '2':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/LTTR2.mp3")).toURI().toString()));
                    break;
                case '3':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/LTTR3.mp3")).toURI().toString()));
                    break;
                case '4':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/LTTR4.mp3")).toURI().toString()));
                    break;
                case '5':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/LTTR5.mp3")).toURI().toString()));
                    break;
                case '6':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/LTTR6.mp3")).toURI().toString()));
                    break;
                case '7':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/LTTR7.mp3")).toURI().toString()));
                    break;
                case '8':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/LTTR8.mp3")).toURI().toString()));
                    break;
                case '9':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/LTTR9.mp3")).toURI().toString()));
                    break;
                case '0':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/LTTR0.mp3")).toURI().toString()));
                    break;
                case '/':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/LTTRSTROKE.mp3")).toURI().toString()));
                    break;
                case ' ':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/LTTRSPACE.mp3")).toURI().toString()));
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

        char[] spellThisWithVoice = playThisChars.toUpperCase().toCharArray();

        for (char letterToPlay: spellThisWithVoice){
            switch (letterToPlay){
                case '!':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/VOICEBELL.mp3")).toURI().toString()));
                    break;
                case '?':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/VOICEYOUGOTMAIL.mp3")).toURI().toString()));
                    break;
                case '#':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/VOICEHELLO.mp3")).toURI().toString()));
                    break;
                case '*':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/VOICE73.mp3")).toURI().toString()));
                    break;
                case '$':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/VOICESTROKEPORTABLE.mp3")).toURI().toString()));
                    break;
                case 'A':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/VOICEA.mp3")).toURI().toString()));
                    break;
                case 'B':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/VOICEB.mp3")).toURI().toString()));
                    break;
                case 'C':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/VOICEC.mp3")).toURI().toString()));
                    break;
                case 'D':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/VOICED.mp3")).toURI().toString()));
                    break;
                case 'E':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/VOICEE.mp3")).toURI().toString()));
                    break;
                case 'F':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/VOICEF.mp3")).toURI().toString()));
                    break;
                case 'G':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/VOICEG.mp3")).toURI().toString()));
                    break;
                case 'H':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/VOICEH.mp3")).toURI().toString()));
                    break;
                case 'I':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/VOICEI.mp3")).toURI().toString()));
                    break;
                case 'J':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/VOICEJ.mp3")).toURI().toString()));
                    break;
                case 'K':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/VOICEK.mp3")).toURI().toString()));
                    break;
                case 'L':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/VOICEL.mp3")).toURI().toString()));
                    break;
                case 'M':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/VOICEM.mp3")).toURI().toString()));
                    break;
                case 'N':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/VOICEN.mp3")).toURI().toString()));
                    break;
                case 'O':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/VOICEO.mp3")).toURI().toString()));
                    break;
                case 'P':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/VOICEP.mp3")).toURI().toString()));
                    break;
                case 'Q':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/VOICEQ.mp3")).toURI().toString()));
                    break;
                case 'R':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/VOICER.mp3")).toURI().toString()));
                    break;
                case 'S':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/VOICES.mp3")).toURI().toString()));
                    break;
                case 'T':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/VOICET.mp3")).toURI().toString()));
                    break;
                case 'U':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/VOICEU.mp3")).toURI().toString()));
                    break;
                case 'V':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/VOICEV.mp3")).toURI().toString()));
                    break;
                case 'W':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/VOICEW.mp3")).toURI().toString()));
                    break;
                case 'X':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/VOICEX.mp3")).toURI().toString()));
                    break;
                case 'Y':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/VOICEY.mp3")).toURI().toString()));
                    break;
                case 'Z':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/VOICEZ.mp3")).toURI().toString()));
                    break;
                case '1':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/VOICE1.mp3")).toURI().toString()));
                    break;
                case '2':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/VOICE2.mp3")).toURI().toString()));
                    break;
                case '3':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/VOICE3.mp3")).toURI().toString()));
                    break;
                case '4':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/VOICE4.mp3")).toURI().toString()));
                    break;
                case '5':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/VOICE5.mp3")).toURI().toString()));
                    break;
                case '6':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/VOICE6.mp3")).toURI().toString()));
                    break;
                case '7':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/VOICE7.mp3")).toURI().toString()));
                    break;
                case '8':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/VOICE8.mp3")).toURI().toString()));
                    break;
                case '9':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/VOICE9.mp3")).toURI().toString()));
                    break;
                case '0':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/VOICE0.mp3")).toURI().toString()));
                    break;
                case '/':
                    musicList.add(new Media(new File(ApplicationFileUtils.getFilePath(ApplicationConstants.APPLICATION_NAME, "/VOICESTROKE.mp3")).toURI().toString()));
                    break;
//				case ' ':
//					musicList.add(new Media(new File ("VOICESPACE.mp3").toURI().toString()));
//					break;
                default:
                    System.out.println("[KST4ContestApp, warning, letter not defined:] cwLetters = " + Arrays.toString(spellThisWithVoice));
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
