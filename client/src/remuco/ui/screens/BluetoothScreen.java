package remuco.ui.screens;

import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.ItemStateListener;
import javax.microedition.lcdui.TextField;

import remuco.comm.BluetoothDevice;

/** Screen to configure a Bluetooth connection. */
public class BluetoothScreen extends Form {

	/**
	 * Implement item state listener in this class because it is already
	 * implemented privately by {@link Form}.
	 */
	private class SearchSelectionChangeListener implements ItemStateListener {

		public void itemStateChanged(Item item) {
			if (item == cgSearch) {
				if (cgSearch.getSelectedIndex() == BluetoothDevice.SEARCH_MANUAL) {
					tfPort.setConstraints(TextField.ANY);
				} else {
					tfPort.setConstraints(TextField.UNEDITABLE);
				}
			}
		}
	}

	private final ChoiceGroup cgSearch;

	private final BluetoothDevice device;

	private final String SEARCH_CHOICES[] = { "Standard", "Failsafe", "Manual" };

	private final TextField tfAddr, tfPort, tfName;

	public BluetoothScreen() {
		this(new BluetoothDevice());
	}

	public BluetoothScreen(BluetoothDevice device) {

		super("Bluetooth");

		this.device = device;

		String label;

		// append(label);

		label = "Address (withput colons, leave empty to scan for)";
		tfAddr = new TextField(label, device.getAddress(), 256, TextField.URL);
		append(tfAddr);

		label = "Service search (change if standard search fails)";
		cgSearch = new ChoiceGroup(label, Choice.EXCLUSIVE, SEARCH_CHOICES,
				null);
		append(cgSearch);

		label = "Port (only relevant on manual service search)";
		tfPort = new TextField(label, device.getPort(), 256, TextField.URL);
		append(tfPort);

		label = "Name (optional)";
		tfName = new TextField(label, device.getName(), 256, TextField.ANY);
		append(tfName);

		setItemStateListener(new SearchSelectionChangeListener());
	}

	/**
	 * Apply user entered values to device and return that device. If a device
	 * has been passed to the constructor of this screen, then the same device
	 * will be returned here. Otherwise a new device will be returned.
	 */
	public BluetoothDevice getDevice() {

		device.setAddress(tfAddr.getString());
		device.setSearch(cgSearch.getSelectedIndex());
		device.setPort(tfPort.getString());
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
		final int search = cgSearch.getSelectedIndex();

		if (address.length() > 0 && address.length() != 12) {
			return "A Bluetooth address has exactly 12 characters!";
		}
		final char[] digits = address.toCharArray();
		for (int i = 0; i < digits.length; i++) {
			boolean good = false;
			good |= digits[i] >= '0' && digits[i] <= '9';
			good |= digits[i] >= 'a' && digits[i] <= 'f';
			good |= digits[i] >= 'A' && digits[i] <= 'F';
			if (!good) {
				return "Bluetooth address contains invalid characters!";
			}
		}

		if (search < 0) {
			return "Please specify a service search strategy!";
		}

		if (search == BluetoothDevice.SEARCH_MANUAL) {
			final int portInt;
			try {
				portInt = Integer.parseInt(port);
			} catch (NumberFormatException e) {
				return "Port must be empty or a number!";
			}
			if (portInt < 1 || portInt > 30) {
				return "Port number out of range (1-30)!";
			}
		}

		return null;

	}

}
