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
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;

import remuco.ui.WaitingScreen;
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

	/** User wants to cancel connection creation */
	protected static final Command CMD_EXIT = new Command("Exit", Command.EXIT,
			30);

	/** Services to search.. looking for services in 'Serial Port Profile' */
	private static final UUID[] UUID_LIST = new UUID[] { new UUID(0x1101) };

	RemoteDevice remoteDevice;

	private DiscoveryAgent agent;

	private GenericStreamConnection connection;

	private Display d;

	private String userMsg = "";

	private boolean interruptFlag;

	private LocalDevice localDevice;

	private Vector remoteDevices;

	private int returnCode;

	private List screenDevices;

	private Vector serviceRecords;

	private Thread thisThread;

	private WaitingScreen ws;

	public synchronized void commandAction(Command c, Displayable d) {
		if (c == List.SELECT_COMMAND) {
			remoteDevice = (RemoteDevice) remoteDevices.elementAt(screenDevices
					.getSelectedIndex());
			// wake up our thread to proceed with service search on that device
			this.notify();
		} else if (c == CMD_EXIT) {
			Log.ln("Connceting canceled by user");
			interruptFlag = true;
			this.notify();
		} else if (c == Alert.DISMISS_COMMAND) {
			connection.close();
			synchronized (connection) {
				connection.notify();
			}
		}
	}

	public void createConnection() {
		interruptFlag = false;
		thisThread = new Thread(this);
		thisThread.start();
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
			Log.ln(this, "Could not get local device - is Bluetooth off?");
			return false;
		}
		agent = localDevice.getDiscoveryAgent();
		Log.ln(this, "initialized");

		ws = new WaitingScreen();
		ws.addCommand(CMD_EXIT);
		ws.setCommandListener(this);

		d.setCurrent(ws);

		remoteDevices = new Vector();
		screenDevices = new List("Choose a device", Choice.IMPLICIT);
		screenDevices.setCommandListener(this);
		screenDevices.addCommand(CMD_EXIT);

		serviceRecords = new Vector();

		return true;

	}

	public synchronized void inquiryCompleted(int arg0) {
		// wake up our thread to proceed with device selection
		this.notify();
	}

	public synchronized void run() {

		long inquiryDuration;

		synchronized (connection) {
			try {
				ws.setMessage("Search devices");

				// find remote devices

				remoteDevices.removeAllElements();

				Log.l("Scan for devices.. ");
				try {
					agent.startInquiry(DiscoveryAgent.GIAC, this);
				} catch (BluetoothStateException e) {
					returnCode = RETURN_ERROR;
					userMsg = "Search failed\n"
							+ "If other Bluetooth applications are currently"
							+ "running, they may block the device search.";
					connection.notify();
					return;
				}

				inquiryDuration = System.currentTimeMillis();
				this.wait(); // wait for device search finish
				inquiryDuration = System.currentTimeMillis() - inquiryDuration;

				Log.ln("ok");

				if (interruptFlag) {
					returnCode = RETURN_USER_CANCEL;
					connection.notify();
					return;
				}

				int n = remoteDevices.size();
				if (n == 0) {
					returnCode = RETURN_ERROR;
					if (inquiryDuration < 1000) {
						userMsg = "Found no devices!\n"
								+ "Bluetooth seems to be off (on this device).";
					} else {
						userMsg = "Found no devices!\n"
								+ "Bluetooth seems to be off or in 'hidden' "
								+ "mode (on the server side).";
					}
					connection.notify();
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

				this.wait(); // wait for user device selection

				if (interruptFlag) {
					returnCode = RETURN_USER_CANCEL;
					connection.notify();
					return;
				}

				Log.ln("Device: " + getDeviceName(remoteDevice));

				// search services

				Log.l("Search service.. ");
				ws.setMessage("Search services");
				d.setCurrent(ws);
				serviceRecords.removeAllElements();
				try {
					agent.searchServices(null, UUID_LIST, remoteDevice, this);
				} catch (BluetoothStateException e) {
					returnCode = RETURN_ERROR;
					userMsg = "Service search failed!";
					connection.notify();
					return;
				}

				this.wait(); // wait for service search finish

				if (interruptFlag) {
					returnCode = RETURN_USER_CANCEL;
					connection.notify();
					return;
				}

				Log.ln("ok");

				n = serviceRecords.size();
				if (n == 0) {
					returnCode = RETURN_ERROR;
					userMsg = "Found no services!\n"
							+ "Make sure Remuco server is running on your computer.";
					connection.notify();
					return;
				} else if (n > 1) {
					Log.ln("Found more than 1 suitable service - use first");
				}

				ServiceRecord sr = (ServiceRecord) serviceRecords.elementAt(0);
				String url = sr.getConnectionURL(
						ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
				Log.ln("SR-URL: " + url);

				// create connection

				ws.setMessage("Create connection");
				Log.l("Create connection.. ");
				try {
					StreamConnection scon = (StreamConnection) Connector
							.open(url);
					connection.setStreams(scon.openDataInputStream(), scon
							.openDataOutputStream());
					Log.ln("ok\n");
				} catch (IOException e) {
					Log.ln("failed (" + e.getMessage() + ")\n");
					returnCode = RETURN_ERROR;
					userMsg = "Connecting to server failed!";
					connection.notify();
					return;
				}

				if (interruptFlag) {
					returnCode = RETURN_USER_CANCEL;
					connection.close();
					connection.notify();
					return;
				}

				returnCode = RETURN_OK;
				connection.notify(); // signalize that we are done
				// (successful)

			} catch (InterruptedException e) {
				Log.ln(this, "I have been interrupted");
				returnCode = RETURN_ERROR;
				userMsg = "Connecting to server failed!";
				connection.notify();
				return;
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

	public String getUserMsg() {
		return userMsg;
	}

	public int getReturnCode() {
		return returnCode;
	}

}
