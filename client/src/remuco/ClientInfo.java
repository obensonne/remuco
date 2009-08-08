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
package remuco;

import java.util.Enumeration;
import java.util.Hashtable;

import remuco.comm.BluetoothFactory;
import remuco.comm.ISerializable;
import remuco.comm.SerialAtom;
import remuco.ui.Theme;
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

	private static final String SYS_PROPS_DEVICE[] = { "device.model",
			"microedition.platform" };

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

	private void addDeviceInfo(Hashtable info, String key, int value) {
		info.put(key, String.valueOf(value));
	}

	private void addDeviceInfo(Hashtable info, String key, String value) {
		info.put(key, value);
	}

	private void addDeviceInfo(Hashtable info, String key, String sysProps[]) {

		String value = "unknown";
		for (int i = 0; i < sysProps.length; i++) {
			try {
				value = System.getProperty(sysProps[i]);
				if (value != null) {
					break;
				}
			} catch (Exception e) {
			}
		}
		info.put(key, value);
	}

	private Hashtable collectDeviceInfos(Config config) {

		final Hashtable info = new Hashtable();

		addDeviceInfo(info, "name", SYS_PROPS_DEVICE);
		addDeviceInfo(info, "touchscreen", Config.TOUCHSCREEN);
		addDeviceInfo(info, "utf8", Config.UTF8);
		addDeviceInfo(info, "bluetooth", BluetoothFactory.BLUETOOTH);
		addDeviceInfo(info, "canvas-width", Config.SCREEN_WIDTH);
		addDeviceInfo(info, "canvas-height", Config.SCREEN_HEIGHT);

		addDeviceInfo(info, "version", config.getAppProperty("MIDlet-Version"));
		addDeviceInfo(info, "theme", config.getOption(Theme.OD_THEME));

		return info;
	}

}
