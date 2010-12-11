package remuco.client.android.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.UnknownHostException;

import remuco.client.common.UserException;
import remuco.client.common.io.Connection;
import remuco.client.common.io.ISocket;

/**
 * Wrapper for a regular socket to be used as an {@link ISocket} a
 * {@link Connection} object.
 */
public class WifiSocket implements ISocket {

	public static final int PORT_DEFAULT = 34271;

	private final InputStream is;

	private final OutputStream os;

	private final java.net.Socket sock;

	/**
	 * Create a new TCP client socket for the given host and port.
	 * 
	 * @param host
	 *            host name or IP address
	 * @param port
	 *            port number
	 * @throws UserException
	 *             if setting up the socket and connection fails
	 */
	public WifiSocket(String host, int port) throws UserException {

		try {
			this.sock = new java.net.Socket(host, port);
		} catch (UnknownHostException e) {
			throw new UserException("Connection Error",
					"Given host name is unknown.");
		} catch (SecurityException e) {
			throw new UserException("Connection Error",
					"Not allowed to connect.");
		} catch (IOException e) {
			throw new UserException("Connection Error",
					"IO error while setting up the connection");
		}

		try {
			is = sock.getInputStream();
		} catch (IOException e) {
			try {
				sock.close();
			} catch (IOException e1) {
			}
			throw new UserException("Connecting failed",
					"IO Error while opening streams.", e);
		}

		try {
			os = sock.getOutputStream();
		} catch (IOException e) {
			try {
				is.close();
				sock.close();
			} catch (IOException e1) {
			}
			throw new UserException("Connecting failed",
					"IO Error while opening streams.", e);
		}

	}

	@Override
	public void close() {
		try {
			sock.close();
		} catch (IOException e) {
		}
		try {
			os.close();
		} catch (IOException e) {
		}
		try {
			is.close();
		} catch (IOException e) {
		}
	}

	@Override
	public InputStream getInputStream() {
		return is;
	}

	@Override
	public OutputStream getOutputStream() {
		return os;
	}

}
