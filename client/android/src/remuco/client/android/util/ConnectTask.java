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
package remuco.client.android.util;

import java.util.TimerTask;

import remuco.client.android.io.WifiSocket;
import remuco.client.android.io.BluetoothSocket;
import remuco.client.common.UserException;
import remuco.client.common.data.ClientInfo;
import remuco.client.common.io.Connection;
import remuco.client.common.io.Connection.IConnectionListener;
import remuco.client.common.io.ISocket;
import remuco.client.common.util.Log;

public class ConnectTask extends TimerTask {

    public final static int WIFI = 0;
    public final static int BLUETOOTH = 1;

    private int type;
	private String hostname;
	private int port;
	private ClientInfo clientInfo;
	private IConnectionListener connectionListener;
	
	
	public ConnectTask(int type, String hostname, ClientInfo clientInfo, IConnectionListener connectionListener) {
        this(type, hostname, 0, clientInfo, connectionListener);
    }
	
	public ConnectTask(int type, String hostname, int port, ClientInfo clientInfo, IConnectionListener connectionListener) {
        this.type = type;
		this.hostname = hostname;
		this.port = port;
		this.clientInfo = clientInfo;
		this.connectionListener = connectionListener;
	}



	@Override
	public void run() {
		Log.ln("[CT] trying to connect " + type + " to " + hostname + " " + port);
        ISocket s = null;

        if (type == WIFI) {
            /*
             * Create a socket (note that WifiSocket is a wrapper around java.net.Socket
             * which implements ISocket - this is because we need a uniform socket
             * interface for JavaME and Android clients). The socket parameters are
             * for connecting to localhost from an emulated Android device. The
             * socket creation should be done in an extra thread (e.g. using the
             * MainLoop) to not block the UI.
             */
		
            try {
                s = new WifiSocket(hostname, port);
            } catch (UserException e) {
                Log.ln("[CT] Wifi socket creation failed: ", e);
			
                // tell the view that we have no connection
                connectionListener.notifyDisconnected(s, e);
			
                return;
            }
        }

        if (type == BLUETOOTH) {
            /*
             * Create a socket (note that BluetoothSocket is a wrapper around android.bluetooth.BluetoothSocket
             * which implements ISocket - this is because we need a uniform socket
             * interface for JavaME and Android clients). The socket parameters are
             * for connecting to localhost from an emulated Android device. The
             * socket creation should be done in an extra thread (e.g. using the
             * MainLoop) to not block the UI.
             */
		
            try {
                s = new BluetoothSocket(hostname);
            } catch (UserException e) {
                Log.ln("[CT] Bluetooth socket creation failed: ", e);
			
                // tell the view that we have no connection
                connectionListener.notifyDisconnected(s, e);
			
                return;
            }
        }

        if (s == null) return;
		
		/* 
		 * Given the socket and the client info, we can set up a connection. A
		 * connection cares about exchanging initial messages between client and
		 * server. If a connections has been established it provides a Player
		 * class which can be used to interact with the remote player. A
		 * connection automatically creates it's own thread, so this call
		 * returns immediately.
		 */
		new Connection(s, connectionListener, 15, clientInfo);
	}
	

}
