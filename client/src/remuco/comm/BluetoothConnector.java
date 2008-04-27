package remuco.comm;

import java.util.Enumeration;
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
public final class BluetoothConnector extends RemoteDevice implements
		DiscoveryListener, IConnector {

	/** Services to search.. looking for services in 'Serial Port Profile' */
	private static final UUID[] UUID_LIST = new UUID[] { new UUID(0x1101) };

	private final DiscoveryAgent agent;

	/**
	 * Pool the connection object.
	 */
	private final Connection conn;

	private final LocalDevice localDevice;

	private final Vector serviceRecords;

	private int serviceSearchResult;

	/**
	 * Creates a new {@link BluetoothConnector} for a device with the address
	 * <code>s</code>.
	 * 
	 * @param s
	 *            the device its address
	 * @throws UserException
	 *             if there is an error with the Bluetooth stack
	 */
	protected BluetoothConnector(String s) throws UserException {

		super(s);

		conn = new Connection();
		serviceRecords = new Vector();

		try {

			localDevice = LocalDevice.getLocalDevice();

		} catch (BluetoothStateException e) {

			Log.ln("[BT] failed to get local device", e);
			throw new UserException("Bluetooth Error",
					"Bluetooth seems to be off.");

		}

		agent = localDevice.getDiscoveryAgent();

	}

	public void deviceDiscovered(RemoteDevice arg0, DeviceClass arg1) {

		Log.asssertNotReached(this);

	}

	/**
	 * Note: Synchronizing this method is not needed, since it only gets called
	 * by the communicator thread.
	 */
	public Connection getConnection() throws UserException {

		Enumeration enu;
		ServiceRecord sr;
		String url;
		StreamConnection sc = null;
		int searchID;

		Log.asssert(this, !conn.isUp());

		// ////// init and start service search ////// //

		serviceRecords.removeAllElements();
		serviceSearchResult = 0;

		try {
			// due to a WTK bug, the following throws a NPExc in the emulator
			// when the device provides no services
			searchID = agent.searchServices(null, UUID_LIST, this, this);

		} catch (BluetoothStateException e) {

			Log.ln("[BT] BluetoothStateException", e);
			throw new UserException("Bluetooth Error",
					"Bluetooth seems to be busy.");
		}

		// ////// wait until search is finished ////// //

		synchronized (this) {
			if (serviceSearchResult == 0) // search still in progress
				try {
					// wake up if serviceSearchCompleted() has been called
					this.wait();

				} catch (InterruptedException e) {

					Log.ln("[BT] interrupted");
					serviceSearchResult = SERVICE_SEARCH_TERMINATED;
					agent.cancelServiceSearch(searchID);
				}
		}

		// ////// check search result ////// //

		switch (serviceSearchResult) {

		case SERVICE_SEARCH_NO_RECORDS:

			throw new UserException("No Server",
					"Found no Remuco service on the selected device");

		case SERVICE_SEARCH_ERROR:

			throw new UserException("Bluetooth Error",
					"Search for Remuco service on the selected device failed.");

		case SERVICE_SEARCH_TERMINATED:

			serviceRecords.removeAllElements();

			return null; // communicator breaks if caused by an interruption

		case SERVICE_SEARCH_DEVICE_NOT_REACHABLE:

			return null; // this is ok (communicator will retry)

		case SERVICE_SEARCH_COMPLETED:
		default:
			break;
		}

		// if we are here, there should be a service we can connect to,
		// otherwise it is an error

		// ////// try to connect to one of the services ////// //

		Log.ln("[BT] found " + serviceRecords.size() + " services");

		for (enu = serviceRecords.elements(); enu.hasMoreElements();) {

			sc = null;

			sr = (ServiceRecord) enu.nextElement();

			url = sr.getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT,
					false);

			Log.ln("[BT] try service " + url);

			try {

				sc = (StreamConnection) Connector.open(url);

			} catch (SecurityException e) {

				throw new UserException("Security Error",
						"Not allowed to create a Bluetooth connection.");

			} catch (Exception e) {

				Log.ln("[BT] failed ", e);

				continue;
			}

			if (conn.up(sc)) {

				Log.ln("[BT] success");
				break;

			} else {

				Log.ln("[BT] failed");
				sc = null;
			}
		}

		serviceRecords.removeAllElements();

		if (!conn.isUp())
			throw new UserException("Connection Error",
					"Could not connect to the server. "
							+ "Please check if the server is running.");

		return conn;
	}

	public void inquiryCompleted(int arg0) {

		Log.asssertNotReached(this);
	}

	public void servicesDiscovered(int transId, ServiceRecord[] srs) {

		int n = srs.length;

		for (int i = 0; i < n; i++) {
			serviceRecords.addElement(srs[i]);
		}
	}

	public void serviceSearchCompleted(int transID, int respCode) {

		Log.ln("[BT] service search finished - return code: " + respCode);

		synchronized (this) {
			serviceSearchResult = respCode;
			this.notify();
		}
	}

}
