package remuco.ui;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;

import remuco.player.ActionParam;
import remuco.player.Feature;
import remuco.player.IRequester;
import remuco.player.Item;
import remuco.player.ItemList;
import remuco.player.Player;
import remuco.ui.screens.ItemlistScreen;
import remuco.ui.screens.WaitingScreen;
import remuco.util.Log;

public final class MediaBrowser implements CommandListener, IRequester,
		IItemListController {

	private static final Command CMD_FILES = new Command("File Browser",
			Command.ITEM, 40);

	private static final Command CMD_MLIB = new Command("Library",
			Command.ITEM, 30);

	private static final Command CMD_PLAYLIST = new Command("Playlist",
			Command.ITEM, 10);

	private static final Command CMD_QUEUE = new Command("Queue", Command.ITEM,
			20);

	private final Display display;

	private Displayable displayableBeforeRequest = null;

	/** Displayable to show when library screen has done its job. */
	private final Displayable parent;

	private final Player player;

	private final CommandList screenRoot;

	/**
	 * Alert that indicates a request to the server has issued and the client is
	 * waiting for the reply.
	 */
	private final WaitingScreen screenWaiting;

	private final Theme theme;

	public MediaBrowser(Displayable parent, Display display, Player player) {

		this.display = display;
		this.parent = parent;
		this.player = player;

		theme = Theme.getInstance();

		screenWaiting = new WaitingScreen();
		screenWaiting.setTitle("Updating");
		screenWaiting.setImage(theme.aicRefresh);
		screenWaiting.setCommandListener(this);

		screenRoot = new CommandList("Media Browser");
		if (player.info.supports(Feature.REQ_PL)) {
			screenRoot.addCommand(CMD_PLAYLIST, theme.licNested);
		}
		if (player.info.supports(Feature.REQ_QU)) {
			screenRoot.addCommand(CMD_QUEUE, theme.licNested);
		}
		if (player.info.supports(Feature.REQ_MLIB)) {
			screenRoot.addCommand(CMD_MLIB, theme.licNested);
		}
		if (player.info.getFileActions().size() > 0) {
			screenRoot.addCommand(CMD_FILES, theme.licNested);
		}
		screenRoot.addCommand(CMD.BACK);
		screenRoot.setCommandListener(this);

		displayableBeforeRequest = screenRoot;
	}

	public void commandAction(Command c, Displayable d) {

		if (c == CMD_PLAYLIST) {

			displayableBeforeRequest = d;
			display.setCurrent(screenWaiting);
			player.reqPlaylist(this);

		} else if (c == CMD_QUEUE) {

			displayableBeforeRequest = d;
			display.setCurrent(screenWaiting);
			player.reqQueue(this);

		} else if (c == CMD_MLIB) {

			displayableBeforeRequest = d;
			display.setCurrent(screenWaiting);
			player.reqMLib(this, null);

		} else if (c == CMD_FILES) {

			displayableBeforeRequest = d;
			display.setCurrent(screenWaiting);
			player.reqFiles(this, null);

		} else if (c == CMD.BACK && d == screenRoot) {

			display.setCurrent(parent);

		} else if (c == WaitingScreen.CMD_CANCEL) {

			player.reqCancel();

			display.setCurrent(displayableBeforeRequest);

		} else {

			Log.bug("Feb 16, 2009.5:11:32 PM");
		}
	}

	public void handleFiles(ItemList files) {

		display.setCurrent(new ItemlistScreen(display, this, files));

	}

	public void handleItem(Item item) {
		// currently disabled
	}

	public void handleLibrary(ItemList library) {
		display.setCurrent(new ItemlistScreen(display, this, library));
	}

	public void handlePlaylist(ItemList playlist) {

		final ItemlistScreen ils = new ItemlistScreen(display, this, playlist);

		if (!player.state.isPlayingFromQueue()) {
			ils.setSelectedItem(player.state.getPosition());
		}

		display.setCurrent(ils);
	}

	public void handleQueue(ItemList queue) {

		final ItemlistScreen ils = new ItemlistScreen(display, this, queue);

		if (player.state.isPlayingFromQueue()) {
			ils.setSelectedItem(player.state.getPosition());
		}

		display.setCurrent(ils);
	}

	public void ilcAction(ItemlistScreen ils, ActionParam a) {

		final ItemList list = ils.getItemList();

		if (list.isPlaylist()) {
			player.actionPlaylist(a);
		} else if (list.isQueue()) {
			player.actionQueue(a);
		} else if (list.isMediaLib()) {
			player.actionMediaLib(a);
		} else if (list.isFiles()) {
			player.actionFiles(a);
		} else {
			Log.bug("Mar 13, 2009.10:55:05 PM");
		}

		display.setCurrent(parent);
	}

	public void ilcBack(ItemlistScreen ils) {

		final ItemList list = ils.getItemList();

		if (list.isRoot()) {
			display.setCurrent(screenRoot);
		} else {
			final String path[] = ils.getItemList().getPathForParent();
			if (list.isMediaLib()) {
				display.setCurrent(screenWaiting);
				player.reqMLib(this, path);
			} else if (list.isFiles()) {
				display.setCurrent(screenWaiting);
				player.reqFiles(this, path);
			} else {
				Log.bug("Mar 13, 2009.10:58:36 PM");
			}
		}

	}

	public void ilcRoot(ItemlistScreen ils) {

		display.setCurrent(screenRoot);

	}

	public void ilcShowNested(ItemlistScreen ils, String[] path) {

		final ItemList list = ils.getItemList();

		if (list.isMediaLib()) {
			display.setCurrent(screenWaiting);
			player.reqMLib(this, path);
		} else if (list.isFiles()) {
			display.setCurrent(screenWaiting);
			player.reqFiles(this, path);
		} else {
			Log.bug("Mar 13, 2009.10:58:36 PM");
		}

	}

	public void showYourself() {

		display.setCurrent(screenRoot);
	}

}
