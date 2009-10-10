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
package remuco.ui.screens;

import java.util.Enumeration;
import java.util.Hashtable;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextField;

import remuco.player.AbstractAction;
import remuco.player.ActionParam;
import remuco.player.ItemAction;
import remuco.player.ItemList;
import remuco.player.PlayerInfo;
import remuco.ui.CMD;
import remuco.ui.IItemListController;
import remuco.ui.Theme;
import remuco.util.Log;

public final class ItemlistScreen extends List implements CommandListener {

	private static final Command CMD_GOTO_PAGE = new Command("Page go to",
			Command.SCREEN, 10);

	private static final Command CMD_MARK = new Command("Mark", Command.SCREEN,
			1);

	private static final Command CMD_MARK_ALL = new Command("Mark all",
			Command.SCREEN, 11);

	private static final Command CMD_PAGE_DOWN = new Command("Page down",
			Command.SCREEN, 9);

	private static final Command CMD_PAGE_UP = new Command("Page up",
			Command.SCREEN, 8);

	private static final Command CMD_ROOT = new Command("Root", Command.SCREEN,
			99);

	/** Pseudo-index for marking all items. */
	private static final int MARK_ALL = -1;

	private final Hashtable actionCommands;

	private final Display display;

	/** Flags indicating for each item if it has been marked by the user. */
	private boolean itemMarkedFlags[] = new boolean[0];

	/** The displayed item list. */
	private final ItemList list;

	private final IItemListController listener;

	/** Number of items which have been marked by the user. */
	private int numberOfMarkedItems = 0;

	/** Number of items. */
	private final int numItems;

	/** Number of nested lists. */
	private final int numNested;

	/** Screen to select a specific page number. */
	private final Form screenPageSelecetion;

	/** Text field to enter a page number. */
	private final TextField tfPageSelection;

	private final Theme theme;

	public ItemlistScreen(Display display, PlayerInfo pinfo,
			IItemListController listener, ItemList list) {

		super("", List.IMPLICIT);

		// init some fields

		this.display = display;
		this.listener = listener;
		this.list = list;

		theme = Theme.getInstance();

		numNested = list.getNumNested();
		numItems = list.getNumItems();

		itemMarkedFlags = new boolean[list.getNumItems()];

		// page selection screen

		if (list.getPageMax() > 1) {
			addCommand(CMD_GOTO_PAGE);
			screenPageSelecetion = new Form("Go to page ..");
			final StringItem si = new StringItem("Page number (1 .. "
					+ (list.getPageMax() + 1) + ")", null);
			si.setLayout(Item.LAYOUT_CENTER);
			screenPageSelecetion.append(si);
			tfPageSelection = new TextField(null,
					String.valueOf(list.getPage() + 1), 6, TextField.NUMERIC);
			tfPageSelection.setLayout(Item.LAYOUT_CENTER);
			screenPageSelecetion.append(tfPageSelection);
			screenPageSelecetion.addCommand(CMD.BACK);
			screenPageSelecetion.addCommand(CMD.OK);
			screenPageSelecetion.setCommandListener(this);
		} else {
			screenPageSelecetion = null;
			tfPageSelection = null;
		}

		// set up content

		setTitle(list.getName());

		final Image licNested = list.isFiles() ? theme.licFiles : theme.licList;

		for (int i = 0; i < numNested; i++) {
			append(list.getNested(i), licNested);
		}
		for (int i = 0; i < numItems; i++) {
			append(list.getItemName(i), theme.licItem);
		}

		// action screen

		// commands

		if (numItems > 0) {
			// FIXME: ambiguous when there are items _and_ nested lists
			setSelectCommand(CMD_MARK);
		} else {
			setSelectCommand(CMD.SELECT);
		}
		addCommand(CMD.BACK);
		addCommand(CMD_ROOT);

		if (numItems > 0) {
			addCommand(CMD_MARK_ALL);
		}

		if (list.getPage() > 0) {
			addCommand(CMD_PAGE_UP);
		}
		if (list.getPage() < list.getPageMax()) {
			addCommand(CMD_PAGE_DOWN);
		}

		actionCommands = new Hashtable(list.getActions().size());

		final Enumeration e = list.getActions().elements();
		while (e.hasMoreElements()) {
			final AbstractAction a = (AbstractAction) e.nextElement();
			final String label;
			if (a.isListAction()) {
				if (numNested == 0) {
					continue;
				}
				label = a.label + " (list)";
			} else { // item action
				if (numItems == 0) {
					continue;
				}
				if (((ItemAction) a).multiple) {
					label = a.label + " (marked)";
				} else {
					label = a.label + " (focussed)";
				}
			}
			final Command c = new Command(label, Command.SCREEN, 10);
			actionCommands.put(c, a);
			addCommand(c);
		}

		// misc

		super.setCommandListener(this);

	}

