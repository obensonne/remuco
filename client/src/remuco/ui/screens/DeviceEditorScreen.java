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

import java.util.Hashtable;

import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.TextField;

import remuco.comm.Device;
import remuco.util.Tools;

public class DeviceEditorScreen extends Form {

	private static final String TEXT_ADDRESS = "a";
	private static final String TEXT_INTRO = "i";
	private static final String TEXT_OPTIONS = "o";
	private static final String TEXT_PORT = "p";

	private static final Hashtable textWiFi, textBluetooth;

	static {

		textWiFi = new Hashtable();

		textWiFi.put(TEXT_ADDRESS, "Host or IP address:");
		textWiFi.put(TEXT_PORT, "Port number (if unsure, do not change)");
		textWiFi.put(TEXT_OPTIONS, "Options (if unsure, leave empty):");

		textBluetooth = new Hashtable();

		textBluetooth.put(TEXT_INTRO,
			"Use this only if the scanning or service search does not work.");
		textBluetooth.put(TEXT_ADDRESS, "Address:");
		textBluetooth.put(TEXT_PORT,
			"Channel (leave empty to detect automatically, otherwise check at server):");
	}

	private final Hashtable text;

	private final TextField tfAddr, tfPort, tfOptions, tfName;

	private final int type;

	/**
	 * Create a new device editor screen to edit the parameters of the given
	 * device.
	 * 
	 * @param device
	 */
	public DeviceEditorScreen(Device device) {

		super("Connection");

		type = device.type;

		if (type == Device.BLUETOOTH) {
			setTitle("Bluetooth Connection");
			text = textBluetooth;
		} else if (type == Device.WIFI) {
			setTitle("Wifi Connection");
			text = textWiFi;
		} else {
			throw new IllegalArgumentException("invalid device type");
		}

		String label;

		label = (String) text.get(TEXT_INTRO);
		if (label != null) {
			append(label);
		}

		String sa[] = Tools.splitString(device.address, ':', false);

		final String address = sa[0];
		final String port;
		final String options;

		if (sa.length > 1) {
			sa = Tools.splitString(sa[1], ';', false);
			port = sa[0];
			if (sa.length > 1) {
				options = sa[1];
			} else {
				options = "";
			}
		} else {
			port = "";
			options = "";
		}

		label = (String) text.get(TEXT_ADDRESS);
		tfAddr = new TextField(label, address, 256, TextField.URL);
		append(tfAddr);

		label = (String) text.get(TEXT_PORT);
		tfPort = new TextField(label, port, 5, TextField.NUMERIC);
		append(tfPort);

		label = (String) text.get(TEXT_OPTIONS);
		tfOptions = new TextField(label, options, 256, TextField.URL);
		if (label != null) {
			append(tfOptions);
		}

		tfName = new TextField("Name (optional)", "", 256,
				TextField.NON_PREDICTIVE);
		append(tfName);
	}

	/**
	 * Get a new device as entered by the user. Call {@link #validate()} before!
	 * 
	 * @return a device
	 */
	public Device getDevice() {

		final StringBuffer sb = new StringBuffer(tfAddr.getString());

		if (tfPort.getString().length() > 0) {
			sb.append(':').append(tfPort.getString());
			if (tfOptions.getString().length() > 0) {
				if (tfOptions.getString().charAt(0) != ';') {
					sb.append(';');
				}
				sb.append(tfOptions.getString());
			}
		}

		return new Device(type, sb.toString(), tfName.getString());
	}

	/**
	 * Validate the user input.
	 * 
	 * @return <code>null</code> if user input is ok, otherwise a string message
	 *         describing what's wrong
	 */
	public String validate() {

		final String address = tfAddr.getString();
		final String port = tfPort.getString();

		if (address.length() == 0) {
			return "Need an address!";
		}
		if (type == Device.BLUETOOTH) {
			if (address.length() != 12) {
				return "A Bluetooth address has exactly 12 digits!";
			}
			final char[] digits = address.toCharArray();
			int i;
			for (i = 0; i < digits.length; i++) {
				boolean good = false;
				good |= digits[i] >= '0' && digits[i] <= '9';
				good |= digits[i] >= 'a' && digits[i] <= 'A';
				good |= digits[i] >= 'A' && digits[i] <= 'F';
				if (!good) {
					return "Bluetooth address contains invalid characters!";
				}
			}
		}

		if (type == Device.BLUETOOTH) {
			if (port.length() > 0) {
				final int portInt;
				try {
					portInt = Integer.parseInt(port);
				} catch (NumberFormatException e) {
					return "Port must be empty or a number!";
				}
				if (portInt < 1 || portInt > 30) {
					return "Port number out of range (1-30)!";
				}
			}
		} else if (type == Device.WIFI) {
			if (port.length() == 0) {
				return "Need a port number!";
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
		}

		return null;
	}

}
