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

import remuco.UserException;
import remuco.util.Log;

public final class Scanner implements DiscoveryListener {

	private DiscoveryAgent agent;

	/**
	 * Indicates if the user has canceled the device scan. If <code>true</code>,
	 * any scan events should get ignored. This variable gets set
	 * <code>true</code> in {@link #cancelScan()} and will be set back to
	 * <code>false</code> in {@link #startScan(IScanResultListener)}.
	 */
	private boolean canceled = false;

	private LocalDevice localDevice;

	private Vector remoteDevices = new Vector();

	private IScanResultListener resultListener;

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
		String[] devices;
		int n, i;

		if (canceled)
			return;

		// iterate over devices and get address and name

		n = remoteDevices.size();

		devices = new String[2 * n];

		for (i = 0; i < n; i++) {

			dev = (RemoteDevice) remoteDevices.elementAt(i);

			devices[2 * i] = dev.getBluetoothAddress();

			try {
				devices[2 * i + 1] = dev.getFriendlyName(false);
			} catch (IOException e) {
				devices[2 * i + 1] = null;
			}
		}

		remoteDevices.removeAllElements();

		resultListener.scanFinished(devices);

		resultListener = null;

	}

	public void servicesDiscovered(int arg0, ServiceRecord[] arg1) {

		Log.asssertNotReached(this);

	}

	public void serviceSearchCompleted(int arg0, int arg1) {

		Log.asssertNotReached(this);

	}

	/**
	 * Starts a scan for nearby devices and returns immediately. When scan if
	 * finished <code>listener</code> gets notified.
	 * 
	 * @param listener
	 *            the listener to notify when scan is finished
	 */
	public void startScan(IScanResultListener listener) throws UserException {

		this.resultListener = listener;

		canceled = false;

		if (localDevice == null) {

			try {

				localDevice = LocalDevice.getLocalDevice();

			} catch (BluetoothStateException e) {

				Log.ln("[BT] failed to get local device", e);
				throw new UserException("Scan Failed",
						"Bluetooth seems to be off.");

			}

			agent = localDevice.getDiscoveryAgent();
		}

		Log.asssert(this, agent);

		try {
			agent.startInquiry(DiscoveryAgent.GIAC, this);
		} catch (BluetoothStateException e) {

			Log.ln("[BT]", e);

			throw new UserException("Scan Failed",
					"Bluetooth seems to be busy.");

		}

	}

}
