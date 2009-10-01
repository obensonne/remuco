package remuco.comm;

import remuco.util.Tools;

public class WifiDevice extends Device {

	private static final String PORT = "34271";

	private String address;

	private String name;

	private String options;

	private String port;

	public WifiDevice() {
		address = "";
		port = PORT;
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

		if (sa.length != 5 || sa[0].charAt(0) != TYPE_WIFI) {
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
		if (name.length() > 0) {
			return name;
		} else {
			return address + ":" + port;
		}
	}

	public String getOptions() {
		return options;
	}

	public String getPort() {
		return port;
	}

	public char getType() {
		return TYPE_WIFI;
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

		sb.append(TYPE_WIFI);
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
