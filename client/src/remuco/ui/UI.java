package remuco.ui;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;

import remuco.Config;
import remuco.IEventListener;
import remuco.UserException;
import remuco.comm.BinaryDataExecption;
import remuco.comm.Communicator;
import remuco.comm.IMessageListener;
import remuco.comm.IMessageSender;
import remuco.comm.Message;
import remuco.comm.Serial;
import remuco.comm.SerializableString;
import remuco.player.ICurrentPlobListener;
import remuco.player.IStateListener;
import remuco.player.Player;
import remuco.player.PlayerList;
import remuco.player.Plob;
import remuco.ui.screens.DeviceSelectorScreen;
import remuco.ui.screens.KeyConfigScreen;
import remuco.ui.screens.LibraryScreen;
import remuco.ui.screens.PlayerListScreen;
import remuco.ui.screens.PlayerScreen;
import remuco.ui.screens.PlaylistScreen;
import remuco.ui.screens.TagEditorScreen;
import remuco.ui.screens.WaitingScreen;
import remuco.util.Log;

/**
 * The main screen once a connection to the server has been established. Shows
 * the current player state and the current plob.
 * 
 * @author Christian Buennig
 * 
 */
public final class UI implements CommandListener, IKeyListener, IStateListener,
		ICurrentPlobListener, IEventListener, IMessageListener, IMessageSender {

	private static final int SEEK_DELAY = 600;

	/**
	 * A generic back command. The general contract is that screens should not
	 * add this command to themselves. Parent screens are responsible to add
	 * this command to child screens and handle it when the user activates it.
	 * <p>
	 * Label is "Back" and priority is 80.
	 */
	public static final Command CMD_BACK = new Command("Back", Command.BACK, 80);

	/**
	 * A generic exit command. Every screen which wants to offer the user to
	 * Immediately exit the application should add this command to itself.
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

	/**
	 * Command to remotely shutdown the server's host.
	 */
	public static final Command CMD_SHUTDOWN_HOST = new Command("Shutdown",
			Command.OK, 101);

	public static final Command CMD_YES = new Command("Yes", Command.OK, 0);

	/** Show the key configuration screen */
	private final static Command CMD_SHOW_KEYCONFIG = new Command("Keys",
			Command.SCREEN, 80);

	private final static Command CMD_THEMES = new Command("Themes",
			Command.SCREEN, 85);

	private static final String CONFIG_THEME = "theme";

	/**
	 * An alert to signal something that results in disconnecting and displaying
	 * the device selector screen.
	 */
	private final Alert alertDisconnect;

	private final Alert alertNoPlayer;

	private Communicator comm;

	private final Display display;

	/**
	 * The name of the currently connected player (or the player to
	 * automatically choose on an automatic reconnect - see
	 * {@link #handleEvent(int, String)} for the event
	 * {@link Communicator#EVENT_CONNECTED}).
	 */
	private String lastChosenPlayer;

	private final CommandListener parent;

	private final Player player;

	private final PlayerList playerList;

	/** Screen to show progress while connecting */
	private final WaitingScreen screenConnecting;

	/** Screen to select a device to connect to */
	private final DeviceSelectorScreen screenDeviceSelector;

	/** Screen to configure key setup */
	private final KeyConfigScreen screenKeyConfig;

	/** Screen to show the remote player's library */
	private final LibraryScreen screenLibrary;

	/** Main screen which shows player state and current plob */
	private final PlayerScreen screenPlayer;

	private final PlayerListScreen screenPlayerList;

	/** Screen to show the playlist/queue */
	private final PlaylistScreen screenPlaylist, screenQueue;

	/** Screen to edit the meta information of a plob */
	private final TagEditorScreen screenTagEditor;

	/** Screen to select a theme */
	private final List screenThemeSelection;

	private final Theme theme;

	/**
	 * Thread responsible for adjusting the volume/progress in the time between
	 * the key volume up/down respectively seek forward/backward has been
	 * pressed and released again.
	 */
	private final Adjuster volumeAdjuster, progressAdjuster;

	public UI(CommandListener parent, Display display) {

		String[] themes;

		this.parent = parent;
		this.display = display;

		// misc //

		player = new Player(this);
		playerList = new PlayerList();

		volumeAdjuster = new Adjuster(player, Adjuster.VOLUME);
		progressAdjuster = new Adjuster(player, Adjuster.PROGRESS);

		theme = new Theme(Config.get(CONFIG_THEME));

		player.registerStateListener(this);
		player.registerCurrentPlobListener(this);

		// screens

		alertDisconnect = new Alert("");
		alertDisconnect.setString("The Remuco server has shut down.");
		alertDisconnect.setTimeout(Alert.FOREVER);

		alertNoPlayer = new Alert("");
		alertNoPlayer.setString("There are no players to control.");
		alertNoPlayer.setTimeout(Alert.FOREVER);

		screenPlayerList = new PlayerListScreen(this);
		screenPlayerList.addCommand(CMD_SHOW_LOG);
		screenPlayerList.addCommand(CMD_BACK);
		screenPlayerList.addCommand(CMD_EXIT);
		screenPlayerList.addCommand(CMD_SHUTDOWN_HOST);

		screenConnecting = new WaitingScreen();
		screenConnecting.setCommandListener(this);

		screenDeviceSelector = new DeviceSelectorScreen(this, display);
		screenDeviceSelector.addCommand(CMD_SHOW_LOG);
		screenDeviceSelector.addCommand(CMD_EXIT);

		screenPlayer = new PlayerScreen(theme, player, this);

		screenPlayer.addCommand(CMD_THEMES);
		screenPlayer.addCommand(CMD_SHOW_KEYCONFIG);
		screenPlayer.addCommand(CMD_BACK);
		screenPlayer.addCommand(CMD_SHOW_LOG);
		screenPlayer.addCommand(CMD_EXIT);
		screenPlayer.setCommandListener(this);

		screenThemeSelection = new List("Themes", List.IMPLICIT);
		themes = Theme.getList();
		for (int i = 0; i < themes.length; i++) {
			screenThemeSelection.append(themes[i], null);
		}
		screenThemeSelection.addCommand(CMD_BACK);
		screenThemeSelection.setCommandListener(this);

		screenKeyConfig = new KeyConfigScreen(this, display);
		screenKeyConfig.addCommand(CMD_BACK);

		screenPlaylist = new PlaylistScreen(this, display, player, false);
		screenPlaylist.addCommand(CMD_BACK);

		screenQueue = new PlaylistScreen(this, display, player, true);
		screenQueue.addCommand(CMD_BACK);

		screenLibrary = new LibraryScreen(this, display, player);
		screenLibrary.addCommand(CMD_BACK);
		screenLibrary.addCommand(CMD_SHOW_LOG);

		screenTagEditor = new TagEditorScreen();
		screenTagEditor.addCommand(CMD_BACK);
		screenTagEditor.addCommand(CMD_OK);
		screenTagEditor.setCommandListener(this);

		// ok, all components initialized, we will start to interact with the
		// user when go() has been called

	}

	public void commandAction(Command c, Displayable d) {

		int i;
		String s, pid, tags;
		Message msg;
		SerializableString ss;

		if (c == CMD_BACK && d == screenPlayerList) { // /// GENERIC /////

			disconnect();
			display.setCurrent(screenDeviceSelector);

		} else if (c == CMD_EXIT) {

			disconnect();
			volumeAdjuster.interrupt();
			parent.commandAction(c, d);

		} else if (c == CMD_SHUTDOWN_HOST) {

			player.ctrlShutdownHost();

		} else if (c == List.SELECT_COMMAND && d == screenDeviceSelector) {

			s = screenDeviceSelector.getSelectedDevice();
			if (s != null) {
				try {
					comm = new Communicator(s, this, this);
					screenConnecting.setMessage("Connecting ..");
					screenConnecting.setCancable(true);
					display.setCurrent(screenConnecting);

				} catch (UserException e) {
					Log.ln("[UI] ", e);
					alertDisconnect.setTitle(e.getError());
					alertDisconnect.setString(e.getDetails());
					display.setCurrent(alertDisconnect, screenDeviceSelector);
				}
			}

		} else if (c == List.SELECT_COMMAND && d == screenPlayerList) {

			if (comm == null) {
				Log.asssertNotReached(this);
				return;
			}

			i = screenPlayerList.getSelectedIndex();

			lastChosenPlayer = screenPlayerList.getString(i);

			ss = new SerializableString();
			ss.set(lastChosenPlayer);

			msg = new Message();
			msg.id = Message.ID_SEL_PLAYER;
			msg.bytes = Serial.out(ss);

			comm.sendMessage(msg);

			// TODO: show a waiting screen ?

		} else if (c == WaitingScreen.CMD_CANCEL) {

			// user canceled connection setup

			disconnect();
			display.setCurrent(screenDeviceSelector);

		} else if (c == CMD_BACK && d == screenPlayer) {

			player.reset();
			lastChosenPlayer = null;

			display.setCurrent(screenPlayerList);

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
			screenPlayer.themeOrPlayerChanged();
			Config.set(CONFIG_THEME, s);
			display.setCurrent(screenPlayer);

		} else if (c == CMD_BACK && d == screenThemeSelection) {

			display.setCurrent(screenPlayer);

		} else if (c == CMD_SHOW_KEYCONFIG) { // /// KEYS /////

			display.setCurrent(screenKeyConfig);

		} else if (c == CMD_BACK && d == screenKeyConfig) {

			display.setCurrent(screenPlayer);

		} else if (c == CMD_BACK && d == screenTagEditor) { // PLOB EDIT //

			display.setCurrent(screenPlayer);

		} else if (c == CMD_OK && d == screenTagEditor) {

			pid = screenTagEditor.getPid();
			tags = screenTagEditor.getTags();

			player.ctrlSetTags(pid, tags);

			// if edited plob is still current plob, update current plob
			if (player.currentPlob != null
					&& player.currentPlob.getId().equals(pid))
				player.currentPlob.setTags(tags);

			display.setCurrent(screenPlayer);

		} else if (c == CMD_BACK && (d == screenPlaylist || d == screenQueue)) {

			// PLAYLIST & QUEUE //

			display.setCurrent(screenPlayer);

		} else if (c == PlaylistScreen.CMD_PLAYLIST && d == screenQueue) {

			display.setCurrent(screenPlaylist);

		} else if (c == PlaylistScreen.CMD_QUEUE && d == screenPlaylist) {

			display.setCurrent(screenQueue);

		} else if (c == CMD_BACK && d == screenLibrary) { // LIBRARY //

			display.setCurrent(screenPlayer);

		} else {

			parent.commandAction(c, d);

		}

	}

	public void currentPlobChanged() {

		screenPlayer.update(player.currentPlob);

	}

	public void handleEvent(int type, String info) { // called by
														// communicator
		// thread

		Message msg;
		SerializableString ss;

		switch (type) {

		case Communicator.EVENT_CONNECTED:

			if (lastChosenPlayer != null) { // this is an automatic reconnect

				ss = new SerializableString();
				ss.set(lastChosenPlayer);

				msg = new Message();
				msg.id = Message.ID_SEL_PLAYER;
				msg.bytes = Serial.out(ss);

				comm.sendMessage(msg);

			} // else: keep waiting screen, wait until player list arrived

			break;

		case Communicator.EVENT_DISCONNECTED: // communicator tries to
			// reconnect

			player.reset(); // ensures player knows it is disconnected

			screenConnecting.setMessage(info);
			screenConnecting.setCancable(true);

			display.setCurrent(screenConnecting);

			break;

		case Communicator.EVENT_ERROR:

			disconnect();
			alertDisconnect.setTitle("Connection Error");
			alertDisconnect.setString(info);
			display.setCurrent(alertDisconnect, screenDeviceSelector);

			break;

		default:

			Log.asssertNotReached(this);

			break;
		}
	}

	public void handleMessage(Message m) { // called by communicator thread

		switch (m.id) {

		case Message.ID_IGNORE:

			break;

		case Message.ID_IFS_SRVDOWN:

			Log.ln("[UI] rxed server down");
			disconnect();
			alertDisconnect.setTitle("Server Down");
			alertDisconnect.setString("The Remuco server said bye.");
			display.setCurrent(alertDisconnect, screenDeviceSelector);

			break;

		case Message.ID_SYN_PLIST:

			try {
				Serial.in(playerList, m.bytes);
				screenPlayerList.set(playerList.getNames());
			} catch (BinaryDataExecption e) {
				Log.ln("[UI] rxed malformed data", e);
				disconnect();
				alertDisconnect.setTitle("Error");
				alertDisconnect.setString("Received malformed data :(");
				display.setCurrent(alertDisconnect, screenDeviceSelector);
			}

			if (playerList.getNames().length == 0) { // no players

				display.setCurrent(alertNoPlayer, screenPlayerList);

			} else if (lastChosenPlayer == null) // not chosen a player yet

				display.setCurrent(screenPlayerList);

			else if (!playerList.contains(lastChosenPlayer)) { // player down

				player.reset();
				lastChosenPlayer = null;
				display.setCurrent(screenPlayerList);
			}

			break;

		case Message.ID_IFS_PINFO:

			if (comm == null || !comm.isConnected())
				return; // don't know if this may happen

			try {
				Serial.in(player, m.bytes);
				screenPlayer.themeOrPlayerChanged();
				/*
				 * the theme update ensures that any representation depending on
				 * the player capabilities is now updated and will therefore
				 * reflect the new player capabilities
				 */
				screenPlayer.update(player.state);
				screenPlayer.update(player.currentPlob);

				display.setCurrent(screenPlayer);

			} catch (BinaryDataExecption e) {
				Log.ln("[UI] rxed malformed data", e);
				disconnect();
				alertDisconnect.setTitle("Error");
				alertDisconnect.setString("Received malformed data :(");
				display.setCurrent(alertDisconnect, screenDeviceSelector);
			}

			break;

		default:

			try {
				player.handleMessage(m);
			} catch (BinaryDataExecption e) {
				Log.ln("[UI] rxed malformed data", e);
				disconnect();
				alertDisconnect.setTitle("Error");
				alertDisconnect.setString("Received malformed data :(");
				display.setCurrent(alertDisconnect, screenDeviceSelector);
			} catch (OutOfMemoryError e) {
				Log.ln("[UI] rxed data too big, not enough memory");
				disconnect();
				alertDisconnect.setTitle("Error");
				alertDisconnect
						.setString("Not enough memory for recevied data");
				display.setCurrent(alertDisconnect, screenDeviceSelector);
			}

			break;
		}

	}

	public void keyPressed(int key) {

		int action = Keys.getActionForKey(key);
		int rating, ratingMax;

		// Log.debug("[UI] pressed key "
		// + screenMain.getKeyName(key)
		// + " (id "
		// + key
		// + ") -> "
		// + (action != Keys.ACTION_NOOP ? Keys.actionNames[action]
		// : "no action") + " (id " + action + ")");

		switch (action) {
		case Keys.ACTION_VOLUP:

			volumeAdjuster.startAdjust(true, 0);
			break;

		case Keys.ACTION_VOLDOWN:

			volumeAdjuster.startAdjust(false, 0);
			break;

		case Keys.ACTION_PLAYPAUSE:

			player.ctrlPlayPause();
			break;

		case Keys.ACTION_NEXT:

			progressAdjuster.startAdjust(true, SEEK_DELAY);
			break;

		case Keys.ACTION_PREV:

			progressAdjuster.startAdjust(false, SEEK_DELAY);
			break;

		case Keys.ACTION_RATEDOWN:

			ratingMax = player.getMaxRating();
			if (player.currentPlob == null || ratingMax == 0
					|| (rating = player.currentPlob.getRating() - 1) < 0)
				break;

			player.currentPlob.setRating(rating);
			player.ctrlRate(rating);
			currentPlobChanged();

			break;

		case Keys.ACTION_RATEUP:

			ratingMax = player.getMaxRating();
			if (player.currentPlob == null
					|| ratingMax == 0
					|| (rating = player.currentPlob.getRating() + 1) > ratingMax)
				break;

			player.currentPlob.setRating(rating);
			player.ctrlRate(rating);
			currentPlobChanged();

			break;

		case Keys.ACTION_VOLMUTE:

			player.ctrlVolumeMute();
			break;

		case Keys.ACTION_STOP:

			player.ctrlStop();
			break;

		case Keys.ACTION_IMAGE:

			if (player.currentPlob != null
					&& player.currentPlob.getImg() != null)
				screenPlayer.tooglePlobImageFullscreen();

			break;

		case Keys.ACTION_PLAYLIST:

			if (player.state.isPlayingFromQueue())
				display.setCurrent(screenQueue);
			else
				display.setCurrent(screenPlaylist);

			break;

		case Keys.ACTION_EDITTAGS:

			if (player.currentPlob != null && player.currentPlob.hasTags()) {

				screenTagEditor.set(player.currentPlob.getId(),
						player.currentPlob.getMeta(Plob.META_TAGS));

				display.setCurrent(screenTagEditor);
			}

			break;

		case Keys.ACTION_LIBRARY:

			screenLibrary.showYourself();

			break;

		case Keys.ACTION_NOOP:

			break;

		default:

			Log.ln("[PLSC] unknown action: " + action);
			break;
		}

	}

	public void keyReleased(int key) {

		int action;
		boolean stillInDelay;

		action = Keys.getActionForKey(key);

		switch (action) {
		case Keys.ACTION_VOLUP:
			volumeAdjuster.stopAdjust();
			break;

		case Keys.ACTION_VOLDOWN:
			volumeAdjuster.stopAdjust();
			break;

		case Keys.ACTION_NEXT:
			stillInDelay = progressAdjuster.stopAdjust();
			if (stillInDelay) { // key pressed for a short time -> no seek
				player.ctrlNext();
			} // else: progress adjuster did some seeks, we are done already
			break;

		case Keys.ACTION_PREV:
			stillInDelay = progressAdjuster.stopAdjust();
			if (stillInDelay) { // key pressed for a short time -> no seek
				player.ctrlPrev();
			} // else: progress adjuster did some seeks, we are done already
			break;

		default:
			break;
		}

	}

	public void sendMessage(Message m) {

		if (comm != null && comm.isConnected())
			comm.sendMessage(m);

	}

	/**
	 * To call once when the app starts to hint the UI that it should start its
	 * work i.e. the user interaction.
	 * 
	 */
	public void showYourself() {

		screenDeviceSelector.showYourself();
	}

	public void stateChanged() {

		screenPlayer.update(player.state);		
	}

	/**
	 * Shut down {@link #comm} , reset {@link #player} and
	 * {@link #lastChosenPlayer}.
	 */
	private void disconnect() {

		if (comm != null) {
			comm.down();
			comm = null;
		}
		player.reset();
		lastChosenPlayer = null;
	}

}
