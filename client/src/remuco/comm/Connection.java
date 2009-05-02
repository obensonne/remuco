/*   
 *   Remuco - A remote control system for media players.
 *   Copyright (C) 2006-2009 Oben Sonne <obensonne@googlemail.com>
 *
 *   This file is part of Remuco.
 *
 *   Remuco is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Remuco is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Remuco.  If not, see <http://www.gnu.org/licenses/>.
 *   
 */
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
import remuco.Remuco;
import remuco.UserException;
import remuco.player.PlayerInfo;
import remuco.util.Log;
import remuco.util.Tools;

/**
 * Send and receive messages.
 * <p>
 * All connection related events (state change and incoming data) are passed to
 * the global timer thread for delivering to event listener.
 * 
 * @author Oben Sonne
 * 
 */
public final class Connection implements Runnable {

	private static final int HELLO_TIMEOUT = 2000;

	private static final byte[] PREFIX = { (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF };

	private static final byte[] PROTO_VERSION = { (byte) 0x08 };

	private static final byte[] SUFFIX = { (byte) 0xFE, (byte) 0xFE,
			(byte) 0xFE, (byte) 0xFE };

	private static final int HELLO_LEN = PREFIX.length + PROTO_VERSION.length
			+ SUFFIX.length;

	private boolean closed = false;

	private final IConnectionListener connectionListener;

	private boolean connectionListenerNotifiedAboutError = false;

	private final DataInputStream dis;

	private final DataOutputStream dos;

	private final IMessageListener messageListener;

	private final StreamConnection sc;

	private final Timer timer;

	private String url = null;

	public Connection(String url, IConnectionListener connectionListener,
			IMessageListener messageListener) throws UserException {

		this.url = url;
		this.connectionListener = connectionListener;
		this.messageListener = messageListener;

		timer = Remuco.getGlobalTimer();

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

		// start the receiver thread delayed to give the UI some time to update
		final Connection conn = this;
		timer.schedule(new TimerTask() {
			public void run() {
				new Thread(conn).start();
			}
		}, 200);
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
			notifyDisconnected(e);
			return;
		}

		notifyConnected(pinfo);

		while (!closed) { // loop receiving messages

			final Message m;

			try {
				m = recv();
			} catch (UserException e) {
				notifyDisconnected(e);
				return;
			}

			if (m.id != Message.IGNORE) {
				notifyMessage(m);
			}
		}

	}

