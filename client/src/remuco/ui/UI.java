package remuco.ui;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;

import remuco.Config;
import remuco.controller.CommController;
import remuco.controller.ICCEventListener;
import remuco.player.ICurrentPlobListener;
import remuco.player.IPlaylistListener;
import remuco.player.ILibraryRequestor;
import remuco.player.IQueueListener;
import remuco.player.IStateListener;
import remuco.player.Info;
import remuco.player.Library;
import remuco.player.Player;
import remuco.player.Plob;
import remuco.ui.screens.DeviceSelectorScreen;
import remuco.ui.screens.KeyConfigScreen;
import remuco.ui.screens.LibraryScreen;
import remuco.ui.screens.PlayerScreen;
import remuco.ui.screens.PlobEditorScreen;
import remuco.ui.screens.PloblistScreen;
import remuco.ui.screens.WaitingScreen;
import remuco.util.Keys;
import remuco.util.Log;

/**
 * The main screen once a connection to the server has been established. Shows
 * the current player state and the current plob.
 * 
 * @author Christian Buennig
 * 
 */
public final class UI implements CommandListener, IKeyListener, IStateListener,
		ICurrentPlobListener, IPlaylistListener, IQueueListener,
		ICCEventListener, ILibraryRequestor {

	/**
	 * A generic back command. The general contract is that screens should not
	 * add this command to theirself. Parent screens are responsible to add this
	 * command to child screens and handle it when the user activates it.
	 * <p>
	 * Label is "Back" and priority is 80.
	 */
	public static final Command CMD_BACK = new Command("Back", Command.BACK, 80);

	/**
	 * A generic exit command. Every screen which wnats to offer the user to
	 * immdiately exit the application should add this command to itself.
	 * However, hen this command gets activated by the user, the command action
	 * must be delegated back to each screen's parent screen. The root screen
	 * (which has no parent screen) is responsible for shutting down.
	 * <p>
	 * Label is "Exit" and priority is 100.
	 */
	public static final Command CMD_EXIT = new Command("Exit", Command.OK, 100);

	/**
	 * A generic command to show some information about the current screen. To
	 * be used by any screen which likes to give information about itself.
	 * <p>
	 * Label is "Info" and priority is 70.
	 */
	public static final Command CMD_INFO = new Command("Info", Command.BACK, 70);

	public static final Command CMD_NO = new Command("No", Command.CANCEL, 0);

	/**
	 * A generic command to commit agree with something, for your convinience
	 * initialized here :) .
	 * <p>
	 * Label is "Ok" and priority is 10.
	 */
	public static final Command CMD_OK = new Command("Ok", Command.OK, 10);

	/**
	 * A command which displays the log. Every may use add this command to
	 * itself. However, hen this command gets activated by the user, the command
	 * action must be delegated back to each screen's parent screen. The root
	 * screen (which has no parent screen) is responsible for displaying the log
	 * and redisplaying the displayable which originally recognized this command
	 * (so the reference to this displayable must also get forwarded when
	 * forwarding the command to the root screen.
	 * <p>
	 * Label is "Log" and priority is 90.
	 */
	public static final Command CMD_SHOW_LOG = new Command("Log", Command.OK,
			90);

	public static final Command CMD_YES = new Command("Yes", Command.OK, 0);

	/*
	 * This screen does not need to know changes in the player info, since the
	 * its representation does not depend on it.
	 */

	private final static Command CMD_DISCONNECT = new Command("Disconnect",
			Command.SCREEN, 95);

	/** Show the key configuration screen */
	private final static Command CMD_KEYS = new Command("Keys", Command.SCREEN,
			80);

	/** Command for the playlist screen. */
	private final static Command CMD_LIBRARY = new Command("Library",
			Command.SCREEN, 50);

	/** Command for the playlist screen */
	private static final Command CMD_PLAYLIST_JUMP = new Command("Play",
			Command.ITEM, 5);

	/** Command for the queue screen */
	private static final Command CMD_QUEUE_JUMP = new Command("Play",
			Command.ITEM, 5);

	/** Command for the queue screen */
	private static final Command CMD_SHOW_PLAYLIST = new Command(
			"Show Playlist", Command.SCREEN, 10);

	/** Command for the playlist screen */
	private static final Command CMD_SHOW_QUEUE = new Command("Show Queue",
			Command.SCREEN, 10);

	private final static Command CMD_THEMES = new Command("Themes",
			Command.SCREEN, 90);

	private static final String CONFIG_THEME = "theme";

	private final Alert alertConnectionError;

	private final CommController cc;

	private final Display display;

	private final CommandListener parent;

	private final Player player;

	/** Screen to show progress while connecting */
	private final WaitingScreen screenConnecting;

	/** Screen to select a device to connect to */
	private final DeviceSelectorScreen screenDeviceSelector;

	/** Screen to configure key setup */
	private final KeyConfigScreen screenKeyConfig;

	/** Screen to show the remote player's library */
	private final LibraryScreen screenLibrary;

	/** Main screen which shows player state and current plob */
	private final PlayerScreen screenMain;

	/** Screen to show the playlist/queue */
	private final PloblistScreen screenPlaylist, screenQueue;

	/** Screen to edit the meta information of a plob */
	private final PlobEditorScreen screenPlobEditor;

	/** Screen to select a theme */
	private final List screenThemeSelection;

	private final Theme theme;

	/**
	 * Thread responsible for adjusting the volume in the time between the key
	 * volume up/down has been pressed and released again.
	 */
	private final VolumeController volumeController;

	public UI(CommandListener parent, Display display) {

		String[] themes;

		this.parent = parent;
		this.display = display;

		// misc //

		cc = new CommController(this);
		player = cc.getPlayer();

		volumeController = new VolumeController(player);
		volumeController.start();

		theme = new Theme(Config.get(CONFIG_THEME));

		player.registerStateListener(this);
		player.registerCurrentPlobListener(this);

		// screens

		alertConnectionError = new Alert("Connection Error");
		alertConnectionError.setCommandListener(this);
		alertConnectionError.setTimeout(Alert.FOREVER);

		screenConnecting = new WaitingScreen();
		screenConnecting.setCommandListener(this);

		screenDeviceSelector = new DeviceSelectorScreen(this, display);
		screenDeviceSelector.addCommand(CMD_EXIT);
		screenDeviceSelector.addCommand(CMD_SHOW_LOG);

		screenMain = new PlayerScreen(theme, this, player.info);

		screenMain.addCommand(CMD_EXIT);
		screenMain.addCommand(CMD_THEMES);
		screenMain.addCommand(CMD_KEYS);
		screenMain.addCommand(CMD_DISCONNECT);
		screenMain.addCommand(CMD_SHOW_LOG);
		screenMain.setCommandListener(this);

		screenThemeSelection = new List("Themes", List.IMPLICIT);
		themes = Theme.getList();
		for (int i = 0; i < themes.length; i++) {
			screenThemeSelection.append(themes[i], null);
		}
		screenThemeSelection.addCommand(CMD_BACK);
		screenThemeSelection.setCommandListener(this);

		screenKeyConfig = new KeyConfigScreen(this, display);
		screenKeyConfig.addCommand(CMD_BACK);

		screenPlaylist = new PloblistScreen(this, display, player);
		screenPlaylist.addCommand(CMD_BACK);
		screenPlaylist.updatePloblist(player.playlist);
		player.registerPlaylistListener(this);

		screenQueue = new PloblistScreen(this, display, player);
		screenQueue.addCommand(CMD_BACK);
		screenQueue.addCommand(CMD_SHOW_PLAYLIST);
		screenQueue.updatePloblist(player.queue);
		player.registerQueueListener(this);

		screenLibrary = new LibraryScreen(this, display, player);
		screenLibrary.addCommand(CMD_BACK);

		screenPlobEditor = new PlobEditorScreen(player.info);
		screenPlobEditor.addCommand(CMD_BACK);
		screenPlobEditor.addCommand(CMD_OK);
		screenPlobEditor.setCommandListener(this);

		// ok, all components initialized, we will start to interact with the
		// user when go() has been called

	}

	public void commandAction(Command c, Displayable d) {

		// TODO: sort commands depending on their usage count

		int i;
		String s;
		Plob plob;

		if (c == CMD_DISCONNECT) { // /// GENERIC /////

			cc.disconnect();

			display.setCurrent(screenDeviceSelector);

		} else if (c == CMD_EXIT) {

			cc.disconnect();
			volumeController.interrupt();
			parent.commandAction(c, d);

		} else if (c == List.SELECT_COMMAND && d == screenDeviceSelector) {

			s = screenDeviceSelector.getSelectedDevice();
			if (s != null) {
				cc.connect(s);
				// the "connecting" screen will appear automatically because
				// the call to connect() will raise an event which gets handled
				// by event()
			}

		} else if (c == WaitingScreen.CMD_CANCEL) {

			// user canceled connection setup

			cc.disconnect();

			display.setCurrent(screenDeviceSelector);

		} else if (c == CMD_THEMES) { // /// THEMES /////

			// preselect the current theme in the theme selection
			for (i = 0; i < screenThemeSelection.size(); i++) {
				if (screenThemeSelection.getString(i).equals(theme.getName())) {
					screenThemeSelection.setSelectedIndex(i, true);
					break;
				}
			}
			display.setCurrent(screenThemeSelection);

		} else if (c == List.SELECT_COMMAND && d == screenThemeSelection) {

			s = screenThemeSelection.getString(screenThemeSelection
					.getSelectedIndex());
			theme.load(s);
			screenMain.updateTheme();
			Config.set(CONFIG_THEME, s);
			display.setCurrent(screenMain);

		} else if (c == CMD_BACK && d == screenThemeSelection) {

			display.setCurrent(screenMain);

		} else if (c == CMD_KEYS) { // /// KEYS /////

			display.setCurrent(screenKeyConfig);

		} else if (c == CMD_BACK && d == screenKeyConfig) {

			display.setCurrent(screenMain);

		} else if (c == CMD_BACK && d == screenPlobEditor) { // PLOB EDIT //

			display.setCurrent(screenMain);

		} else if (c == CMD_OK && d == screenPlobEditor) {

			plob = screenPlobEditor.getPlobEdited();

			player.ctrlUpdatePlob(plob);

			// if the edited plob is the current, update the screen:
			if (player.currentPlob != null
					&& plob.getPid().equals(player.currentPlob.getPid())) {
				screenMain.updatePlob(plob);
			}

			display.setCurrent(screenMain);

		} else if (c == CMD_SHOW_PLAYLIST) { // PLAYLIST & QUEUE //

			display.setCurrent(screenPlaylist);

		} else if (c == CMD_SHOW_QUEUE) {

			display.setCurrent(screenQueue);

		} else if (c == CMD_PLAYLIST_JUMP) {

			i = screenPlaylist.getSelectedIndex();

			if (i >= 0)
				player.ctrlJump(i + 1);

			display.setCurrent(screenMain);

		} else if (c == CMD_QUEUE_JUMP) {

			i = screenQueue.getSelectedIndex();

			if (i >= 0)
				player.ctrlJump(-(i + 1));

			display.setCurrent(screenMain);

		} else if (c == CMD_BACK && (d == screenPlaylist || d == screenQueue)) {

			display.setCurrent(screenMain);

		} else if (c == CMD_LIBRARY) { // LIBRARY //

			player.reqLibrary(this);

		} else if (c == CMD_BACK && d == screenLibrary) {

			display.setCurrent(screenPlaylist);

		} else {

			parent.commandAction(c, d);

		}

	}

	public void currentPlobChanged() {

		screenMain.updatePlob(player.currentPlob);

	}

	public void event(int type, String msg) {

		switch (type) {

		case EVENT_CONNECTED:

			connected();

			break;

		case EVENT_CONNECTING:

			//screenConnecting.setMessage(msg); TODO change this
			screenConnecting.setMessage("Lost connection. Reconnect..");
			screenConnecting.setCancable(true);

			display.setCurrent(screenConnecting);

			break;

		case EVENT_ERROR:

			alertConnectionError.setString(msg);

			display.setCurrent(alertConnectionError);

			break;

		default:

			Log.asssertNotReached(this);

			break;
		}
	}

	/**
	 * To call once when the app starts to hint the UI that it should start its
	 * work i.e. the user interaction.
	 * 
	 */
	public void go() {

		screenDeviceSelector.showYourself();

	}

	public void handleLibrary(Library lib) {

		screenLibrary.updateLibrary(lib);

		display.setCurrent(screenLibrary);
		
	}

	public void keyPressed(int key) {

		int action = Keys.getActionForKey(key);
		int rating;

//		Log.debug("[UI] pressed key "
//				+ screenMain.getKeyName(key)
//				+ " (id "
//				+ key
//				+ ") -> "
//				+ (action != Keys.ACTION_NOOP ? Keys.actionNames[action]
//						: "no action") + " (id " + action + ")");

		switch (action) {
		case Keys.ACTION_VOLUP:

			volumeController.startVolumeAdjustment(true);
			break;

		case Keys.ACTION_VOLDOWN:

			volumeController.startVolumeAdjustment(false);
			break;

		case Keys.ACTION_PLAYPAUSE:

			player.ctrlTooglePlayPause();
			break;

		case Keys.ACTION_NEXT:

			player.ctrlNext();
			break;

		case Keys.ACTION_PREV:

			player.ctrlPrev();
			break;

		case Keys.ACTION_RATEDOWN:

			if (player.currentPlob == null
					|| !player.info.hasFeature(Info.FEATURE_RATE)
					|| (rating = player.currentPlob.getRating() - 1) < 0)
				break;

			player.currentPlob.setRating(rating);
			player.ctrlRate(rating);
			currentPlobChanged();

			break;

		case Keys.ACTION_RATEUP:

			if (player.currentPlob == null
					|| !player.info.hasFeature(Info.FEATURE_RATE)
					|| (rating = player.currentPlob.getRating() + 1) > player.info
							.getRatingMax())
				break;

			player.currentPlob.setRating(rating);
			player.ctrlRate(rating);
			currentPlobChanged();

			break;

		case Keys.ACTION_VOLMUTE:

			player.ctrlVolume(0);
			break;

		case Keys.ACTION_STOP:

			player.ctrlStop();
			break;

		case Keys.ACTION_IMAGE:

			if (player.currentPlob != null
					&& player.currentPlob.getImg() != null)
				screenMain.tooglePlobImageFullscreen();

			break;

		case Keys.ACTION_PLAYLIST:

			if (!player.info.hasFeature(Info.FEATURE_PLAYLIST))
				break;

			if (player.state.isPlayingFromQueue())
				display.setCurrent(screenQueue);
			else
				display.setCurrent(screenPlaylist);

			break;

		case Keys.ACTION_EDITPLOB:

			if (player.currentPlob == null
					|| !player.info.hasFeature(Info.FEATURE_PLOB_EDIT))
				break;

			screenPlobEditor.setPlob(player.currentPlob);

			display.setCurrent(screenPlobEditor);

			break;

		case Keys.ACTION_NOOP:

			break;

		default:

			Log.ln("[PLSC] unknown action: " + action);
			break;
		}

	}

	public void keyReleased(int key) {

		int action = Keys.getActionForKey(key);

		switch (action) {
		case Keys.ACTION_VOLUP:
			volumeController.stopVolumeAdjustment();
			break;

		case Keys.ACTION_VOLDOWN:
			volumeController.stopVolumeAdjustment();
			break;

		default:
			break;
		}

	}

	public void playlistChanged() {

		screenPlaylist.updatePloblist(player.playlist);

	}

	public void queueChanged() {

		screenQueue.updatePloblist(player.queue);

	}

	public void stateChanged() {

		screenMain.updateState(player.state);

	}

	/**
	 * Do thins to do when we just connected to a Remuco server. That is mainly
	 * updating UI components depending on the features of the remote player
	 * respectively its proxy.
	 */
	private void connected() {

		screenMain.updateTheme(); /*
									 * this also ensures that any representation
									 * depending on the player info is now
									 * updated and will therefore reflect the
									 * new player info (this method might has
									 * been called due to a reconnect to the
									 * server and before we we've been connected
									 * to another player ..)
									 */
		screenMain.updateState(player.state);
		screenMain.updatePlob(player.currentPlob);

		// player info dependent settings

		if (player.info.hasFeature(Info.FEATURE_QUEUE))
			screenPlaylist.addCommand(CMD_SHOW_QUEUE);
		else
			screenPlaylist.removeCommand(CMD_SHOW_QUEUE);

		if (player.info.hasFeature(Info.FEATURE_PLAYLIST_JUMP)) {
			screenPlaylist.setSelectCommand(CMD_PLAYLIST_JUMP);
		} else {
			screenPlaylist.removeCommand(CMD_PLAYLIST_JUMP);
		}

		if (player.info.hasFeature(Info.FEATURE_LIBRARY))
			screenPlaylist.addCommand(CMD_LIBRARY);
		else
			screenPlaylist.removeCommand(CMD_LIBRARY);

		if (player.info.hasFeature(Info.FEATURE_QUEUE_JUMP)) {
			screenQueue.setSelectCommand(CMD_QUEUE_JUMP);
		} else {
			screenQueue.removeCommand(CMD_QUEUE_JUMP);
		}

		// let's go

		display.setCurrent(screenMain);

	}

}
