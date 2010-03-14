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
package remuco.client.jme.io;

import java.io.IOException;
import java.util.Vector;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;

import remuco.client.common.UserException;
import remuco.client.common.util.Log;

public final class BluetoothScanner implements DiscoveryListener, IScanner {

	private final DiscoveryAgent agent;

	/**
	 * Indicates if the user has canceled the device scan. If <code>true</code>,
	 * any scan events should get ignored. This variable gets set
	 * <code>true</code> in {@link #cancelScan()} and will be set back to
	 * <code>false</code> in {@link #startScan(IScanListener)}.
	 */
	private boolean canceled = false;

	private IScanListener listener;

	private final LocalDevice localDevice;

	private final Vector remoteDevices = new Vector();

	private final UserException startScanException;

	public BluetoothScanner() {

		LocalDevice ldev;
		UserException sse;

		try {
			ldev = LocalDevice.getLocalDevice();
			sse = null;
		} catch (BluetoothStateException e) {
			Log.ln("[BS] failed to get local device", e);
			sse = new UserException("Bluetooth Error",
					"Bluetooth seems to be off.");
			ldev = null;
		}

		startScanException = sse;
		localDevice = ldev;
		if (localDevice != null) {
			agent = localDevice.getDiscoveryAgent();
		} else {
			agent = null;
		}

	}

	public void cancelScan() {

		canceled = true;

		agent.cancelInquiry(this);

	}

	public void deviceDiscovered(RemoteDevice dev, DeviceClass dc) {

		Log.ln("found a device");

		remoteDevices.addElement(dev);

	}

	public void inquiryCompleted(int arg0) {

		Log.ln("search finished");

		if (canceled)
			return;

		// iterate over devices and get address and name

		final BluetoothDevice devices[] = new BluetoothDevice[remoteDevices.size()];

		for (int i = 0; i < devices.length; i++) {

			final RemoteDevice dev = (RemoteDevice) remoteDevices.elementAt(i);

			String address = dev.getBluetoothAddress();
			String name;
			try {
				name = dev.getFriendlyName(false);
			} catch (IOException e) {
				name = "";
			}

			devices[i] = new BluetoothDevice();
			devices[i].setAddress(address);
			devices[i].setName(name);
		}

		remoteDevices.removeAllElements();

		listener.notifyScannedDevices(devices);

		listener = null;

	}

	public void servicesDiscovered(int arg0, ServiceRecord[] arg1) {
		Log.bug("Feb 22, 2009.6:25:42 PM");
	}

	public void serviceSearchCompleted(int arg0, int arg1) {
		Log.bug("Feb 22, 2009.6:25:47 PM");
	}

	/**
	 * Starts a scan for nearby devices and returns immediately. When scan is
	 * finished <code>listener</code> gets notified.
	 * 
	 * @param listener
	 *            the listener to notify when scan is finished
	 * @throws UserException
	 *             if scan initiation failed
	 */
	public void startScan(IScanListener listener) throws UserException {

		this.listener = listener;

		canceled = false;

		if (startScanException != null) {
			throw startScanException;
		}

		try {
			agent.startInquiry(DiscoveryAgent.GIAC, this);
		} catch (BluetoothStateException e) {

			Log.ln("[BS]", e);

			throw new UserException("Bluetooth Error",
					"Bluetooth seems to be busy. "
							+ "Cannot scan for nearby devices right now.");

		}

	}

}
