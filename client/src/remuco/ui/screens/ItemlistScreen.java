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
import java.util.TimerTask;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.ImageItem;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextField;

import remuco.MainLoop;
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

	private class ActionAlert extends Form implements CommandListener {

		private static final int LAYOUT = Item.LAYOUT_CENTER
				| Item.LAYOUT_NEWLINE_AFTER;

		private final Display display;

		private final ImageItem element;

		private final StringItem issue, solution;

		private final Displayable parent;

		protected ActionAlert(Display display, Displayable parent) {
			super("Action");
			this.display = display;
			this.parent = parent;
			this.issue = new StringItem(null, "");
			this.issue.setLayout(LAYOUT);
			this.element = new ImageItem(null, theme.licItem, LAYOUT, null);
			this.solution = new StringItem(null, "");
			this.solution.setLayout(LAYOUT);
			this.append(new ImageItem(null, theme.aicHmpf, LAYOUT, null));
			this.append(issue);
			this.append("\n");
			this.append(element);
			this.append("\n");
			this.append(solution);
			this.addCommand(CMD.OK);
			this.setCommandListener(this);
		}

		public void commandAction(Command c, Displayable d) {
			this.display.setCurrent(parent);
		}

		protected void show(AbstractAction a, String issue, String solution) {

			this.issue.setLabel(a.label);
			this.issue.setText(issue + "\n");
			this.element.setImage(a.isListAction() ? theme.licList
					: theme.licItemMarked);
			this.solution.setText(solution);
			this.display.setCurrent(this);
		}
	}

	private class AutoMarker extends TimerTask {

		private int lastIndex = -1;

		public void run() {

			final int index = getSelectedIndex();
			if (index == lastIndex || index < 0) { // nothing or same selected
				return;
			}
			if (lastIndex >= numNested) { // unmark last item
				set(lastIndex, getString(lastIndex), theme.licItem);
				lastIndex = -1;
			}
			if (index < numNested) { // no item selected
				return;
			}
			// new item selected
			lastIndex = index;
			set(index, getString(index), theme.licItemMarked);
		}

	}

	private static final Command CMD_GOTO_PAGE = new Command("Page go to",
			Command.SCREEN, 10);

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

	private final ActionAlert aa;

	private final Hashtable actionCommands;

	private AutoMarker autoMarker = null;

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

		aa = new ActionAlert(display, this);

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

		setSelectCommand(CMD.SELECT);
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
					label = a.label + " (items)";
				} else {
					label = a.label + " (item)";
				}
			}
			final Command c = new Command(label, Command.SCREEN, 10);
			actionCommands.put(c, a);
			addCommand(c);
		}

		// misc

		if (numItems > 0) {
			enableAutoMarker();
		}

		super.setCommandListener(this);

	}

	public void commandAction(Command c, Displayable d) {

		if (c == CMD_ROOT) {

			disableAutoMarker();
			listener.ilcRoot(this);

		} else if (c == CMD.BACK && d == this) {

			disableAutoMarker();
			listener.ilcBack(this);

		} else if (c == CMD_MARK_ALL) {

			toggleItemMark(MARK_ALL);
			updateItemIcons();

		} else if (c == CMD_PAGE_UP) {

			disableAutoMarker();
			listener.ilcGotoPage(this, list.getPage() - 1);

		} else if (c == CMD_PAGE_DOWN) {

			disableAutoMarker();
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
				disableAutoMarker();
				listener.ilcGotoPage(this, page - 1);
			}

		} else if (c == CMD.BACK && d == screenPageSelecetion) {

			display.setCurrent(this);

		} else if (c == CMD.SELECT && d == this) {

			final int index = getSelectedIndex();
			if (index < 0) {
				return;
			}

			if (index < numNested) { // nested list selected

				disableAutoMarker();
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

	private void disableAutoMarker() {

		if (autoMarker == null) {
			return;
		}

		autoMarker.cancel();
		autoMarker = null;

	}

	private void enableAutoMarker() {

		if (autoMarker != null) {
			return;
		}

		autoMarker = new AutoMarker();
		MainLoop.schedule(autoMarker, 100, 100);

	}

	private void handleAction(AbstractAction a) {

		final int index = getSelectedIndex();
		if (index < 0) {
			return;
		}

		if (a.isItemAction()) {

			final ItemAction ia = (ItemAction) a;

			if (!ia.multiple && numberOfMarkedItems > 1) {

				aa.show(a, "is only applicable to a single item.",
					"Currently multiple items are marked.");

			} else if (index < numNested && numberOfMarkedItems == 0) {

				if (ia.multiple) {

					aa.show(a, "is only applicable to items.",
						"Mark one or more items to perform this action.");

				} else {

					aa.show(a, "is only applicable to an item.",
						"Mark an item to perform this action.");
				}

			} else {

				final int positions[];
				final String ids[];

				if (numberOfMarkedItems == 0) { // use single auto marked item

					final int itemNo = index - numNested;

					positions = new int[] { list.getItemPosAbsolute(itemNo) };
					ids = new String[] { list.getItemID(itemNo) };

				} else { // use all user marked items

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
				disableAutoMarker();
				listener.ilcAction(this, ap);
			}

		} else { // list action

			if (index >= numNested) {

				aa.show(a, "is only applicable to a list.",
					"Focus a list to perform this action.");

			} else {

				final int listNo = index;
				disableAutoMarker();
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

		if (numberOfMarkedItems > 0) {
			disableAutoMarker();
		} else {
			enableAutoMarker();
		}

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
