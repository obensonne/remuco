package remuco.comm;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import javax.microedition.io.ConnectionNotFoundException;
import javax.microedition.io.Connector;
import javax.microedition.io.SocketConnection;
import javax.microedition.io.StreamConnection;

import remuco.ClientInfo;
import remuco.UserException;
import remuco.player.PlayerInfo;
import remuco.util.Log;
import remuco.util.Tools;

/**
 * Connection offers methods to comfortably use a {@link StreamConnection} for
 * {@link Message} exchange.
 * 
 * To create a connection, first create a connector with the
 * {@link ConnectorFactory} and then use the connector to create the connection.
 * 
 * @author Oben Sonne
 * 
 */
public final class Connection implements Runnable {

	/**
	 * A task for notifying a disconnection with another thread as the one
	 * calling {@link #send(Message)}.
	 */
	private class NotifyDisconnectTask extends TimerTask {

		private final UserException e;

		public NotifyDisconnectTask(UserException e) {
			this.e = e;
		}

		public void run() {
			connectionListener.notifyDisconnected(e);
		}

	}

	/**
	 * This task is used to delay the start of the receiver thread. The delay
	 * shall ensure, that the UI is able to update before the receiver thread
	 * raises new updates.
	 */
	private class StartReceiveThreadTask extends TimerTask {

		private final Connection conn;

		public StartReceiveThreadTask(Connection conn) {
			this.conn = conn;
		}

		public void run() {
			new Thread(conn).run();
		}

	}

	private static final Timer HELPER_THREAD = new Timer();

	private static final int HELLO_TIMEOUT = 2000;

