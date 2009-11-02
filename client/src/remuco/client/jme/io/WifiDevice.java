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
package remuco.client.jme.io;

import remuco.client.common.util.Tools;
import remuco.comm.IDevice;

public class WifiDevice implements IDevice {

	private static final String PORT = "34271";

	private String address;

	private String name;

	private String options;

	private String port;

	public WifiDevice() {
		address = "";
		port = PORT;
		options = "";
		name = "";
	}

	/**
	 * Create a new device based on a flattened device.
	 * 
	 * @param flat
	 *            flattened device
	 * @throws IllegalArgumentException
	 *             if <em>flat</em> is malformed
	 */
	public WifiDevice(String flat) throws IllegalArgumentException {

		String sa[];

		sa = Tools.splitString(flat, FIELD_SEP, false);

		if (sa.length != 5 || sa[0].charAt(0) != TYPE_WIFI) {
			throw new IllegalArgumentException();
		}

		address = sa[1];

		port = sa[2];
		try {
			Integer.parseInt(port);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException();
		}

		options = sa[3];
		name = sa[4];
	}

	/** Compares 2 device based on address and port. */
	public boolean equals(Object obj) {

		if (obj == this) {
			return true;
		}

		if (obj == null) {
			return false;
		}

		if (!(obj instanceof WifiDevice)) {
			return false;
		}

		final WifiDevice other = (WifiDevice) obj;

		return other.address.equals(address) && other.port.equals(port);

	}

	public String getAddress() {
		return address;
	}

	public String getLabel() {
		if (name.length() > 0) {
			return name;
		} else {
			return address + ":" + port;
		}
	}

	public String getName() {
		return name;
	}

	public String getOptions() {
		return options;
	}

	public String getPort() {
		return port;
	}

	public char getType() {
		return TYPE_WIFI;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setOptions(String options) {
		this.options = options;
	}

	public void setPort(String port) {
		this.port = port;
	}

	/** Create flat representation of this device. */
	public String toString() {

		final StringBuffer sb = new StringBuffer();

		sb.append(TYPE_WIFI);
		sb.append(FIELD_SEP);
		sb.append(address);
		sb.append(FIELD_SEP);
		sb.append(port);
		sb.append(FIELD_SEP);
		sb.append(options);
		sb.append(FIELD_SEP);
		sb.append(name);

		return sb.toString();
	}

}
