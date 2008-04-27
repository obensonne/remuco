package remuco.comm;

import remuco.UserException;

public interface IConnector {

	/**
	 * Get the connection to this device. This method <i>blocks</i> until
	 * connection creation has been finished or failed.
	 * 
	 * @return the created connection or <code>null</code>, if connecting
	 *         failed for now but could succeed a later time
	 * @throws UserException
	 *             if connecting failed and it does not make much sense to retry
	 */
	public Connection getConnection() throws UserException;

}
