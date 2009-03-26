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

import remuco.Config;

public interface IDeviceSelectionListener {

	/**
	 * Notify a device selection.
	 * 
	 * @param type
	 *            the type of the selected device
	 * @param addr
	 *            the address of the selected device
	 * 
	 * @see Config#DEVICE_TYPE_BLUETOOTH
	 * @see Config#DEVICE_TYPE_INET
	 */
	public void notifySelectedDevice(String type, String addr);

}
