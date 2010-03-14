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
package remuco.client.jme.ui.screens;

import remuco.client.jme.io.IDevice;

/** IDevice screens are forms to configure a connection to a remote device. */
public interface IDeviceScreen {

	/**
	 * Apply user entered values to device and return that device. If a device
	 * has been passed to the constructor of this screen, then the same device
	 * will be returned here. Otherwise a new device will be returned.
	 */
	public IDevice getDevice();

	/**
	 * Validate the user input.
	 * 
	 * @return <code>null</code> if user input is ok, otherwise a string message
	 *         describing what's wrong
	 */
	public String validate();

}
