package remuco.comm;

public interface IDevice {

	/** Char to separate fields in of a flattened device. */
	public static final char FIELD_SEP = '|';

	/** Char to separate devices in a flattened device list. */
	public static final char LIST_SEP = '\n';

	public static final char TYPE_BLUETOOTH = 'B';

	public static final char TYPE_WIFI = 'W';

	/** Get a descriptive name of the device. */
	public String getLabel();

	/**
	 * Get the device type, one of {@link #TYPE_WIFI} or {@link #TYPE_BLUETOOTH}
	 * . This must be the first character in a flattened representation of the
	 * device.
	 */
	public char getType();

}
