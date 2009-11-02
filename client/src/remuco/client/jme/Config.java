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
package remuco.client.jme;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Graphics;
import javax.microedition.midlet.MIDlet;
import javax.microedition.rms.InvalidRecordIDException;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreFullException;
import javax.microedition.rms.RecordStoreNotFoundException;
import javax.microedition.rms.RecordStoreNotOpenException;

import remuco.OptionDescriptor;
import remuco.client.common.data.ClientInfo;
import remuco.client.common.serial.Serial;
import remuco.client.common.util.Log;
import remuco.client.common.util.Tools;
import remuco.client.jme.io.BluetoothDevice;
import remuco.client.jme.io.IDevice;
import remuco.client.jme.io.WifiDevice;
import remuco.client.jme.ui.KeyBindings;
import remuco.client.jme.ui.Theme;
import remuco.client.jme.ui.screenies.TitleScreeny;
import remuco.client.jme.ui.screens.PlayerScreen;
import remuco.client.jme.ui.screens.OptionsScreen.IOptionListener;
import remuco.comm.Connection;

/**
 * Config provides global access to various configuration options.
 */
public final class Config {

	public static final String DEVICE_NAME;

	public static final int IMG_MAX_SIZE;

	/** List of all option descriptors. */
	public static final Vector OPTION_DESCRIPTORS;

	/** Available screen size for a canvas screen. */
	public static final int SCREEN_WIDTH, SCREEN_HEIGHT;

	/** Indicates if the current device supports pointer events. */
	public static final boolean TOUCHSCREEN;

	/** Indicates if UTF-8 is supported. */
	public static final boolean UTF8;

	private static final int FIRST_RECORD_ID = 1;

	private static Config instance = null;

	private static final String OPTION_KEY_DEVS = "__devs__";

	private static final String RECORD = "options";

	static {

		// get screen size

		final Canvas c = new Canvas() {
			protected void paint(Graphics g) {
			}
		};
		SCREEN_WIDTH = c.getWidth();
		SCREEN_HEIGHT = c.getHeight();
		IMG_MAX_SIZE = Math.min(SCREEN_WIDTH, SCREEN_HEIGHT);
		TOUCHSCREEN = c.hasPointerEvents();

		// check encoding support

		boolean b;

		try {
			"".getBytes(Serial.ENCODING);
			Log.ln("[CF] " + Serial.ENCODING + ": yes");
			b = true;
		} catch (UnsupportedEncodingException e) {
			Log.ln("[CF] " + Serial.ENCODING + ": no");
			b = false;
		}

		UTF8 = b;

		// device name

		String dn = "unknown";

		final String props[] = { "device.model", "microedition.platform" };
		for (int i = 0; i < props.length; i++) {
			try {
				dn = System.getProperty(props[i]);
				if (dn != null) {
					break;
				}
			} catch (Exception e) {
			}
		}

		DEVICE_NAME = dn;

		// option descriptors

		OPTION_DESCRIPTORS = new Vector();

		OPTION_DESCRIPTORS.addElement(Theme.OD_THEME);
		OPTION_DESCRIPTORS.addElement(TitleScreeny.OD_INFO_LEVEL);
		OPTION_DESCRIPTORS.addElement(ClientInfo.OD_PAGE_SIZE);
		OPTION_DESCRIPTORS.addElement(ClientInfo.OD_IMG_SIZE);
		OPTION_DESCRIPTORS.addElement(ClientInfo.OD_IMG_TYPE);
		OPTION_DESCRIPTORS.addElement(PlayerScreen.OD_IMG_KEEPFS);
		OPTION_DESCRIPTORS.addElement(Connection.OD_PING);

	}

	/**
	 * Get the singleton config instance. <em>Must not</em> get called from a
	 * static context!
	 * 
	 * @return the config
	 */
	public static Config getInstance() {
		return instance;
	}

	/**
	 * Initialize. Must be called before any call to {@link #getInstance()}.
	 * 
	 * @param midlet
	 *            the MIDlet to use for access to application properties
	 */
	public static void init(MIDlet midlet) {
		if (instance == null) {
			instance = new Config(midlet);
		}
	}

