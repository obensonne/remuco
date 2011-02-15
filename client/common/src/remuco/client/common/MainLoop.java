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
package remuco.client.common;

import java.util.Timer;
import java.util.TimerTask;

import remuco.client.common.io.Connection;
import remuco.client.common.util.Log;

/**
 * Global main loop, supposed to be used for
 * <ul>
 * <li>handling events from a {@link Connection} (decoupled from the
 * {@link Connection}'s receiver thread and from the threads calling methods on
 * a {@link Connection}),
 * <li>scheduling repetitive tasks,
 * <li>scheduling delayed tasks and
 * <li>synchronizing access to objects where critical race conditions may occur.
 * </ul>
 * <p>
 * <em>Note:</em> The methods {@link #enable()} and {@link #disable()} should
 * get called only from the application's entry and exit points to prevent race
 * conditions and to ensure other methods (<em>schedule...</em>) are not called
 * before {@link #enable()} or after {@link #disable()}. Further, methods in
 * this class should be called only from a non-static context (otherwise there
 * is a risk they get called during class initializations before
 * {@link #enable()} has been called the first time).
 */
public class MainLoop {

	/** Timer task to periodically log that the main loop is alive. */
	private static class AliveLogger extends TimerTask {

		private final long startTime = System.currentTimeMillis();

		public void run() {
			final long now = System.currentTimeMillis();
			final long seconds = (now - startTime) / 1000;
			Log.ln("main loop alive (" + seconds + ")");
		}

	}

	private static Timer timer;

	/** Disable the main loop. Does nothing if the loop is already disabled. */
	public static void disable() {

		if (timer != null) {
			timer.cancel();
			timer = null;
		}

	}

	/** Enable the main loop. Does nothing if the loop is already enabled. */
	public static void enable() {

		if (timer == null) {
			timer = new Timer();
			timer.schedule(new AliveLogger(), 60000, 60000);
		}
	}

	/** See {@link Timer#schedule(TimerTask, long)} with 10ms delay. */
	public static void schedule(TimerTask task) {
		if (timer == null) return;

		timer.schedule(task, 10);
	}

	/** See {@link Timer#schedule(TimerTask, long)}. */
	public static void schedule(TimerTask task, long delay) {
		if (timer == null) return;

		timer.schedule(task, delay);

	}

	/** See {@link Timer#schedule(TimerTask, long, long)}. */
	public static void schedule(TimerTask task, long delay, long period) {
		if (timer == null) return;

		timer.schedule(task, delay, period);

	}

}
