package remuco.comm;

import java.util.Hashtable;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DataElement;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;

import remuco.UserException;
import remuco.util.Log;

public final class BluetoothServiceFinder implements DiscoveryListener,
		IServiceFinder {

	private class BTD extends RemoteDevice {

		public BTD(String addr) {
			super(addr);
		}

	}

	/** Service name in default language. */
	private static final int ATTRIBUTE_NAME = 0x0100;

	private static final int ATTRIBUTE_LIST[] = new int[] { ATTRIBUTE_NAME };

	private static final String DEFAULT_SERVICE_NAME = "NoName";

	private static final int SECURITY = ServiceRecord.NOAUTHENTICATE_NOENCRYPT;

	/** Remuco service UUID */
	private final static String UUID = "025fe2ae07624bed90f2d8d778f020fe";

	private static final UUID[] UUID_LIST = new UUID[] { new UUID(UUID, false) };

	/**
	 * Get the name of a service (the player behind the remuco service).
	 * 
	 * @param sr
	 *            the service to get the name from
	 * @return the name or {@value #DEFAULT_SERVICE_NAME} if name is not
	 *         available;
	 */
	private static String getServiceName(ServiceRecord sr) {

		DataElement de = sr.getAttributeValue(ATTRIBUTE_NAME);

		if (de == null) {
			Log.ln("failed to get service name");
			return DEFAULT_SERVICE_NAME;
		}

		try {
			return (String) de.getValue();
		} catch (ClassCastException e) {
			Log.ln("failed to get service name", e);
			return DEFAULT_SERVICE_NAME;
		}

	}

	private DiscoveryAgent agent = null;

	private LocalDevice localDevice = null;

	private final Hashtable services;

	/** ID of the current service search or -1 if no search is active. */
	private int serviceSearchID = -1;

	private IServiceListener listener = null;

	public BluetoothServiceFinder() {

		services = new Hashtable();

	}

	public void cancelServiceSearch() {

		synchronized (services) {

			if (serviceSearchID > -1) {

				agent.cancelServiceSearch(serviceSearchID);
				serviceSearchID = -1;

				// listener will be set to null in 'serviceSearchCompleted()'

			}
		}
	}

	public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod) {
		// unused
	}

	public void findServices(String addr, IServiceListener listener)
			throws UserException {

		synchronized (services) {

			if (serviceSearchID > -1)
				return;

			this.listener = listener;

			services.clear();
			
			initBluetooth();

			final BTD btd = new BTD(addr);

			try {
				serviceSearchID = agent.searchServices(ATTRIBUTE_LIST,
						UUID_LIST, btd, this);

			} catch (BluetoothStateException e) {

				Log.ln("[BT] BluetoothStateException", e);
				throw new UserException("Bluetooth Error",
						"Bluetooth seems to be busy. Cannot search for Remuco services right now.");

			} catch (NullPointerException e) {
				// WTK emulator throws a NPExc when there are no services
				return;
			}

		}
	}

	public void inquiryCompleted(int discType) {
		// unused
	}

	public void servicesDiscovered(int transId, ServiceRecord[] srs) {

		synchronized (services) {

			for (int i = 0; i < srs.length; i++) {

				String url = srs[i].getConnectionURL(SECURITY, false);

				String name = getServiceName(srs[i]);

				services.put(name, url); // assuming names are unique
			}
		}
	}

	public void serviceSearchCompleted(int transID, int respCode) {

		Log.ln("[BT] service search finished (" + respCode + ")");

		synchronized (services) {

			serviceSearchID = -1;

			switch (respCode) { // check search result

			case SERVICE_SEARCH_NO_RECORDS:

				listener.notifyServices(null, new UserException("No Service",
						"Found no Remuco service on the selected device"));
				break;

			case SERVICE_SEARCH_ERROR:

				listener
						.notifyServices(
								null,
								new UserException("Bluetooth Error",
										"Search for Remuco service on the selected device failed."));
				break;

			case SERVICE_SEARCH_TERMINATED:

				// nothing to do
				break;

			case SERVICE_SEARCH_DEVICE_NOT_REACHABLE:

				listener
						.notifyServices(null, new UserException(
								"Bluetooth Error",
								"Selected device is not reachable."));
				break;

			case SERVICE_SEARCH_COMPLETED:

				listener.notifyServices(services, null);
				break;

			default:

				listener.notifyServices(services, null);
				break;

			}

			listener = null;

		}
	}

	private void initBluetooth() throws UserException {


		if (localDevice == null) {
			try {
				localDevice = LocalDevice.getLocalDevice();

			} catch (BluetoothStateException e) {

				Log.ln("[BT] failed to get local device", e);
				throw new UserException("Bluetooth Error",
						"Bluetooth seems to be off.");
			}
		}

		if (agent == null) {
			agent = localDevice.getDiscoveryAgent();
		}

	}

}
