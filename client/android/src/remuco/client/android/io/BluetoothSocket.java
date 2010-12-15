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
package remuco.client.android.io;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Looper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import remuco.client.common.UserException;
import remuco.client.common.io.Connection;
import remuco.client.common.io.ISocket;

/**
 * Wrapper for a regular socket to be used as an {@link ISocket} a
 * {@link Connection} object.
 */
public class BluetoothSocket implements ISocket {

	private final InputStream is;

	private final OutputStream os;

	private final android.bluetooth.BluetoothSocket sock;

	private boolean isClosed = false;

	/** Remuco service UUID */
	private final UUID REMUCO_UUID = UUID.fromString("025fe2ae-0762-4bed-90f2-d8d778f020fe");

	/**
	 * Create a new Bluetooth client socket for the given host.
	 * 
	 * @param host
	 *            device mac bluetooth address
	 * @throws UserException
	 *             if setting up the socket and connection fails
	 */
	public BluetoothSocket(String host) throws UserException {

		try {
            Looper.prepare();
		} catch (RuntimeException e) {}

		try {
            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(host);
			this.sock = device.createRfcommSocketToServiceRecord(REMUCO_UUID);
            this.sock.connect();
            isClosed = false;
		} catch (SecurityException e) {
			throw new UserException("Connection Error",
					"Not allowed to connect.");
		} catch (IOException e) {
			throw new UserException("Connection Error",
					"IO error while setting up the connection");
		}

		try {
			is = sock.getInputStream();
		} catch (IOException e) {
			try {
				sock.close();
			} catch (IOException e1) {
			}
			throw new UserException("Connecting failed",
					"IO Error while opening streams.", e);
		}

		try {
			os = sock.getOutputStream();
		} catch (IOException e) {
			try {
				is.close();
				sock.close();
			} catch (IOException e1) {
			}
			throw new UserException("Connecting failed",
					"IO Error while opening streams.", e);
		}

	}

	@Override
	public void close() {
        if (isClosed) return;
        isClosed = true;
		try {
			sock.close();
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

	@Override
	public InputStream getInputStream() {
		return is;
	}

	@Override
	public OutputStream getOutputStream() {
		return os;
	}

}