	private static void closeRecord(RecordStore rs) {

		if (rs == null)
			return;

		try {
			rs.closeRecordStore();
			Log.ln("[CF] close: ok");
		} catch (RecordStoreNotOpenException e) {
			Log.ln("[CF] close: not open!");
		} catch (RecordStoreException e) {
			Log.ln("[CF] close: unknown error", e);
		}

	}

	private static RecordStore openRecord(String name) {

		RecordStore rs = null;
		int rsUsed, rsTotal;

		try {
			rs = RecordStore.openRecordStore(RECORD, true);
			rsUsed = (rs.getSize() / 1024) + 1;
			rsTotal = (rs.getSizeAvailable() / 1024) + 1;
			Log.ln("[CF] open: ok, using " + rsUsed + "/" + rsTotal + "KB");
			return rs;
		} catch (RecordStoreFullException e) {
			Log.ln("[CF] open: error, full");
		} catch (RecordStoreNotFoundException e) {
			Log.ln("[CF] open: error, not found ???");
		} catch (RecordStoreException e) {
			Log.ln("[CF] open: unknown error", e);
		}

		return null;
	}

	public final Vector devices = new Vector();

	/** Recommended list icon size. */
	public final int SUGGESTED_LICS;

	protected final boolean loadedSuccessfully;

	private int[] keyBindings = new int[0];

	private final Vector optionListener = new Vector();

	/** Options loaded from / saved to a record store. */
	private final Hashtable options = new Hashtable();

	/**
	 * Create a new configuration.
	 * 
	 * @param midlet
	 *            the MIDlet to use for access certain information
	 */
	private Config(MIDlet midlet) {

		SUGGESTED_LICS = Display.getDisplay(midlet).getBestImageHeight(
			Display.LIST_ELEMENT);

		loadedSuccessfully = load();

		validateOptionDescriptorOptions();
	}

	public void addOptionListener(IOptionListener ol) {
		optionListener.addElement(ol);
	}

	public int[] getKeyBindings() {
		return keyBindings;
	}

	/**
	 * Get the value of a configuration option.
	 * 
	 * @param od
	 *            the option's descriptor
	 * @return the option's value or its default if the option is not set
	 */
	public String getOption(OptionDescriptor od) {

		final String val = (String) options.get(od.id);
		return val != null ? val : od.def;

	}

