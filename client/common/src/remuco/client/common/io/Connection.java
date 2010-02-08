/*   
 *   Remuco - A remote control system for media players.
 *   Copyright (C) 2006-2010 by the Remuco team, see AUTHORS.
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
package remuco.client.common.io;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.TimerTask;

import remuco.client.common.MainLoop;
import remuco.client.common.UserException;
import remuco.client.common.data.ClientInfo;
import remuco.client.common.data.PlayerInfo;
import remuco.client.common.player.Player;
import remuco.client.common.serial.BinaryDataExecption;
import remuco.client.common.serial.Serial;
import remuco.client.common.util.Log;
import remuco.client.common.util.Tools;

/**
 * A connection sets up a connection to the server and handles receiving and
 * sending of messages from/to the server.
 */
public final class Connection implements Runnable {

	/**
	 * Interface for classes interested in the state of a {@link Connection}.
	 */
	public interface IConnectionListener {

		/**
		 * Notifies a successful connection.
		 * 
		 * @param player
		 *            the connected player
		 */
		public void notifyConnected(Player player);

		/**
		 * Notifies a disconnection.
		 * 
		 * @param sock
		 *            the socket used by the broken connection - if it is worth
		 *            trying to reconnect, otherwise <code>null</code>
		 * @param reason
		 *            the user exception describing the reason for disconnecting
		 */
		public void notifyDisconnected(ISocket sock, UserException reason);

	}

	private class PingTask extends TimerTask {

		private final Message m;

		private PingTask() {
			m = new Message();
			m.id = Message.IGNORE;
		}

		public void run() {
			if (closed) {
				cancel();
			} else {
				send(m);
			}
		}

	}

	private static final int HELLO_TIMEOUT = 2000;

	private static final byte[] PREFIX = { (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF };

	private static final byte[] PROTO_VERSION = { (byte) 0x0A };

	private static final byte[] SUFFIX = { (byte) 0xFE, (byte) 0xFE,
			(byte) 0xFE, (byte) 0xFE };

	private static final int HELLO_LEN = PREFIX.length + PROTO_VERSION.length
			+ SUFFIX.length;

	private boolean closed = false;

	private final IConnectionListener connectionListener;

	private boolean connectionListenerNotifiedAboutError = false;

	private final DataInputStream dis;

	private final DataOutputStream dos;

	private final ClientInfo initialClientInfo;

	private final int initialPingInterval;

	private TimerTask ping;

	/** Flag indicating if a reconnect has chance to succeed. */
	private boolean reconnect = true;

	private final ISocket sock;

	/**
	 * Create a new connection.
	 * <p>
	 * A new thread for receiving data is created automatically. Once this
	 * thread has set up a connection to the server, it notifies the listener
	 * using {@link IConnectionListener#notifyConnected(Player)}. If setting up
	 * the connection fails the listener is notified using
	 * {@link IConnectionListener#notifyDisconnected(ISocket, UserException)}.
	 * <p>
	 * If the connection has been set up, a {@link Player} object is created and
	 * received messages are passed to that player.
	 * <p>
	 * Notifications and messages are passed via the {@link MainLoop} thread to
	 * decouple their handling from the receiver thread used by this connection.
	 * 
	 * @param sock
	 *            socket providing streams for the connection
	 * @param listener
	 *            connection event listener
	 * @param ping
	 *            initial ping interval (may be adjusted later using
	 *            {@link #setPing(int)})
	 * @param ci
	 *            initial client info to send to the server (client info updates
	 *            can be sent using {@link #send(ClientInfo)})
	 */
	public Connection(ISocket sock, IConnectionListener listener, int ping,
			ClientInfo ci) {

		this.sock = sock;
		this.connectionListener = listener;
		this.initialPingInterval = ping;
		this.initialClientInfo = ci;

		Log.ln("[CN] sock: " + sock);

		dis = new DataInputStream(sock.getInputStream());
		dos = new DataOutputStream(sock.getOutputStream());

		new Thread(this).start();
	}

	/**
	 * Close the connection. If the connection is already closed, this method
	 * has no effect. There will be no connection events for a
	 * {@link IConnectionListener} after a call to this method.
	 * 
	 */
	public void close() {

		Log.ln("[CN] down by user");

		connectionListenerNotifiedAboutError = true;

		downPrivate();
	}

	public boolean isClosed() {
		return closed;
	}

	public void run() {

		// delay startup a little bit to give UI a chance to update
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
		}

		final PlayerInfo pinfo;

		try {
			pinfo = up();
		} catch (UserException e) {
			notifyDisconnected(e);
			return;
		}

		setPing(initialPingInterval);

		final Player player = new Player(this, pinfo);

		MainLoop.schedule(new TimerTask() {
			public void run() {
				connectionListener.notifyConnected(player);
			}
		});

		while (!closed) { // loop receiving messages

			final Message m;

			try {
				m = recv();
			} catch (UserException e) {
				notifyDisconnected(e);
				return;
			}

			if (m.id == Message.IGNORE) {
				continue;
			}

			// notify new message in main loop

			MainLoop.schedule(new TimerTask() {
				public void run() {
					try {
						player.handleMessage(m);
					} catch (BinaryDataExecption e) {
						notifyDisconnected("Connection Error",
							"Received malformed data.", e);
					} catch (OutOfMemoryError e) {
						m.data = null;
						notifyDisconnected("Memory Error",
							"Received data too big.", null);
					}
				}
			});

		}

	}

