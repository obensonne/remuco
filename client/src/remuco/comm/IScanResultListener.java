package remuco.comm;

/**
 * Interface for classes searching devices using the {@link BluetoothScanner}.
 * 
 * @author Christian Buennig
 * 
 */
public interface IScanResultListener {

	/**
	 * Hands out the found devices. The string array <code>devices</code> is
	 * formated as follows:<br>
	 * 
	 * Let <code>n</code> be the number of found devices and
	 * <code>0 < i < n</code>. Then <code>devices[2*i]</code> is the
	 * address of the <code>i</code>th device and <code>devices[2*i+1]</code>
	 * is the name of the <code>i</code>th device or <code>null</code> if
	 * the name could net get detected.
	 * 
	 * @param devices
	 *            the found devices
	 * 
	 * @see BluetoothScanner#startScan(IDeviceSearcher)
	 */
	public void scanFinished(String[] devices);

}
