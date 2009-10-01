package remuco.ui.screens;

import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.TextField;

import remuco.comm.WifiDevice;

/** Screen to configure a Bluetooth connection. */
public class WifiScreen extends Form {

	private final WifiDevice device;

	private final TextField tfAddr, tfPort, tfOptions, tfName;

	public WifiScreen() {
		this(new WifiDevice());
	}

	public WifiScreen(WifiDevice device) {

		super("Bluetooth");

		this.device = device;

		String label;

		label = "Host or IP address:";
		tfAddr = new TextField(label, device.getAddress(), 256, TextField.URL);
		append(tfAddr);

		label = "Port (for manual service search)";
		tfPort = new TextField(label, device.getPort(), 256, TextField.NUMERIC);
		append(tfPort);

		label = "Options (if unsure, leave empty)";
		tfOptions = new TextField(label, device.getName(), 256, TextField.URL);
		append(tfOptions);

		label = "Name (optional)";
		tfName = new TextField(label, device.getName(), 256, TextField.ANY);
		append(tfName);
	}

	/**
	 * Apply user entered values to device and return that device. If a device
	 * has been passed to the constructor of this screen, then the same device
	 * will be returned here. Otherwise a new device will be returned.
	 */
	public WifiDevice getDevice() {

		device.setAddress(tfAddr.getString());
		device.setPort(tfPort.getString());
		device.setOptions(tfOptions.getString());
		device.setName(tfName.getString());

		return device;
	}

	/**
	 * Validate the user input.
	 * 
	 * @return <code>null</code> if user input is ok, otherwise a string message
	 *         describing what's wrong
	 */
	public String validate() {

		final String address = tfAddr.getString();
		final String port = tfPort.getString();

		if (address.length() == 0) {
			return "Need a host name or IP address!";
		}

		final int portInt;
		try {
			portInt = Integer.parseInt(port);
		} catch (NumberFormatException e) {
			return "Port must be a number!";
		}
		if (portInt < 1 || portInt > 65536) {
			return "Port number out of range (1-65536)!";
		}

		return null;

	}

}
