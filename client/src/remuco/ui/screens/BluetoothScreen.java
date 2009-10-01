package remuco.ui.screens;

import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.ItemStateListener;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextField;

import remuco.comm.BluetoothDevice;
import remuco.comm.Device;

/** Screen to configure a Bluetooth connection. */
public class BluetoothScreen extends Form implements IDeviceScreen {

	/**
	 * Implement item state listener in this class because it is already
	 * implemented privately by {@link Form}.
	 */
	private class SearchSelectionChangeListener implements ItemStateListener {

		public void itemStateChanged(Item item) {
			if (item == cgSearch) {
				if (cgSearch.getSelectedIndex() == BluetoothDevice.SEARCH_MANUAL) {
					tfPort.setConstraints(PORT_ON);
				} else {
					tfPort.setConstraints(PORT_OFF);
				}
			}
		}
	}

	/** Text field constraints for port (editable). */
	private static final int PORT_ON = TextField.NUMERIC;

	/** Text field constraints for port (uneditable). */
	private static final int PORT_OFF = PORT_ON | TextField.UNEDITABLE;

	private static final String WELCOME_1 = "In most cases just pressing OK here is fine.",
			WELCOME_2 = "The fields below are only relevant if automatic connection setup fails.";

	private final ChoiceGroup cgSearch;

	private final BluetoothDevice device;

	private final String SEARCH_CHOICES[] = { "Standard", "Failsafe", "Manual" };

	private final TextField tfAddr, tfPort, tfName;

	public BluetoothScreen(BluetoothDevice device) {

		super("Bluetooth");

		this.device = device;

		if (device.getAddress().length() == 0) {
			final StringItem si = new StringItem(WELCOME_1, WELCOME_2);
			si.setLayout(Item.LAYOUT_CENTER);
			append(si);
		}

		String label;

		if (device.getAddress().length() == 0) {
			label = "Address (without colons, leave empty to scan for)";
		} else {
			label = "Address (without colons)";
		}
		tfAddr = new TextField(label, device.getAddress(), 256, TextField.URL);
		append(tfAddr);

		label = "Service search (change if standard search fails)";
		cgSearch = new ChoiceGroup(label, Choice.EXCLUSIVE, SEARCH_CHOICES,
				null);
		append(cgSearch);

		label = "Port (for manual service search)";
		tfPort = new TextField(label, device.getPort(), 256, PORT_OFF);
		if (device.getSearch() == BluetoothDevice.SEARCH_MANUAL) {
			tfPort.setConstraints(PORT_ON);
		}
		append(tfPort);

		label = "Name (optional)";
		tfName = new TextField(label, device.getName(), 256, TextField.ANY);
		append(tfName);

		setItemStateListener(new SearchSelectionChangeListener());

	}

	public Device getDevice() {

		device.setAddress(tfAddr.getString());
		device.setSearch(cgSearch.getSelectedIndex());
		device.setPort(tfPort.getString());
		device.setName(tfName.getString());

		return device;
	}

	public String validate() {

		final String address = tfAddr.getString();
		final String port = tfPort.getString();
		final int search = cgSearch.getSelectedIndex();

		if ((device.getAddress().length() > 0 || address.length() > 0)
				&& address.length() != 12) {
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
				return "Port must be a number!";
			}
			if (portInt < 1 || portInt > 30) {
				return "Port number out of range (1-30)!";
			}
		}

		return null;

	}

}
