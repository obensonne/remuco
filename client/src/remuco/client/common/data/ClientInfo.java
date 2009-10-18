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
package remuco.client.common.data;

import java.util.Enumeration;
import java.util.Hashtable;

import remuco.Config;
import remuco.OptionDescriptor;
import remuco.Remuco;
import remuco.client.common.serial.ISerializable;
import remuco.client.common.serial.SerialAtom;
import remuco.util.Log;

public final class ClientInfo implements ISerializable {

	public static final OptionDescriptor OD_IMG_SIZE = new OptionDescriptor(
			"img-size", "Image size", Math.min(200, Config.IMG_MAX_SIZE), 0,
			Config.IMG_MAX_SIZE);

	public static final OptionDescriptor OD_IMG_TYPE = new OptionDescriptor(
			"img-type", "Image type", "JPEG", "JPEG,PNG");

	public static final OptionDescriptor OD_PAGE_SIZE = new OptionDescriptor(
			"page-size", "Page size of lists", 50, 10, 10000);

	private static final int[] ATOMS_FMT = new int[] { SerialAtom.TYPE_I,
			SerialAtom.TYPE_S, SerialAtom.TYPE_I, SerialAtom.TYPE_AS,
			SerialAtom.TYPE_AS };

	private final SerialAtom[] atoms;

	/**
	 * Create a new client info.
	 * 
	 * @param complete
	 *            if <code>true</code>, also include informative device
	 *            information, if <code>false</code>, just include user
	 *            configuration options and required device information
	 */
	public ClientInfo(boolean complete) {

		atoms = SerialAtom.build(ATOMS_FMT);

		final Config config = Config.getInstance();

		atoms[0].i = Integer.parseInt(config.getOption(OD_IMG_SIZE));
		atoms[1].s = config.getOption(OD_IMG_TYPE);
		atoms[2].i = Integer.parseInt(config.getOption(OD_PAGE_SIZE));

		if (complete) {

			final Hashtable info = collectDeviceInfos(config);

			atoms[3].as = new String[info.size()];
			atoms[4].as = new String[info.size()];

			final Enumeration enu = info.keys();
			int i = 0;
			while (enu.hasMoreElements()) {
				final String key = (String) enu.nextElement();
				final String val = (String) info.get(key);
				atoms[3].as[i] = key;
				atoms[4].as[i] = val;
				i++;
			}
		}
	}

	public SerialAtom[] getAtoms() {
		return atoms;
	}

	public void notifyAtomsUpdated() {
		Log.bug("Feb 22, 2009.6:25:29 PM");
	}

	private void addDeviceInfo(Hashtable info, String key, boolean value) {
		info.put(key, value ? "yes" : "no");
	}

	private void addDeviceInfo(Hashtable info, String key, String value) {
		if (value == null) {
			value = "unknown";
		}
		info.put(key, value);
	}

	private Hashtable collectDeviceInfos(Config config) {

		final Hashtable info = new Hashtable();

		addDeviceInfo(info, "name", Config.DEVICE_NAME);
		addDeviceInfo(info, "touch", Config.TOUCHSCREEN);
		addDeviceInfo(info, "utf8", Config.UTF8);
		addDeviceInfo(info, "version", Remuco.VERSION);

		return info;
	}

}
