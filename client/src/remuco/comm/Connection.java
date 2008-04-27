package remuco.comm;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;

import javax.microedition.io.StreamConnection;

import remuco.UserException;
import remuco.player.Player;
import remuco.util.Log;
import remuco.util.Tools;

/**
 * Connection offers methods to comfortably use a {@link StreamConnection} for
 * {@link Message} exchange.
 * 
 * To create a connection, first create a connector with the
 * {@link ConnectorFactory} and then use the connector to create the connection.
 * 
 * @author Christian Buennig
 * 
 */
public final class Connection {

	private static final byte PROTO_VERSION = 0x07;

	private static final byte[] IO_PREFIX = { (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF };

	private static final byte[] IO_SUFFIX = { (byte) 0xFE, (byte) 0xFE,
			(byte) 0xFE, (byte) 0xFE };

	private static final byte[] PROTO_VER_BA = { (byte) PROTO_VERSION };

	private DataInputStream dis;

	private DataOutputStream dos;

	private StreamConnection sc;

	/** Shut down the connection. */
	protected void down() {

		Log.ln("[CN] going down");

		try {

			if (dis != null)
				dis.close();
		} catch (IOException e) {
		}
		try {
			if (dos != null)
				dos.close();
		} catch (IOException e) {
		}
		try {
			if (sc != null)
				sc.close();
		} catch (IOException e) {
		}

		dis = null;
		dos = null;
		sc = null;

	}

	protected boolean isUp() {

		return check();

	}

	/**
	 * Receives a message.
	 * 
	 * @param m
	 *            The message to store the received message id and binary data
	 *            in. On error return, <code>m</code> is in an undefined
	 *            state.
	 * @return <code>false</code> if receiving fails (in this case the
	 *         connection is guaranteed to be down on method return),
	 *         <code>true</code> if receiving was successful
	 * @throws InterruptedException
	 */
	protected boolean recv(Message m) throws InterruptedException {

		if (!check())
			return false;

		m.bytes = null;

		int size, skipped;

		Log.ln("[CN] waiting for msg");

		try {
			if (readAndCompare(IO_PREFIX) < 0) {
				Log.ln("[CN] prefix differs");
				down();
				return false;
			}

			m.id = dis.readInt();
			size = dis.readInt();

			Log.ln("[CN] rxed msg (id " + m.id + ", payload " + size + ")");

			if (size > 0) {

				try {
					m.bytes = new byte[size]; // may throw OutOfMemoryError
					dis.readFully(m.bytes);
				} catch (OutOfMemoryError e) {
					Log.ln("[CN] out of memory, discard incoming data (" + size
							+ "B)");
					while (size > 0) {
						Tools.sleep(100);
						skipped = (int) dis.skip(size);
						size -= skipped;
					}
					Log.ln("[CN] discarded incoming data");
					m.id = Message.ID_IGNORE;
					m.bytes = null;
				}
			}

			if (readAndCompare(IO_SUFFIX) < 0) {
				Log.ln("[CN] suffix differs");
				down();
				return false;
			}

		} catch (EOFException e) {
			Log.ln("[CN] EOF on connection", e);
			down();
			return false;
		} catch (IOException e) {
			Log.ln("[CN] connection broken", e);
			down();
			return false;
		}

		return true;

	}

	/**
	 * Sends a message.
	 * <p>
	 * Note: This method is synchronized. Currently calls to this method always
	 * have their origin in {@link Player} which already cares about
	 * synchronization. However, to prevent future bugs, this should stay
	 * synchronized.
	 * 
	 * @param m
	 *            The message to send. Content does not get changed.
	 * @return <code>false</code> if sending fails (in this case the
	 *         connection is guaranteed to be down on method return),
	 *         <code>true</code> if sending was successful
	 */
	protected synchronized boolean send(Message m) {

		Log.asssert(this, m != null);

		if (!check())
			return false;

		Log.ln("[CN] send msg (id " + m.id + ", payload "
				+ (m.bytes != null ? m.bytes.length : 0) + ")");

		try {
			dos.write(IO_PREFIX);
			dos.writeInt(m.id);
			if (m.bytes != null) {
				dos.writeInt(m.bytes.length);
				dos.write(m.bytes);
			} else {
				dos.writeInt(0);
			}
			dos.write(IO_SUFFIX);
			dos.flush();
		} catch (IOException e) {
			Log.ln("[CN] connection broken", e);
			down();
			return false;
		}

		return true;

	}

	/**
	 * After a stream connection has been established with the server, this
	 * method completes the connection setup by opening the input and output
	 * streams and waiting for the <i>HELLO</i> message from the server.
	 * <p>
	 * This method blocks until the <i>HELLO</i> message has been received, but
	 * waiting time is limited by a certain maximum.
	 * <p>
	 * When this method returns successfully, {@link Message}s can be sent and
	 * received with {@link #send(Message)} and {@link #recv(Message)}.
	 * <p>
	 * The connection must be down (see {@link #down()}) to call this method.
	 * 
	 * @param sc
	 *            the {@link StreamConnection} to use
	 * @return <code>false</code> if the other end does not seem to be the
	 *         Remuco server or if the connection has been broken by other
	 *         reasons (connection is guaranteed to be down on method return)
	 *         <p>
	 *         <code>true</code> if the connection to the server has been set
	 *         up successfully
	 * 
	 * @throws UserException
	 *             if the Remuco server uses a different protocol version
	 */
	protected boolean up(StreamConnection sc) throws UserException {

		Log.asssert(this, sc);
		Log.asssert(this, this.sc == null && dis == null && dos == null);

		final byte[] ba = new byte[IO_PREFIX.length + PROTO_VER_BA.length
				+ IO_SUFFIX.length];
		int n, wait;

		this.sc = sc;

		try {

			dis = sc.openDataInputStream();
			dos = sc.openDataOutputStream();

		} catch (IOException e) {

			Log.ln("[CN] open streams failed", e);
			down();
			return false;

		}

		// ////// wait until there is enough data for hello message ////// //

		n = 0;
		wait = 0;

		while (wait < 2000 && n < ba.length) {

			try {

				n = dis.available();

			} catch (IOException e) {

				Log.ln("[CN] waiting for hello msg failed", e);
				down();
				return false;
			}

			Tools.sleep(200);

			wait += 200;
		}

		if (n < ba.length) {

			Log.ln("[CN] not enough data for hello msg (need " + ba.length
					+ " bytes, only " + n + " available)");
			down();
			return false;
		}

		// ////// read hello message ////// //

		try {

			if (readAndCompare(IO_PREFIX) < 0) {
				Log.ln("[CN] IO prefix differs");
				down();
				return false;
			}
			if (readAndCompare(PROTO_VER_BA) < 0) {
				down();
				throw new UserException("Connecting failed",
						"The server has a different protocol version.");
			}
			if (readAndCompare(IO_SUFFIX) < 0) {
				Log.ln("[CN] IO suffix differs");
				down();
				return false;
			}

			Log.ln("[CN] rx'ed hello message");

		} catch (IOException e) {
			Log.ln("[CN] rx'ing hello msg failed", e);
			down();
			return false;
		}

		return true;
	}

	/**
	 * Checks the connection state. Calls {@link #down()} if needed.
	 * 
	 * @return <code>true</code> if the connection is up, <code>false</code>
	 *         otherwise
	 */
	private boolean check() {

		if (sc != null && dis != null && dos != null)
			return true;

		// shut down if not done already:
		if (sc != null || dis != null || dos != null)
			down();

		return false;
	}

	/**
	 * Reads <code>ba.length</code> bytes from the connection and compares the
	 * received bytes with <code>ba</code>.
	 * 
	 * @param ba
	 *            the byte array to compare to
	 * @return <b>-1</b> if the received bytes differ, <b>0</b> if they are
	 *         equal
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	private int readAndCompare(byte[] ba) throws IOException {

		int n, i;
		byte[] bar;

		n = ba.length;
		bar = new byte[n];

		// read

		// the following may throw an InterruptedException which gets handled
		// by the caller of recv() (Communicator)
		dis.readFully(bar);

		// compare

		for (i = 0; i < n; i++) {

			if (bar[i] != ba[i]) {
				return -1;
			}
		}

		return 0;
	}

}
