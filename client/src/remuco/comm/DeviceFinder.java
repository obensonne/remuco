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

import remuco.Remuco;
import remuco.UserException;
import remuco.util.Log;

public final class DeviceFinder implements DiscoveryListener {

	private DiscoveryAgent agent;

	/**
	 * Indicates that the user has canceled the device search and therefore any
	 * search events after {@link #cancelSearch()} has been called should get
	 * ignored. This variable gets set <code>true</code> in
	 * {@link #cancelSearch()} and will be set back to <code>false</code> in
	 * {@link #startSearch(IDeviceSearcher)}.
	 */
	private boolean canceled = false;

	private LocalDevice localDevice;

	private Vector remoteDevices = new Vector();

	private IDeviceSearcher searcher;

	public void cancelSearch() {

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

		if (Remuco.EMULATION) {

			devices = new String[2];
			devices[0] = "001122334455";
			devices[1] = "EmulatedDevice";

		} else {

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

		}

		remoteDevices.removeAllElements();

		searcher.searchFinished(devices);

		searcher = null;

	}

	public void servicesDiscovered(int arg0, ServiceRecord[] arg1) {

		Log.asssertNotReached(this);

	}

	public void serviceSearchCompleted(int arg0, int arg1) {

		Log.asssertNotReached(this);

	}

	/**
	 * Starts a search for nearby devices and returns immediately. When search
	 * if finished <code>searcher</code> gets notified.
	 * 
	 * @param searcher
	 *            the device searcher requesting the device list
	 */
	public void startSearch(IDeviceSearcher searcher) throws UserException {

		this.searcher = searcher;

		canceled = false;

		if (localDevice == null) {

			try {

				localDevice = LocalDevice.getLocalDevice();

			} catch (BluetoothStateException e) {

				Log.ln("[BT] Could not get local device: " + e.getMessage());
				throw new UserException("Device search failed",
						"Bluetooth seems to be off.");

			}

			agent = localDevice.getDiscoveryAgent();
		}

		Log.asssert(this, agent);

		try {
			agent.startInquiry(DiscoveryAgent.GIAC, this);
		} catch (BluetoothStateException e) {

			Log.ln("[BT]" + e.getMessage());

			throw new UserException("Device search failed",
					"Bluetooth seems to be blocked by another application.");

		}

	}

}
