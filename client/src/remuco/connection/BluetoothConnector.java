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
package remuco.connection;

import java.io.IOException;
import java.util.Vector;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.List;

import remuco.util.Log;

/**
 * This connector creates a connection with another bluetooth device using the
 * serial port profile. The creation is done by interacting with the user
 * (exploring and selecting remote deivces and services).<br>
 * 
 * @see remuco.connection.GenericStreamConnection
 * @author Christian Buennig
 * 
 */
public class BluetoothConnector implements Runnable, CommandListener,
        DiscoveryListener, IConnector {

    /** User issues a remote device scan */
    private static final Command CMD_SCAN = new Command("Scan", Command.SCREEN,
            20);

    /** Services to search.. looking for services in 'Serial Port Profile' */
    private static final UUID[] UUID_LIST = new UUID[] { new UUID(0x1101) };

    /** User wants to cancel connection creation */
    protected static final Command CMD_EXIT = new Command("Exit", Command.EXIT,
            30);

    private DiscoveryAgent agent;

    private GenericStreamConnection connection;

    private Display d;

    private Form f;

    private LocalDevice localDevice;

    private Vector remoteDevices;

    private List screenDevices;

    private Vector serviceRecords;

    private Thread thisThread;

    RemoteDevice remoteDevice;

    public synchronized void commandAction(Command c, Displayable d) {
        if (c == List.SELECT_COMMAND) {
            remoteDevice = (RemoteDevice) remoteDevices.elementAt(screenDevices
                    .getSelectedIndex());
            // wake up our thread to proceed with service search on that device
            this.notify();
        } else if (c == CMD_EXIT) {
            connection.close();
            synchronized (connection) {
                connection.notify();
            }
        } else if (c == CMD_SCAN) {
            if (thisThread != null && thisThread.isAlive()) {
                f.append("..");
            } else {
                thisThread = new Thread(this);
                thisThread.start();
            }
        }
    }

    public void createConnection() {
        /*
         * We now do nothing else than setting up UI - the user shall issue the
         * creating with the UI and then the extra creation thread gets started
         */
        d.setCurrent(f);
    }

    public void deviceDiscovered(RemoteDevice dev, DeviceClass devClass) {
        remoteDevices.addElement(dev);
    }

    public GenericStreamConnection getConnection() {
        return connection;
    }

    public boolean init(Display d) {

        this.d = d;
        connection = new GenericStreamConnection();

        try {
            localDevice = LocalDevice.getLocalDevice();
        } catch (BluetoothStateException e) {
            Log.ln(this, "Could not get local device");
            return false;
        }
        agent = localDevice.getDiscoveryAgent();
        Log.ln(this, "initialized");

        f = new Form("BT Connector");
        f.addCommand(CMD_EXIT);
        f.addCommand(CMD_SCAN);
        f.setCommandListener(this);

        remoteDevices = new Vector();
        screenDevices = new List("Choose a device", Choice.IMPLICIT);
        screenDevices.setCommandListener(this);

        serviceRecords = new Vector();

        return true;

    }

    public synchronized void inquiryCompleted(int arg0) {
        // wake up our thread to proceed with device selection
        this.notify();
    }

    public synchronized void run() {
        synchronized (connection) {
            try {
                f.deleteAll();
                d.setCurrent(f);

                // find remote devices

                remoteDevices.removeAllElements();
                output("=> Search devices.. ");
                try {
                    agent.startInquiry(DiscoveryAgent.GIAC, this);
                    this.wait();
                } catch (BluetoothStateException e) {
                    output("failed\n");
                    output("!! Error starting inquiry\n");
                    connection.notify(); // signalize that we are done
                    return;
                }
                output("ok\n");

                int n = remoteDevices.size();
                if (n == 0) {
                    output("!! Error: No devices found!\n");
                    connection.notify(); // signalize that we are done
                    return;
                }

                // select a device

                screenDevices.deleteAll();
                for (int i = 0; i < n; i++) {
                    screenDevices.append(
                            getDeviceName((RemoteDevice) remoteDevices
                                    .elementAt(i)), null);
                }

                d.setCurrent(screenDevices);
                this.wait();
                d.setCurrent(f);
                output("=> Device: ");
                output(getDeviceName(remoteDevice) + "\n");

                // search services

                output("=> Search service.. ");
                serviceRecords.removeAllElements();
                try {
                    agent.searchServices(null, UUID_LIST, remoteDevice, this);
                } catch (BluetoothStateException e) {
                    output("!! Error starting service search\n");
                    connection.notify(); // signalize that we are done
                    return;
                }
                this.wait();

                output("ok\n");

                n = serviceRecords.size();
                if (n == 0) {
                    output("!! Error: No services found!\n");
                    connection.notify(); // signalize that we are done
                    return;
                } else if (n > 1) {
                    output("Found more than 1 suitable service - use first");
                }

                ServiceRecord sr = (ServiceRecord) serviceRecords.elementAt(0);
                String url = sr.getConnectionURL(
                        ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
                output("SR-URL: " + url);

                // create connection

                output("=> Create connection.. ");
                try {
                    StreamConnection scon = (StreamConnection) Connector
                            .open(url);
                    connection.setStreams(scon.openDataInputStream(), scon
                            .openDataOutputStream());
                    output("ok\n");
                } catch (IOException e) {
                    output("failed (" + e.getMessage() + ")\n");
                    connection.notify(); // signalize that we are done
                    return;
                }

                connection.notify(); // signalize that we are done (successful)

            } catch (InterruptedException e) {
                Log.ln(this, "I have been interrupted");
            }
        }
    }

    public void servicesDiscovered(int tid, ServiceRecord[] srs) {
        int n = srs.length;
        for (int i = 0; i < n; i++) {
            serviceRecords.addElement(srs[i]);

        }
    }

    public synchronized void serviceSearchCompleted(int arg0, int arg1) {
        // wake up our thread to proceed with using the found services
        this.notify();
    }

    private String getDeviceName(RemoteDevice rd) {
        try {
            return rd.getFriendlyName(false);
        } catch (IOException e) {
            return rd.getBluetoothAddress();
        }
    }

    private void output(String msg) {
        f.append(msg);
        Log.l(msg);

    }

}
