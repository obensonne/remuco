/*   
 *   Remuco - A remote control system for media players.
 *   Copyright (C) 2006-2009 Oben Sonne <obensonne@googlemail.com>
 *
 *   This file is part of Remuco.
 *
 *   Remuco is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Remuco is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Remuco.  If not, see <http://www.gnu.org/licenses/>.
 *   
 */
package remuco.ui;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.ImageItem;
import javax.microedition.lcdui.StringItem;

import remuco.player.ActionParam;
import remuco.player.Feature;
import remuco.player.IRequester;
import remuco.player.Item;
import remuco.player.ItemList;
import remuco.player.Player;
import remuco.ui.screens.ItemlistScreen;
import remuco.ui.screens.SearchScreen;
import remuco.ui.screens.WaitingScreen;
import remuco.util.Log;

public final class MediaBrowser implements CommandListener, IRequester,
		IItemListController {

	/** Nice dialog to ask what to do next after an action has been executed. */
	private class PostActionDialog extends Form implements CommandListener {

		private final Command CMD_LIST = new Command("List", Command.BACK, 1);
		private final Command CMD_MAIN = new Command("Main", Command.OK, 1);

		private ItemlistScreen ils = null;

		private final MediaBrowser outer;

		public PostActionDialog(MediaBrowser outer) {

			super("Action Done");

			this.outer = outer;

			final int layout;
			layout = StringItem.LAYOUT_CENTER | StringItem.LAYOUT_NEWLINE_AFTER;

			append(" \n");
			append(new ImageItem(null, theme.aicYes, layout, ""));
			append(" \n");

			StringItem text;
			text = new StringItem("What's next?", null);
			text.setLayout(layout);
			append(text);
			text = new StringItem(
					"Go back to the main screen or show the list again?", null);
			text.setLayout(layout);
			append(text);

			addCommand(CMD_MAIN);
			addCommand(CMD_LIST);

			setCommandListener(this);

		}

		public void commandAction(Command c, Displayable d) {

			if (c == CMD_LIST && ils != null) {
				if (ils.getItemList().isPlaylist()) {
					// playlist may have been changed by action
					outer.commandAction(CMD_PLAYLIST, screenRoot);
				} else if (ils.getItemList().isQueue()) {
					// queue may have been changed by action
					outer.commandAction(CMD_QUEUE, screenRoot);
				} else {
					display.setCurrent(ils);
				}
			} else {
				display.setCurrent(parent);
			}
			ils = null;
		}

		public void show(ItemlistScreen ils) {
			this.ils = ils;
			display.setCurrent(this);
		}

	}

	private static final Command CMD_FILES = new Command("File Browser",
			Command.ITEM, 40);

	private static final Command CMD_MLIB = new Command("Library",
			Command.ITEM, 30);

	private static final Command CMD_PLAYLIST = new Command("Playlist",
			Command.ITEM, 10);

	private static final Command CMD_QUEUE = new Command("Queue", Command.ITEM,
			20);

	private static final Command CMD_SEARCH = new Command("Search",
			Command.ITEM, 50);

	private final Display display;

	private Displayable displayableBeforeRequest = null;

	/** Displayable to show when library screen has done its job. */
	private final Displayable parent;

	private final Player player;

	/** Dialog asking what to do next after executing an action. */
	private final PostActionDialog screenPostActionDialog;

	private final CommandList screenRoot;
	
	private final SearchScreen screenSearch;

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

		screenPostActionDialog = new PostActionDialog(this);

		screenRoot = new CommandList("Media Browser");
		if (player.info.supports(Feature.REQ_PL)) {
			screenRoot.addCommand(CMD_PLAYLIST, theme.licList);
		}
		if (player.info.supports(Feature.REQ_QU)) {
			screenRoot.addCommand(CMD_QUEUE, theme.licQueue);
		}
		if (player.info.supports(Feature.REQ_MLIB)) {
			screenRoot.addCommand(CMD_MLIB, theme.licMLib);
		}
		if (player.info.getFileActions().size() > 0) {
			screenRoot.addCommand(CMD_FILES, theme.licFiles);
		}
		if (player.info.getSearchMask().length > 0) {
			screenRoot.addCommand(CMD_SEARCH, theme.licSearch);
		}
		screenRoot.addCommand(CMD.BACK);
		screenRoot.setCommandListener(this);
		
		screenSearch = new SearchScreen(player.info.getSearchMask());
		screenSearch.addCommand(CMD.OK);
		screenSearch.addCommand(CMD.BACK);
		screenSearch.setCommandListener(this);

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

		} else if (c == CMD_SEARCH) {

			display.setCurrent(screenSearch);

		} else if (c == CMD.BACK && d == screenSearch) {

			display.setCurrent(screenRoot);

		} else if (c == CMD.OK && d == screenSearch) {

			displayableBeforeRequest = d;
			display.setCurrent(screenWaiting);
			player.reqSearch(this, screenSearch.getQuery());

		} else if (c == CMD.BACK && d == screenRoot) {

			displayableBeforeRequest = screenRoot; // release item list screen
			display.setCurrent(parent);

		} else if (c == WaitingScreen.CMD_CANCEL) {

			player.reqCancel();

			display.setCurrent(displayableBeforeRequest);

		} else {

			Log.bug("Feb 16, 2009.5:11:32 PM");
		}
	}

	public void handleFiles(ItemList files) {

		display.setCurrent(new ItemlistScreen(display, player.info, this, files));

	}

	public void handleItem(Item item) {
		// currently disabled
	}

	public void handleLibrary(ItemList library) {
		display.setCurrent(new ItemlistScreen(display, player.info, this,
				library));
	}

	public void handlePlaylist(ItemList playlist) {

		final ItemlistScreen ils = new ItemlistScreen(display, player.info,
				this, playlist);

		if (!player.state.isPlayingFromQueue()) {
			ils.setSelectedItem(player.state.getPosition());
		}

		display.setCurrent(ils);
	}

	public void handleQueue(ItemList queue) {

		final ItemlistScreen ils = new ItemlistScreen(display, player.info,
				this, queue);

		if (player.state.isPlayingFromQueue()) {
			ils.setSelectedItem(player.state.getPosition());
		}

		display.setCurrent(ils);
	}

	public void handleSearch(ItemList search) {

		display.setCurrent(new ItemlistScreen(display, player.info, this,
				search));
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

		screenPostActionDialog.show(ils);
	}

	public void ilcBack(ItemlistScreen ils) {

		final ItemList list = ils.getItemList();

		if (list.isRoot()) {
			display.setCurrent(screenRoot);
		} else {
			final String path[] = ils.getItemList().getPathForParent();
			if (list.isMediaLib()) {
				displayableBeforeRequest = ils;
				display.setCurrent(screenWaiting);
				player.reqMLib(this, path);
			} else if (list.isFiles()) {
				displayableBeforeRequest = ils;
				display.setCurrent(screenWaiting);
				player.reqFiles(this, path);
			} else if (list.isSearch()) {
				display.setCurrent(screenSearch);				
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
			displayableBeforeRequest = ils;
			display.setCurrent(screenWaiting);
			player.reqMLib(this, path);
		} else if (list.isFiles()) {
			displayableBeforeRequest = ils;
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
