package remuco.comm;

import remuco.util.Log;

/**
 * On devices which have no JSR-82 support, references to bluetooth classes
 * could make the application fail. For this purpose this factory provides the
 * creation of bluetooth classes without references in the byte code.
 * 
 * @author Oben Sonne
 * 
 */
public class BluetoothFactory {

	/** Indicates if the current device has Bluetooth. */
	public static final boolean BLUETOOTH;

	static {

		// check for bluetooth

		boolean b = true;

		try {
			Class.forName("javax.bluetooth.LocalDevice");
			Log.ln("[BF] bluetooth: yes");
		} catch (Exception e) {
			Log.ln("[BF] bluetooth: no");
			b = false;
		}

		BLUETOOTH = b;

	}

	public static IScanner createBluetoothScanner() {

		final Class c;

		try {
			c = Class.forName("remuco.comm.BluetoothScanner");
		} catch (ClassNotFoundException e) {
			Log.bug("Feb 2, 2009.11:26:27 PM", e);
			return null;
		}

		try {
			return (IScanner) c.newInstance();
		} catch (InstantiationException e) {
			Log.bug("Feb 2, 2009.11:26:51 PM", e);
			return null;
		} catch (IllegalAccessException e) {
			Log.bug("Feb 2, 2009.11:26:59 PM", e);
			return null;
		}

	}

	public static IServiceFinder createBluetoothServiceFinder() {

		final Class c;
		try {
			c = Class.forName("remuco.comm.BluetoothServiceFinder");
		} catch (ClassNotFoundException e) {
			Log.bug("Feb 3, 2009.12:47:14 AM", e);
			return null;
		}

		try {
			return (IServiceFinder) c.newInstance();
		} catch (InstantiationException e) {
			Log.bug("Feb 3, 2009.12:47:19 AM", e);
			return null;
		} catch (IllegalAccessException e) {
			Log.bug("Feb 3, 2009.12:47:27 AM", e);
			return null;
		}
	}

}
