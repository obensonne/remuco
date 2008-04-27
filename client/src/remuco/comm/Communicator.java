package remuco.comm;

import remuco.ClientInfo;
import remuco.IEventListener;
import remuco.UserException;
import remuco.util.Log;
import remuco.util.Tools;

/**
 * The communicator is the top level interface for server communication. To be
 * used by the player to send and receive {@link Message}s. Based on a device
 * address it sets up a connection to that device and also continuously tries to
 * reset up the connection if broken.
 * 
 * @author Christian Buennig
 * 
 */
public final class Communicator implements Runnable, IMessageSender {

	public static final int EVENT_CONNECTED = 11;

	public static final int EVENT_DISCONNECTED = 12;

	/**
	 * Indicates a serious error. When this event gets raised, the communicator
	 * takes care to shut down itself automatically (so a call to
	 * {@link #down()} is not necessary).
	 */
	public static final int EVENT_ERROR = 10;

	private static final int CONN_RETRY_IVAL = 10;

	private Connection conn;

	private final IConnector connector;

	private final IEventListener evl;

	private boolean interrupted;

	private final IMessageListener ml;

	private final Thread thread;

	/**
	 * Creates a device specific communicator for message exchange. The
	 * communicator runs in an own thread and signals its state to the event
	 * listener <code>evl</code. When connected it forwards
	 * received messages to <code>ml</code>. Messages can be sent via {@link #sendMessage(Message)}.
	 * 
	 * @param device
	 *            the device to communicate with
	 * @param evl
	 *            handler for the received messages
	 */
	public Communicator(String device, IEventListener evl, IMessageListener ml)
			throws UserException {

		connector = ConnectorFactory.createConnector(device);

		this.evl = evl;
		this.ml = ml;

		thread = new Thread(this);
		thread.start();
	}

	/**
	 * Shuts down the communication layer. The communication layer's thread gets
	 * interrupted.
	 * 
	 */
	public void down() {

		Log.ln("[CM] going down");

		interrupted = true;
		
		if (conn != null)
			conn.down();

		if (thread.isAlive())
			thread.interrupt();

	}

	public final boolean isConnected() {
		return conn != null && conn.isUp();
	}

	public void run() {

		Message msg = new Message();

		Log.ln("[CM] starting");

		conn = null;

		while (!interrupted) { // thread loop

			// //////// continuously try to connect //////////

			for (int n = 0; !interrupted; n++) {

				try {

					conn = connector.getConnection();

				} catch (UserException e) { // serious error

					Log.ln("[CM] ", e);
					evl.handleEvent(EVENT_ERROR, e.getError() + "\n"
							+ e.getDetails());
					conn = null;
					break;
				}

				if (interrupted)
					break;

				if (conn == null) { // failed -> raise event and retry

					evl.handleEvent(EVENT_DISCONNECTED, "Connecting failed\n("
							+ n + " times)\n" + "Retry ..");
					Tools.sleep(CONN_RETRY_IVAL * 1000);

				} else { // connected -> send client info and break loop

					msg.id = Message.ID_IFC_CINFO;
					msg.bytes = Serial.out(ClientInfo.ci);

					if (!conn.send(msg)) {
						evl.handleEvent(EVENT_ERROR,
								"Failed to send data to server!");
						conn = null;
						break;
					}

					evl.handleEvent(EVENT_CONNECTED, null);
					break;
				}
			}

			if (conn == null || interrupted) { // break thread
				// loop
				break;
			}

			// //////// continuously receive messages //////////

			while (!interrupted) { // receive loop

				msg.bytes = null; // a gift to the GC

				try {
					if (!conn.recv(msg)) { // net error -> break receive loop
						conn = null;
						break;
					}
				} catch (InterruptedException e) {
					Log.ln("[CM] recv interrupted");
					continue;
				}

				// forward received message to message listener
				ml.handleMessage(msg);
			}

			// //////// connection broken -> raise event and retry //////////

			if (!interrupted) { // connection broken,
				evl.handleEvent(EVENT_DISCONNECTED,
						"Lost connection.\nReconnect..");

				Tools.sleep(2000);
			}
		}

		// clean connection
		if (conn != null) {
			if (conn.isUp())
				conn.down();
			conn = null;
		}

		Log.ln("[CM] stopped");

	}

	public void sendMessage(Message m) {

		if (conn == null || !conn.isUp())
			return;

		conn.send(m); // errors on 'net' will be detected and handled by
		// receiveMessages()

	}

}
