package remuco.comm;


/**
 * Interface for classes interested in device scan results.
 * 
 * @see IScanner#startScan(IScanListener)
 * 
 * @author Oben Sonne
 * 
 */
public interface IScanListener {

	/**
	 * Hands out the found devices.
	 * 
	 * @param devices
	 *            the found devices, where element <code>3 * i</code> is the
	 *            address of device <code>i</code>, element
	 *            <code>3 * i + 1</code> its name and element
	 *            <code>3 * i + 2</code> its type
	 */
	public void notifyScannedDevices(String[] devices);

}
