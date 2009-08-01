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
import javax.microedition.lcdui.Graphics;
import javax.microedition.midlet.MIDlet;
import javax.microedition.rms.InvalidRecordIDException;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreFullException;
import javax.microedition.rms.RecordStoreNotFoundException;
import javax.microedition.rms.RecordStoreNotOpenException;

import remuco.comm.Serial;
import remuco.ui.KeyBindings;
import remuco.ui.Theme;
import remuco.ui.screenies.TitleScreeny;
import remuco.util.Log;
import remuco.util.Tools;

/**
 * {@link Config} provides global access to various configuration options.
 * 
 * @author Oben Sonne
 * 
 */
public final class Config {

	/** Device type marker. */
	public static final String DEVICE_TYPE_BLUETOOTH = "B";

	/** Device type marker. */
	public static final String DEVICE_TYPE_INET = "I";

	/** Available screen size for a canvas screen. */
	public static final int SCREEN_WIDTH, SCREEN_HEIGHT;
	
	public static final int IMG_MAX_SIZE;

	/** Indicates if the current device supports pointer events. */
	public static final boolean TOUCHSCREEN;

	/** Indicates if UTF-8 is supported. */
	public static final boolean UTF8;

	/** Name of the application property that indicates emulation mode. */
	protected static final String APP_PROP_EMULATION = "Remuco-emulation";

	private static final char DEVICE_SPLITTER = ',';

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

	public final Vector optionDescriptors = new Vector();

	private final Vector devices = new Vector();

	private int[] keyBindings = new int[0];

	private final boolean loadedSuccessfully;

	/** MIDlet for application property access. */
	private final MIDlet midlet;

	private final Vector optionListener = new Vector();

	/** Options loaded from / saved to a record store. */
	private final Hashtable options = new Hashtable();

	/**
	 * Create a new configuration.
	 * 
	 * @param midlet
	 *            the MIDlet to use for access to application properties
	 */
	private Config(MIDlet midlet) {

		this.midlet = midlet;

		loadedSuccessfully = load();

		initOptions();

	}

	/**
	 * Add a device to the list of known devices. The new device will be placed
	 * on top of the list.
	 * 
	 * @param addr
	 *            the device address
	 * @param name
	 *            the device name or <code>null</code> for an unknown name -
	 *            note that the empty string will also be treated as an unknown
	 *            name
	 * @param type
	 *            the device type (either {@link #DEVICE_TYPE_BLUETOOTH} or
	 *            {@link #DEVICE_TYPE_INET})
	 * 
	 */
	public void addKnownDevice(String addr, String name, String type) {

		int pos;

		pos = devices.indexOf(addr);
		while (pos != -1 && pos % 3 != 0) {
			pos = devices.indexOf(addr, pos);
		}

		if (pos == -1) {
			devices.insertElementAt(addr, 0);
			devices.insertElementAt(name, 1);
			devices.insertElementAt(type, 2);
		} else {
			devices.removeElementAt(pos);
			devices.removeElementAt(pos);
			devices.removeElementAt(pos);
			devices.insertElementAt(addr, 0);
			devices.insertElementAt(name, 1);
			devices.insertElementAt(type, 2);
		}

	}

	public void addOptionListener(IOptionListener ol) {
		optionListener.addElement(ol);
	}

	/**
	 * Delete a device from the list of known devices.
	 * 
	 * @param addr
	 *            the address of the device to delete
	 */
	public void deleteKnownDevice(String addr) {

		int pos;

		pos = devices.indexOf(addr);
		while (pos != -1 && pos % 3 != 0) {
			pos = devices.indexOf(addr, pos);
		}

		if (pos != -1) {
			devices.removeElementAt(pos);
			devices.removeElementAt(pos);
			devices.removeElementAt(pos);
		}
	}

	public String getAppProperty(String name) {
		return midlet.getAppProperty(name);
	}

	public int[] getKeyBindings() {
		return keyBindings;
	}

