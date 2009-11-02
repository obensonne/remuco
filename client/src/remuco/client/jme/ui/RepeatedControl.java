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
package remuco.client.jme.ui;

import java.util.TimerTask;

import remuco.client.common.player.Player;
import remuco.client.common.util.Log;

/**
 * Timer task to periodically do a player control (intended for controls
 * triggered by holding down a key).
 * <p>
 * {@link RepeatedControl} tasks automatically stop if the player to control is
 * disconnected.
 */
public class RepeatedControl extends TimerTask {

	public static final int SEEK = 1;

	public static final int VOLUME = 0;

	private final int direction;

	private boolean neverRun = true;

	private final Player player;

	private final int type;

	/**
	 * Create a new repeated control task.
	 * 
	 * @param type
	 *            either {@link #SEEK} or {@link #VOLUME}
	 * @param player
	 *            the player to control
	 * @param direction
	 *            either <code>-1</code> or <code>1</code> for seeking
	 *            backward/forward respectively adjusting volume down/up
	 */
	public RepeatedControl(int type, Player player, int direction) {
		this.type = type;
		this.player = player;
		this.direction = direction;
	}

	/**
	 * Same as {@link TimerTask#cancel()}, only differs in return value.
	 * 
	 * @return <code>true</code> if the task has never run, <code>false</code>
	 *         if it has run one or more times
	 */
	public boolean cancel() {

		return super.cancel() & neverRun; // == neverRun for repeated tasks
	}

	public void run() {

		// actually we have to synchronize here to be perfect, but we do not
		// need to be perfect
		neverRun = false;

		if (player.getConnection().isClosed()) { // stop if disconnected
			super.cancel();
			return;
		}

		switch (type) {
		case VOLUME:
			player.ctrlVolume(direction);
			break;
		case SEEK:
			player.ctrlSeek(direction);
			break;
		default:
			Log.bug("Mar 17, 2009.9:11:31 PM");
			break;
		}

	}

}