	/**
	 * Saves the current configuration. This automatically saves the current
	 * {@link KeyBindings} configuration using
	 * {@link KeyBindings#getConfiguration()}.
	 * 
	 * @return <code>true</code> on success, <code>false</code> if errors
	 *         occured
	 */
	public boolean save() {

		boolean ret = true;

		// delete old config

		try {
			RecordStore.deleteRecordStore(RECORD);
			Log.ln("[CF] save: deleted old config");
		} catch (RecordStoreNotFoundException e) {
			Log.ln("[CF] save: no config yet");
		} catch (RecordStoreException e) {
			Log.ln("[CF] save: unknown error", e);
			return false;
		}

		// open record

		final RecordStore rs = openRecord(RECORD);

		if (rs == null)
			return false;

		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final DataOutputStream dos = new DataOutputStream(baos);

		// save key config

		try {
			dos.writeInt(keyBindings.length);
			for (int i = 0; i < keyBindings.length; i++) {
				dos.writeInt(keyBindings[i]);
			}
		} catch (IOException e) {
			Log.ln("[CF] save: unknown IO error", e);
		}

		byte ba[];

		ba = baos.toByteArray();
		baos.reset();

		try {
			final int rid = rs.addRecord(ba, 0, ba.length);
			if (rid != FIRST_RECORD_ID) {
				Log.ln("[CF] save: WARNING, keys not in record 1 !!!");
				closeRecord(rs);
				return false;
			}
		} catch (RecordStoreNotOpenException e) {
			Log.ln("[CF] save: error, not open ???");
			return false;
		} catch (RecordStoreFullException e) {
			Log.ln("[CF] save: error, full");
			closeRecord(rs);
			return false;
		} catch (RecordStoreException e) {
			Log.ln("[CF] save: unknown error", e);
			closeRecord(rs);
			return false;
		}

		// flatten devices

		final StringBuffer sb = new StringBuffer();

		final Enumeration devs = devices.elements();

		while (devs.hasMoreElements()) {
			sb.append(devs.nextElement());
			sb.append(IDevice.LIST_SEP);
		}
		if (sb.length() > 0) {
			sb.deleteCharAt(sb.length() - 1);
		}

		options.put(OPTION_KEY_DEVS, sb.toString());

		// save options

		final Enumeration keys = options.keys();

		while (keys.hasMoreElements()) {

			final String key = (String) keys.nextElement();
			final String val = (String) options.get(key);

			try {
				dos.writeUTF(key);
				dos.writeUTF(val);
			} catch (IOException e) {
				Log.ln("[CF] save: bad string (" + key + "/" + val + ")", e);
				ret = false;
				continue;
			}

			ba = baos.toByteArray();
			baos.reset();

			try {
				rs.addRecord(ba, 0, ba.length);
			} catch (RecordStoreNotOpenException e) {
				Log.ln("[CF] save: error, not open ???");
				return false;
			} catch (RecordStoreFullException e) {
				Log.ln("[CF] save: error, full");
				closeRecord(rs);
				return false;
			} catch (RecordStoreException e) {
				Log.ln("[CF] save: unknown error", e);
				closeRecord(rs);
				return false;
			}
		}

		// ok, done

		closeRecord(rs);

		Log.ln("[CF] save: " + (ret ? "success" : "erros"));

		return ret;

	}

	public void setKeyBindings(int[] keyBindings) {
		this.keyBindings = keyBindings;
	}

	/**
	 * Set a configuration option which will be saved later when {@link #save()}
	 * gets called. After setting the option, all option listeners get notified
	 * that the option <i>od</i> has changed.
	 * 
	 * @param od
	 *            option's descriptor
	 * @param value
	 *            option value (use <code>null</code> to unset an option)
	 */
	public void setOption(OptionDescriptor od, String value) {

		if (value == null) {
			options.remove(od.id);
		} else {
			options.put(od.id, value);
		}

		final Enumeration enu = optionListener.elements();
		while (enu.hasMoreElements()) {
			IOptionListener ol = (IOptionListener) enu.nextElement();
			ol.optionChanged(od);
		}
	}

	/**
	 * Remove all option listeners which only are alive during a session.
	 */
	protected void removeSessionOptionListener() {

		final int len = optionListener.size();

		IOptionListener ol;

		for (int i = len - 1; i >= 0; i--) {
			ol = (IOptionListener) optionListener.elementAt(i);
			if (ol.isSessionOptionListener()) {
				optionListener.removeElementAt(i);
			}
		}
	}