	private static final byte[] IO_PREFIX = { (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF };

	private static final byte[] IO_SUFFIX = { (byte) 0xFE, (byte) 0xFE,
			(byte) 0xFE, (byte) 0xFE };

	private static final byte PROTO_VERSION = 0x08;

	private static final byte[] PROTO_VER_BA = { (byte) PROTO_VERSION };

	/** Received data is malformed. */
	private static final UserException UE_MALFORMED_DATA = new UserException(
			"Connection broken", "Received data is malformed.");

	private boolean closed = false;

	private final IConnectionListener connectionListener;

	private boolean connectionListenerNotifiedAboutError = false;

	private final DataInputStream dis;

	private final DataOutputStream dos;

	private final IMessageListener messageListener;

	private final StreamConnection sc;

	public Connection(String url, IConnectionListener connectionListener,
			IMessageListener messageListener) throws UserException {

		this.connectionListener = connectionListener;
		this.messageListener = messageListener;

		Log.ln("[CN] url: " + url);
		
		try {
			sc = (StreamConnection) Connector.open(url);
			logSocketOptions(sc);
		} catch (ConnectionNotFoundException e) {
			Log.ln("[CN] open url failed", e);
			throw new UserException("Connecting failed", "Target not found.", e);
		} catch (SecurityException e) {
			Log.ln("[CN] open url failed", e);
			throw new UserException("Connecting failed",
					"Not allowed to connect.", e);
		} catch (IOException e) {
			Log.ln("[CN] open url failed", e);
			throw new UserException("Connecting failed",
					"There was an IO error while setting up the connection.", e);
		}

		try {

			dis = sc.openDataInputStream();
			dos = sc.openDataOutputStream();

		} catch (IOException e) {

			Log.ln("[CN] open streams failed", e);
			downPrivate();
			throw new UserException("Connecting failed",
					"There was an IO error while opening the IO streams.", e);

		}

		HELPER_THREAD.schedule(new StartReceiveThreadTask(this), 200);
	}

	/**
	 * Shut down the connection. If the connection is already down, this method
	 * has no effect. There will be no connection events for a
	 * {@link IConnectionListener} after a call to this method.
	 * 
	 */
	public void down() {

		Log.ln("[CN] down by user");

		connectionListenerNotifiedAboutError = true;

		downPrivate();
	}

	public void run() {

		final PlayerInfo pinfo;

		try {
			pinfo = up();
		} catch (UserException e) {
			if (!connectionListenerNotifiedAboutError) {
				// it seems we don't need a notification timer here
				connectionListenerNotifiedAboutError = true;
				connectionListener.notifyDisconnected(e);
			}
			return;
		}

		connectionListener.notifyConnected(this, pinfo);

		while (!closed) {

			Message m;

			try {
				m = recv();
			} catch (UserException e) {
				if (!connectionListenerNotifiedAboutError) {
					connectionListenerNotifiedAboutError = true;
					connectionListener.notifyDisconnected(e);
					/*
					 * If notification does not work here, it should work if a
					 * notifier task is used.
					 */
				}
				return;
			}

			messageListener.notifyMessage(this, m);

		}

	}

	/**
	 * Sends a message.
	 * 
	 * @param m
	 *            The message to send. Content does not get changed.
	 */
	public synchronized void send(Message m) {

		UserException ue = null;

		synchronized (sc) {

			if (closed)
				return;

			try {
				sendPrivate(m);
			} catch (IOException e) {
				Log.ln("[CN] connection broken", e);
				downPrivate();
				if (!connectionListenerNotifiedAboutError) {
					connectionListenerNotifiedAboutError = true;
					ue = new UserException("Connection broken",
							"There was an IO error while sending data.", e);
				}
			}
		}

		if (ue != null) {
			// notification from an extra thread:
			HELPER_THREAD.schedule(new NotifyDisconnectTask(ue), 500);
		}
	}

	private void downPrivate() {

		Log.ln("[CN] going down");

		closed = true;

		try {
			dis.close();
		} catch (IOException e) {
		}
		try {
			dos.close();
		} catch (IOException e) {
		}
		try {
			sc.close();
		} catch (IOException e) {
		}

	}

	private void logSocketOptions(StreamConnection conn) throws IOException {
		if (sc instanceof SocketConnection) {
			final SocketConnection sock = (SocketConnection) sc;
			final StringBuffer sb = new StringBuffer("[CN] socket options: ");
			try {
				sb.append(sock.getSocketOption(SocketConnection.DELAY));
				sb.append(',');
				sb.append(sock.getSocketOption(SocketConnection.KEEPALIVE));
				sb.append(',');
				sb.append(sock.getSocketOption(SocketConnection.LINGER));
				sb.append(',');
				sb.append(sock.getSocketOption(SocketConnection.RCVBUF));
				sb.append(',');
				sb.append(sock.getSocketOption(SocketConnection.SNDBUF));
			} catch (IllegalArgumentException e) {
				Log.bug("Feb 2, 2009.7:39:47 PM");
				return;
			}
			Log.ln(sb.toString());
		}
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

	/**
	 * Receives a message.
	 * 
	 * @return the received message
	 * 
	 * @throws UserException
	 *             if there was an error (in this case the connection is
	 *             guaranteed to be down)
	 */
	private Message recv() throws UserException {

		final Message m = new Message();

		if (closed)
			return m;

		int size, skipped;

		Log.ln("[CN] waiting for msg");

		try {
			if (readAndCompare(IO_PREFIX) < 0) {
				Log.ln("[CN] prefix differs");
				downPrivate();
				throw UE_MALFORMED_DATA;
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
					m.id = Message.ID_IGNORE;
					m.bytes = null;
					while (size > 0) {
						Tools.sleep(100);
						skipped = (int) dis.skip(size);
						size -= skipped;
					}
					Log.ln("[CN] discarded incoming data");
				}
			}

			if (readAndCompare(IO_SUFFIX) < 0) {
				Log.ln("[CN] suffix differs");
				downPrivate();
				throw UE_MALFORMED_DATA;
			}

		} catch (EOFException e) {
			Log.ln("[CN] connection broken", e);
			downPrivate();
			throw new UserException("Connection broken",
					"Connection closed by other side.", e);
		} catch (IOException e) {
			Log.ln("[CN] connection broken", e);
			downPrivate();
			throw new UserException("Connection broken",
					"There was an IO error while receiving data.", e);
		}

		if (m.id == Message.ID_BYE) {
			downPrivate();
			throw new UserException("Disconnected.", "Remote player said bye.");
		}

		return m;

	}

	/** Send a message without exception handling. */
	private void sendPrivate(Message m) throws IOException {

		Log.ln("[CN] send msg (id " + m.id + ", payload "
				+ (m.bytes != null ? m.bytes.length : 0) + ")");

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
	}

	/**
	 * This method blocks until the <i>HELLO</i> message has been received, but
	 * waiting time is limited {@value #HELLO_TIMEOUT} ms.
	 * <p>
	 * When this method returns, {@link Message}s can be sent and received with
	 * {@link #send(Message)} and {@link #recv(Message)}.
	 * 
	 * @throws UserException
	 *             if connecting fails
	 */
	private PlayerInfo up() throws UserException {

		// ////// wait until there is enough data for hello message ////// //

		final byte[] ba = new byte[IO_PREFIX.length + PROTO_VER_BA.length
				+ IO_SUFFIX.length];

		int n = 0, wait = 0;

		while (wait < HELLO_TIMEOUT && n < ba.length) {

			try {

				n = dis.available();

			} catch (IOException e) {

				Log.ln("[CN] waiting for hello msg failed", e);
				downPrivate();
				throw new UserException(
						"Connecting failed",
						"There was an IO error while waiting for 'hello' from server.",
						e);
			}

			Tools.sleep(HELLO_TIMEOUT / 10);

			wait += HELLO_TIMEOUT / 10;
		}

		if (n < ba.length) {

			Log.ln("[CN] not enough data for hello msg (need " + ba.length
					+ " bytes, only " + n + " available)");
			downPrivate();
			throw new UserException("Connecting failed",
					"Timeout while waiting for 'hello' from server.");
		}

		// ////// read hello message ////// //

		try {

			if (readAndCompare(IO_PREFIX) < 0) {
				Log.ln("[CN] IO prefix differs");
				downPrivate();
				throw new UserException("Connecting failed",
						"Malformed 'hello' from server.");
			}
			if (readAndCompare(PROTO_VER_BA) < 0) {
				downPrivate();
				throw new UserException("Connecting failed",
						"Incompatible server version.");
			}
			if (readAndCompare(IO_SUFFIX) < 0) {
				Log.ln("[CN] IO suffix differs");
				downPrivate();
				throw new UserException("Connecting failed",
						"Malformed 'hello' from server.");
			}

			Log.ln("[CN] rx'ed hello message");

		} catch (IOException e) {
			Log.ln("[CN] rx'ing hello msg failed", e);
			downPrivate();
			throw new UserException(
					"Connecting failed",
					"There was an IO error while receiving 'hello' from server.",
					e);
		}

		final Message msg = new Message();

		msg.id = Message.ID_CINFO;
		msg.bytes = Serial.out(ClientInfo.ci);

		try {
			sendPrivate(msg);
		} catch (IOException e) {
			downPrivate();
			throw new UserException("Connecting failed",
					"There was an IO error while sending client description.",
					e);
		}

		final Message msgPlayerInfo = recv();

		final PlayerInfo pinfo = new PlayerInfo();

		try {

			Serial.in(pinfo, msgPlayerInfo.bytes);

		} catch (BinaryDataExecption e) {
			Log.ln("[UI] rxed malformed data", e);
			downPrivate();
			throw new UserException("Connecting failed",
					"Received player description data is malformed.", e);
		}

		return pinfo;

	}

}
