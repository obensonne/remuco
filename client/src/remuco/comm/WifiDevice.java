package remuco.comm;

import remuco.util.Tools;

public class WifiDevice extends Device {

	/**
	 * Char indicating the device type in a flattened representation of this
	 * device. This char is the first character in a flattened representation.
	 */
	public static final char TYPE_CHAR = 'W';

	private String address;

	private String name;

	private String options;

	private String port;

	public WifiDevice() {
		address = "";
		port = InetServiceFinder.PORT;
		options = "";
		port = "";
	}

	/**
	 * Create a new device based on a flattened device.
	 * 
	 * @param flat
	 *            flattened device
	 * @throws IllegalArgumentException
	 *             if <em>flat</em> is malformed
	 */
	public WifiDevice(String flat) throws IllegalArgumentException {

		String sa[];

		sa = Tools.splitString(flat, FIELD_SEP, false);

		if (sa.length != 5 || sa[0].charAt(0) != TYPE_CHAR) {
			throw new IllegalArgumentException();
		}

		address = sa[1];

		port = sa[2];
		try {
			Integer.parseInt(port);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException();
		}

		options = sa[3];
		name = sa[4];
	}

	/** Compares 2 device based on address and port. */
	public boolean equals(Object obj) {

		if (obj == this) {
			return true;
		}

		if (obj == null) {
			return false;
		}

		if (!(obj instanceof WifiDevice)) {
			return false;
		}

		final WifiDevice other = (WifiDevice) obj;

		return other.address.equals(address) && other.port.equals(port);

	}

	public String getAddress() {
		return address;
	}

	public String getName() {
		return name;
	}

	public String getOptions() {
		return options;
	}

	public String getPort() {
		return port;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setOptions(String options) {
		this.options = options;
	}

	public void setPort(String port) {
		this.port = port;
	}

	/** Create flat representation of this device. */
	public String toString() {

		final StringBuffer sb = new StringBuffer();

		sb.append(TYPE_CHAR);
		sb.append(FIELD_SEP);
		sb.append(address);
		sb.append(FIELD_SEP);
		sb.append(port);
		sb.append(FIELD_SEP);
		sb.append(options);
		sb.append(FIELD_SEP);
		sb.append(name);

		return sb.toString();
	}

}
