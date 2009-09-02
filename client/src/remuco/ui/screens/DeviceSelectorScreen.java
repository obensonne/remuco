/*   
 *   Remuco - A remote control system for media players.
 *   Copyright (C) 2006-2009 Oben Sonne <obensonne@googlemail.com>
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
package remuco.ui.screens;

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
import remuco.comm.BluetoothFactory;
import remuco.comm.Device;
import remuco.comm.IDeviceSelectionListener;
import remuco.comm.IScanListener;
import remuco.comm.IScanner;
import remuco.comm.InetServiceFinder;
import remuco.ui.CMD;
import remuco.ui.CommandList;
import remuco.ui.Theme;
import remuco.util.Log;

public final class DeviceSelectorScreen extends List implements
		CommandListener, IScanListener {

	private static final Command CMD_ADD = new Command("Add", Command.SCREEN,
			10);

	private static final Command CMD_DT_BLUETOOTH = new Command("Bluetooth",
			Command.SCREEN, 10);

	private static final Command CMD_DT_WIFI = new Command("WiFi",
			Command.SCREEN, 20);

	private static final Command CMD_REMOVE = new Command("Remove",
			Command.SCREEN, 30);

	private final Alert alertScanProblem, alertConfirmRemove;

	private final IScanner bluetoothScanner;

	private final Config config;

	private final Display display;

	private final IDeviceSelectionListener listener;

	private final CommandListener parent;

	private Device scanResults[] = new Device[0];

	private final CommandList screenDeviceTypeSelection;

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

		if (BluetoothFactory.BLUETOOTH) {
			bluetoothScanner = BluetoothFactory.createBluetoothScanner();
		} else {
			bluetoothScanner = null;
		}

		alertScanProblem = new Alert("");
		alertScanProblem.setType(AlertType.ERROR);
		alertScanProblem.setTimeout(Alert.FOREVER);
		alertScanProblem.setCommandListener(this);

		alertConfirmRemove = new Alert("Confirmation");
		alertConfirmRemove.setString("Please confirm ..");
		alertConfirmRemove.setType(AlertType.WARNING);
		alertConfirmRemove.setTimeout(Alert.FOREVER);
		alertConfirmRemove.addCommand(CMD.NO);
		alertConfirmRemove.addCommand(CMD.YES);
		alertConfirmRemove.setCommandListener(this);

		screenDeviceTypeSelection = new CommandList("Add Connection", CMD.OK);
		screenDeviceTypeSelection.addCommand(CMD.BACK);
		if (bluetoothScanner != null) {
			screenDeviceTypeSelection.addCommand(CMD_DT_BLUETOOTH,
				theme.licBluetooth);
		}
		screenDeviceTypeSelection.addCommand(CMD_DT_WIFI, theme.licWifi);
		screenDeviceTypeSelection.setCommandListener(this);

		screenScanning = new WaitingScreen();
		screenScanning.setTitle("Scanning");
		screenScanning.setImage(theme.aicBluetooth);
		screenScanning.setMessage("Searching for Bluetooth devices.");
		screenScanning.setCommandListener(this);

		screenScanResults = new List("Scan Results", List.IMPLICIT);
		screenScanResults.setSelectCommand(CMD.SELECT);
		screenScanResults.addCommand(CMD.BACK);
		screenScanResults.setCommandListener(this);

		addCommand(CMD_ADD);
		setSelectCommand(CMD.SELECT);
		setCommandListener(this);
	}

	public void commandAction(Command c, Displayable d) {

		if (c.equals(CMD.SELECT) && d == this) { // DEVICE SELECTED //

			final int index = getSelectedIndex();
			if (index < 0) {
				return;
			}

			// move selected device to top of device list
			final Device device = (Device) config.devices.elementAt(index);
			config.devices.removeElementAt(index);
			config.devices.insertElementAt(device, 0);

			update();

			listener.notifySelectedDevice(device);

		} else if (c == WaitingScreen.CMD_CANCEL) { // cancel scan

			bluetoothScanner.cancelScan();

			display.setCurrent(this);

		} else if (c == CMD.BACK && d == screenDeviceTypeSelection) {

			display.setCurrent(this);

		} else if (c == CMD_DT_BLUETOOTH) {

			try {
				bluetoothScanner.startScan(this);
				display.setCurrent(screenScanning);
			} catch (UserException e) {
				alertScanProblem.setTitle("Scan Error");
				alertScanProblem.setString(e.getError() + ": " + e.getDetails());
				alertScanProblem.setType(AlertType.ERROR);
				display.setCurrent(alertScanProblem, this);
			}

		} else if (c == CMD_DT_WIFI) {

			final Device device = new Device(Device.WIFI, ":"
					+ InetServiceFinder.PORT, "");
			final DeviceEditorScreen des = new DeviceEditorScreen(device);
			des.addCommand(CMD.BACK);
			des.addCommand(CMD.OK);
			des.setCommandListener(this);
			display.setCurrent(des);

		} else if (c == CMD.OK && d instanceof DeviceEditorScreen) {

			final DeviceEditorScreen des = (DeviceEditorScreen) d;

			final String problem = des.validate();

			if (problem != null) {

				display.setCurrent(new Alert("Oops..", problem, null,
						AlertType.ERROR), des);

			} else {

				final Device device = des.getDevice();

				config.devices.removeElement(device);
				config.devices.insertElementAt(device, 0);

				update();

				display.setCurrent(this);

			}

		} else if (c == CMD.BACK && d instanceof DeviceEditorScreen) {

			display.setCurrent(this);

		} else if (c == CMD.BACK && d == screenScanResults) {

			display.setCurrent(this);

		} else if (c == CMD.SELECT && d == screenScanResults) {

			final int index = screenScanResults.getSelectedIndex();
			if (index == -1) {
				return;
			}

			final Device device = scanResults[index];

			if (config.devices.contains(device)) {
				Log.debug("dev exists");
			}
			config.devices.removeElement(device);
			config.devices.insertElementAt(device, 0);

			update();

			listener.notifySelectedDevice(device);

		} else if (c == CMD_ADD) {

			display.setCurrent(screenDeviceTypeSelection);

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

	public void notifyScannedDevices(Device devs[]) {

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

			screenScanResults.append(scanResults[i].name, theme.licBluetooth);
		}

		display.setCurrent(screenScanResults);
	}

	/**
	 * On first time display, use this method instead of
	 * {@link Display#setCurrent(Displayable)}.
	 */
	public void show() {

		update();

		if (config.devices.isEmpty()) {
			if (BluetoothFactory.BLUETOOTH) {
				display.setCurrent(screenDeviceTypeSelection);
			} else {
				final Alert alert = new Alert("No Bluetooth",
						"Bluetooth is not available. "
								+ "Only Wifi connections are possible.",
						theme.aicWifi, AlertType.INFO);
				final Device device = new Device(Device.WIFI, ":"
						+ InetServiceFinder.PORT, "");
				display.setCurrent(alert, new DeviceEditorScreen(device));
			}
		} else
			display.setCurrent(this);

	}

	/**
	 * Update list to show all known devices. As a side effect, {@link #devices}
	 * gets updated to the devices returned by {@link Config#getKnownDevices()}.
	 */
	private void update() {

		if (config.devices.isEmpty()) {
			removeCommand(CMD_REMOVE);
		} else {
			addCommand(CMD_REMOVE);
		}

		deleteAll();

		final Enumeration enu = config.devices.elements();

		while (enu.hasMoreElements()) {

			Device device = (Device) enu.nextElement();

			final Image icon;

			if (device.type == Device.BLUETOOTH) {
				icon = theme.licBluetooth;
			} else if (device.type == Device.WIFI) {
				icon = theme.licWifi;
			} else {
				Log.bug("Jan 28, 2009.10:57:37 PM");
				icon = null;
			}

			final String label;

			if (device.name.length() == 0) {
				label = device.address;
			} else {
				label = device.name;
			}

			append(label, icon);
		}

		setSelectedIndex(0, true);
	}

}
