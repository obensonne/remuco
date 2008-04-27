package remuco.comm;

import remuco.UserException;
import remuco.util.Tools;

public final class ConnectorFactory {

	/**
	 * Creates a new connector for the given device address.
	 * 
	 * @param device
	 *            a device address, which can be a Bluetooth hardware address,
	 *            an IPv4 address or a IP host name
	 * @return
	 * @throws UserException
	 *             if a serious error occurred
	 */
	public static IConnector createConnector(String device)
			throws UserException {

		if (isBluetoothAddress(device))
			return new BluetoothConnector(device);

		if (isIPv4Address(device))
			return new IPConnector(device);

		if (device == null || device.length() == 0)
			throw new IllegalArgumentException("invalid device address");

		// if here, we assume it is an IP host name

		return new IPConnector(device);

	}

	private static boolean isBluetoothAddress(String addr) {

		if (addr == null)
			return false;

		if (addr.length() != 12)
			return false;

		try {
			Long.parseLong(addr, 16);
		} catch (NumberFormatException e) {
			return false;
		}

		return true;
	}

	private static boolean isIPv4Address(String addr) {

		String[] numbers;
		int l;

		if (addr == null)
			return false;

		numbers = Tools.splitString(addr, ".");

		if (numbers.length != 4)
			return false;

		for (int i = 0; i < 4; i++) {
			l = numbers[i].length();
			if (l < 1 && l > 3)
				return false;
			try {
				Integer.parseInt(numbers[i]);
			} catch (NumberFormatException e) {
				return false;
			}
		}

		return true;

	}

}
