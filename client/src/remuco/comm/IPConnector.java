package remuco.comm;

import javax.microedition.io.ConnectionNotFoundException;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;

import remuco.UserException;
import remuco.util.Log;

public final class IPConnector implements IConnector {

	private static final String PORT = "34271";

	private final String addr, url;

	/**
	 * Pool the connection object.
	 */
	private final Connection conn;

	protected IPConnector(String addr) {

		this.addr = addr;

		if (addr.indexOf(':') >= 0)
			url = "socket://" + addr;
		else
			url = "socket://" + addr + ":" + PORT;

		conn = new Connection();

		Log.ln("My hostname: " + System.getProperty("microedition.hostname"));

	}

	public Connection getConnection() throws UserException {

		StreamConnection sc;

		try {

			Log.ln("[IP] try to connect to " + addr);
			sc = (StreamConnection) Connector.open(url);

		} catch (IllegalArgumentException e) {

			throw new UserException("Error", addr
					+ "is not a valid device address." + " (" + e.getMessage()
					+ ")");

		} catch (ConnectionNotFoundException e) {

			// this happens if the host is not reachable or if the server is not
			// running
			throw new UserException("Connection Error", "Could not connect to "
					+ addr + " (" + e.getMessage()
					+ "). Please check if the server is running.");

		} catch (SecurityException e) {

			throw new UserException("Security Error",
					"Not allowed to create an IP connection (" + e.getMessage()
							+ ").");

		} catch (Exception e) {

			// TODO: what kind of errors go here ?
			Log.ln("[IP] failed", e);
			return null;

		}

		if (conn.up(sc)) {
			Log.ln("[IP] success");
			return conn;
		} else {
			Log.ln("[IP] failed");
			return null;
		}
	}

}
