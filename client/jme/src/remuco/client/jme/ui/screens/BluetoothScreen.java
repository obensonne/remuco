/*   
 *   Remuco - A remote control system for media players.
 *   Copyright (C) 2006-2009 by the Remuco team, see AUTHORS.
 *
 *   This file is part of Remuco.
 *
 *   Remuco is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Remuco is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Remuco.  If not, see <http://www.gnu.org/licenses/>.
 *   
 */
package remuco.client.jme.ui.screens;

import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.ItemStateListener;
import javax.microedition.lcdui.TextField;

import remuco.client.jme.io.BluetoothDevice;
import remuco.client.jme.io.IDevice;

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
					tfChan.setConstraints(CHAN_ON);
				} else {
					tfChan.setConstraints(CHAN_OFF);
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

	private final static String CHAN_CHOICES[] = { "Search for",
			"Search for (failsafe)", "Set manually" };

	/** Text field constraints for channel (uneditable). */
	private static final int CHAN_OFF = TextField.NUMERIC
			| TextField.UNEDITABLE;

	/** Text field constraints for channel (editable). */
	private static final int CHAN_ON = TextField.NUMERIC;

	private static final int SEC_AUTHENTICATE_INDEX = 0;

	private final static String SEC_CHOICES[] = { "Authenticate", "Encrypt" };

	private static final int SEC_ENCRYPT_INDEX = 1;

	private final ChoiceGroup cgScan, cgSearch, cgSecurity;

	private final BluetoothDevice device;

	private final TextField tfAddr, tfChan, tfName;

	/** Indicator if this screen configures a new or existing device. */
	private final boolean virgin;

	public BluetoothScreen(BluetoothDevice device) {

		super("Bluetooth");

		this.device = device;

		virgin = device.getAddress().length() == 0;

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
			label = null;
			value = "001122AABBCC";
			constraints = ADDR_OFF;
		} else {
			label = "Address";
			value = device.getAddress();
			constraints = ADDR_ON;
		}
		tfAddr = new TextField(label, value, 256, constraints);
		append(tfAddr);

		if (virgin) {
			append("When scanning, ensure your computer's Bluetooth is in "
					+ "visible mode.");
		}

		// search type //

		label = "Channel";
		cgSearch = new ChoiceGroup(label, Choice.EXCLUSIVE, CHAN_CHOICES, null);
		cgSearch.setSelectedIndex(device.getSearch(), true);
		if (!virgin) {
			append(cgSearch);
		}

		// channel //

		label = null;
		if (device.getSearch() == BluetoothDevice.SEARCH_MANUAL) {
			constraints = CHAN_ON;
		} else {
			constraints = CHAN_OFF;
		}
		tfChan = new TextField(label, device.getChan(), 256, constraints);
		if (!virgin) {
			append(tfChan);
		}

		// security //

		label = "Security";
		cgSecurity = new ChoiceGroup(label, Choice.MULTIPLE, SEC_CHOICES, null);
		cgSecurity.setSelectedIndex(SEC_AUTHENTICATE_INDEX,
			device.isAuthenticate());
		cgSecurity.setSelectedIndex(SEC_ENCRYPT_INDEX, device.isEncrypt());
		if (!virgin) {
			append(cgSecurity);
		}

		// name //

		label = "Name (optional):";
		tfName = new TextField(label, device.getName(), 256, TextField.ANY);
		if (!virgin) {
			append(tfName);
		}

		setItemStateListener(new SearchSelectionChangeListener());

	}

	public IDevice getDevice() {

		if (!virgin || cgScan.getSelectedIndex() == ADDR_TYPE_MANUAL) {
			device.setAddress(tfAddr.getString());
		}
		device.setSearch(cgSearch.getSelectedIndex());
		device.setPort(tfChan.getString());
		device.setName(tfName.getString());
		device.setAuthenticate(cgSecurity.isSelected(SEC_AUTHENTICATE_INDEX));
		device.setEncrypt(cgSecurity.isSelected(SEC_ENCRYPT_INDEX));

		return device;
	}

	public String validate() {

		final String address = tfAddr.getString();
		final String chan = tfChan.getString();
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
			final int chanInt;
			try {
				chanInt = Integer.parseInt(chan);
			} catch (NumberFormatException e) {
				return "Channel must be a number!";
			}
			if (chanInt < 1 || chanInt > 30) {
				return "Channel number out of range (1-30)!";
			}
		} else if (chan.length() == 0) {
			// not a problem now, but may be later, let's fix it silently:
			tfChan.setString("1");
		}

		return null;

	}
}
