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

import remuco.UserException;
import remuco.player.Player;

/**
 * Interface for classes interested in the state of a {@link Connection}.
 * 
 * @see Connection#Connection(String, IConnectionListener, IMessageListener)
 * 
 * @author Oben Sonne
 */
public interface IConnectionListener {

	/**
	 * Notifies a successful connection.
	 * 
	 * @param player
	 *            the connected player
	 */
	public void notifyConnected(Player player);

	/**
	 * Notifies a disconnection.
	 * 
	 * @param url
	 *            the URL of the broken connection if it is worth trying to
	 *            reconnect, or <code>null</code> otherwise
	 * @param reason
	 *            the user exception describing the reason for disconnecting
	 */
	public void notifyDisconnected(String url, UserException reason);

}
