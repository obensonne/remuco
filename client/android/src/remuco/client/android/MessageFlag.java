/*   
 *   Remuco - A remote control system for media players.
 *   Copyright (C) 2006-2010 by the Remuco team, see AUTHORS.
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
package remuco.client.android;

public abstract class MessageFlag {

	public static final int ITEM_CHANGED = 10;
	public static final int PROGRESS_CHANGED = 11;
	public static final int STATE_CHANGED = 12;
	
	public static final int CONNECTED = 1;
	public static final int DISCONNECTED = 2;
	
	public static final int TICK = 20;
	
	// control messages
	public static final int CTRL_PLAY_PAUSE 	= 30;
	public static final int CTRL_PREV			= 31;
	public static final int CTRL_NEXT			= 32;
	public static final int CTRL_VOLUME_UP		= 33;
	public static final int CTRL_VOLUME_DOWN	= 34;
	public static final int CTRL_RATE 			= 35;
	
	public static final int PLAYLIST = 40;
	public static final int QUEUE = 41;
	public static final int SEARCH = 42;
	
}