	/**
	 * Sends a message.
	 * 
	 * @param m
	 *            The message to send. Content does not get changed.
	 */
	public void send(Message m) {

		synchronized (dos) {

			if (closed)
				return;

			try {
				sendPrivate(m);
			} catch (IOException e) {
				Log.ln("[CN] connection broken", e);
				downPrivate();
				notifyDisconnected(new UserException("Connection broken",
						"There was an IO error while sending data.", e));

			}
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

	/** Notification is done via the global timer thread. */
	private void notifyConnected(final PlayerInfo pinfo) {

		final Connection conn = this;
		timer.schedule(new TimerTask() {
			public void run() {
				connectionListener.notifyConnected(conn, pinfo);
			}
		}, 0);
	}

	/**
	 * Notifies the connection listener that the connection is
	 * down/broken/disconnected (but only if the listener has not yet been
	 * notified).
	 * <p>
	 * Notification is done via the global timer thread.
	 * 
	 * @param ue
	 *            the user exception describing the disconnect reason
	 */
	private void notifyDisconnected(final UserException ue) {

		if (!connectionListenerNotifiedAboutError) {

			connectionListenerNotifiedAboutError = true;

			timer.schedule(new TimerTask() {
				public void run() {
					connectionListener.notifyDisconnected(url, ue);
				}
			}, 200);
		}
	}

	/** Notification is done via the global timer thread. */
	private void notifyMessage(final Message m) {

		final Connection conn = this;
		timer.schedule(new TimerTask() {
			public void run() {
				messageListener.notifyMessage(conn, m);
			}
		}, 0);
	}

	/**
	 * Reads <code>ba.length</code> data from the connection and compares the
	 * received data with <code>ba</code>.
	 * 
	 * @param ba
	 *            the byte array to compare to
	 * @return <b>-1</b> if the received data differ, <b>0</b> if they are equal
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	private int readAndCompare(byte[] ba) throws IOException {

		int n, i;
		byte[] bar;

		n = ba.length;
		bar = new byte[n];

		// the following may throw an InterruptedException which gets handled
		// by the caller of recv() (Communicator)
		dis.readFully(bar);

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

		Log.ln("[CN] waiting for msg");

		try {
			m.id = dis.readShort();
			final int size = dis.readInt();

			Log.ln("[CN] read msg: " + m.id + ", " + size + "B");

			if (size > 0) {

				try {
					m.data = new byte[size]; // may throw OutOfMemoryError
					dis.readFully(m.data);
				} catch (OutOfMemoryError e) {
					m.id = Message.IGNORE;
					m.data = null;
					Log.ln("[CN] out of mem, try to skip " + size + "B)");
					skip(size);
				}
			}

			Log.ln("[CN] read msg: done");

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

		if (m.id == Message.CONN_BYE) {
			downPrivate();
			url = null; // suppress reconnecting
			throw new UserException("Disconnected.", "Remote player said bye.");
		}

		return m;

	}

	/** Send a message without exception handling. */
	private void sendPrivate(Message m) throws IOException {

		Log.ln("[CN] send msg: " + m.id + ", "
				+ (m.data != null ? m.data.length : 0) + "B");

		dos.writeShort(m.id);
		if (m.data != null) {
			dos.writeInt(m.data.length);
			dos.write(m.data);
		} else {
			dos.writeInt(0);
		}
		dos.flush();

		Log.ln("[CN] send msg: done");
	}

	private void skip(int num) throws IOException {

		long rest = num;
		while (rest > 0) {
			Tools.sleep(20);
			final long avail = dis.available();
			if (avail == 0) {
				break;
			}
			rest -= dis.skip(Math.min(avail, 10240));
		}
		;
		Log.ln("[CN] skipped " + (num - rest) + "B");

	}

	/**
	 * This method blocks until the <i>HELLO</i> message has been received, but
	 * waiting time is limited to {@link #HELLO_TIMEOUT}.
	 * <p>
	 * When this method returns, {@link Message}s can be sent and received with
	 * {@link #send(Message)} and {@link #recv(Message)}.
	 * 
	 * @throws UserException
	 *             if connecting fails
	 */
	private PlayerInfo up() throws UserException {

		// ////// wait until there is enough data for hello message ////// //

		int n = 0, wait = 0;

		while (wait < HELLO_TIMEOUT && n < HELLO_LEN) {

			try {

				n = dis.available();

			} catch (IOException e) {

				Log.ln("[CN] waiting for hello msg failed", e);
				downPrivate();
				throw new UserException("Connecting failed",
						"There was an IO error while waiting for the hello "
								+ "message from the server.", e);
			}

			Tools.sleep(HELLO_TIMEOUT / 10);

			wait += HELLO_TIMEOUT / 10;
		}

		if (n < HELLO_LEN) {

			Log.ln("[CN] not enough data for hello msg (need " + HELLO_LEN
					+ "B, only " + n + "B available)");
			downPrivate();
			throw new UserException("Connecting failed",
					"Timeout while waiting for the hello message from the "
							+ "server.");
		}

		// ////// read hello message ////// //

		try {

			if (readAndCompare(PREFIX) < 0) {
				Log.ln("[CN] IO prefix differs");
				downPrivate();
				url = null; // suppress reconnecting;
				throw new UserException("Connecting failed",
						"Received a malformed hello message from the server.");
			}
			if (readAndCompare(PROTO_VERSION) < 0) {
				downPrivate();
				url = null; // suppress reconnecting;
				throw new UserException("Connecting failed",
						"Incompatible server version.");
			}
			if (readAndCompare(SUFFIX) < 0) {
				Log.ln("[CN] IO suffix differs");
				downPrivate();
				url = null; // suppress reconnecting;
				throw new UserException("Connecting failed",
						"Received a malformed hello message from the server.");
			}

			Log.ln("[CN] rx'ed hello message");

		} catch (IOException e) {
			Log.ln("[CN] rx'ing hello msg failed", e);
			downPrivate();
			throw new UserException("Connecting failed",
					"There was an IO error while receiving the hello message "
							+ "from the server.", e);
		}

		final Message msgCI = new Message();

		msgCI.id = Message.CONN_CINFO;
		msgCI.data = Serial.out(ClientInfo.getInstance());

		try {
			sendPrivate(msgCI);
		} catch (IOException e) {
			downPrivate();
			throw new UserException("Connecting failed",
					"There was an IO error while sending client description.",
					e);
		}

		final Message msgPI = recv();

		final PlayerInfo pinfo = new PlayerInfo();

		try {

			Serial.in(pinfo, msgPI.data);

		} catch (BinaryDataExecption e) {
			Log.ln("[UI] rxed malformed data", e);
			downPrivate();
			throw new UserException("Connecting failed",
					"Received player description data is malformed.", e);
		}

		return pinfo;

	}
}
