package remuco.comm;

import remuco.Config;

public interface IDeviceSelectionListener {

	/**
	 * Notify a device selection.
	 * 
	 * @param type
	 *            the type of the selected device
	 * @param addr
	 *            the address of the selected device
	 * 
	 * @see Config#DEVICE_TYPE_BLUETOOTH
	 * @see Config#DEVICE_TYPE_INET
	 */
	public void notifySelectedDevice(String type, String addr);

}
