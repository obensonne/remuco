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
package remuco.client.jme;

import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

import remuco.client.common.MainLoop;
import remuco.client.common.util.Log;

public class Entry extends MIDlet {

	private Remuco remuco;

	protected void destroyApp(boolean unconditional)
			throws MIDletStateChangeException {

		remuco.destroy();
		remuco = null;
		MainLoop.disable();

		Log.ln("[EN] managed exit");
	}

	/**
	 * Same as {@link #notifyDestroyed()} but does additional clean up.
	 */
	protected void notifyExit() {

		remuco = null;
		MainLoop.disable();

		Log.ln("[EN] user exit");
		
		notifyDestroyed();
		
	}

	protected void pauseApp() {

		if (remuco != null) {
			remuco.sleep();
		}
		Log.ln("[EN] paused");

	}

	protected void startApp() throws MIDletStateChangeException {

		MainLoop.enable();

		if (remuco == null) {
			remuco = new Remuco(this);
			Log.ln("[EN] started");
		} else {
			remuco.wakeup();
			Log.ln("[EN] resumed");
		}

	}

}
