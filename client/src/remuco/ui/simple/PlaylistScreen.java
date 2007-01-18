package remuco.ui.simple;

import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;

import remuco.connection.RemotePlayer;
import remuco.data.ObservablePlayerState;
import remuco.data.PlayerControl;
import remuco.data.Song;
import remuco.ui.IScreen;

public class PlaylistScreen implements IScreen {

    private static final Command CMD_BACK = new Command("Back", Command.BACK,
            10);

    private static final Command CMD_INFO = new Command("INFO", Command.ITEM,
            10);

    private boolean active;

    private long currentPlID;

    private Display display;

    private Displayable displayable;

    private List list;

    private ObservablePlayerState ops;

    private PlayerControl pc;

    private CommandListener pcl;

    private RemotePlayer player;

    private SongForm songForm;

    public PlaylistScreen() {
        active = false;
        list = new List("Playlist", Choice.IMPLICIT);
        list.addCommand(CMD_BACK);
        list.addCommand(CMD_INFO);
        list.setCommandListener(this);
        displayable = list;
        songForm = new SongForm("Song Info");
        songForm.addCommand(CMD_BACK);
        songForm.setCommandListener(this);

        currentPlID = 0;

        pc = new PlayerControl();
    }

    public synchronized void commandAction(Command c, Displayable d) {
        if (c == CMD_BACK) {
            if (d == list) {
                setActive(false);
                pcl.commandAction(IScreen.CMD_DISPOSE, d);
            } else {
                // back form song info screen
                setActive(true);
            }
        } else if (c == List.SELECT_COMMAND) {
            pc.set(PlayerControl.CODE_JUMP, (short) list.getSelectedIndex());
            player.control(pc);
            pcl.commandAction(IScreen.CMD_DISPOSE, d);
        } else if (c == CMD_INFO) {
            if (ops.playlistGetLength() == 0) {
                return;
            }
            // switch to song info screen
            setActive(false);
            songForm.setSong(ops.playlistGetSong((short) list
                    .getSelectedIndex()));
            display.setCurrent(songForm);
        } else {
            pcl.commandAction(c, d);
        }
    }

    public Displayable getDisplayable() {
        return displayable;
    }

    public void notifyPlayerStateChange() {
        if (active) {
            renderScreen();
        }
    }

    public void setActive(boolean active) {
        this.active = true;
        if (active) {
            display.setCurrent(list);
            renderScreen();
        }
    }

    public void setUp(CommandListener pcl, Display d, RemotePlayer player) {
        this.pcl = pcl;
        display = d;
        this.player = player;
        ops = player.getObservablePlayerState();
        ops.addObserver(this);
    }

    /**
     * Render the playlist screen.
     */
    private void renderScreen() {
        synchronized (ops) {
            int plLen = ops.playlistGetLength();
            // only render pl if changed
            if (ops.playlistGetID() != currentPlID) {
                Song s;
                while (list.size() > 0) {
                    list.delete(0);
                }
                currentPlID = ops.playlistGetID();
                if (plLen == 0) {
                    list.setTitle("Empty");
                } else {
                    for (short i = 0; i < plLen; i++) {
                        s = ops.playlistGetSong(i);
                        list.append(s.getTag(Song.TAG_TITLE) + " ("
                                + s.getTag(Song.TAG_ARTIST) + ")", null);
                    }
                }
            }
            // allways set index
            if (plLen > 0) {
                list.setSelectedIndex(ops.playlistGetPosition(), true);
            }
        }
    }
}
