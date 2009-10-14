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
package remuco.comm;

import java.util.Hashtable;

import remuco.UserException;

/**
 * Interface for service listener.
 * 
 * @see IServiceFinder
 */
public interface IServiceListener {

	/**
	 * Notify a service listener that services has been found.
	 * 
	 * @param services
	 *            a hash table with service names as keys and service connection
	 *            URLs as values
	 * @param ex
	 *            <code>null</code> if service search was successful, otherwise
	 *            a description of the occurred error (in that case,
	 *            <i>services</i> is <code>null</code>)
	 */
	public void notifyServices(Hashtable services, UserException ex);

}
