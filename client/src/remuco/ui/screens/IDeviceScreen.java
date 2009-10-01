package remuco.ui.screens;

import remuco.comm.Device;

/** Device screens are forms to configure a connection to a remote device. */
public interface IDeviceScreen {

	/**
	 * Apply user entered values to device and return that device. If a device
	 * has been passed to the constructor of this screen, then the same device
	 * will be returned here. Otherwise a new device will be returned.
	 */
	public Device getDevice();

	/**
	 * Validate the user input.
	 * 
	 * @return <code>null</code> if user input is ok, otherwise a string message
	 *         describing what's wrong
	 */
	public String validate();

}
