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
import java.util.Timer;
import java.util.TimerTask;

import remuco.Remuco;
import remuco.UserException;

public final class InetServiceFinder implements IServiceFinder {

	public static final String PORT = "34271";

	private final Object lock;

	private TimerTask notifier = null;

	private final Timer timer;

	public InetServiceFinder() {

		lock = new Object();

		timer = Remuco.getGlobalTimer();

	}

	public void cancelServiceSearch() {

		synchronized (lock) {
			if (notifier != null) {
				notifier.cancel();
				notifier = null;
			}
		}

	}

	public void findServices(String addr, final IServiceListener listener)
			throws UserException {

		final String url;

		if (addr.indexOf(':') >= 0)
			url = "socket://" + addr;
		else
			url = "socket://" + addr + ":" + PORT;

		final Hashtable services = new Hashtable(1);
		services.put("Player", url);
		
		synchronized (lock) {

			if (notifier != null) {
				return;
			}
			
			notifier = new TimerTask() {
				public void run() {
					listener.notifyServices(services, null);
				}
			};
		}

		// fake a service search:
		timer.schedule(notifier, 1000);

	}

}
