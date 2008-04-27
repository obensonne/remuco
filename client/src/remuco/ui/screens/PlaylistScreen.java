package remuco.ui.screens;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;

import remuco.player.IPlaylistListener;
import remuco.player.IPlobRequestor;
import remuco.player.IQueueListener;
import remuco.player.Player;
import remuco.player.Plob;
import remuco.player.PlobList;
import remuco.ui.UI;

/**
 * Screen to display the playlist or queue.
 * 
 * TODO: automatically preselect current plob in playlist/queue
 */
public final class PlaylistScreen extends List implements CommandListener,
		IPlobRequestor, IPlaylistListener, IQueueListener {

	private static final Command CMD_PLAY = new Command("Play", Command.ITEM,
			10);

	public static final Command CMD_PLAYLIST = new Command("Playlist",
			Command.ITEM, 30);

	public static final Command CMD_QUEUE = new Command("Queue", Command.ITEM,
			30);

	private static final Command CMD_SHOW = new Command("Show", Command.ITEM,
			20);

	private static final String PLAYLIST_ID = "__PLAYLIST__";

	private static final String QUEUE_ID = "__QUEUE__";

	private final Display display;

	/** An always valid reference to the player's playlist/queue. */
	private final PlobList list;

	private final CommandListener parent;

	private final Player player;

	private final Plob plob;

	/**
	 * Indicates that a playlist screen shows the content of the queue and not
	 * the regular playlist.
	 */
	private boolean queueMode;

	private final PlobInfoScreen screenPlobInfo;

	private final WaitingScreen screenWaiting;

	/**
	 * Screen to show a playlist or queue.
	 * 
	 * @param display
	 * @param player
	 * @param parent
	 *            the command listener to delegate the following commands to:
	 *            <ul>
	 *            <li>commands added with {@link #addCommand(Command)}
	 *            <li>{@link UI#CMD_BACK}
	 *            <li>{@link #CMD_PLAYLIST} and {@link #CMD_QUEUE}
	 *            </ul>
	 */
	public PlaylistScreen(CommandListener parent, Display display,
			Player player, boolean queueMode) {

		super(queueMode ? "Queue" : "Playlist", List.IMPLICIT);

		super.setCommandListener(this);

		this.display = display;
		this.parent = parent;
		this.player = player;
		this.queueMode = queueMode;

		plob = new Plob();

		screenPlobInfo = new PlobInfoScreen();
		screenPlobInfo.addCommand(UI.CMD_BACK);
		screenPlobInfo.setCommandListener(this);

		screenWaiting = new WaitingScreen();
		screenWaiting.setTitle("Updating");
		screenWaiting.setCommandListener(this);

		if (queueMode) {
			addCommand(CMD_PLAYLIST);
			list = player.queue;
			player.registerQueueListener(this);
		} else {
			addCommand(CMD_QUEUE);
			list = player.playlist;
			player.registerPlaylistListener(this);
		}

		updateList();

	}

	public void commandAction(Command c, Displayable d) {

		int i;

		if (c == CMD_SHOW) {

			player.reqPlob(list.getPlobID(getSelectedIndex()), plob, this);

			screenWaiting.setCancable(true);
			display.setCurrent(screenWaiting);

		} else if (c == CMD_PLAY) {

			i = getSelectedIndex();
			player.ctrlJump(queueMode ? QUEUE_ID : PLAYLIST_ID, i);

			parent.commandAction(UI.CMD_BACK, this);

		} else if (c == UI.CMD_BACK && d == screenPlobInfo) {

			display.setCurrent(this);

		} else if (c == WaitingScreen.CMD_CANCEL) { // canceled a plob request

			display.setCurrent(this);

		} else {

			parent.commandAction(c, d);

		}
	}

	public void handlePlob(Plob p) { // FYI: p == this.plob

		if (!screenWaiting.isShown())
			// user has canceled plob request
			return;

		screenPlobInfo.setPlob(p);

		display.setCurrent(screenPlobInfo);
	}

	public void playlistChanged() { // doesn't get called in queue mode
		if (isShown()) {
			screenWaiting.setCancable(false);
			display.setCurrent(screenWaiting);
			updateList();
			display.setCurrent(this);
		} else {
			updateList();			
		}
	}

	public void queueChanged() { // only gets called in queue mode
		if (isShown()) {
			screenWaiting.setCancable(false);
			display.setCurrent(screenWaiting);
			updateList();
			display.setCurrent(this);
		} else {
			updateList();			
		}
	}

	public void setSelectedPlob(int nr) {

		if (nr >= 0 && nr < size()) {
			setSelectedIndex(nr, true);
		}
	}

	public void setSelectedPlob(String pid) {

		int len;

		len = list.getNumPlobs();

		if (len > size())
			return;

		for (int i = 0; i < len; i++) {
			if (list.getPlobID(i).equals(pid))
				setSelectedIndex(i, true);
		}
	}

	private void updateList() {

		int len;

		deleteAll();

		len = list.getNumPlobs();

		// show or hide item dependent commands

		if (len == 0) {

			removeCommand(CMD_SHOW);
			removeCommand(CMD_PLAY);

		} else {

			addCommand(CMD_SHOW);
			addCommand(CMD_PLAY);
			setSelectCommand(CMD_PLAY);
		}

		for (int i = 0; i < len; i++)
			append(list.getPlobName(i), null);

	}

}
