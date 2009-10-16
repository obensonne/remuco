/*   
 *   Remuco - A remote control system for media players.
 *   Copyright (C) 2006-2009 by the Remuco team, see AUTHORS.
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
package remuco.ui.screens;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.ImageItem;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.StringItem;

import remuco.client.common.data.ActionParam;
import remuco.client.common.data.Item;
import remuco.client.common.data.ItemList;
import remuco.client.common.player.Feature;
import remuco.client.common.player.IRequester;
import remuco.client.common.player.Player;
import remuco.ui.CMD;
import remuco.ui.IItemListController;
import remuco.ui.Theme;
import remuco.util.Log;

public final class MediaBrowserScreen extends List implements CommandListener,
		IRequester, IItemListController {

	/** Nice dialog to ask what to do next after an action has been executed. */
	private class PostActionDialog extends Form implements CommandListener {

		private final Command CMD_LIST = new Command("List", Command.BACK, 1);
		private final Command CMD_MAIN = new Command("Main", Command.OK, 1);

		private ItemlistScreen ils = null;

		public PostActionDialog() {

			super("Action Done");

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

			this.setCommandListener(this);
		}

		public void commandAction(Command c, Displayable d) {

			if (c == CMD_LIST && ils != null) {
				ilcGotoPage(ils, ils.getItemList().getPage());
			} else {
				// in theory we do not know that the external command listener
				// handles the back command, however, in practice we do ..
				MediaBrowserScreen.this.commandAction(CMD.BACK, MediaBrowserScreen.this);
			}
			ils = null;
		}

		public void show(ItemlistScreen ils) {
			this.ils = ils;
			display.setCurrent(this);
		}
	}

	private final int cmdIndexPlaylist, cmdIndexQeue, cmdIndexMLib,
			cmdIndexFiles, cmdIndexSearch;

	private final Display display;

	private CommandListener externalCommandListener;

	private final Player player;

	/** Dialog asking what to do next after executing an action. */
	private final PostActionDialog screenPostActionDialog;

	private final SearchScreen screenSearch;

	/**
	 * Alert that indicates a request to the server has issued and the client is
	 * waiting for the reply.
	 */
	private final WaitingScreen screenWaiting;

	private final Theme theme;

	public MediaBrowserScreen(Display display, Player player) {

		super("Media Browser", List.IMPLICIT);

		this.display = display;
		this.player = player;

		theme = Theme.getInstance();

		if (player.info.supports(Feature.REQ_PL)) {
			cmdIndexPlaylist = append("Playlist", theme.licList);
		} else {
			cmdIndexPlaylist = -2;
		}
		if (player.info.supports(Feature.REQ_QU)) {
			cmdIndexQeue = append("Queue", theme.licQueue);
		} else {
			cmdIndexQeue = -2;
		}
		if (player.info.supports(Feature.REQ_MLIB)) {
			cmdIndexMLib = append("Library", theme.licMLib);
		} else {
			cmdIndexMLib = -2;
		}
		if (player.info.getFileActions().size() > 0) {
			cmdIndexFiles = append("Files", theme.licFiles);
		} else {
			cmdIndexFiles = -2;
		}
		if (player.info.getSearchMask().length > 0) {
			cmdIndexSearch = append("Search", theme.licSearch);
		} else {
			cmdIndexSearch = -2;
		}
		setSelectCommand(CMD.SELECT);
		setCommandListener(this);

		screenWaiting = new WaitingScreen();
		screenWaiting.setTitle("Updating");
		screenWaiting.setImage(theme.aicRefresh);
		screenWaiting.setCommandListener(this);

		screenPostActionDialog = new PostActionDialog();

		screenSearch = new SearchScreen(player.info.getSearchMask());
		screenSearch.addCommand(CMD.OK);
		screenSearch.addCommand(CMD.BACK);
		screenSearch.setCommandListener(this);
	}

	public void commandAction(Command c, Displayable d) {

		if (c == CMD.SELECT && d == this) {

			final int index = getSelectedIndex();

			if (index == -1) {
				// ignore

			} else if (index == cmdIndexPlaylist) {

				screenWaiting.attachProperty(d);
				display.setCurrent(screenWaiting);
				player.reqPlaylist(this, 0);

			} else if (index == cmdIndexQeue) {

				screenWaiting.attachProperty(d);
				display.setCurrent(screenWaiting);
				player.reqQueue(this, 0);

			} else if (index == cmdIndexMLib) {

				screenWaiting.attachProperty(d);
				display.setCurrent(screenWaiting);
				player.reqMLib(this, null, 0);

			} else if (index == cmdIndexFiles) {

				screenWaiting.attachProperty(d);
				display.setCurrent(screenWaiting);
				player.reqFiles(this, null, 0);

			} else if (index == cmdIndexSearch) {

				display.setCurrent(screenSearch);

			} else {

				Log.bug("Aug 21, 2009.9:59:30 PM");
			}

		} else if (c == CMD.BACK && d == screenSearch) {

			display.setCurrent(this);

		} else if (c == CMD.OK && d == screenSearch) {

			screenWaiting.attachProperty(d);
			display.setCurrent(screenWaiting);
			player.reqSearch(this, screenSearch.getQuery(), 0);

		} else if (c == WaitingScreen.CMD_CANCEL) {

			player.reqCancel();

			final Displayable next = (Displayable) screenWaiting.detachProperty();
			if (next != null) {
				display.setCurrent(next);
			} // else: too late for cancel

		} else if (externalCommandListener != null) {

			externalCommandListener.commandAction(c, d);

		} else {

			Log.bug("Feb 16, 2009.5:11:32 PM");
		}
	}

	public void handleFiles(ItemList files) {

		if (screenWaiting.detachProperty() == null) {
			return; // already canceled
		}
		display.setCurrent(new ItemlistScreen(display, player.info, this, files));

	}

	public void handleItem(Item item) {
		// currently disabled
	}

	public void handleLibrary(ItemList library) {

		if (screenWaiting.detachProperty() == null) {
			return; // already canceled
		}
		display.setCurrent(new ItemlistScreen(display, player.info, this,
				library));
	}

	public void handlePlaylist(ItemList playlist) {

		if (screenWaiting.detachProperty() == null) {
			return; // already canceled
		}
		final ItemlistScreen ils = new ItemlistScreen(display, player.info,
				this, playlist);

		if (!player.state.isPlayingFromQueue()) {
			ils.setSelectedItem(player.state.getPosition());
		}

		display.setCurrent(ils);
	}

	public void handleQueue(ItemList queue) {

		if (screenWaiting.detachProperty() == null) {
			return; // already canceled
		}
		final ItemlistScreen ils = new ItemlistScreen(display, player.info,
				this, queue);

		if (player.state.isPlayingFromQueue()) {
			ils.setSelectedItem(player.state.getPosition());
		}

		display.setCurrent(ils);
	}

	public void handleSearch(ItemList search) {

		if (screenWaiting.detachProperty() == null) {
			return; // already canceled
		}
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
		} else if (list.isSearch()) {
			player.actionSearch(a);
		} else {
			Log.bug("Mar 13, 2009.10:55:05 PM");
		}

		screenPostActionDialog.show(ils);
	}

	public void ilcBack(ItemlistScreen ils) {

		final ItemList list = ils.getItemList();

		if (list.isSearch()) {
			display.setCurrent(screenSearch);
		} else if (list.isRoot()) {
			display.setCurrent(this);
		} else {
			final String path[] = ils.getItemList().getPathForParent();
			if (list.isMediaLib()) {
				screenWaiting.attachProperty(ils);
				display.setCurrent(screenWaiting);
				player.reqMLib(this, path, 0);
			} else if (list.isFiles()) {
				screenWaiting.attachProperty(ils);
				display.setCurrent(screenWaiting);
				player.reqFiles(this, path, 0);
			} else {
				Log.bug("Mar 13, 2009.10:58:36 PM");
			}
		}
	}

	public void ilcGotoPage(ItemlistScreen ils, int page) {

		final ItemList list = ils.getItemList();

		screenWaiting.attachProperty(ils);
		display.setCurrent(screenWaiting);

		if (list.isPlaylist()) {
			player.reqPlaylist(this, page);
		} else if (list.isQueue()) {
			player.reqQueue(this, page);
		} else if (list.isMediaLib()) {
			player.reqMLib(this, list.getPath(), page);
		} else if (list.isFiles()) {
			player.reqFiles(this, list.getPath(), page);
		} else if (list.isSearch()) {
			player.reqSearch(this, list.getPath(), page);
		} else {
			Log.bug("Mar 13, 2009.10:58:36 PM");
		}

	}

	public void ilcRoot(ItemlistScreen ils) {

		display.setCurrent(this);

	}

	public void ilcShowNested(ItemlistScreen ils, String[] path) {

		final ItemList list = ils.getItemList();

		screenWaiting.attachProperty(ils);
		display.setCurrent(screenWaiting);

		if (list.isMediaLib()) {
			player.reqMLib(this, path, 0);
		} else if (list.isFiles()) {
			player.reqFiles(this, path, 0);
		} else {
			Log.bug("Mar 13, 2009.10:58:36 PM");
		}

	}

	public void setCommandListener(CommandListener l) {
		if (l == this) {
			super.setCommandListener(l);
		} else {
			externalCommandListener = l;
		}
	}
}
