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

import remuco.util.Log;

/**
 * On devices which have no JSR-82 support, references to bluetooth classes
 * could make the application fail. For this purpose this factory provides the
 * creation of bluetooth classes without references in the byte code.
 * 
 * @author Oben Sonne
 * 
 */
public class BluetoothFactory {

	/** Indicates if the current device has Bluetooth. */
	public static final boolean BLUETOOTH;

	static {

		// check for bluetooth

		boolean b = true;

		try {
			Class.forName("javax.bluetooth.LocalDevice");
			Log.ln("[BF] bluetooth: yes");
		} catch (Exception e) {
			Log.ln("[BF] bluetooth: no");
			b = false;
		}

		BLUETOOTH = b;

	}

	public static IScanner createBluetoothScanner() {

		final Class c;

		try {
			c = Class.forName("remuco.comm.BluetoothScanner");
		} catch (ClassNotFoundException e) {
			Log.bug("Feb 2, 2009.11:26:27 PM", e);
			return null;
		}

		try {
			return (IScanner) c.newInstance();
		} catch (InstantiationException e) {
			Log.bug("Feb 2, 2009.11:26:51 PM", e);
			return null;
		} catch (IllegalAccessException e) {
			Log.bug("Feb 2, 2009.11:26:59 PM", e);
			return null;
		}

	}

	public static IServiceFinder createBluetoothServiceFinder() {

		final Class c;
		try {
			c = Class.forName("remuco.comm.BluetoothServiceFinder");
		} catch (ClassNotFoundException e) {
			Log.bug("Feb 3, 2009.12:47:14 AM", e);
			return null;
		}

		try {
			return (IServiceFinder) c.newInstance();
		} catch (InstantiationException e) {
			Log.bug("Feb 3, 2009.12:47:19 AM", e);
			return null;
		} catch (IllegalAccessException e) {
			Log.bug("Feb 3, 2009.12:47:27 AM", e);
			return null;
		}
	}

}
