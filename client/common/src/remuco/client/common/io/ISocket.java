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
package remuco.client.common.io;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * A simple socket interface to abstract device specific details about sockets
 * and stream connections.
 */
public interface ISocket {

	/**
	 * Close this socket and all its streams.
	 */
	public void close();

	/**
	 * Get the input stream for this socket.
	 */
	public InputStream getInputStream();

	/**
	 * Get the output stream for this socket.
	 */
	public OutputStream getOutputStream();

}
