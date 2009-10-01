package remuco.comm;

import remuco.util.Tools;

public class BluetoothDevice {

	/** Service search strategy. */
	public static final int SEARCH_STANDARD = 0, SEARCH_FAILSAFE = 1,
			SEARCH_MANUAL = 2;

	/**
	 * Char indicating the device type in a flattened representation of this
	 * device. This char is the first character in a flattened representation.
	 */
	public static final char TYPE_CHAR = 'B';

	/** Char to separate fields in of a flattened device. */
	private static final char FIELD_SEP = '|';

	private static final int SEARCH_TYPE_LAST = SEARCH_MANUAL;

	private String address;

	private String name;

	private String port;

	private int search;

	public BluetoothDevice() {
		address = "";
		search = SEARCH_STANDARD;
		port = "1";
		name = "";
	}

	/**
	 * Create a new device based on a flattened device.
	 * 
	 * @param flat
	 *            flattened device
	 * @throws IllegalArgumentException
	 *             if <em>flat</em> is malformed
	 */
	public BluetoothDevice(String flat) throws IllegalArgumentException {

		String sa[];

		sa = Tools.splitString(flat, FIELD_SEP, false);

		if (sa.length != 5) {
			throw new IllegalArgumentException();
		}

		address = sa[1];

		try {
			search = Integer.parseInt(sa[2]);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException();
		}
		if (search < 0 && search > SEARCH_TYPE_LAST) {
			throw new IllegalArgumentException();
		}

		port = sa[3];
		try {
			Integer.parseInt(port);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException();
		}

		name = sa[4];
	}

	/** Compares 2 device based solely on its address. */
	public boolean equals(Object obj) {

		if (obj == this) {
			return true;
		}

		if (obj == null) {
			return false;
		}

		if (!(obj instanceof BluetoothDevice)) {
			return false;
		}

		final BluetoothDevice other = (BluetoothDevice) obj;

		return other.address.equals(address);
	}

	public String getAddress() {
		return address;
	}

	public String getName() {
		return name;
	}

	public String getPort() {
		return port;
	}

	public int getSearch() {
		return search;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public void setSearch(int search) {
		this.search = search;
	}

	/** Create flat representation of this device. */
	public String toString() {

		final StringBuffer sb = new StringBuffer();

		sb.append(TYPE_CHAR);
		sb.append(FIELD_SEP);
		sb.append(address);
		sb.append(FIELD_SEP);
		sb.append(search);
		sb.append(FIELD_SEP);
		sb.append(port);
		sb.append(FIELD_SEP);
		sb.append(name);

		return sb.toString();
	}

}
