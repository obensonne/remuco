package remuco.ui;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;

import remuco.player.IPlobRequestor;
import remuco.player.IPloblistRequestor;
import remuco.player.Player;
import remuco.player.Plob;
import remuco.player.PlobList;
import remuco.ui.screens.PlobInfoScreen;
import remuco.ui.screens.PloblistScreen;
import remuco.ui.screens.WaitingScreen;
import remuco.util.Log;

public final class MediaBrowser implements CommandListener, IPloblistRequestor,
		IPlobRequestor {

	private static final Command CMD_LIBRARY = new Command("Library",
			Command.ITEM, 30);

	private static final Command CMD_PLAYLIST = new Command("Playlist",
			Command.ITEM, 10);

	private static final Command CMD_QUEUE = new Command("Queue", Command.ITEM,
			20);

	private static final Command CMD_UP = new Command("Up", Command.BACK, 2);

	private final Display display;

	private Displayable displayableBeforeRequest = null;

	/** Displayable to show when library screen has done its job. */
	private final Displayable parent;

	private final Player player;

	/**
	 * Specifies the last requested item (is either a plob id or a ploblist path
	 * or <code>null</code>). Used to check if incoming plobs or ploblists are
	 * the one we've requested at last.
	 */
	private String requestedItem = null;

	private final PlobInfoScreen screenPlobInfo;

	private final PloblistScreen screenPloblist;

	private final CommandList screenRoot;

	/**
	 * Alert that indicates a request to the server has issued and the client is
	 * waiting for the reply.
	 */
	private final WaitingScreen screenWaiting;

	public MediaBrowser(Displayable parent, Display display, Player player) {

		this.display = display;
		this.parent = parent;
		this.player = player;

		screenPlobInfo = new PlobInfoScreen();
		screenPlobInfo.addCommand(CMD.BACK);
		screenPlobInfo.setCommandListener(this);

		screenWaiting = new WaitingScreen();
		screenWaiting.setTitle("Updating");
		screenWaiting.setCommandListener(this);

		screenPloblist = new PloblistScreen();
		screenPloblist.addCommand(CMD_UP);
		screenPloblist.addCommand(CMD.BACK);
		screenPloblist.addCommand(CMD.INFO);
		screenPloblist.addCommand(CMD.SELECT);
		screenPloblist.setSelectCommand(CMD.SELECT);
		screenPloblist.setCommandListener(this);

		screenRoot = new CommandList("Media Browser");
		if (player.info.supportsPlaylist()) {
			screenRoot.addCommand(CMD_PLAYLIST, Theme.LIST_ICON_PLOBLIST);
		}
		if (player.info.supportsQueue()) {
			screenRoot.addCommand(CMD_QUEUE, Theme.LIST_ICON_PLOBLIST);
		}
		if (player.info.supportsLibrary()) {
			screenRoot.addCommand(CMD_LIBRARY, Theme.LIST_ICON_PLOBLIST);
		}
		screenRoot.addCommand(CMD.BACK);
		screenRoot.setCommandListener(this);

		displayableBeforeRequest = screenRoot;
	}

	public void commandAction(Command c, Displayable d) {

		if (c == CMD_PLAYLIST) {

			displayableBeforeRequest = d;
			display.setCurrent(screenWaiting);
			player.reqPloblist(PlobList.PATH_PLAYLIST_S, this);
			requestedItem = PlobList.PATH_PLAYLIST_S;

		} else if (c == CMD_QUEUE) {

			displayableBeforeRequest = d;
			display.setCurrent(screenWaiting);
			player.reqPloblist(PlobList.PATH_QUEUE_S, this);
			requestedItem = PlobList.PATH_QUEUE_S;

		} else if (c == CMD_LIBRARY) {

			displayableBeforeRequest = d;
			display.setCurrent(screenWaiting);
			player.reqPloblist("", this);
			requestedItem = "";

		} else if (c == CMD.SELECT && d == screenPloblist) {

			final int index = screenPloblist.getSelectedIndex();

			if (index < 0) {
				return;
			}

			final PlobList pl = screenPloblist.getPloblist();

			if (screenPloblist.getImage(index) == Theme.LIST_ICON_PLOBLIST) {
				displayableBeforeRequest = d;
				display.setCurrent(screenWaiting);
				final String path = pl.getPathForNested(index);
				player.reqPloblist(path, this);
				requestedItem = path;
			} else {
				player.ctrlJump(pl.getPath(), index - pl.getNumNested());
				display.setCurrent(parent);
			}

		} else if (c == CMD.INFO && d == screenPloblist) {

			final int index = screenPloblist.getSelectedIndex();

			if (index < 0) {
				return;
			}

			final PlobList pl = screenPloblist.getPloblist();

			if (screenPloblist.getImage(index) == Theme.LIST_ICON_PLOBLIST) {

				displayableBeforeRequest = d;
				display.setCurrent(screenWaiting);
				final String path = pl.getPathForNested(index);
				player.reqPloblist(path, this);
				requestedItem = path;

			} else { // PLOB item

				if (player.info.supportsPlobInfo()) {

					displayableBeforeRequest = d;
					display.setCurrent(screenWaiting);
					final String id = pl.getPlobID(index - pl.getNumNested());
					player.reqPlob(id, this);
					requestedItem = id;

				} else {

					screenPlobInfo.setPlob(pl.getPlobName(index
							- pl.getNumNested()));
					display.setCurrent(screenPlobInfo);
				}
			}

		} else if (c == CMD_UP && d == screenPloblist) {

			final PlobList pl = screenPloblist.getPloblist();

			if (pl == null) {
				display.setCurrent(screenRoot);
				return;
			}

			final String parentPath = pl.getParentPath();

			if (parentPath == null) {
				display.setCurrent(screenRoot);
				return;
			}

			requestedItem = parentPath;
			displayableBeforeRequest = d;

			player.reqPloblist(parentPath, this);

		} else if (c == CMD.BACK && d == screenPlobInfo) {

			display.setCurrent(screenPloblist);

		} else if (c == CMD.BACK && (d == screenRoot || d == screenPloblist)) {

			display.setCurrent(parent);

		} else if (c == WaitingScreen.CMD_CANCEL) {

			requestedItem = null;

			display.setCurrent(displayableBeforeRequest);

		} else {

			Log.bug("Feb 16, 2009.5:11:32 PM");
		}
	}

	public void handlePlob(Plob p) {

		if (requestedItem == null || !requestedItem.equals(p.getId())) {
			return;
		}

		screenPlobInfo.setPlob(p);

		display.setCurrent(screenPlobInfo);
	}

	public void handlePloblist(PlobList pl) {

		if (requestedItem == null || !requestedItem.equals(pl.getPath())) {
			return;
		}

		screenPloblist.setPloblist(pl);
		
		if (pl.isPlaylist() && !player.state.isPlayingFromQueue()) {
			try {
				screenPloblist.setSelectedIndex(player.state.getPosition(), true);
			} catch (ArrayIndexOutOfBoundsException e) {
			}
		} else if (pl.isQueue() && player.state.isPlayingFromQueue()) {
			try {
				screenPloblist.setSelectedIndex(player.state.getPosition(), true);
			} catch (ArrayIndexOutOfBoundsException e) {
			}
		}

		display.setCurrent(screenPloblist);
	}

	public void showYourself() {

		if (!player.info.supportsPlaylist() && !player.info.supportsQueue()
				&& !player.info.supportsLibrary()) {

			display.setCurrent(new Alert("Media Browsing",
					"Media browsing not supported.", null, AlertType.INFO),
					parent);

		} else {

			display.setCurrent(screenRoot);
		}
	}

}
