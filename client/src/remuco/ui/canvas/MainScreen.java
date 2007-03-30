package remuco.ui.canvas;

import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;

import remuco.Main;
import remuco.connection.RemotePlayer;
import remuco.data.ObservablePlayerState;
import remuco.data.PlayerControl;
import remuco.data.Song;
import remuco.ui.IScreen;
import remuco.ui.simple.PlaylistScreen;
import remuco.util.Log;
import remuco.util.Tools;

public class MainScreen implements IScreen, IKeyListener {

	/** RemucoCommand on main screen: Exit */
	protected static final Command CMD_EXIT = new Command("Exit", Command.EXIT,
			1);

	private static final String APP_PROP_UI_THEME_LIST = "remuco-ui-canvas-themes";

	/** RemucoCommand on main screen: Theme change */
	private static final Command CMD_BACK_FROM_THEME_LIST = new Command("Back",
			Command.BACK, 1);

	/** RemucoCommand on main screen: Theme change */
	private static final Command CMD_THEME = new Command("Themes",
			Command.SCREEN, 2);

	private boolean active;

	private Display display;

	private Displayable displayable;

	private ObservablePlayerState ops;

	private PlayerControl pc;

	private CommandListener pcl;

	private RemotePlayer player;

	private IScreen screenPlaylist;

	private SongScreen ss;

	private List themeList;

	private String[] themeName;

	private long volumeAdjustStart;

	public MainScreen() {
	}

	public void commandAction(Command c, Displayable d) {
		int i;
		if (c == CMD_EXIT) {
			pcl.commandAction(IScreen.CMD_DISPOSE, d);
		} else if (c == CMD_THEME) {
			setActive(false);
			display.setCurrent(themeList);
			displayable = themeList;
		} else if (c == CMD_BACK_FROM_THEME_LIST) {
			setActive(true);
		} else if (c == List.SELECT_COMMAND && d == themeList) {
			i = themeList.getSelectedIndex();
			Theme.load(themeName[i]);
			ss.updateTheme();
			ss.updatePlayerState(ops);
			setActive(true);
			System.gc(); // free the old theme data
		} else if (c == IScreen.CMD_DISPOSE) {
			// a screen set by us, sent this command
			setActive(true);
		} else {
			pcl.commandAction(c, d);
		}
	}

	public Displayable getDisplayable() {
		return ss;
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
				ss.updatePlayerState(ops);
				ss.repaint();
			}
			break;
		case KEY_RATE_DOWN:
			s = ops.getCurrentSong();
			i = s.getRating();
			if (i != Song.RATING_NONE && i != 0) {
				pc.set(PlayerControl.CODE_RATE, (short) (i - 1));
				player.control(pc);
				s.setRating(i - 1, s.getRatingMax());
				ss.updatePlayerState(ops);
				ss.repaint();
			}
			break;
		case KEY_SHOW_PLAYLIST:
			Log.ln("show pl");
			setActive(false);
			screenPlaylist.setActive(true);
			display.setCurrent(screenPlaylist.getDisplayable());
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
			if (volDiff > 100 - volCurrent)
				volDiff = 100 - volCurrent;
			pc.set(PlayerControl.CODE_VOL, (short) (volCurrent + volDiff));
		} else {
			if (volCurrent <= 0)
				return;
			if (volDiff > volCurrent)
				volDiff = volCurrent;
			pc.set(PlayerControl.CODE_VOL, (short) (volCurrent - volDiff));
		}
		player.control(pc);

	}

	public void notifyPlayerStateChange() {
		ss.updatePlayerState(ops);
		if (displayable == ss) {
			ss.repaint();
		}
	}

	public void setActive(boolean active) {
		this.active = active;
		if (active) {
			display.setCurrent(ss);
			displayable = ss;
			ss.repaint();
		}

	}

	public void setUp(CommandListener pcl, Display d, RemotePlayer player) {
		this.pcl = pcl;
		this.display = d;
		this.player = player;
		init();
	}

	private int init() {

		int i;

		themeName = Tools.splitString(Main.getAPropString(
				APP_PROP_UI_THEME_LIST, "default"), ",");
		Theme.load(themeName[0]);

		ops = player.getObservablePlayerState();
		ops.addObserver(this);
		pc = new PlayerControl();

		ss = new SongScreen(this);
		ss.addCommand(CMD_EXIT);
		ss.addCommand(CMD_THEME);
		ss.setCommandListener(this);

		screenPlaylist = new PlaylistScreen();
		screenPlaylist.setUp(this, display, player);

		themeList = new List("Themes", Choice.IMPLICIT);
		themeList.addCommand(CMD_BACK_FROM_THEME_LIST);
		themeList.setCommandListener(this);
		for (i = 0; i < themeName.length; i++) {
			themeList.append(themeName[i], null);
		}

		return 0;
	}

}
