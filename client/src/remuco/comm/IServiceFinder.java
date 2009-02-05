package remuco.comm;

import remuco.UserException;

/**
 * A finder searching for Remuco services.
 * 
 * @author Oben Sonne
 * 
 */
public interface IServiceFinder {

	/**
	 * Cancel a currently active service search.
	 */
	public void cancelServiceSearch();

	/**
	 * Find all available Remuco services on a device.
	 * 
	 * @param addr
	 *            address of the device to search for services
	 * @param listener
	 *            the {@link IServiceListener} to notify if service search is
	 *            finished
	 * @throws UserException
	 *             if there was an error in starting the search
	 */
	public void findServices(String addr, IServiceListener listener)
			throws UserException;

}