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
package remuco.comm;

import remuco.util.Log;

/**
 * A message received from or to send to the Remuco server.
 * 
 * @see Connection
 * @see Serial
 */
public final class Message {

	public static final int IGNORE = 0;

	private static final int CONN = 100;
	// use for player list if needed: CONN
	public static final int CONN_PINFO = CONN + 10;
	public static final int CONN_CINFO = CONN + 20;
	public static final int CONN_BYE = CONN + 90;

	private static final int SYNC = 200;
	public static final int SYNC_STATE = SYNC;
	public static final int SYNC_PROGRESS = SYNC + 1;
	public static final int SYNC_ITEM = SYNC + 2;

	private static final int CTRL = 300;

	public static final int CTRL_PLAYPAUSE = CTRL;
	public static final int CTRL_NEXT = CTRL + 1;
	public static final int CTRL_PREV = CTRL + 2;
	public static final int CTRL_SEEK = CTRL + 3;
	public static final int CTRL_VOLUME = CTRL + 4;
	public static final int CTRL_REPEAT = CTRL + 5;
	public static final int CTRL_SHUFFLE = CTRL + 6;
	public static final int CTRL_FULLSCREEN = CTRL + 7;
	public static final int CTRL_RATE = CTRL + 8;
	public static final int CTRL_TAG = CTRL + 30;
	public static final int CTRL_SHUTDOWN = CTRL + 90;

	private static final int ACT = 400;

	public static final int ACT_PLAYLIST = ACT;
	public static final int ACT_QUEUE = ACT + 1;
	public static final int ACT_MEDIALIB = ACT + 2;
	public static final int ACT_FILES = ACT + 3;
	public static final int ACT_SEARCH = ACT + 4;

	private static final int REQ = 500;

	public static final int REQ_ITEM = REQ;
	public static final int REQ_PLAYLIST = REQ + 1;
	public static final int REQ_QUEUE = REQ + 2;
	public static final int REQ_MLIB = REQ + 3;
	public static final int REQ_FILES = REQ + 4;
	public static final int REQ_SEARCH = REQ + 5;

	public static boolean isRequest(int id) {
		return id >= REQ && id < REQ + 100;
	}
	
	/**
	 * ID / type of the message. See constants <code>ID_...</code>
	 */
	public int id = IGNORE;

	/**
	 * Message data in binary / serialized data format. This is the data as it
	 * will get transmitted to the server.
	 * 
	 * @see Serial
	 */
	public byte[] data = null;

	/** Creates a new message with id {@link #IGNORE} and no data. */
	public Message() {
	}

	public String toString() {
		int len = data == null ? 0 : data.length;
		return "(id: " + id + ", data: " + len + ")";
	}

	/**
	 * Dump the message's binary data into the log.
	 * 
	 * @see Log
	 * 
	 */
	protected void dumpBin() {

		Log.ln(toString());
		Log.ln(data);
	}

}
