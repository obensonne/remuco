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
package remuco.client.jme.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.ConnectionNotFoundException;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;

import remuco.UserException;
import remuco.client.common.io.ISocket;
import remuco.client.common.util.Log;

/** JavaME implementation of the Remuco-internal socket interface. */
public class Socket implements ISocket {

	/** URL used to create this socket connection. */
	public final String url;

	private final InputStream is;

	private final OutputStream os;

	private final StreamConnection sc;

	/**
	 * Create a new client socket using the given URL.
	 * 
	 * @throws UserException
	 *             if socket setup fails
	 */
	public Socket(String url) throws UserException {

		this.url = url;

		try {
			sc = (StreamConnection) Connector.open(url);
		} catch (ConnectionNotFoundException e) {
			Log.ln("[CN] open url failed", e);
			throw new UserException("Connecting failed", "Target not found.", e);
		} catch (SecurityException e) {
			Log.ln("[CN] open url failed", e);
			throw new UserException("Connecting failed",
					"Not allowed to connect.", e);
		} catch (IOException e) {
			Log.ln("[CN] open url failed", e);
			throw new UserException("Connecting failed",
					"IO Error while setting up the connection.", e);
		}

		try {
			is = sc.openInputStream();
		} catch (IOException e) {
			try {
				sc.close();
			} catch (IOException e1) {
			}
			Log.ln("[CN] open streams failed", e);
			throw new UserException("Connecting failed",
					"IO Error while opening streams.", e);
		}

		try {
			os = sc.openOutputStream();
		} catch (IOException e) {
			try {
				sc.close();
				is.close();
			} catch (IOException e1) {
			}
			Log.ln("[CN] open streams failed", e);
			throw new UserException("Connecting failed",
					"IO Error while opening streams.", e);
		}
	}

	public void close() {

		try {
			sc.close();
		} catch (IOException e) {
		}
		try {
			os.close();
		} catch (IOException e) {
		}
		try {
			is.close();
		} catch (IOException e) {
		}
	}

	public InputStream getInputStream() {
		return is;
	}

	public OutputStream getOutputStream() {
		return os;
	}

	public String toString() {
		return url;
	}

}
