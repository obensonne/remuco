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
package remuco.player;

import java.util.Enumeration;
import java.util.Vector;

import remuco.comm.ISerializable;
import remuco.comm.Message;
import remuco.comm.SerialAtom;
import remuco.util.Log;

public final class ItemList implements ISerializable {

	public static final int TYPE_MLIB = Message.REQ_MLIB;

	public static final int TYPE_PLAYLIST = Message.REQ_PLAYLIST;

	public static final int TYPE_QUEUE = Message.REQ_QUEUE;

	public static final int TYPE_SEARCH = Message.REQ_SEARCH;

	private static final int[] ATOMS_FMT = new int[] { SerialAtom.TYPE_AS,
			SerialAtom.TYPE_AS, SerialAtom.TYPE_AS, SerialAtom.TYPE_AS,
			SerialAtom.TYPE_I, SerialAtom.TYPE_I, SerialAtom.TYPE_I,
			SerialAtom.TYPE_AI, SerialAtom.TYPE_AS, SerialAtom.TYPE_AB,
			SerialAtom.TYPE_AS, SerialAtom.TYPE_AI, SerialAtom.TYPE_AS,
			SerialAtom.TYPE_AS };

	private static final int TYPE_FILES = Message.REQ_FILES;

	private static final String UNKNWON = "#~@X+.YO?/";

	private final Vector actions;

	private final SerialAtom[] atoms;

	private boolean haveItemActions = false;

	private boolean haveItemActionsMultiple = false;

	private boolean haveListActions = false;

	private int page, pageMax, itemOffset;

	private String path[], nested[], itemIDs[], itemNames[];

	private final int type;

	/** Create a new playlist, queue or media library item list. */
	public ItemList(int type) {

		atoms = SerialAtom.build(ATOMS_FMT);

		actions = new Vector();

		this.type = type;

	}

	/** Create a new file browser item list. */
	public ItemList(Vector fileActions) {

		atoms = SerialAtom.build(ATOMS_FMT);

		actions = fileActions;

		this.type = TYPE_FILES;

		haveListActions = false;
		haveItemActions = fileActions.size() > 0;

		final Enumeration e = fileActions.elements();
		while (e.hasMoreElements()) {
			final ItemAction ia = (ItemAction) e.nextElement();
			if (ia.multiple) {
				haveItemActionsMultiple = true;
				break;
			}
		}

	}

	public Vector getActions() {
		return actions;
	}

	public SerialAtom[] getAtoms() {
		return atoms;
	}

	/** Get the ID of item <em>i</em> (starting from 0). */
	public String getItemID(int i) {

		try {
			return itemIDs[i];
		} catch (NullPointerException e) {
			return UNKNWON;
		} catch (ArrayIndexOutOfBoundsException e) {
			return UNKNWON;
		}
	}

	/** Get the name of item <em>i</em> (starting from 0). */
	public String getItemName(int i) {

		try {
			return itemNames[i];
		} catch (NullPointerException e) {
			return UNKNWON;
		} catch (ArrayIndexOutOfBoundsException e) {
			return UNKNWON;
		}
	}

	/**
	 * Get the absolute position of item <em>i</em> (may differ from <em>i</em>
	 * due to paging).
	 */
	public int getItemPosAbsolute(int i) {
		return itemOffset + i;
	}

	// public Vector getItemActionsMultiple() {
	// return itemActionsMultiple;
	// }
	//
	// public Vector getItemActionsSingle() {
	// return itemActionsSingle;
	// }

	/**
	 * Get the name of the item list (which is the last element in the list's
	 * path).
	 * 
	 * @return the name
	 */
	public String getName() {

		final String name;

		if (isPlaylist()) {
			name = "Playlist";
		} else if (isQueue()) {
			name = "Queue";
		} else if (isSearch()) {
			name = "Search Results";
		} else if (isRoot()) {
			if (isMediaLib()) {
				name = "Library";
			} else if (isFiles()) {
				name = "Files";
			} else {
				Log.bug("Jul 1, 2009.5:25:17 PM");
				name = "XXX";
			}
		} else { // media lib or files, not root
			name = path[path.length - 1];
		}

		if (pageMax != 0) {
			return name + " " + (page + 1) + "/" + (pageMax + 1);
		} else {
			return name;
		}

	}

