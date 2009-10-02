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

import remuco.UserException;

/**
 * A finder searching for Remuco services.
 * 
 * @author Oben Sonne
 * 
 */
public interface IServiceFinder {

	/**
	 * Cancel a currently active service search.
	 */
	public void cancelServiceSearch();

	/**
	 * Find all available Remuco services on a device.
	 * 
	 * @param iDevice
	 *            device to search for services
	 * @param listener
	 *            the {@link IServiceListener} to notify if service search is
	 *            finished
	 * @throws UserException
	 *             if there was an error in starting the search
	 */
	public void findServices(IDevice iDevice, IServiceListener listener)
			throws UserException;

}