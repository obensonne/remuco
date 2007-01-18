/*
 * Copyright (C) 2006 Christian Buennig - See COPYING
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 * 
 */
package remuco.server;

import java.io.IOException;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DataElement;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;

import remuco.connection.GenericStreamConnection;
import remuco.util.Log;


/**
 * Server.
 * <p>
 * The server accepts connections from bluetooth clients to a specific service
 * record and then creates a {@link remuco.connection.GenericStreamConnection}. Rest
 * is like described in {@link remuco.server.IServer}.
 * 
 * @author Christian Buennig
 * 
 */
public class Server implements Runnable {

    // Remuco specific service UUID (32 char: 0-9,A-F - no '0x' prefix)
    private final static UUID uuid = new UUID(
            "102030405060708090A0B0C0D0E0F010", false);

    private VirutalPlayer player;

    private boolean shutDown = false;

    private StreamConnectionNotifier server;

    public void init() throws Exception {

        // init bluetooth stack
        LocalDevice device;
        try {
            device = LocalDevice.getLocalDevice();
            device.setDiscoverable(DiscoveryAgent.GIAC); // set Discover mode
        } catch (BluetoothStateException e1) {
            throw new Exception("Initializing bluetooth stack failed");
        }
        
        // setup service
        String serviceName = "RemucoServer";

        String url = "btspp://localhost:" + uuid.toString() + ";name="
                + serviceName;
        Log.ln(this, "server url: " + url);

        ServiceRecord serviceRecord;
        try {
            // create a server connection object
            server = (StreamConnectionNotifier) Connector.open(url);
            // get service record
            serviceRecord = device.getRecord(server);
        } catch (IOException e) {
            throw new Exception("Setting up service failed.");
        }
        // set ServiceRecrod ServiceAvailability (0x0008) attribute to
        // indicate our service is available
        // 0xFF indicate fully available status
        // This operation is optional
        serviceRecord.setAttributeValue(0x0008, new DataElement(
                DataElement.U_INT_1, 0xFF));

        // Print the service record, which already contains
        // some default values
        Log.ln(this, "ServiceRecord: " + serviceRecord.toString());
        int ids[] = serviceRecord.getAttributeIDs();
        for (int i = 0; i < ids.length; i++) {
            Log.ln(ids[i] + "=" + serviceRecord.getAttributeValue(ids[i]).toString());
        }

        // Set the Major Service Classes flag in Bluetooth stack.
        // We choose Telephony Service
        // TODO: auf Netzwerk setzen oder was anders (HI?)
        // rec.setDeviceServiceClasses(SERVICE_TELEPHONY);
        
    }

    public void run() {
        int ioErrors = 0;
        StreamConnection scon = null;
        GenericStreamConnection con;
        RemoteDevice rdev;
        while (!shutDown && ioErrors < 10) {

            Log.ln(this, "waiting for client connection..");

            // start accepting client connection (blocking)
            try {
                scon = server.acceptAndOpen();
                ioErrors = 0;
            } catch (IOException e) {
                ioErrors++;
                Log.ln(this, "IO error (while waiting for client connection): "
                        + e.getMessage());
                continue;
            }

            try {
                // retrieve + log the remote device object
                rdev = RemoteDevice.getRemoteDevice(scon);
                Log.ln(this, "client " + rdev.getFriendlyName(false) + "("
                        + rdev.getBluetoothAddress() + ") has connected");
                con = new GenericStreamConnection(scon.openDataInputStream(),
                        scon.openDataOutputStream());
                Log.ln(this, "Start player skeleton");
                new PlayerSkeleton(con, player);
                Log.ln(this, "Started player skeleton");

            } catch (IOException e) {
                Log.ln(this, "setting up client connection failed: "
                        + e.getMessage());
                continue;
            }
        }
        try {
            server.close();
        } catch (IOException e) {
        }

    }

    public void setPlayer(VirutalPlayer player) {
        this.player = player;
    }

}
