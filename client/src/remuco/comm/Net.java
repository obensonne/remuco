package remuco.comm;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;

import javax.microedition.io.StreamConnection;

import remuco.Common;
import remuco.UserException;
import remuco.util.Log;
import remuco.util.Tools;

/**
 * Net offers methods to comfortably use a {@link StreamConnection} for
 * {@link Message} exchange. Net expects {@link Message}s in binary format.
 * 
 * To create a net conncetion use the {@link DeviceConnector}.
 * 
 * @author Christian Buennig
 * 
 */
public final class Net {

	private static final byte[] IO_PREFIX = { (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF };

	private static final byte[] IO_SUFFIX = { (byte) 0xFE, (byte) 0xFE,
			(byte) 0xFE, (byte) 0xFE };

	private static final byte[] PROTO_VER_BA = { (byte) Common.PROTO_VERSION };

	private DataInputStream dis;

	private DataOutputStream dos;

	private StreamConnection sc;

	/**
	 * Set down the net.
	 * 
	 */
	protected void down() {

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
	 *            in. {@link Message#sd} does not get touched. On error return,
	 *            <code>m</code> is in an undefined state.
	 * @return <b>-1</b> if receiving fails (in this case net is guaranteed to
	 *         be down on method return), <b>0</b> if receiving was successful
	 */
	protected int recv(Message m) {

		if (!check())
			return -1;

		m.bd = null;

		int size, skipped;

		try {
			if (readAndCompare(IO_PREFIX) < 0) {
				Log.ln("[NT] prefix differs");
				down();
				return -1;
			}

			m.id = dis.readInt();
			size = dis.readInt();

			Log.debug("[NT] rxed msg " + m.id + "(" + size + "B)");

			if (size > 0) {

				try {
					m.bd = new byte[size]; // this may throw OutOfMemoryError
					dis.readFully(m.bd);
				} catch (OutOfMemoryError e) {
					Log.ln("[NT] out of memory, discard incoming data (" + size
							+ "B)");
					while (size > 0) {
						Tools.sleep(100);
						skipped = (int) dis.skip(size);
						size -= skipped;
					}
					Log.ln("[NT] discarded incoming data");
					m.id = Message.ID_IGNORE;
					m.bd = null;
				}
			}

			if (readAndCompare(IO_SUFFIX) < 0) {
				Log.ln("[NT] suffix differs");
				down();
				return -1;
			}

		} catch (EOFException e) {
			Log.ln("[NT] EOF on connection: " + e.getMessage());
			down();
			return -1;
		} catch (IOException e) {
			Log.ln("[NT] connection broken: " + e.getMessage());
			down();
			return -1;
		}

		return 0;

	}

	/**
	 * Sends a message.
	 * 
	 * @param m
	 *            The message with to send. Content does not get changed.
	 * @return <b>-1</b> if sending fails (in this case net is guaranteed to be
	 *         down on method return), <b>0</b> if sending was successful
	 */
	protected int send(Message m) {

		Log.asssert(this, m != null);

		if (!check())
			return -1;

		Log.ln("[NT] will now send data (" + (m.bd != null ? m.bd.length : 0)
				+ " bytes + frame data)");

		try {
			dos.write(IO_PREFIX);
			dos.writeInt(m.id);
			if (m.bd != null) {
				dos.writeInt(m.bd.length);
				dos.write(m.bd);
			} else {
				dos.writeInt(0);
			}
			dos.write(IO_SUFFIX);
			dos.flush();
		} catch (IOException e) {
			Log.ln("[NT] connection broken: " + e.getMessage());
			down();
			return -1;
		}

		return 0;

	}

	/**
	 * After a stream connection has been established with the server, this
	 * method completes the connection setup by opening the input and output
	 * streams and waiting for the <i>HELLO</i> message from the server.
	 * <p>
	 * This method blocks until the <i>HELLO</i> message has been received, but
	 * waiting time is limited by a certain maximum.
	 * <p>
	 * When this method returns (successfully) messages ({@link Message}) can
	 * be sent and received with {@link #send(Message)} and
	 * {@link #recv(Message)}.
	 * <p>
	 * The net must be down (see {@link #down()}) to call this method.
	 * 
	 * @param sc
	 *            the {@link StreamConnection} to use for communication
	 * @return <b>-1</b> if the other end does not seem to be the Remuco server
	 *         or if the connection has been broken by other reasons (net is
	 *         guaranteed to be down on method return)
	 *         <p>
	 *         <b>0</b> if the connection to the server has been set up
	 *         successfully
	 * 
	 * @throws UserException
	 *             if the Remuco server uses a different protocol version
	 */
	protected int up(StreamConnection sc) throws UserException {

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

			Log.ln("[NT] open streams failed: " + e.getMessage());
			down();
			return -1;

		}

		// wait until enough data for the hello message is present or until
		// a timeout expires

		n = 0;
		wait = 0;

		while (wait < 2000 && n < ba.length) {

			try {

				n = dis.available();

			} catch (IOException e) {

				Log.ln("[NT] waiting for hello msg failed: " + e.toString());
				down();
				return -1;
			}

			Tools.sleep(200);

			wait += 200;

		}

		if (n < ba.length) {

			Log.ln("[NT] not enough data for hello msg (need " + ba.length
					+ " bytes, only " + n + " available)");
			down();
			return -1;

		}

		try {

			if (readAndCompare(IO_PREFIX) < 0) {
				Log.ln("[NT] IO prefix differs");
				down();
				return -1;
			}
			if (readAndCompare(PROTO_VER_BA) < 0) {
				down();
				throw new UserException("Connecting failed",
						"The server has a different protocol version.");
			}
			if (readAndCompare(IO_SUFFIX) < 0) {
				Log.ln("[NT] IO suffix differs");
				down();
				return -1;
			}

		} catch (IOException e) {
			Log.ln("[NT] rxing hello msg failed: " + e.getMessage());
			down();
			return -1;
		}

		return 0;

	}

	/**
	 * Reads <code>ba.length</code> bytes from the net connection and compares
	 * the received bytes with <code>ba</code>.
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

		dis.readFully(bar);

		// compare

		for (i = 0; i < n; i++) {

			if (bar[i] != ba[i]) {
				return -1;
			}

		}

		return 0;

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

}