	public void commandAction(Command c, Displayable d) {

		if (c == CMD_ROOT) {

			listener.ilcRoot(this);

		} else if (c == CMD.BACK && d == this) {

			listener.ilcBack(this);

		} else if (c == CMD_MARK_ALL) {

			toggleItemMark(MARK_ALL);
			updateItemIcons();

		} else if (c == CMD_PAGE_UP) {

			listener.ilcGotoPage(this, list.getPage() - 1);

		} else if (c == CMD_PAGE_DOWN) {

			listener.ilcGotoPage(this, list.getPage() + 1);

		} else if (c == CMD_GOTO_PAGE) {

			display.setCurrent(screenPageSelecetion);

		} else if (c == CMD.OK && d == screenPageSelecetion) {

			int page = -1;
			try {
				page = Integer.parseInt(tfPageSelection.getString());
			} catch (NumberFormatException e) {
			}
			if (page < 1 || page > list.getPageMax() + 1) {
				tfPageSelection.setString(Integer.toString(list.getPage() + 1));
			} else {
				listener.ilcGotoPage(this, page - 1);
			}

		} else if (c == CMD.BACK && d == screenPageSelecetion) {

			display.setCurrent(this);

		} else if ((c == CMD_MARK || c == CMD.SELECT) && d == this) {

			final int index = getSelectedIndex();
			if (index < 0) {
				return;
			}

			if (index < numNested) { // nested list selected

				listener.ilcShowNested(this, list.getPathForNested(index));

			} else { // item selected

				toggleItemMark(index - numNested);
				updateItemIcons();
			}

		} else if (actionCommands.containsKey(c)) {

			handleAction((AbstractAction) actionCommands.get(c));

		} else {
			Log.bug("unexpected ILS-command: " + c.getLabel());
		}

	}

	public ItemList getItemList() {
		return list;
	}

	public void setCommandListener(CommandListener l) {
		Log.bug("Mar 9, 2009.8:53:00 PM");
	}

	public void setSelectedItem(int nr) {

		if (list == null) {
			return;
		}
		if (nr >= 0 && nr < numItems) {
			final int index = numNested + nr;
			if (index < size()) {
				setSelectedIndex(index, true);
			}
		}
	}

	public void setSelectedNested(String name) {

		for (int i = 0; i < numNested; i++) {
			if (list.getNested(i).equals(name)) {
				if (i < size()) {
					setSelectedIndex(i, true);
				}
				break;
			}
		}

	}

	private void actionAlert(String msg) {

		final Alert alert = new Alert("Oops..");
		alert.setImage(theme.aicHmpf);
		alert.setString(msg);
		alert.setTimeout(2000);
		display.setCurrent(alert, this);
	}

	private void handleAction(AbstractAction a) {

		final int index = getSelectedIndex();
		if (index < 0) {
			return;
		}

		if (a.isItemAction()) {

			final ItemAction ia = (ItemAction) a;

			if (ia.multiple && numberOfMarkedItems == 0) {

				actionAlert("This action requires one or more marked items.");

			} else if (index < numNested) {

				actionAlert("This is an item action, not applicable to lists.");

			} else {

				final int positions[];
				final String ids[];

				if (!ia.multiple) { // use focussed item

					final int itemNo = index - numNested;

					positions = new int[] { list.getItemPosAbsolute(itemNo) };
					ids = new String[] { list.getItemID(itemNo) };

				} else { // use marked items

					positions = new int[numberOfMarkedItems];
					ids = new String[numberOfMarkedItems];

					int n = 0;
					for (int i = 0; i < numItems; i++) {

						if (itemMarkedFlags[i]) {
							positions[n] = list.getItemPosAbsolute(i);
							ids[n] = list.getItemID(i);
							n++;
						}
					}
				}

				final ActionParam ap;
				if (list.isPlaylist() || list.isQueue()) {
					ap = new ActionParam(a.id, positions, ids);
				} else {
					ap = new ActionParam(a.id, list.getPath(), positions, ids);
				}
				listener.ilcAction(this, ap);
			}

		} else { // list action

			if (index >= numNested) {

				actionAlert("This is a list action, not applicable to items");

			} else {

				final int listNo = index;
				listener.ilcAction(this, new ActionParam(a.id,
						list.getPathForNested(listNo), null, null));
			}
		}
	}

	private void toggleItemMark(int index) {

		if (itemMarkedFlags.length == 0) {
			return;
		}

		if (index == MARK_ALL) {

			for (int i = 0; i < itemMarkedFlags.length; i++) {
				itemMarkedFlags[i] = true;
			}
			numberOfMarkedItems = itemMarkedFlags.length;

			if (getSelectedIndex() < numNested && numItems > 0) {
				setSelectedIndex(numNested, true); // jump to first item
			}

			return;
		}

		if (itemMarkedFlags[index]) {
			itemMarkedFlags[index] = false;
			numberOfMarkedItems--;
		} else {
			itemMarkedFlags[index] = true;
			numberOfMarkedItems++;
		}

		if (index < numItems - 1) {
			setSelectedIndex(numNested + index + 1, true); // jump to next item
		}
	}

	private void updateItemIcons() {

		for (int i = 0; i < itemMarkedFlags.length; i++) {
			final int index = numNested + i;
			if (itemMarkedFlags[i]) {
				set(index, getString(index), theme.licItemMarked);
			} else {
				set(index, getString(index), theme.licItem);
			}
		}
	}
}
