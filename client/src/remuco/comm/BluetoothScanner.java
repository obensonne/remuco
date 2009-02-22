package remuco.comm;

import java.io.IOException;
import java.util.Vector;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;

import remuco.Config;
import remuco.UserException;
import remuco.util.Log;

public final class BluetoothScanner implements DiscoveryListener, IScanner {

	private final DiscoveryAgent agent;

	/**
	 * Indicates if the user has canceled the device scan. If <code>true</code>,
	 * any scan events should get ignored. This variable gets set
	 * <code>true</code> in {@link #cancelScan()} and will be set back to
	 * <code>false</code> in {@link #startScan(IScanListener)}.
	 */
	private boolean canceled = false;

	private final LocalDevice localDevice;

	private Vector remoteDevices = new Vector();

	private IScanListener listener;

	private final UserException startScanException;

	public BluetoothScanner() {

		LocalDevice ldev;
		UserException sse;

		try {
			ldev = LocalDevice.getLocalDevice();
			sse = null;
		} catch (BluetoothStateException e) {
			Log.ln("[BS] failed to get local device", e);
			sse = new UserException("Bluetooth Error",
					"Bluetooth seems to be off.");
			ldev = null;
		}

		startScanException = sse;
		localDevice = ldev;
		if (localDevice != null) {
			agent = localDevice.getDiscoveryAgent();
		} else {
			agent = null;
		}

	}

	public void cancelScan() {

		canceled = true;

		agent.cancelInquiry(this);

	}

	public void deviceDiscovered(RemoteDevice dev, DeviceClass dc) {

		Log.ln("found a device");

		remoteDevices.addElement(dev);

	}

	public void inquiryCompleted(int arg0) {

		Log.ln("search finished");

		RemoteDevice dev;
		int n, i;

		if (canceled)
			return;

		// iterate over devices and get address and name

		n = remoteDevices.size();

		final String devices[] = new String[3 * n];

		for (i = 0; i < n; i++) {

			dev = (RemoteDevice) remoteDevices.elementAt(i);

			devices[3 * i] = dev.getBluetoothAddress();

			try {
				devices[3 * i + 1] = dev.getFriendlyName(false);
			} catch (IOException e) {
				devices[3 * i + 1] = null;
			}

			devices[3 * i + 2] = Config.DEVICE_TYPE_BLUETOOTH;
		}

		remoteDevices.removeAllElements();

		listener.notifyScannedDevices(devices);

		listener = null;

	}

	public void servicesDiscovered(int arg0, ServiceRecord[] arg1) {

		Log.asssertNotReached(this);

	}

	public void serviceSearchCompleted(int arg0, int arg1) {

		Log.asssertNotReached(this);

	}

	/**
	 * Starts a scan for nearby devices and returns immediately. When scan is
	 * finished <code>listener</code> gets notified.
	 * 
	 * @param listener
	 *            the listener to notify when scan is finished
	 * @throws UserException
	 *             if scan initiation failed
	 */
	public void startScan(IScanListener listener) throws UserException {

		this.listener = listener;

		canceled = false;

		if (startScanException != null) {
			throw startScanException;
		}

		try {
			agent.startInquiry(DiscoveryAgent.GIAC, this);
		} catch (BluetoothStateException e) {

			Log.ln("[BS]", e);

			throw new UserException("Bluetooth Error",
					"Bluetooth seems to be busy. "
							+ "Cannot scan for nearby devices right now.");

		}

	}

}