	/**
	 * Get all known devices.
	 * 
	 * @return the devices as a vector containing 3 strings for each device -
	 *         its address (element <code>3*i</code> for device <code>i</code>),
	 *         its name (element <code>3*i+1</code>, may be <code>null</code>)
	 *         and its type (element <code>3*i+2</code>, one of
	 *         {@link #DEVICE_TYPE_BLUETOOTH} or {@link #DEVICE_TYPE_INET})
	 */
	public Vector getKnownDevices() {
		return devices;
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

		ByteArrayOutputStream baos;
		DataOutputStream dos;
		byte[] ba;
		String key, val;
		Enumeration keys;
		boolean ret = true;
		int rid;

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

		devicesIntoOptions();

		baos = new ByteArrayOutputStream();
		dos = new DataOutputStream(baos);

		// save key config

		try {
			dos.writeInt(keyBindings.length);
			for (int i = 0; i < keyBindings.length; i++) {
				dos.writeInt(keyBindings[i]);
			}
		} catch (IOException e) {
			Log.ln("[CF] save: unknown IO error", e);
		}

		ba = baos.toByteArray();
		baos.reset();

		try {
			rid = rs.addRecord(ba, 0, ba.length);
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

		// save options

		keys = options.keys();

		while (keys.hasMoreElements()) {

			key = (String) keys.nextElement();
			val = (String) options.get(key);

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

	protected boolean loadedSuccessfully() {
		return loadedSuccessfully;
	}

	/** @see #devicesIntoOptions() */
	private void devicesFromOptions() {

		String val;
		String[] devs;

		devices.removeAllElements();

		val = (String) options.get(OPTION_KEY_DEVS);

		if (val == null)
			return;

		devs = Tools.splitString(val, DEVICE_SPLITTER, true);

		if (devs.length == 0)
			return;

		if (devs.length % 3 != 0) {
			Log.ln("[CF] option 'devs' malformed");
			return;
		}

		for (int i = 0; i < devs.length; i++) {
			devices.addElement(devs[i].length() > 0 ? devs[i] : null);
		}

	}

	/**
	 * Transform the device vector {@link #devices} into an option string.
	 * Device names that are <code>null</code> are stored as empty strings but
	 * will be re-transformed to <code>null</code> when read by
	 * {@link #devicesFromOptions()}. This means a non-<code>null</code> device
	 * name of length zero will be interpreted later by
	 * {@link #devicesFromOptions()} as a <code>null</code> device name (which
	 * is just o.k.).
	 */
	private void devicesIntoOptions() {

		StringBuffer val = new StringBuffer(100);
		String s;

		int len;

		len = devices.size();

		if (len == 0) {
			options.remove(OPTION_KEY_DEVS);
			return;
		}

		for (int i = 0; i < len; i++) {
			s = (String) devices.elementAt(i);
			val.append(s != null ? s : "").append(DEVICE_SPLITTER);
		}

		if (len > 0)
			val.deleteCharAt(val.length() - 1);

		options.put(OPTION_KEY_DEVS, val.toString());

	}

	/**
	 * Collect all option descriptors and validate corresponding stored values.
	 */
	private void initOptions() {

		optionDescriptors.addElement(Theme.OD_THEME);
		optionDescriptors.addElement(TitleScreeny.OD_INFO_LEVEL);
		optionDescriptors.addElement(ClientInfo.OD_PAGE_SIZE);
		optionDescriptors.addElement(ClientInfo.OD_IMG_SIZE);
		optionDescriptors.addElement(ClientInfo.OD_IMG_TYPE);

		final Enumeration enu = optionDescriptors.elements();

		while (enu.hasMoreElements()) {

			final OptionDescriptor od = (OptionDescriptor) enu.nextElement();

			// check if stored values are still valid
			if (od.type == OptionDescriptor.TYPE_CHOICE) {
				final String stored = getOption(od);
				if (stored != null && Tools.getIndex(od.choices, stored) < 0) {
					options.put(od.id, null);
				}
			} else if (od.type == OptionDescriptor.TYPE_INT) {
				final String stored = getOption(od);
				try {
					int i = Integer.parseInt(stored);
					if (i < od.min || i > od.max) {
						options.put(od.id, null);
					}
				} catch (NumberFormatException e) {
					options.put(od.id, null);
				}
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

		ByteArrayInputStream bais;
		DataInputStream dis;
		byte[] ba;
		int nextId;
		String key, val;
		boolean ret = true;

		// open record

		final RecordStore rs = openRecord(RECORD);

		if (rs == null)
			return false;

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

		// update device list

		devicesFromOptions();

		Log.ln("[CF] load: " + (ret ? "success" : "erros"));

		return ret;

	}

}
