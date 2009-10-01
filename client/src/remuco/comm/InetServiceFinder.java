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
package remuco.comm;

import java.util.Hashtable;
import java.util.TimerTask;

import remuco.MainLoop;
import remuco.UserException;

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

	public void findServices(Device device, final IServiceListener listener)
			throws UserException {

		final WifiDevice wd = (WifiDevice) device;

		final StringBuffer url = new StringBuffer("socket://");

		url.append(wd.getAddress());
		url.append(':');
		url.append(wd.getPort());

		final String options = wd.getOptions();
		if (options.length() > 0) {
			if (options.charAt(0) != ';') {
				url.append(';');
			}
			url.append(options);
		}

		final Hashtable services = new Hashtable(1);
		services.put("Player", url);

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