	/**
	 * Load the configuration.
	 * 
	 * @return <code>true</code> if loading was successful, <code>false</code>
	 *         if errors occurred (in this case defaults are used for the
	 *         configurations which could not get set, so the application can
	 *         continue its work as normal)
	 */
	private boolean load() {

		boolean ret = true;

		// open record

		final RecordStore rs = openRecord(RECORD);

		if (rs == null)
			return false;

		int nextId;

		try {
			nextId = rs.getNextRecordID();
		} catch (RecordStoreNotOpenException e) {
			Log.ln("[CF] load: error, not open ???");
			return false;
		} catch (RecordStoreException e) {
			Log.ln("[CF] load: unknown error", e);
			closeRecord(rs);
			return false;
		}

		ByteArrayInputStream bais;
		DataInputStream dis;
		byte[] ba;

		// first record contains key bindings:

		try {
			ba = new byte[rs.getRecordSize(FIRST_RECORD_ID)];
			rs.getRecord(FIRST_RECORD_ID, ba, 0);

			bais = new ByteArrayInputStream(ba);
			dis = new DataInputStream(bais);

		} catch (RecordStoreNotOpenException e) {
			Log.ln("[CF] load: error, not open ???");
			return false;
		} catch (InvalidRecordIDException e) {
			Log.ln("[CF] load: record seems to be empty", e);
			closeRecord(rs);
			return true;
		} catch (RecordStoreException e) {
			Log.ln("[CF] load: unknown error", e);
			closeRecord(rs);
			return false;
		}

		try {
			final int len = dis.readInt();
			final int kb[] = new int[len];
			for (int i = 0; i < len; i++) {
				kb[i] = dis.readInt();
			}
			keyBindings = kb;
		} catch (NegativeArraySizeException e) {
			Log.ln("[CF] load: keys malformed", e);
			ret = false;
		} catch (EOFException e) {
			Log.ln("[CF] load: keys malformed", e);
			ret = false;
		} catch (IOException e) {
			Log.ln("[CF] load: unknown IO error", e);
			ret = false;
		}

		// next record contains options

		for (int i = FIRST_RECORD_ID + 1; i < nextId; i++) {

			try {

				ba = new byte[rs.getRecordSize(i)];
				rs.getRecord(i, ba, 0);

				bais = new ByteArrayInputStream(ba);
				dis = new DataInputStream(bais);

			} catch (RecordStoreNotOpenException e) {
				Log.ln("[CF] load: error, not open ???");
				return false;
			} catch (InvalidRecordIDException e) {
				continue;
			} catch (RecordStoreException e) {
				Log.ln("[CF] load: unknown error", e);
				closeRecord(rs);
				return false;
			}

			final String key, val;

			try {
				key = dis.readUTF();
				val = dis.readUTF();
				Log.ln("[CF] load: " + key + " = '" + val + "'");
			} catch (IOException e) {
				Log.ln("[CF] load: error, bad strings in record " + i, e);
				ret = false;
				continue;
			}

			options.put(key, val);

		}

		// ok, done

		closeRecord(rs);

		// unflatten devices

		devices.removeAllElements();

		final String val = (String) options.get(OPTION_KEY_DEVS);

		if (val != null && val.length() > 0) {

			final String flad[] = Tools.splitString(val, IDevice.LIST_SEP, false);

			for (int i = 0; i < flad.length; i++) {
				final IDevice iDevice;
				if (flad[i].length() == 0) {
					ret = false;
					continue;
				}
				try {
					if (flad[i].charAt(0) == IDevice.TYPE_WIFI) {
						iDevice = new WifiDevice(flad[i]);
					} else if (flad[i].charAt(0) == IDevice.TYPE_BLUETOOTH) {
						iDevice = new BluetoothDevice(flad[i]);
					} else {
						ret = false;
						continue;
					}
				} catch (IllegalArgumentException e) {
					ret = false;
					continue;
				}
				devices.addElement(iDevice);
			}
		}

		Log.ln("[CF] load: " + (ret ? "success" : "erros"));

		return ret;

	}

	/**
	 * Check if all saved options still are valid according to the constrains of
	 * their option descriptors.
	 * 
	 * @see #OPTION_DESCRIPTORS
	 */
	private void validateOptionDescriptorOptions() {

		final Enumeration enu = OPTION_DESCRIPTORS.elements();

		while (enu.hasMoreElements()) {

			final OptionDescriptor od = (OptionDescriptor) enu.nextElement();

			// check if stored values are still valid
			if (od.type == OptionDescriptor.TYPE_CHOICE) {
				final String stored = getOption(od);
				if (stored != null && Tools.getIndex(od.choices, stored) < 0) {
					options.remove(od.id);
				}
			} else if (od.type == OptionDescriptor.TYPE_INT) {
				final String stored = getOption(od);
				try {
					int i = Integer.parseInt(stored);
					if (i < od.min || i > od.max) {
						options.remove(od.id);
					}
				} catch (NumberFormatException e) {
					options.remove(od.id);
				}
			}
		}
	}

}
