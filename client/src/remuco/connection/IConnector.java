package remuco.connection;

import javax.microedition.lcdui.Display;

/**
 * A connector is responsible for creating a connection (if needed, by
 * interacting with the user).<br>
 * Creating a connection with a connector might look like this:
 * 
 * <pre>
 *  Displaye display = ...;
 *  GenericStreamConnection connection;
 *  IConnector connector = new ...;
 *  if (connector.init(display)) {
 *      connection = connector.getConnection();
 *      synchronized (connection) {
 *          connector.createConnection();
 *          try {
 *              connection.wait();
 *              if (connection.isOpen()) {
 *                  // :-) .. let's go
 *              } else {
 *                  // :-/ .. connection creation failed
 *              }
 *          } catch (InterruptedException e) {
 *              // :-/ .. connection creation failed
 *          }
 *      }
 *  }
 * </pre>
 * 
 * @see remuco.connection.GenericStreamConnection
 * @author Christian Buennig
 * 
 */
public interface IConnector {

	public static final String APP_PROP_CONNTYPE = "remuco-connection";

	public static final String CONNTYPE_BLUETOOTH = "bluetooth";

	public static final int RETURN_ERROR = 9;

	public static final int RETURN_OK = 2;

	public static final int RETURN_USER_CANCEL = 1;

	/**
	 * Creates a connection (in a new Thread). If creation has finished a
	 * <code>notify()</code> is called on the connection (see
	 * {@link #getConnection()}) which than can be used. So the calling thread
	 * should do a <code>wait()</code> on the connection after calling this
	 * method.
	 * 
	 */
	public void createConnection();

	/**
	 * Returns the GenericStreamConnection created (<i>or to create!</i>) by
	 * the IConnector<br>
	 * The returned object is also the mutex to wait on, when calling
	 * {@link #createConnection()}. If a connection has been established or
	 * something has failed, a <code>notify()</code> is called on the
	 * connection.
	 */
	public GenericStreamConnection getConnection();

	public int getReturnCode();

	public String getUserMsg();

	/**
	 * Init the connector. After the initialization, the connection object
	 * reference is not null and does not change anymore.
	 * 
	 * @param d
	 *            the display to use for user interaction
	 * 
	 * @return true if initialization has been successfull, false otherwise
	 */
	public boolean init(Display d);

}