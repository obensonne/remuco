package remuco.comm;

import java.util.Vector;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;

import remuco.UserException;
import remuco.util.Log;

/**
 * A DeviceConnector is responsible to create net connections to a specific
 * device. This DeviceConnector uses Bluetooth (serial port profile) to create
 * net connections.
 * <p>
 * <i>Note:</i> You can get only one net connection at the same time from the
 * DeviceConnector. Before creating a new net connection, the old one must be
 * closed (this is for object pooling reasons and easy to change once more than
 * one connection at a time is needed)!
 * 
 */
public final class DeviceConnector extends RemoteDevice implements
		DiscoveryListener {

	/** Services to search.. looking for services in 'Serial Port Profile' */
	private static final UUID[] UUID_LIST = new UUID[] { new UUID(0x1101) };

	private DiscoveryAgent agent;

	private LocalDevice localDevice;

	private boolean searchingServices = false;

	private boolean connecting = false;

	private final Vector serviceRecords = new Vector();

	/**
	 * Pool the net connection object.
	 */
	private final Net net = new Net();

	/**
	 * Creates a new {@link DeviceConnector} for a device with the address
	 * <code>s</code>.
	 * 
	 * @param s
	 *            the device its address
	 * @throws UserException
	 *             if there is an error with the Bluetooth stack
	 */
	protected DeviceConnector(String s) throws UserException {

		super(s);

		try {

			localDevice = LocalDevice.getLocalDevice();

		} catch (BluetoothStateException e) {

			Log.ln("[DC] Could not get local device" + e.getMessage());
			throw new UserException("Bluetooth Error",
					"Bluetooth seems to be off.");

		}

		agent = localDevice.getDiscoveryAgent();

	}

	/**
	 * Initiates the creatin of a net connection to this device. Returns
	 * immediately. To get the created connection call
	 * {@link #getNetConnection()}
	 * 
	 * @throws UserException
	 *             if there is an error with the Bluetooth stack
	 */
	protected void createNetConnection() throws UserException {

		Log.asssert(!connecting && !searchingServices);
		Log.asssert(net == null || !net.isUp());

		connecting = true;

		try {

			serviceRecords.removeAllElements();
			
			agent.searchServices(null, UUID_LIST, this, this);

			searchingServices = true;

		} catch (BluetoothStateException e) {

			Log.ln("[DC] BluetoothStateException: " + e.getMessage());
			throw new UserException("Bluetooth Error",
					"Bluetooth seems to be off.");

		}

	}

	/**
	 * Get the net connection to this device. Must be called after
	 * {@link #createNetConnection()}.
	 * 
	 * @return the created net connection or <code>null</code>, if it was not
	 *         possible to connect (e.g. because the server is not running or
	 *         out of range)
	 * @throws UserException
	 *             if the server uses a different Remuco protocol version
	 */
	protected Net getNetConnection() throws UserException {

		Log.asssert(connecting);

		int n;
		ServiceRecord sr;
		String url;
		StreamConnection sc = null;

		// wait until search is finished

		if (searchingServices) {

			try {

				synchronized (serviceRecords) {
					// we will wake up from this if serviceSearchCompleted() has
					// been called
					serviceRecords.wait();
				}

			} catch (InterruptedException e) {

				Log.ln("[DC] interrupted");

			}

		}

		n = serviceRecords.size();

		Log.ln("[DC] found " + n + " services");

		// if (n == 0) {
		// throw new UserException("No services found",
		// "Probably the Remuco server is not running.");
		// }

		for (int i = 0; i < n; i++) {

			sc = null;

			sr = (ServiceRecord) serviceRecords.elementAt(i);

			url = sr.getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT,
					false);

			Log.ln("[DC] try service " + i + "(" + url + ") .. ");

			try {

				sc = (StreamConnection) Connector.open(url);

			} catch (Exception e) {

				Log.ln("[DC] failed: " + e.toString());

				continue;

			}

			if (net.up(sc) < 0) {

				Log.ln("[DC] failed");
				sc = null;
				continue;

			} else {

				Log.ln("[DC] success");
				break;

			}

		}

		serviceRecords.removeAllElements();

		connecting = false;

		return net.isUp() ? net : null;

	}

	public void deviceDiscovered(RemoteDevice arg0, DeviceClass arg1) {

		Log.asssertNotReached();

	}

	public void inquiryCompleted(int arg0) {

		Log.asssertNotReached();

	}

	public void serviceSearchCompleted(int transID, int respCode) {

		Log.asssert(searchingServices);

		Log.ln("[DC] Service search finished. Return code: " + respCode);

		searchingServices = false;
		
		synchronized (serviceRecords) {
			serviceRecords.notify();
		}

	}

	public void servicesDiscovered(int transId, ServiceRecord[] srs) {

		Log.asssert(searchingServices);

		int n = srs.length;
		synchronized (serviceRecords) {
			for (int i = 0; i < n; i++) {
				serviceRecords.addElement(srs[i]);
			}
		}

	}

}
