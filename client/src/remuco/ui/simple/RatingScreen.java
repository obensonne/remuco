package remuco.ui.simple;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Gauge;

import remuco.connection.RemotePlayer;
import remuco.data.ObservablePlayerState;
import remuco.data.PlayerControl;
import remuco.data.Song;
import remuco.ui.IScreen;

public class RatingScreen implements IScreen {

    private static final Command CMD_BACK = new Command("Back", Command.BACK,
            10);

    private static final Command CMD_OK = new Command("Ok", Command.OK, 20);

    private Display display;

    private Displayable displayable;

    private Form errorForm;

    private Form form;

    private ObservablePlayerState ops;

    private CommandListener pcl;

    private RemotePlayer player;

    private Gauge ratingGauge;

    public RatingScreen() {
        ratingGauge = new Gauge("", true, 5, 0);
        form = new Form("Rating");
        form.append(ratingGauge);
        form.addCommand(CMD_BACK);
        form.addCommand(CMD_OK);
        form.setCommandListener(this);
        displayable = form;

        errorForm = new Form("Rating");
        errorForm.append("It is not possible to rate this song!");
        errorForm.addCommand(CMD_BACK);
        errorForm.setCommandListener(this);
    }

    public void commandAction(Command c, Displayable d) {
        if (c == CMD_BACK) {
            setActive(false);
            pcl.commandAction(IScreen.CMD_DISPOSE, d);
        } else if (c == CMD_OK) {
            player.control(new PlayerControl(PlayerControl.CODE_RATE,
                    (short) ratingGauge.getValue()));
            ops.getCurrentSong().setRating(ratingGauge.getValue(),
                    ratingGauge.getMaxValue());
            setActive(false);
            pcl.commandAction(IScreen.CMD_DISPOSE, d);
        } else {
            pcl.commandAction(c, d);
        }
    }

    public Displayable getDisplayable() {
        return displayable;
    }

    public void notifyPlayerStateChange() {
    }

    public void setActive(boolean active) {

        if (active) {
            Song s = ops.playlistGetSong(ops.playlistGetPosition());
            if (s == null) {
                return;
            }
            int i = s.getRatingMax();
            if (i == Song.RATING_NONE) {
                display.setCurrent(errorForm);
                return;
            }
            ratingGauge.setMaxValue(i);
            i = s.getRating();
            if (i == Song.RATING_NONE) {
                display.setCurrent(errorForm);
                return;
            }
            ratingGauge.setValue(i);
            display.setCurrent(form);
        }
    }

    public void setUp(CommandListener pcl, Display d, RemotePlayer player) {
        this.pcl = pcl;
        this.display = d;
        this.player = player;
        this.ops = player.getObservablePlayerState();
    }
}
