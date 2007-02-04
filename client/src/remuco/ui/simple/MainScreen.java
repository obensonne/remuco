package remuco.ui.simple;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;

import remuco.connection.RemotePlayer;
import remuco.data.ObservablePlayerState;
import remuco.data.PlayerControl;
import remuco.data.PlayerState;
import remuco.data.Song;
import remuco.proto.Remuco;
import remuco.ui.IScreen;

public class MainScreen implements IScreen {

    /** RemucoCommand on main screen: Exit */
    private static final Command CMD_EXIT = new Command("Exit", Command.EXIT,
            90);

    /** Play-control command: Skip to next currentSong */
    private static final Command CMD_PC_NEXT = new Command("Next",
            Command.SCREEN, 1);

    /** Play-control command : Toogle play/pause */
    private static final Command CMD_PC_PLAYPAUSE = new Command("Play/Pause",
            Command.SCREEN, 3);

    /** Play-control command: Skip to next currentSong */
    private static final Command CMD_PC_PREV = new Command("Previous",
            Command.SCREEN, 2);

    /** Play-control command: Toogle play/pause */
    private static final Command CMD_PC_RESTART = new Command("Restart",
            Command.SCREEN, 5);

    /** Play-control command: Toogle play/pause */
    private static final Command CMD_PC_STOP = new Command("Stop",
            Command.SCREEN, 4);

    /** Show the playlist screen */
    private static final Command CMD_SHOW_PL = new Command("PlayList",
            Command.SCREEN, 10);

    /** Show the rating screen */
    private static final Command CMD_SHOW_RATE = new Command("Rate",
            Command.SCREEN, 20);

    /** Show the volume screen */
    private static final Command CMD_SHOW_VOLUME = new Command("Volume",
            Command.SCREEN, 20);

    private boolean active;

    private Display display;

    private Displayable displayable;

    private ObservablePlayerState ops;

    private CommandListener pcl;

    private RemotePlayer player;

    private IScreen screenPlaylist, screenVolume, screenRating;

    private SongForm sf;

    public MainScreen() {
    }

    public void commandAction(Command c, Displayable d) {
        if (c == CMD_EXIT) {
            setActive(false);
            pcl.commandAction(IScreen.CMD_DISPOSE, d);
        } else if (c == IScreen.CMD_DISPOSE) {
            // a screen set by us, sent this command
            setActive(true);
        } else if (c == CMD_PC_NEXT) {
            // sf.setTitle(stateToString(RemotePlayer.ST_INBETWEEN));
            player.control(PlayerControl.PC_NEXT);
        } else if (c == CMD_PC_PLAYPAUSE) {
            // sf.setTitle(stateToString(RemotePlayer.ST_INBETWEEN));
            player.control(PlayerControl.PC_PLAY_PAUSE);
        } else if (c == CMD_PC_PREV) {
            // sf.setTitle(stateToString(RemotePlayer.ST_INBETWEEN));
            player.control(PlayerControl.PC_PREV);
        } else if (c == CMD_PC_RESTART) {
            // sf.setTitle(stateToString(RemotePlayer.ST_INBETWEEN));
            player.control(PlayerControl.PC_RESTART);
        } else if (c == CMD_PC_STOP) {
            // sf.setTitle(stateToString(RemotePlayer.ST_INBETWEEN));
            player.control(PlayerControl.PC_STOP);
        } else if (c == CMD_SHOW_PL) {
            screenPlaylist.setActive(true);
        } else if (c == CMD_SHOW_VOLUME) {
            screenVolume.setActive(true);
        } else if (c == CMD_SHOW_RATE) {
            // set current song to null => this will rerender the song screen
            // if rating screen returna
            screenRating.setActive(true);
            sf.setSong(null);
        } else {
            pcl.commandAction(c, d);
        }
    }

    public void notifyPlayerStateChange() {
        if (active) {
            renderScreen();
        }
    }

    public void setActive(boolean active) {
        this.active = active;
        if (active) {
            display.setCurrent(displayable);
            renderScreen();
        }
    }

    public void setUp(CommandListener pcl, Display d, RemotePlayer player) {
        this.pcl = pcl;
        this.display = d;
        this.player = player;
        this.ops = player.getObservablePlayerState();
        init();
    }

    private void init() {
        active = false;

        sf = new SongForm("No Song");
        sf.addCommand(CMD_EXIT);
        sf.addCommand(CMD_PC_NEXT);
        sf.addCommand(CMD_PC_PLAYPAUSE);
        sf.addCommand(CMD_PC_PREV);
        sf.addCommand(CMD_PC_RESTART);
        sf.addCommand(CMD_PC_STOP);
        sf.addCommand(CMD_SHOW_PL);
        sf.addCommand(CMD_SHOW_VOLUME);
        sf.addCommand(CMD_SHOW_RATE);
        sf.setCommandListener(this);

        displayable = sf;

        screenPlaylist = new PlaylistScreen();
        screenPlaylist.setUp(this, display, player);

        screenVolume = new VolumeScreen();
        screenVolume.setUp(this, display, player);

        screenRating = new RatingScreen();
        screenRating.setUp(this, display, player);

        ops.addObserver(this);
    }

    /**
     * Update the screen in while regarding whether we are currently active or
     * not.
     */
    private void renderScreen() {
        synchronized (ops) {
            Song s = ops.getCurrentSong();
            if (s == null) {
                sf.setSong(null);
            } else {
                // only render song if changed
                if (sf.getSong() != s) {
                    sf.setSong(s);
                }
            }
            sf.setTitle(stateToString(ops.getState()));

        }
    }

    private String stateToString(byte state) {
        String s;
        switch (state) {
            case PlayerState.ST_PAUSED:
                s = "Paused";
                break;
            case PlayerState.ST_PLAYING:
                s = "Playing";
                break;
            case PlayerState.ST_STOPPED:
                s = "Stopped";
                break;
            case PlayerState.ST_ERROR:
                s = "Error";
                break;
            case PlayerState.ST_PROBLEM:
                s = "Problem";
                break;
            case PlayerState.ST_OFF:
                s = "Player is off";
                break;
            case PlayerState.ST_UNKNOWN:
                s = "unknown";
                break;
            case Remuco.REM_PS_STATE_SRVOFF:
                s = "Server is down";
                break;
            default:
                s = "Unknown";
                break;
        }
        return s;
    }

    public Displayable getDisplayable() {
        return displayable;
    }

}
