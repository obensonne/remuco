package remuco.ui.screens;

import java.util.Timer;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.List;

import remuco.Config;
import remuco.Remuco;
import remuco.comm.BinaryDataExecption;
import remuco.comm.Connection;
import remuco.comm.Message;
import remuco.player.Feature;
import remuco.player.IItemListener;
import remuco.player.IProgressListener;
import remuco.player.IStateListener;
import remuco.player.Item;
import remuco.player.Player;
import remuco.player.PlayerInfo;
import remuco.ui.CMD;
import remuco.ui.CommandList;
import remuco.ui.KeyBindings;
import remuco.ui.MediaBrowser;
import remuco.ui.RepeatedControl;
import remuco.ui.Theme;
import remuco.ui.screenies.ItemScreeny;
import remuco.ui.screenies.Screeny;
import remuco.ui.screenies.ScreenyException;
import remuco.ui.screenies.StateScreeny;
import remuco.util.Log;

/*
 * Eventuell nochmal aufteilen in reine grafische klasse (PlayerScreen) und
 * eine Klasse, die die ganzen Kommandos, KeyEvents und SubScreens handhabt
 * (PlayerHandler).
 */
public final class PlayerScreen extends Canvas implements IItemListener,
		IStateListener, IProgressListener, CommandListener {

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

	private static final String CONFIG_OPTION_THEME = "theme";

	private static final int SEEK_DELAY = 600;

	private final Alert alertFeature;

	private final Config config;

	private final Connection conn;

	private final Display display;

	private CommandListener externalCommandListener = null;

	/** Screen for browsing the remote player's media */
	private final MediaBrowser mediaBrowser;

	private final Player player;

	private RepeatedControl recoSeek;

	private RepeatedControl recoVolume = null;

	/** Screen to configure key setup */
	private final KeyBindingsScreen screenKeyConfig;

	private final CommandList screenOptions;

	/** Screen to edit the meta information of a item */
	private final TagEditorScreen screenTagEditor;

	/** Screen to select a theme */
	private final List screenThemeSelection;

	private boolean screenTooSmall = false;

	private final ItemScreeny screenyPlob;

	private final StateScreeny screenyState;

	private final Theme theme;

	private final Timer timer;

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

		config = Config.getInstance();

		timer = Remuco.getGlobalTimer();

		theme = Theme.getInstance();

		theme.load(config.getOption(CONFIG_OPTION_THEME));

		player = new Player(conn, pinfo);

		player.setStateListener(this);
		player.setItemListener(this);
		player.setProgressListener(this);

		if (pinfo.supportsMediaBrowser()) {
			super.addCommand(CMD_MEDIA);
		}
		super.addCommand(CMD_OPTIONS);
		super.setCommandListener(this);

		screenOptions = new CommandList("Options");
		screenOptions.addCommand(CMD_THEMES, theme.licThemes);
		screenOptions.addCommand(CMD_KEYS, theme.licKeys);
		if (pinfo.supports(Feature.SHUTDOWN)) {
			screenOptions.addCommand(CMD_SHUTDOWN_HOST, theme.licOff);
		}
		screenOptions.addCommand(CMD.BACK);
		screenOptions.setCommandListener(this);

		screenThemeSelection = new List("Themes", List.IMPLICIT);
		final String[] themes = config.getThemeList();
		for (int i = 0; i < themes.length; i++) {
			screenThemeSelection.append(themes[i], theme.licThemes);
		}
		screenThemeSelection.setSelectCommand(CMD.SELECT);
		screenThemeSelection.addCommand(CMD.BACK);
		screenThemeSelection.setCommandListener(this);

		screenyState = new StateScreeny(player.info);
		screenyPlob = new ItemScreeny(player.info);

		screenKeyConfig = new KeyBindingsScreen(this, display);
		screenKeyConfig.addCommand(CMD.BACK);

		screenTagEditor = new TagEditorScreen();
		screenTagEditor.addCommand(CMD.BACK);
		screenTagEditor.addCommand(CMD.OK);
		screenTagEditor.setCommandListener(this);

		alertFeature = new Alert("", "", null, AlertType.INFO);

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

			screenOptions.addCommand(CMD_DISCONNECT, theme.licDisconnect);

		} else if (cmd == CMD.EXIT) {

			screenOptions.addCommand(cmd, theme.licOff);

		} else if (cmd == CMD.LOG) {

			screenOptions.addCommand(cmd, theme.licLog);

		} else {

			screenOptions.addCommand(cmd, theme.licItem);
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

			final String name = screenThemeSelection.getString(screenThemeSelection.getSelectedIndex());
			theme.load(name);
			initScreenies(); // let new theme take effect
			config.setOption(CONFIG_OPTION_THEME, name);
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

			// if edited item is still current item, update current item
			if (player.item.getId().equals(pid)) {
				player.item.setTags(tags);
			}

			display.setCurrent(this);

		} else if (externalCommandListener != null) {

			externalCommandListener.commandAction(c, d);

		} else {

			Log.bug("Feb 2, 2009.6:46:34 PM (unhandled command)");
		}
	}

	/** Do some clean up before this player screen gets finally disposed. */
	public void dispose() {

		stopVolumeControl();
		stopSeek();
		conn.down();

	}

	public void handleMessageForPlayer(Message msg) throws BinaryDataExecption {
		player.handleMessage(msg);
	}

	public void notifyItemChanged() {

		screenyPlob.updateData(player.item);
		repaint(screenyPlob);
	}

	public void notifyProgressChanged() {

		screenyPlob.updateData(player.progress);
		repaint(screenyPlob);

	}

	public void notifyStateChanged() {

		screenyState.updateData(player.state);
		repaint(screenyState);
	}

	/** Set an <em>external</em> command listener. */
	public void setCommandListener(CommandListener l) {
		externalCommandListener = l;
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

			if (!checkFeature(Feature.CTRL_VOLUME)) {
				break;
			}

			startVolumeControl(1);
			break;

		case KeyBindings.ACTION_VOLDOWN:

			if (!checkFeature(Feature.CTRL_VOLUME)) {
				break;
			}

			startVolumeControl(-1);
			break;

		case KeyBindings.ACTION_PLAYPAUSE:

			player.ctrlPlayPause();
			break;

		case KeyBindings.ACTION_NEXT:

			startSeek(1);
			break;

		case KeyBindings.ACTION_PREV:

			startSeek(-1);
			break;

		case KeyBindings.ACTION_RATEDOWN:

			if (!checkFeature(Feature.CTRL_RATE)) {
				break;
			}

			rating = player.item.getRating() - 1;
			ratingMax = player.info.getMaxRating();
			if (player.item.isNone() || ratingMax == 0 || rating < 0)
				break;

			player.item.setRating(rating);
			player.ctrlRate(rating);

			notifyItemChanged();

			break;

		case KeyBindings.ACTION_RATEUP:

			if (!checkFeature(Feature.CTRL_RATE)) {
				break;
			}

			rating = player.item.getRating() + 1;
			ratingMax = player.info.getMaxRating();
			if (player.item.isNone() || ratingMax == 0 || rating > ratingMax)
				break;

			player.item.setRating(rating);
			player.ctrlRate(rating);

			notifyItemChanged();

			break;

		case KeyBindings.ACTION_VOLMUTE:

			if (!checkFeature(Feature.CTRL_VOLUME)) {
				break;
			}

			player.ctrlVolume(0);
			break;

		case KeyBindings.ACTION_IMAGE:

			if (player.item.getImg() != null) {

				screenyPlob.updateData(ItemScreeny.ToogleImageFullScreen);

				repaint(screenyPlob.getX(), screenyPlob.getY(),
					screenyPlob.getWidth(), screenyPlob.getHeight());
			}

			break;

		case KeyBindings.ACTION_EDITTAGS:

			if (!checkFeature(Feature.CTRL_TAG)) {
				break;
			}

			final String currentTags = player.item.getMeta(Item.META_TAGS);

			screenTagEditor.set(player.item.getId(), currentTags);

			display.setCurrent(screenTagEditor);

			break;

		case KeyBindings.ACTION_REPEAT:

			if (!checkFeature(Feature.CTRL_REPEAT)) {
				break;
			}

			player.ctrlToggleRepeat();

			break;

		case KeyBindings.ACTION_SHUFFLE:

			if (!checkFeature(Feature.CTRL_SHUFFLE)) {
				break;
			}

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

		final int action = KeyBindings.getInstance().getActionForKey(key);

		switch (action) {

		case KeyBindings.ACTION_VOLUP:
			stopVolumeControl();
			break;

		case KeyBindings.ACTION_VOLDOWN:
			stopVolumeControl();
			break;

		case KeyBindings.ACTION_NEXT: {
			final boolean notSeeked = stopSeek();
			if (notSeeked) { // key pressed for a short time -> no seek
				if (checkFeature(Feature.CTRL_NEXT)) {
					player.ctrlNext();
				}
			} else { // progress adjuster did some seeks, we are done already
				checkFeature(Feature.CTRL_SEEK);
			}
			break;
		}
		case KeyBindings.ACTION_PREV:
			final boolean notSeeked = stopSeek();
			if (notSeeked) { // key pressed for a short time -> no seek
				if (checkFeature(Feature.CTRL_PREV)) {
					player.ctrlPrev();
				}
			} else { // progress adjuster did some seeks, we are done already
				checkFeature(Feature.CTRL_SEEK);
			}
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

	private boolean checkFeature(int feature) {

		final boolean enabled = player.info.supports(feature);

		if (!enabled) {

			final String msg;

			switch (feature) {

			case Feature.CTRL_FULLSCREEN:
				msg = "Sorry, toggling full screen mode is not possible.";
				break;
			case Feature.CTRL_NEXT:
				msg = "Sorry, skipping to the next item is not possible.";
				break;
			case Feature.CTRL_PREV:
				msg = "Sorry, skipping to the previous item is not possible.";
				break;
			case Feature.CTRL_PLAYBACK:
				msg = "Sorry, controlling playback is not possible.";
				break;
			case Feature.CTRL_REPEAT:
				msg = "Sorry, toggling repeat mode is not possible.";
				break;
			case Feature.CTRL_SHUFFLE:
				msg = "Sorry, toggling shuffle mode is not possible.";
				break;
			case Feature.CTRL_SEEK:
				msg = "Sorry, seeking is not possible.";
				break;
			case Feature.CTRL_RATE:
				msg = "Sorry, rating is not possible.";
				break;
			case Feature.CTRL_VOLUME:
				msg = "Sorry, volume control is not possible.";
				break;
			case Feature.CTRL_TAG:
				msg = "Sorry, tagging items is not possible.";
				break;
			default:
				Log.bug("unexpected feature check: " + feature);
				msg = "Sorry, this is not possible.";
				break;
			}

			alertFeature.setString(msg);
			display.setCurrent(alertFeature);
		}

		return enabled;
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

			// ////// item ////// //

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

	private void startSeek(int direction) {

		if (recoSeek != null) {
			recoSeek.cancel();
		}
		recoSeek = new RepeatedControl(RepeatedControl.SEEK, player, direction);
		timer.schedule(recoSeek, SEEK_DELAY, 321);
	}

	private void startVolumeControl(int direction) {

		if (recoVolume != null) {
			recoVolume.cancel();
		}
		recoVolume = new RepeatedControl(RepeatedControl.VOLUME, player,
				direction);
		timer.schedule(recoVolume, 0, 253);
	}

	/**
	 * Stop repeated seek task.
	 * 
	 * @return <code>true</code> if no seek done yet (see
	 *         {@link RepeatedControl#cancel()})
	 */
	private boolean stopSeek() {

		final boolean ret;
		if (recoSeek != null) {
			ret = recoSeek.cancel();
			recoSeek = null;
		} else {
			ret = true;
		}
		return ret;
	}

	private void stopVolumeControl() {

		if (recoVolume != null) {
			recoVolume.cancel();
			recoVolume = null;
		}
	}

}
