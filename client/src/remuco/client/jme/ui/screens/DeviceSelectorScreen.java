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
package remuco.client.jme.ui.screens;

import java.util.Enumeration;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;

import remuco.Config;
import remuco.UserException;
import remuco.client.common.util.Log;
import remuco.client.jme.io.BluetoothDevice;
import remuco.client.jme.io.BluetoothFactory;
import remuco.client.jme.io.IScanListener;
import remuco.client.jme.io.IScanner;
import remuco.client.jme.ui.CMD;
import remuco.client.jme.ui.CommandList;
import remuco.client.jme.ui.Theme;
import remuco.comm.IDevice;
import remuco.comm.IDeviceSelectionListener;
import remuco.comm.WifiDevice;

public final class DeviceSelectorScreen extends List implements
		CommandListener, IScanListener {

	private static final Command CMD_ADD = new Command("Add", Command.SCREEN,
			10);

	/** Back command for sub screens to get back to this screen. */
	private static final Command CMD_BACK_TO_ME = new Command("Back",
			Command.BACK, 1);

	private static final Command CMD_DT_BLUETOOTH = new Command("Bluetooth",
			Command.SCREEN, 10);

	private static final Command CMD_DT_WIFI = new Command("WiFi",
			Command.SCREEN, 20);

	private static final Command CMD_EDIT = new Command("Edit", Command.SCREEN,
			20);

	private static final Command CMD_REMOVE = new Command("Remove",
			Command.SCREEN, 30);

	private final Alert alertScanProblem, alertConfirmRemove;

	private final IScanner bluetoothScanner;

	private final Config config;

	private final Display display;

	private final IDeviceSelectionListener listener;

	private final CommandListener parent;

	private IDevice scanResults[] = new IDevice[0];

	private final WaitingScreen screenScanning;

	private final List screenScanResults;

	private final Theme theme;

	public DeviceSelectorScreen(CommandListener parent, Display display,
			IDeviceSelectionListener listener) {

		super("Connections", IMPLICIT);

		this.parent = parent;
		this.display = display;
		this.listener = listener;

		config = Config.getInstance();
		theme = Theme.getInstance();

		// TODO: could be created on the fly and attached to waiting screen
		if (BluetoothFactory.BLUETOOTH) {
			bluetoothScanner = BluetoothFactory.createBluetoothScanner();
		} else {
			bluetoothScanner = null;
		}

		alertScanProblem = new Alert("");
		alertScanProblem.setType(AlertType.ERROR);
		alertScanProblem.setTimeout(Alert.FOREVER);
		alertScanProblem.setCommandListener(this);

		// TODO what about an Alert factory? get rid of forever living alerts
		alertConfirmRemove = new Alert("Confirmation");
		alertConfirmRemove.setString("Please confirm ..");
		alertConfirmRemove.setType(AlertType.WARNING);
		alertConfirmRemove.setTimeout(Alert.FOREVER);
		alertConfirmRemove.addCommand(CMD.NO);
		alertConfirmRemove.addCommand(CMD.YES);
		alertConfirmRemove.setCommandListener(this);

		// TODO: could be created on the fly
		screenScanning = new WaitingScreen();
		screenScanning.setTitle("Scanning");
		screenScanning.setImage(theme.aicBluetooth);
		screenScanning.setMessage("Searching for Bluetooth devices.");
		screenScanning.setCommandListener(this);

		screenScanResults = new List("Scan Results", List.IMPLICIT);
		screenScanResults.setSelectCommand(CMD.SELECT);
		screenScanResults.addCommand(CMD_BACK_TO_ME);
		screenScanResults.setCommandListener(this);

		addCommand(CMD_ADD);
		setSelectCommand(CMD.SELECT);
		setCommandListener(this);

		update();
	}

	public void commandAction(Command c, Displayable d) {

		if (c.equals(CMD.SELECT) && d == this) { // DEVICE SELECTED //

			final int index = getSelectedIndex();
			if (index < 0) {
				return;
			}

			// move selected device to top of device list
			final IDevice iDevice = (IDevice) config.devices.elementAt(index);
			config.devices.removeElementAt(index);
			config.devices.insertElementAt(iDevice, 0);

			update();

			listener.notifySelectedDevice(iDevice);

		} else if (c == WaitingScreen.CMD_CANCEL) { // cancel scan

			bluetoothScanner.cancelScan();

			display.setCurrent(this);

		} else if (c == CMD_BACK_TO_ME) {

			display.setCurrent(this);

		} else if (c == CMD_DT_BLUETOOTH) {

			if (!BluetoothFactory.BLUETOOTH) {
				display.setCurrent(new Alert("No Bluetooth",
						"Sorry, it looks like Bluetooth is not supported "
								+ "on this device. Please use WiFi instead.",
						null, AlertType.INFO), d);
				return;
			}

			showDeviceEditorScreen(new BluetoothDevice());

		} else if (c == CMD_DT_WIFI) {

			showDeviceEditorScreen(new WifiDevice());

		} else if (c == CMD.OK && d instanceof IDeviceScreen) {

			final IDeviceScreen des = (IDeviceScreen) d;

			final String problem = des.validate();

			if (problem != null) {

				display.setCurrent(new Alert("Oops..", problem, null,
						AlertType.ERROR), d);
				return;
			}

			final IDevice iDevice = des.getDevice();

			if (iDevice instanceof BluetoothDevice
					&& ((BluetoothDevice) iDevice).getAddress().length() == 0) {

				try {
					bluetoothScanner.startScan(this);
					display.setCurrent(screenScanning);
				} catch (UserException e) {
					alertScanProblem.setTitle("Scan Error");
					alertScanProblem.setString(e.getError() + ": "
							+ e.getDetails());
					alertScanProblem.setType(AlertType.ERROR);
					display.setCurrent(alertScanProblem, this);
				}

			} else {

				config.devices.removeElement(iDevice);
				config.devices.insertElementAt(iDevice, 0);

				update();

				display.setCurrent(this);
			}

		} else if (c == CMD.SELECT && d == screenScanResults) {

			final int index = screenScanResults.getSelectedIndex();
			if (index == -1) {
				return;
			}

			final IDevice iDevice = scanResults[index];

			if (config.devices.contains(iDevice)) {
				Log.debug("dev exists");
			}
			config.devices.removeElement(iDevice);
			config.devices.insertElementAt(iDevice, 0);

			update();

			display.setCurrent(this);

		} else if (c == CMD_ADD) {

			display.setCurrent(getAddScreen());

		} else if (c == CMD_EDIT) {

			final int index = getSelectedIndex();
			if (index < 0) {
				return;
			}

			final IDevice iDevice = (IDevice) config.devices.elementAt(index);

			showDeviceEditorScreen(iDevice);

		} else if (c == CMD_REMOVE) {

			final int index = getSelectedIndex();
			if (index < 0) {
				return;
			}

			alertConfirmRemove.setString("Remove connection "
					+ getString(index) + " ?");

			display.setCurrent(alertConfirmRemove);

		} else if (c == CMD.NO && d == alertConfirmRemove) {

			display.setCurrent(this);

		} else if (c == CMD.YES && d == alertConfirmRemove) {

			final int index = getSelectedIndex();

			if (index < 0) {
				return;
			}

			config.devices.removeElementAt(index);
			update();

			display.setCurrent(this);

		} else if (c == Alert.DISMISS_COMMAND) {

			display.setCurrent(this);

		} else {

			parent.commandAction(c, d); // includes LOG
		}
	}

	/** Get displayable used to add a new device. */
	public Displayable getAddScreen() {

		final CommandList dts = new CommandList("Add Connection", CMD.OK);
		dts.addCommand(CMD_BACK_TO_ME);
		dts.addCommand(CMD_DT_BLUETOOTH, theme.licBluetooth);
		dts.addCommand(CMD_DT_WIFI, theme.licWifi);
		dts.setCommandListener(this);
		return dts;
	}

	public void notifyScannedDevices(BluetoothDevice devs[]) {

		if (devs.length == 0) {
			alertScanProblem.setTitle("No Devices");
			alertScanProblem.setString("Please check if your computer is in "
					+ "range, its Bluetooth is enabled and visible.");
			display.setCurrent(alertScanProblem, this);

			return;
		}

		scanResults = devs;

		screenScanResults.deleteAll();

		for (int i = 0; i < devs.length; i += 3) {

			screenScanResults.append(scanResults[i].getLabel(),
				theme.licBluetooth);
		}

		display.setCurrent(screenScanResults);
	}

	/** Create and show a device editor screen for the given device. */
	private void showDeviceEditorScreen(IDevice iDevice) {

		final Displayable d;

		if (iDevice instanceof WifiDevice) {
			d = new WifiScreen((WifiDevice) iDevice);
		} else if (iDevice instanceof BluetoothDevice) {
			d = new BluetoothScreen((BluetoothDevice) iDevice);
		} else {
			Log.bug("Oct 1, 2009.10:55:54 PM");
			return;
		}

		d.addCommand(CMD_BACK_TO_ME);
		d.addCommand(CMD.OK);
		d.setCommandListener(this);
		display.setCurrent(d);
	}

	/** Update list to show all known devices. */
	private void update() {

		if (config.devices.isEmpty()) {
			removeCommand(CMD_EDIT);
			removeCommand(CMD_REMOVE);
		} else {
			addCommand(CMD_EDIT);
			addCommand(CMD_REMOVE);
		}

		deleteAll();

		final Enumeration enu = config.devices.elements();

		while (enu.hasMoreElements()) {

			IDevice iDevice = (IDevice) enu.nextElement();

			final Image icon;

			if (iDevice.getType() == IDevice.TYPE_BLUETOOTH) {
				icon = theme.licBluetooth;
			} else if (iDevice.getType() == IDevice.TYPE_WIFI) {
				icon = theme.licWifi;
			} else {
				Log.bug("Jan 28, 2009.10:57:37 PM");
				icon = null;
			}

			append(iDevice.getLabel(), icon);
		}

		if (size() > 0) {
			setSelectedIndex(0, true);
		}
	}

}
