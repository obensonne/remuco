package remuco.ui.screens;

import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.ItemStateListener;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextField;

import remuco.comm.BluetoothDevice;
import remuco.comm.IDevice;

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
			} else if (item == cgScan) {
				if (cgScan.getSelectedIndex() == ADDR_TYPE_MANUAL) {
					tfAddr.setConstraints(ADDR_ON);
				} else {
					tfAddr.setConstraints(ADDR_OFF);
				}
			} else if (item == cgSecurity) {
				if (cgSecurity.isSelected(SEC_ENCRYPT_INDEX)) {
					cgSecurity.setSelectedIndex(SEC_AUTHENTICATE_INDEX, true);
				}
			}
		}
	}

	private static final String ADDR_CHOICES[] = { "Scan for", "Set manually" };
	/** Text field constraints for address (uneditable). */
	private static final int ADDR_OFF = TextField.URL | TextField.UNEDITABLE;

	/** Text field constraints for address (editable). */
	private static final int ADDR_ON = TextField.URL;

	/** Scan strategy. */
	private static final int ADDR_TYPE_SCAN = 0, ADDR_TYPE_MANUAL = 1;

	private final static String PORT_CHOICES[] = { "Search for",
			"Search for (failsafe)", "Set manually" };

	/** Text field constraints for port (uneditable). */
	private static final int PORT_OFF = TextField.NUMERIC
			| TextField.UNEDITABLE;

	/** Text field constraints for port (editable). */
	private static final int PORT_ON = TextField.NUMERIC;

	private static final int SEC_AUTHENTICATE_INDEX = 0;

	private final static String SEC_CHOICES[] = { "Authenticate", "Encrypt" };

	private static final int SEC_ENCRYPT_INDEX = 1;

	/** Welcome message to show on new devices. */
	private static final String WELCOME = "In most cases just pressing OK "
			+ "here is fine. Tweak the fields below if automatic address and "
			+ "port search fails.";

	private final ChoiceGroup cgScan, cgSearch, cgSecurity;

	private final BluetoothDevice device;

	private final TextField tfAddr, tfPort, tfName;

	/** Indicator if this screen configures a new or existing device. */
	private final boolean virgin;

	public BluetoothScreen(BluetoothDevice device) {

		super("Bluetooth");

		this.device = device;

		virgin = device.getAddress().length() == 0;

		if (virgin) {
			final StringItem si = new StringItem(null, WELCOME);
			si.setLayout(Item.LAYOUT_CENTER);
			append(si);
		}

		String label, value;
		int constraints;

		// scan type //

		if (virgin) {
			label = "Address";
			cgScan = new ChoiceGroup(label, Choice.EXCLUSIVE, ADDR_CHOICES,
					null);
			cgScan.setSelectedIndex(ADDR_TYPE_SCAN, true);
			append(cgScan);
		} else {
			cgScan = null;
		}

		// address //

		if (virgin) {
			label = "Manual address:";
			value = "001122AABBCC";
			constraints = ADDR_OFF;
		} else {
			label = "Address";
			value = device.getAddress();
			constraints = ADDR_ON;
		}
		tfAddr = new TextField(label, value, 256, constraints);
		append(tfAddr);

		// search type //

		label = "Port";
		cgSearch = new ChoiceGroup(label, Choice.EXCLUSIVE, PORT_CHOICES, null);
		cgSearch.setSelectedIndex(device.getSearch(), true);
		append(cgSearch);

		// port //

		label = "Manual port:";
		if (device.getSearch() == BluetoothDevice.SEARCH_MANUAL) {
			constraints = PORT_ON;
		} else {
			constraints = PORT_OFF;
		}
		tfPort = new TextField(label, device.getPort(), 256, constraints);
		append(tfPort);

		// security //

		label = "Security";
		cgSecurity = new ChoiceGroup(label, Choice.MULTIPLE, SEC_CHOICES, null);
		cgSecurity.setSelectedIndex(SEC_AUTHENTICATE_INDEX,
			device.isAuthenticate());
		cgSecurity.setSelectedIndex(SEC_ENCRYPT_INDEX, device.isEncrypt());
		append(cgSecurity);

		// name //

		label = "Name (optional):";
		tfName = new TextField(label, device.getName(), 256, TextField.ANY);
		append(tfName);

		setItemStateListener(new SearchSelectionChangeListener());

	}

	public IDevice getDevice() {

		if (!virgin || cgScan.getSelectedIndex() == ADDR_TYPE_MANUAL) {
			device.setAddress(tfAddr.getString());
		}
		device.setSearch(cgSearch.getSelectedIndex());
		device.setPort(tfPort.getString());
		device.setName(tfName.getString());
		device.setAuthenticate(cgSecurity.isSelected(SEC_AUTHENTICATE_INDEX));
		device.setEncrypt(cgSecurity.isSelected(SEC_ENCRYPT_INDEX));

		return device;
	}

	public String validate() {

		final String address = tfAddr.getString();
		final String port = tfPort.getString();
		final int search = cgSearch.getSelectedIndex();

		if (!virgin || cgScan.getSelectedIndex() == ADDR_TYPE_MANUAL) {
			if (address.length() != 12) {
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
		} else if (port.length() == 0) {
			// not a problem now, but may be later, let's fix it silently:
			tfPort.setString("1");
		}

		return null;

	}
}
