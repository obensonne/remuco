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
package remuco.client.midp.io;

import java.util.Hashtable;
import java.util.TimerTask;

import remuco.client.common.MainLoop;
import remuco.client.common.UserException;
import remuco.client.midp.util.MIDPTools;

public final class InetServiceFinder implements IServiceFinder {

	private final Object lock;

	private TimerTask notifier = null;

	public InetServiceFinder() {

		lock = new Object();
	}

	public void cancelServiceSearch() {

		synchronized (lock) {
			if (notifier != null) {
				notifier.cancel();
				notifier = null;
			}
		}
	}

	public void findServices(IDevice iDevice, final IServiceListener listener)
			throws UserException {

		final WifiDevice wd = (WifiDevice) iDevice;

		final Hashtable services = MIDPTools.buildManualServiceList("socket",
			wd.getAddress(), wd.getPort(), wd.getOptions());

		synchronized (lock) {

			if (notifier != null) {
				return;
			}

			notifier = new TimerTask() {
				public void run() {
					listener.notifyServices(services, null);
					synchronized (lock) {
						notifier = null;
					}
				}
			};
		}

		// fake a service search:
		MainLoop.schedule(notifier, 1000);

	}

}
