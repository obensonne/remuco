package remuco.ui.canvas;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;

import remuco.connection.RemotePlayer;
import remuco.data.ObservablePlayerState;
import remuco.data.PlayerControl;
import remuco.data.Song;
import remuco.ui.IScreen;
import remuco.util.Log;

public class MainScreen implements IScreen, KeyListener {

	/** RemucoCommand on main screen: Exit */
	protected static final Command CMD_EXIT = new Command("Exit", Command.EXIT,
			90);

	private boolean active;

	private Display display;

	private Displayable displayable;

	private ObservablePlayerState ops;

	private PlayerControl pc;

	private CommandListener pcl;

	private RemotePlayer player;

	private SongScreen sc;

	private long volumeAdjustStart;

	public MainScreen() {
	}

	public void commandAction(Command c, Displayable d) {
		if (c == CMD_EXIT)
			pcl.commandAction(IScreen.CMD_DISPOSE, d);
		else
			pcl.commandAction(c, d);
	}

	public Displayable getDisplayable() {
		return sc;
	}

	public void keyPressed(int key) {
		if (player == null)
			return;
		int i;
		Song s;
		switch (key) {
		case KEY_PLAY_PAUSE:
			player.control(PlayerControl.PC_PLAY_PAUSE);
			break;
		case KEY_STOP:
			player.control(PlayerControl.PC_STOP);
			break;
		case KEY_RESTART:
			player.control(PlayerControl.PC_RESTART);
			break;
		case KEY_NEXT:
			player.control(PlayerControl.PC_NEXT);
			break;
		case KEY_PREV:
			player.control(PlayerControl.PC_PREV);
			break;
		case KEY_VOLUME_UP:
			volumeAdjustStart = System.currentTimeMillis();
			break;
		case KEY_VOLUME_DOWN:
			volumeAdjustStart = System.currentTimeMillis();
			break;
		case KEY_RATE_UP:
			s = ops.getCurrentSong();
			i = s.getRating();
			if (i != Song.RATING_NONE && i != s.getRatingMax()) {
				pc.set(PlayerControl.CODE_RATE, (short) (i + 1));
				player.control(pc);
				s.setRating(i + 1, s.getRatingMax());
				sc.update(ops);
				sc.repaint();
			}
			break;
		case KEY_RATE_DOWN:
			s = ops.getCurrentSong();
			i = s.getRating();
			if (i != Song.RATING_NONE && i != 0) {
				pc.set(PlayerControl.CODE_RATE, (short) (i - 1));
				player.control(pc);
				s.setRating(i - 1, s.getRatingMax());
				sc.update(ops);
				sc.repaint();
			}
			break;
		case KEY_SHOW_PLAYLIST:
			Log.ln("show pl");
			break;
		}
	}

	public void keyReleased(int key) {

		if (player == null)
			return;

		if (key != KEY_VOLUME_UP && key != KEY_VOLUME_DOWN)
			return;

		long volDiff;
		int volCurrent;

		volDiff = (int) (System.currentTimeMillis() - volumeAdjustStart) / 100;
		volCurrent = ops.getVolume();

		if (key == KEY_VOLUME_UP) {
			if (volCurrent >= 100)
				return;
			pc.set(PlayerControl.CODE_VOL, (short) (volCurrent + volDiff));
		} else {
			if (volCurrent <= 0)
				return;
			pc.set(PlayerControl.CODE_VOL, (short) (volCurrent - volDiff));
		}
		player.control(pc);

	}

	public void notifyPlayerStateChange() {
		sc.update(ops);
		if (displayable == sc) {
			sc.repaint();
		}
	}

	public void setActive(boolean active) {
		this.active = active;
		if (active) {
			display.setCurrent(sc);
			displayable = sc;
			sc.repaint();
		}

	}

	public void setUp(CommandListener pcl, Display d, RemotePlayer player) {
		this.pcl = pcl;
		this.display = d;
		this.player = player;
		init();
	}

	private int init() {

		if (player != null)
			this.ops = player.getObservablePlayerState();
		if (ops != null)
			ops.addObserver(this);
		this.pc = new PlayerControl();
        
		sc = new SongScreen(this, new Theme(Theme.DEFAULT));
		sc.addCommand(CMD_EXIT);
		sc.setCommandListener(this);

		return 0;
	}

	// private static int mapKeyToCommand(int key) {
	//		
	// }

}
