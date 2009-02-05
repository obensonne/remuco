package remuco.comm;

import java.util.Hashtable;

import remuco.UserException;

/**
 * Interface for service listener.
 * 
 * @author Oben Sonne
 * 
 * @see IServiceFinder
 *
 */
public interface IServiceListener {

	/**
	 * Notify a service listener that services has been found.
	 * 
	 * @param services
	 *            a hash table with service names as keys and service connection
	 *            URLs as values
	 * @param ex
	 *            <code>null</code> if service search was successful, otherwise
	 *            a description of the occurred error (in that case,
	 *            <i>services</i> is <code>null</code>)
	 */
	public void notifyServices(Hashtable services, UserException ex);

}