	/**
	 * Send client info to the server.
	 * <p>
	 * To be called whenever client info related options (e.g. image size or
	 * item list page size) change.
	 * <p>
	 * Errors are handled as in {@link #send(Message)}.
	 * 
	 * @param ci
	 *            the new client info to send
	 */
	public void send(ClientInfo ci) {

		final Message msg = new Message();

		msg.id = Message.CONN_CINFO;
		msg.data = Serial.out(ci);

		send(msg);
	}

	/**
	 * Sends a message.
	 * <p>
	 * If sending fails, the listener set in
	 * {@link #Connection(ISocket, IConnectionListener, int)} is notified using
	 * {@link IConnectionListener#notifyDisconnected(ISocket, UserException)}.
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
				notifyDisconnected("Connection broken",
					"IO Error while sending data.", e);

			}
		}
	}

	/**
	 * Set ping interval.
	 * 
	 * @param interval
	 *            in seconds (0 disables pinging)
	 */
	public void setPing(int interval) {

		if (ping != null) {
			ping.cancel();
			ping = null;
		}

		if (interval > 0) {
			ping = new PingTask();
			MainLoop.schedule(ping, interval * 1000, interval * 1000);
		}
	}

	private void downPrivate() {

		closed = true;

		Log.ln("[CN] going down");

		setPing(0);

		sock.close();
	}

	/** See {@link #notifyDisconnected(UserException)}. */
	private void notifyDisconnected(String error, String details, Exception e) {
		notifyDisconnected(new UserException(error, details, e));
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

			MainLoop.schedule(new TimerTask() {
				public void run() {
					connectionListener.notifyDisconnected(reconnect ? sock
							: null, ue);
				}
			});
		}
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
					"IO error while receiving data.", e);
		}

		if (m.id == Message.CONN_BYE) {
			downPrivate();
			reconnect = false; // suppress reconnecting
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
						"IO error while waiting for the hello message.", e);
			}

			Tools.sleep(HELLO_TIMEOUT / 10);

			wait += HELLO_TIMEOUT / 10;
		}

		if (n < HELLO_LEN) {

			Log.ln("[CN] not enough data for hello msg (need " + HELLO_LEN
					+ "B, only " + n + "B available)");
			downPrivate();
			throw new UserException("Connecting failed",
					"Timeout while waiting for the hello message.");
		}

		// ////// read hello message ////// //

		try {

			if (readAndCompare(PREFIX) < 0) {
				Log.ln("[CN] IO prefix differs");
				downPrivate();
				reconnect = false; // suppress reconnecting
				throw new UserException("Connecting failed",
						"Received a malformed hello message.");
			}
			if (readAndCompare(PROTO_VERSION) < 0) {
				downPrivate();
				reconnect = false; // suppress reconnecting
				throw new UserException("Connecting failed",
						"Server and client have incompatible versions. See "
								+ "the FAQ on the Remuco web site"
								+ " for more information.");
			}
			if (readAndCompare(SUFFIX) < 0) {
				Log.ln("[CN] IO suffix differs");
				downPrivate();
				reconnect = false; // suppress reconnecting
				throw new UserException("Connecting failed",
						"Received a malformed hello message.");
			}

			Log.ln("[CN] rx'ed hello message");

		} catch (IOException e) {
			Log.ln("[CN] rx'ing hello msg failed", e);
			downPrivate();
			throw new UserException("Connecting failed",
					"IO error while receiving the hello message.", e);
		}

		final Message msgCI = new Message();

		msgCI.id = Message.CONN_CINFO;
		msgCI.data = Serial.out(initialClientInfo);

		try {
			sendPrivate(msgCI);
		} catch (IOException e) {
			downPrivate();
			throw new UserException("Connecting failed",
					"IO error while sending client info.", e);
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
