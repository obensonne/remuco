package remuco.ui.simple;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Gauge;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.ItemStateListener;

import remuco.connection.RemotePlayer;
import remuco.data.ObservablePlayerState;
import remuco.data.PlayerControl;
import remuco.ui.IScreen;
import remuco.util.Log;
import remuco.util.Tools;

public class VolumeScreen implements IScreen, ItemStateListener {

    /**
     * When the user adjusts the volume percentage bar, she probably adjusts it
     * fastly subsequentally. To avoid allways sending the new volume to the
     * player, if the value changes by only one percent, this timer waits a bit
     * before sending the new volume. While it waits, the user may still adjust
     * the volume.
     * 
     * @author Christian Buennig
     * 
     */
    private class VolumeTimer extends Thread {

        /**
         * After the user changes the volume, this time is waited before sending
         * the finally adjusted volume.
         */
        private static final int WAIT_MS = 1000;

        private boolean done = false;

        private Object mutex = new Object();

        public void run() {
            while (!done) {
                synchronized (mutex) {
                    try {
                        mutex.wait();
                    } catch (InterruptedException e) {
                        Log.ln("VolumeTimer has been interrupted");
                        break;
                    }
                }
                Tools.sleep(WAIT_MS);
                pc.set(PlayerControl.CODE_VOL, newVol);
                player.control(pc);
            }
        }
    }

    private static final Command CMD_BACK = new Command("Back", Command.BACK,
            10);

    private Display display;

    private Displayable displayable;

    private Form form;

    private byte newVol;

    private PlayerControl pc;

    private CommandListener pcl;

    private RemotePlayer player;

    private ObservablePlayerState ps;

    private Gauge volGauge;

    private VolumeTimer volTimer;

    public VolumeScreen() {
        volGauge = new Gauge("", true, 100, 50);
        form = new Form("Volume");
        form.append(volGauge);
        form.addCommand(CMD_BACK);
        form.setCommandListener(this);
        form.setItemStateListener(this);
        displayable = form;

        volTimer = new VolumeTimer();
        volTimer.start();
        pc = new PlayerControl();
    }

    public void commandAction(Command c, Displayable d) {
        if (c == CMD_BACK) {
            setActive(false);
            pcl.commandAction(IScreen.CMD_DISPOSE, d);
        } else {
            pcl.commandAction(c, d);
        }
    }

    public Displayable getDisplayable() {
        return displayable;
    }

    public synchronized void itemStateChanged(Item item) {
        if (item == volGauge) {
            newVol = (byte) volGauge.getValue();
            synchronized (volTimer.mutex) {
                volTimer.mutex.notify();
            }
        }
    }

    public void notifyPlayerStateChange() {
        // the volume gauge gets adjusted once when the screen gets active

        // as a consequence, if the player's volume changes when the volume
        // screen is active, this does not reflect in the gauge (except, of
        // course, if the user herself changed the volume)

        // this is beacuse it is not easy to determine if we or the player
        // changed the volume (if itemStateChanged(Item) is called)
    }

    public void setActive(boolean active) {
        if (active) {
            volGauge.setValue(ps.getVolume());
            form.setItemStateListener(this);
            display.setCurrent(form);
        } else {
            form.setItemStateListener(null);
        }
    }

    public void setUp(CommandListener pcl, Display d, RemotePlayer player) {
        this.pcl = pcl;
        this.display = d;
        this.player = player;
        this.ps = player.getObservablePlayerState();
    }

}