	/** Get the name of the nested item list <em>i</em> (starting from 0). */
	public String getNested(int i) {

		try {
			return nested[i];
		} catch (NullPointerException e) {
			return UNKNWON;
		} catch (ArrayIndexOutOfBoundsException e) {
			return UNKNWON;
		}
	}

	public int getNumItems() {

		return itemIDs != null ? itemIDs.length : 0;
	}

	public int getNumNested() {

		return nested != null ? nested.length : 0;
	}

	public int getPage() {
		return page;
	}

	public int getPageMax() {
		return pageMax;
	}

	/**
	 * Get the item list's path. When the item list is a search result, the path
	 * lists the search query parameters.
	 * 
	 * @return the path elements (never <code>null</code>)
	 */
	public String[] getPath() {
		return path == null ? new String[0] : path;
	}

	public String[] getPathForNested(int i) {

		final String name;

		if (i >= nested.length) {
			name = UNKNWON;
		} else {
			name = nested[i];
		}

		final String pathNested[] = new String[path.length + 1];

		for (int j = 0; j < path.length; j++) {
			pathNested[j] = path[j];
		}
		pathNested[path.length] = name;

		return pathNested;
	}

	/**
	 * Get the path of the parent list.
	 * 
	 * @return the parent path or <code>null</code> if this list is a root list
	 */
	public String[] getPathForParent() {

		if (isRoot()) {
			return null;
		}

		final String parent[] = new String[path.length - 1];

		for (int i = 0; i < parent.length; i++) {
			parent[i] = path[i];
		}

		return parent;

	}

	public boolean hasItemActions() {
		return haveItemActions && itemIDs.length > 0;
	}

	public boolean hasItemActionsMultiple() {
		return haveItemActionsMultiple && itemIDs.length > 0;
	}

	public boolean hasListActions() {
		return haveListActions && nested.length > 0;
	}

	public boolean isFiles() {
		return type == TYPE_FILES;
	}

	public boolean isMediaLib() {
		return type == TYPE_MLIB;
	}

	public boolean isPlaylist() {
		return type == TYPE_PLAYLIST;
	}

	public boolean isQueue() {
		return type == TYPE_QUEUE;
	}

	/**
	 * Check if the list is a root list, i.e. it has no parent list. This is
	 * true for playlist, queue and the top level list of the media library.
	 */
	public boolean isRoot() {
		return path == null || path.length == 0;
	}

	public boolean isSearch() {
		return type == TYPE_SEARCH;
	}

	public void notifyAtomsUpdated() {

		path = atoms[0].as;
		nested = atoms[1].as;
		itemIDs = atoms[2].as;
		itemNames = atoms[3].as;

		itemOffset = atoms[4].i;
		page = atoms[5].i;
		pageMax = atoms[6].i;

		if (type == TYPE_FILES) { // no dynamic actions
			return;
		}

		int off;

		actions.removeAllElements();

		off = 11;
		haveListActions = atoms[off].ai.length > 0;
		for (int i = 0; i < atoms[off].ai.length; i++) {
			actions.addElement(new ListAction(atoms[off].ai[i],
					atoms[off + 1].as[i], atoms[off + 2].as[i]));
		}

		off = 7;
		haveItemActions = atoms[off].ai.length > 0;
		for (int i = 0; i < atoms[off].ai.length; i++) {
			final ItemAction ia = new ItemAction(atoms[off].ai[i],
					atoms[off + 1].as[i], atoms[off + 2].ab[i],
					atoms[off + 3].as[i]);
			actions.addElement(ia);
			if (ia.multiple) {
				haveItemActionsMultiple = true;
			}
		}

	}

	public String toString() {

		final StringBuffer sb = new StringBuffer("ItemList: /");
		for (int i = 0; i < path.length; i++) {
			sb.append(path[i]).append('/');
		}
		return sb.toString();
	}

}
