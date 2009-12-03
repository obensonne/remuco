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

public class BluetoothDevice implements IDevice {

	/** Service search strategy. */
	public static final int SEARCH_STANDARD = 0, SEARCH_FAILSAFE = 1,
			SEARCH_MANUAL = 2;

	private static final int SEARCH_TYPE_LAST = SEARCH_MANUAL;

	private String address;

	private boolean authenticate, encrypt;

	private String chan;

	private String name;

	private int search;

	public BluetoothDevice() {
		address = "";
		search = SEARCH_STANDARD;
		chan = "1";
		name = "";
		authenticate = false;
		encrypt = false;
	}

	/**
	 * Create a new device based on a flattened device.
	 * 
	 * @param flat
	 *            flattened device
	 * @throws IllegalArgumentException
	 *             if <em>flat</em> is malformed
	 */
	public BluetoothDevice(String flat) throws IllegalArgumentException {

		String sa[];

		sa = Tools.splitString(flat, FIELD_SEP, false);

		if (sa.length != 7 || sa[0].charAt(0) != TYPE_BLUETOOTH) {
			throw new IllegalArgumentException();
		}

		address = sa[1];

		try {
			search = Integer.parseInt(sa[2]);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException();
		}
		if (search < 0 && search > SEARCH_TYPE_LAST) {
			throw new IllegalArgumentException();
		}

		chan = sa[3];
		try {
			Integer.parseInt(chan);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException();
		}

		authenticate = sa[4].equals("true");
		encrypt = sa[5].equals("true");

		name = sa[6];
	}

	public String getAddress() {
		return address;
	}

	public String getChan() {
		return chan;
	}

	public String getLabel() {
		if (name.length() > 0) {
			return name;
		} else {
			return address;
		}
	}

	public String getName() {
		return name;
	}

	public int getSearch() {
		return search;
	}

	public char getType() {
		return TYPE_BLUETOOTH;
	}

	public boolean isAuthenticate() {
		return authenticate;
	}

	public boolean isEncrypt() {
		return encrypt;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public void setAuthenticate(boolean authenticate) {
		this.authenticate = authenticate;
	}

	public void setEncrypt(boolean encrypt) {
		this.encrypt = encrypt;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setPort(String port) {
		this.chan = port;
	}

	public void setSearch(int search) {
		this.search = search;
	}

	/** Create flat representation of this device. */
	public String toString() {

		final StringBuffer sb = new StringBuffer();

		sb.append(TYPE_BLUETOOTH);
		sb.append(FIELD_SEP);
		sb.append(address);
		sb.append(FIELD_SEP);
		sb.append(search);
		sb.append(FIELD_SEP);
		sb.append(chan);
		sb.append(FIELD_SEP);
		sb.append(authenticate);
		sb.append(FIELD_SEP);
		sb.append(encrypt);
		sb.append(FIELD_SEP);
		sb.append(name);

		return sb.toString();
	}

}
