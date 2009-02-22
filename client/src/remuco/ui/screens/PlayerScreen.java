package remuco.ui.screens;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.List;

import remuco.Config;
import remuco.comm.BinaryDataExecption;
import remuco.comm.Connection;
import remuco.comm.Message;
import remuco.player.IPlobListener;
import remuco.player.IStateListener;
import remuco.player.Player;
import remuco.player.PlayerInfo;
import remuco.player.Plob;
import remuco.ui.Adjuster;
import remuco.ui.CMD;
import remuco.ui.CommandList;
import remuco.ui.KeyBindings;
import remuco.ui.MediaBrowser;
import remuco.ui.Theme;
import remuco.ui.screenies.PlobScreeny;
import remuco.ui.screenies.Screeny;
import remuco.ui.screenies.ScreenyException;
import remuco.ui.screenies.StateScreeny;
import remuco.util.Log;

/*
 * Eventuell nochmal aufteilen in reine grafische klasse (PlayerScreen) und
 * eine Klasse, die die ganzen Kommandos, KeyEvents und SubScreens handhabt
 * (PlayerHandler).
 */
public final class PlayerScreen extends Canvas implements IPlobListener,
		IStateListener, CommandListener {

	/**
	 * A wrapper command for the command {@link CMD#BACK} added externally to
	 * this screen.
	 */
	private static final Command CMD_DISCONNECT = new Command("Disconnect",
			Command.SCREEN, 92);

	private static final Command CMD_KEYS = new Command("Key Bindings",
			Command.SCREEN, 2);

	private static final Command CMD_MEDIA = new Command("Media",
			Command.SCREEN, 1);

	private static final Command CMD_OPTIONS = new Command("More",
			Command.BACK, 2);

	private static final Command CMD_SHUTDOWN_HOST = new Command(
			"Shutdown Host", Command.SCREEN, 95);

	private static final Command CMD_THEMES = new Command("Themes",
			Command.SCREEN, 1);

	private static final String CONFIG_THEME = "theme";

	private static final int SEEK_DELAY = 600;

	private final Connection conn;

	private final Display display;

	private CommandListener externalCommandListener = null;

	private final Player player;

	/** Screen to configure key setup */
	private final KeyBindingsScreen screenKeyConfig;

	/** Screen for browsing the remote player's media */
	private final MediaBrowser mediaBrowser;

	private final CommandList screenOptions;

	/** Screen to edit the meta information of a plob */
	private final TagEditorScreen screenTagEditor;

	/** Screen to select a theme */
	private final List screenThemeSelection;

	private boolean screenTooSmall = false;

	private final PlobScreeny screenyPlob;

	private final StateScreeny screenyState;

	private final Theme theme;

	/**
	 * Thread responsible for adjusting the volume/progress in the time between
	 * the key volume up/down respectively seek forward/backward has been
	 * pressed and released again.
	 */
	private final Adjuster volumeAdjuster, progressAdjuster;

	/**
	 * Create a new player screen.
	 * 
	 * @param display
	 *            display to use
	 * @param conn
	 *            connection to the remote player
	 * @param pinfo
	 *            information about the remote player
	 */
	public PlayerScreen(Display display, Connection conn, PlayerInfo pinfo) {

		this.display = display;
		this.conn = conn;

		theme = Theme.getInstance();

		theme.load(Config.get(CONFIG_THEME));

		player = new Player(conn, pinfo);

		player.registerStateListener(this);
		player.registerCurrentPlobListener(this);

		volumeAdjuster = new Adjuster(player, Adjuster.VOLUME);
		progressAdjuster = new Adjuster(player, Adjuster.PROGRESS);

		super.addCommand(CMD_MEDIA);
		super.addCommand(CMD_OPTIONS);
		super.setCommandListener(this);

		screenOptions = new CommandList("Options");
		screenOptions.addCommand(CMD_THEMES, theme.LIST_ICON_THEMES);
		screenOptions.addCommand(CMD_KEYS, theme.LIST_ICON_KEYS);
		if (pinfo.supportsShutdownHost()) {
			screenOptions.addCommand(CMD_SHUTDOWN_HOST, theme.LIST_ICON_OFF);
		}
		screenOptions.addCommand(CMD.BACK);
		screenOptions.setCommandListener(this);

		screenThemeSelection = new List("Themes", List.IMPLICIT);
		final String[] themes = Config.getThemeList();
		for (int i = 0; i < themes.length; i++) {
			screenThemeSelection.append(themes[i], theme.LIST_ICON_THEMES);
		}
		screenThemeSelection.addCommand(CMD.BACK);
		screenThemeSelection.addCommand(CMD.SELECT);
		screenThemeSelection.setSelectCommand(CMD.SELECT);
		screenThemeSelection.setCommandListener(this);

		screenyState = new StateScreeny(player.info);
		screenyPlob = new PlobScreeny(player.info);

		screenKeyConfig = new KeyBindingsScreen(this, display);
		screenKeyConfig.addCommand(CMD.BACK);

		screenTagEditor = new TagEditorScreen();
		screenTagEditor.addCommand(CMD.BACK);
		screenTagEditor.addCommand(CMD.OK);
		screenTagEditor.setCommandListener(this);

		mediaBrowser = new MediaBrowser(this, display, player);

		/*
		 * Some devices do not call sizeChanged() when this screen is shown the
		 * first time. To ensure screenies are initialized properly, we call
		 * this here already.
		 */
		initScreenies();

	}

	/**
	 * All commands added here will be appended as item commands to an option
	 * screen.
	 */
	public void addCommand(Command cmd) {

		if (cmd == CMD.BACK) {

			screenOptions
					.addCommand(CMD_DISCONNECT, theme.LIST_ICON_DISCONNECT);

		} else if (cmd == CMD.EXIT) {

			screenOptions.addCommand(cmd, theme.LIST_ICON_OFF);

		} else if (cmd == CMD.LOG) {

			screenOptions.addCommand(cmd, theme.LIST_ICON_LOG);

		} else {

			screenOptions.addCommand(cmd, theme.LIST_ICON_PLOB);
		}

	}

	public void commandAction(Command c, Displayable d) {

		if (c == CMD_MEDIA) {

			mediaBrowser.showYourself();

		} else if (c == CMD_OPTIONS) {

			display.setCurrent(screenOptions);

		} else if (c == CMD.BACK && d == screenOptions) { // OPTIONS //

			display.setCurrent(this);

		} else if (c == CMD_DISCONNECT) {

			if (externalCommandListener != null) {
				externalCommandListener.commandAction(CMD.BACK, this);
			} else {
				Log.bug("Feb 2, 2009.7:30:08 PM");
			}

		} else if (c == CMD_THEMES) {

			// preselect the current theme in the theme selection
			for (int i = 0; i < screenThemeSelection.size(); i++) {
				if (screenThemeSelection.getString(i).equals(theme.getName())) {
					screenThemeSelection.setSelectedIndex(i, true);
					break;
				}
			}
			display.setCurrent(screenThemeSelection);

		} else if (c == CMD_SHUTDOWN_HOST) {

			player.ctrlShutdownHost();

		} else if (c == CMD_KEYS) {

			display.setCurrent(screenKeyConfig);

		} else if (c == CMD.SELECT && d == screenThemeSelection) { // THEMES
			// //

			final String name = screenThemeSelection
					.getString(screenThemeSelection.getSelectedIndex());
			theme.load(name);
			initScreenies(); // let new theme take effect
			Config.set(CONFIG_THEME, name);
			display.setCurrent(this);

		} else if (c == CMD.BACK && d == screenThemeSelection) {

			display.setCurrent(screenOptions);

		} else if (c == CMD.BACK && d == screenKeyConfig) { // KEYS //

			display.setCurrent(screenOptions);

		} else if (c == CMD.BACK && d == screenTagEditor) { // TAGS //

			display.setCurrent(this);

		} else if (c == CMD.OK && d == screenTagEditor) {

			final String pid = screenTagEditor.getPid();
			final String tags = screenTagEditor.getTags();

			player.ctrlSetTags(pid, tags);

			// if edited plob is still current plob, update current plob
			if (player.plob.getId().equals(pid)) {
				player.plob.setTags(tags);
			}

			display.setCurrent(this);

		} else if (externalCommandListener != null) {

			externalCommandListener.commandAction(c, d);

		} else {

			Log.bug("Feb 2, 2009.6:46:34 PM (unhandled command)");
		}
	}

	public void currentPlobChanged() {

		screenyPlob.updateData(player.plob);
		repaint(screenyPlob);
	}

	/** Do some clean up before this player screen gets finally disposed. */
	public void dispose() {

		volumeAdjuster.interrupt();
		conn.down();

	}

	public void handleMessageForPlayer(Message msg) throws BinaryDataExecption {
		player.handleMessage(msg);
	}

	/** Set an <em>external</em> command listener. */
	public void setCommandListener(CommandListener l) {
		externalCommandListener = l;
	}

	public void stateChanged() {

		screenyState.updateData(player.state);
		repaint(screenyState);
	}

	protected void keyPressed(int key) {

		final int action = KeyBindings.getInstance().getActionForKey(key);
		final int rating, ratingMax;

		// Log.debug("[UI] pressed key "
		// + screenMain.getKeyName(key)
		// + " (id "
		// + key
		// + ") -> "
		// + (action != KeyBindings.ACTION_NOOP ?
		// KeyBindings.actionNames[action]
		// : "no action") + " (id " + action + ")");

		switch (action) {
		case KeyBindings.ACTION_VOLUP:

			volumeAdjuster.startAdjust(true, 0);
			break;

		case KeyBindings.ACTION_VOLDOWN:

			volumeAdjuster.startAdjust(false, 0);
			break;

		case KeyBindings.ACTION_PLAYPAUSE:

			player.ctrlPlayPause();
			break;

		case KeyBindings.ACTION_NEXT:

			progressAdjuster.startAdjust(true, SEEK_DELAY);
			break;

		case KeyBindings.ACTION_PREV:

			progressAdjuster.startAdjust(false, SEEK_DELAY);
			break;

		case KeyBindings.ACTION_RATEDOWN:

			rating = player.plob.getRating() - 1;
			ratingMax = player.info.getMaxRating();
			if (player.plob.isNone() || ratingMax == 0 || rating < 0)
				break;

			player.plob.setRating(rating);
			player.ctrlRate(rating);

			currentPlobChanged();

			break;

		case KeyBindings.ACTION_RATEUP:

			rating = player.plob.getRating() + 1;
			ratingMax = player.info.getMaxRating();
			if (player.plob.isNone() || ratingMax == 0 || rating > ratingMax)
				break;

			player.plob.setRating(rating);
			player.ctrlRate(rating);

			currentPlobChanged();

			break;

		case KeyBindings.ACTION_VOLMUTE:

			player.ctrlVolumeMute();
			break;

		case KeyBindings.ACTION_IMAGE:

			if (player.plob.getImg() != null) {

				screenyPlob.updateData(PlobScreeny.ToogleImageFullScreen);

				repaint(screenyPlob.getX(), screenyPlob.getY(), screenyPlob
						.getWidth(), screenyPlob.getHeight());
			}

			break;

		case KeyBindings.ACTION_EDITTAGS:

			if (player.plob.hasTags()) {

				screenTagEditor.set(player.plob.getId(), player.plob
						.getMeta(Plob.META_TAGS));

				display.setCurrent(screenTagEditor);
			}

			break;

		case KeyBindings.ACTION_REPEAT:

			player.ctrlToggleRepeat();

			break;

		case KeyBindings.ACTION_SHUFFLE:

			player.ctrlToggleShuffle();

			break;

		case KeyBindings.ACTION_NOOP:

			break;

		default:

			Log.ln("[PLSC] unknown action: " + action);
			break;
		}

	}

	protected void keyReleased(int key) {

		final boolean stillInDelay;

		final int action = KeyBindings.getInstance().getActionForKey(key);

		switch (action) {

		case KeyBindings.ACTION_VOLUP:
			volumeAdjuster.stopAdjust();
			break;

		case KeyBindings.ACTION_VOLDOWN:
			volumeAdjuster.stopAdjust();
			break;

		case KeyBindings.ACTION_NEXT:
			stillInDelay = progressAdjuster.stopAdjust();
			if (stillInDelay) { // key pressed for a short time -> no seek
				player.ctrlNext();
			} // else: progress adjuster did some seeks, we are done already
			break;

		case KeyBindings.ACTION_PREV:
			stillInDelay = progressAdjuster.stopAdjust();
			if (stillInDelay) { // key pressed for a short time -> no seek
				player.ctrlPrev();
			} // else: progress adjuster did some seeks, we are done already
			break;

		default:
			break;
		}
	}

	/**
	 * This methods finally paints the player screen. Gets called externally -
	 * e.g. as a result of {@link #repaint(Screeny)} or {@link #repaint()}
	 * (which may get called by ourselves).
	 * 
	 * @param g
	 *            the graphics where we can draw our screen into
	 * @see Canvas
	 */
	protected void paint(Graphics g) {

		if (screenTooSmall) {

			int y;

			g.setColor(0xFFFFFF);
			g.fillRect(0, 0, getWidth(), getHeight());

			g.setColor(0);
			g.setFont(Theme.FONT_SMALL);

			y = getHeight() / 2 - Theme.FONT_SMALL.getHeight();
			g.drawString("The screen is too small", getWidth() / 2, y,
					Graphics.HCENTER | Graphics.BASELINE);

			y += Theme.FONT_SMALL.getHeight() * 2;
			g.drawString("for the theme " + theme.getName() + "!",
					getWidth() / 2, y, Graphics.HCENTER | Graphics.BASELINE);

		} else {
			screenyState.draw(g);
			screenyPlob.draw(g);
		}
	}

	protected void sizeChanged(int w, int h) {

		super.sizeChanged(w, h);

		initScreenies();

		repaint(); // guess we need this
	}

	private void initScreenies() {

		int w, h, x, y, anchor;

		if (getWidth() == 0 || getHeight() == 0)
			return;

		try {
			// ////// playback, volume, repeat and shuffle ////// //

			anchor = Screeny.TOP_LEFT;
			x = 0;
			w = getWidth();
			h = getHeight() / 4; // max 1/4 for state
			y = 0;
			screenyState.initRepresentation(x, y, anchor, w, h);

			// ////// plob ////// //

			anchor = Screeny.TOP_LEFT;
			x = 0;
			w = getWidth();
			y = screenyState.getNextY();
			h = getHeight() - y;
			screenyPlob.initRepresentation(x, y, anchor, w, h);

			screenTooSmall = false;

		} catch (ScreenyException e) {

			screenTooSmall = true;
			Log.ln("[PD] screen too small", e);
		}
	}

	/** Request a repaint for the region occupied by a screeny. */
	private void repaint(Screeny s) {

		repaint(s.getX(), s.getY(), s.getWidth(), s.getHeight());
	}
}
