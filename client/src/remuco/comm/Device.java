package remuco.comm;

import remuco.util.Tools;

public class Device {

	/** Char to separate devices in a flattened device list. */
	public static final char LIST_SEP = '\n';

	/** Device type. */
	public static final int WIFI = 0, BLUETOOTH = 1;

	/** Char to separate fields in of a flattened device. */
	private static final char FIELD_SEP = '|';

	/** Address (host + port + options) of this device. */
	public final String address;

	/** Name of this device. */
	public final String name;

	/** Type of this device, one of {@link #WIFI} or {@link #BLUETOOTH}. */
	public final int type;

	/**
	 * Create a new device
	 * 
	 * @param type
	 *            one of {@link #WIFI} or {@link #BLUETOOTH}
	 * @param address
	 *            address string (host + port + options)
	 * @param name
	 *            optional name (use empty string if not known)
	 */
	public Device(int type, String address, String name) {
		this.type = type;
		this.address = address;
		this.name = name;
	}

	/**
	 * Create a device from a flat representation.
	 * 
	 * @param flat
	 *            flattened device (as returned by {@link #toString()})
	 * @throws IllegalArgumentException
	 *             if the flat string is malformed
	 */
	public Device(String flat) throws IllegalArgumentException {

		String sa[];

		sa = Tools.splitString(flat, FIELD_SEP, false);

		if (sa.length != 3) {
			throw new IllegalArgumentException();
		}

		try {
			type = Integer.parseInt(sa[0]);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException();
		}

		address = sa[1];
		name = sa[2];
	}

	public boolean equals(Object obj) {

		if (obj == this) {
			return true;
		}

		if (obj == null) {
			return false;
		}

		if (!(obj instanceof Device)) {
			return false;
		}

		final Device other = (Device) obj;

		return other.type == type && other.address.equals(address);

	}

	public String toString() {

		final StringBuffer sb = new StringBuffer();

		sb.append(type);
		sb.append(FIELD_SEP);
		sb.append(address);
		sb.append(FIELD_SEP);
		sb.append(name);

		return sb.toString();
	}

}
