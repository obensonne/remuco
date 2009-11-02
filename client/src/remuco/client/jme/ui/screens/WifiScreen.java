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

import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.TextField;

import remuco.client.jme.io.WifiDevice;
import remuco.comm.IDevice;

/** Screen to configure a Bluetooth connection. */
public class WifiScreen extends Form implements IDeviceScreen {

	private final WifiDevice device;

	private final TextField tfAddr;

	private final TextField tfName;

	private final TextField tfOptions;

	private final TextField tfPort;

	public WifiScreen(WifiDevice device) {

		super("WiFi");

		this.device = device;

		String label;

		label = "Host or IP address";
		tfAddr = new TextField(label, device.getAddress(), 256, TextField.URL);
		append(tfAddr);

		label = "Port (if unsure, do not change)";
		tfPort = new TextField(label, device.getPort(), 256, TextField.NUMERIC);
		append(tfPort);

		label = "Options (if unsure, leave empty)";
		tfOptions = new TextField(label, device.getOptions(), 256, TextField.URL);
		append(tfOptions);

		label = "Name (optional)";
		tfName = new TextField(label, device.getName(), 256, TextField.ANY);
		append(tfName);
	}

	public IDevice getDevice() {

		device.setAddress(tfAddr.getString());
		device.setPort(tfPort.getString());
		device.setOptions(tfOptions.getString());
		device.setName(tfName.getString());

		return device;
	}

	public String validate() {

		final String address = tfAddr.getString();
		final String port = tfPort.getString();

		if (address.length() == 0) {
			return "Need a host name or IP address!";
		}

		final int portInt;
		try {
			portInt = Integer.parseInt(port);
		} catch (NumberFormatException e) {
			return "Port must be a number!";
		}
		if (portInt < 1 || portInt > 65536) {
			return "Port number out of range (1-65536)!";
		}

		return null;

	}

}
