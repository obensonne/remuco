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
import remuco.util.Log;
import remuco.util.Tools;

/**
 * Helper class which provides various configuration values and tools.
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

	/** Indicates if UTF-8 is supported. */
	public static final boolean UTF8;

	private static final String APP_PROP_THEMES = "Remuco-themes";

	private static final String[] APP_PROP_ALLKEYS = new String[] { APP_PROP_THEMES };

	private static final Hashtable applicationProperties = new Hashtable();

	private static final String DEVICE_SPLITTER = ",";

	private static final Vector devices = new Vector();

	private static final int FIRST_RECORD_ID = 1;

	private static final String KEY_DEVICES = "devs";

	private static boolean loaded = false;

	private static final Hashtable options = new Hashtable();

	private static final String RECORD = "options";

	static {

		Log.debug("init config");

		// get screen size

		final Canvas c = new Canvas() {
			protected void paint(Graphics g) {
			}
		};
		SCREEN_WIDTH = c.getWidth();
		SCREEN_HEIGHT = c.getHeight();

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
	 * Get the value of a configuration option.
	 * 
	 * @param name
	 *            the option to get the value of
	 * @return the option's value or <code>null</code> if the option is not set
	 */
	public static String get(String name) {

		return (String) options.get(name);

	}

	/**
	 * Get the value of a configuration option.
	 * 
	 * @param name
	 *            the option to get the value of
	 * @param def
	 *            the default alternative, if the option is not set
	 * @return the option's value or it's default
	 */
	public static String get(String name, String def) {

		return (String) options.get(name);

	}

	/**
	 * Get a property defined in the application's manifest or jad file.
	 * 
	 * @param key
	 *            property name, one of <code>Config.APP_PROP_..</code>
	 * @return the property's value or <code>null</code> if the property is not
	 *         set
	 */
	public static String getApplicationProperty(String key) {

		return (String) applicationProperties.get(key);

	}
	
	private static int[] keyBindings = new int[0];
	
	public static int[] getKeyBindings() {
		return keyBindings;
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
	 * 
	 * 
	 */
	public static void knownDevicesAdd(String addr, String name, String type) {

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

	/**
	 * Delete a device from the list of known devices.
	 * 
	 * @param addr
	 *            the address of the device to delete
	 */
	public static void knownDevicesDelete(String addr) {

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

	/**
	 * Forget all known devices.
	 * 
	 */
	public static void knownDevicesDeleteAll() {

		devices.removeAllElements();

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
	public static Vector knownDevicesGet() {
		return devices;
	}

	/**
	 * Load the configuration.
	 * 
	 * @return <code>true</code> if loading was successful, <code>false</code>
	 *         if errors occurred (in this case defaults are used for the
	 *         configurations which could not get set, so the application can
	 *         continue its work as normal)
	 */
	public static boolean load() {

		ByteArrayInputStream bais;
		DataInputStream dis;
		byte[] ba;
		int nextId;
		String key, val;
		boolean ret = true;

		// open record

		if (loaded)
			return true; // may happen if Remuco gets started more than once

		loaded = true;

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

		// load keys

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

		// load options

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

	/**
	 * Saves the current configuration. This automatically saves the current
	 * {@link KeyBindings} configuration using {@link KeyBindings#getConfiguration()}.
	 * 
	 * @return <code>true</code> on success, <code>false</code> if errors
	 *         occured
	 */
	public static boolean save() {

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

	public static void setKeyBindings(int[] keyBindings) {
		Config.keyBindings = keyBindings;
	}

	/**
	 * Set an configuration option which will be saved later when
	 * {@link #save()} gets called.
	 * 
	 * @param name
	 *            option name (name {@value #KEY_DEVICES} is reserved !)
	 * @param value
	 *            the option's value (use <code>null</code> to unset an option)
	 */
	public static void set(String name, String value) {

		if (value == null)
			options.remove(name);

		options.put(name, value);

	}

	/**
	 * Makes all known application properties accessible to other classes via
	 * the method {@link #getApplicationProperty(String)}. Known properties are
	 * <code>Config.APP_PROP_..</code>.
	 * 
	 * @param midlet
	 *            the midlet which has access to the application properties
	 */
	protected static void setApplicationProperties(MIDlet midlet) {

		String val;

		for (int i = 0; i < APP_PROP_ALLKEYS.length; i++) {
			val = midlet.getAppProperty(APP_PROP_ALLKEYS[i]);
			if (val != null) {
				applicationProperties.put(APP_PROP_ALLKEYS[i], val);
			}
		}

	}
	
	public static String[] getThemeList() {
		
		final String list = getApplicationProperty(APP_PROP_THEMES);
		if (list != null) {
			return Tools.splitString(list, ",");
		} else {
			return null;
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

	/** @see #devicesIntoOptions() */
	private static void devicesFromOptions() {

		String val;
		String[] devs;

		devices.removeAllElements();

		val = (String) options.get(KEY_DEVICES);

		if (val == null)
			return;

		devs = Tools.splitString(val, DEVICE_SPLITTER);

		if (devs.length == 0)
			return;

		if (devs.length % 3 != 0) {
			Log.ln("[CF] option devs malformed");
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
	private static void devicesIntoOptions() {

		StringBuffer val = new StringBuffer(100);
		String s;

		int len;

		len = devices.size();

		if (len == 0) {
			options.remove(KEY_DEVICES);
			return;
		}

		for (int i = 0; i < len; i++) {
			s = (String) devices.elementAt(i);
			val.append(s != null ? s : "").append(DEVICE_SPLITTER);
		}

		if (len > 0)
			val.deleteCharAt(val.length() - 1);

		options.put(KEY_DEVICES, val.toString());

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

}
