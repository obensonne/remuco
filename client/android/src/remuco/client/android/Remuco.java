package remuco.client.android;

import remuco.client.android.io.Socket;
import remuco.client.android.util.AndroidLogPrinter;
import remuco.client.common.MainLoop;
import remuco.client.common.UserException;
import remuco.client.common.data.ClientInfo;
import remuco.client.common.io.Connection;
import remuco.client.common.io.ISocket;
import remuco.client.common.io.Connection.IConnectionListener;
import remuco.client.common.player.Player;
import remuco.client.common.util.Log;
import android.app.Activity;
import android.os.Bundle;

public class Remuco extends Activity implements IConnectionListener {

	/*
	 * Example Android Activity to give an idea how a client uses the common
	 * framework and talks to the remote player.
	 * 
	 * By no mean this is clean code here, it's just there to show what is
	 * needed to connect to and interact with the server.
	 */

	private FooBar foobar;

	@Override
	public void notifyConnected(Player player) {

		Log.ln("connected, yippie");

		/* FooBar interacts with the remote player. */
		foobar = new FooBar(player);
	}

	@Override
	public void notifyDisconnected(ISocket sock, UserException reason) {
		Log.ln("disconnected, oh no (" + reason + ")");
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// enable the main loop (timer thread)
		MainLoop.enable();

		// set log output (classes in common use Log for logging)
		Log.setOut(new AndroidLogPrinter());

		/*
		 * Create a socket (note that Socket is a wrapper around java.net.Socket
		 * which implements ISocket - this is because we need a uniform socket
		 * interface for JavaME and Android clients). The socket parameters are
		 * for connecting to localhost from an emulated Android device. The
		 * socket creation should be done in an extra thread (e.g. using the
		 * MainLoop) to not block the UI.
		 */
		final Socket s;
		try {
			s = new Socket("10.0.2.2", Socket.PORT_DEFAULT);
		} catch (UserException e) {
			Log.ln("socket creation failed: ", e);
			return;
		}

		/*
		 * Create a client info, describing this device. Users should be able to
		 * set the first 3 parameters.
		 */
		final ClientInfo ci = new ClientInfo(200, "PNG", 50, null);

		/*
		 * Given the socket and the client info, we can set up a connection. A
		 * connection cares about exchanging initial messages between client and
		 * server. If a connections has been established it provides a Player
		 * class which can be used to interact with the remote player. A
		 * connection automatically creates it's own thread, so this call
		 * returns immediately.
		 */
		new Connection(s, this, 15, ci);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		Log.ln("onDestroy");

		// close the connection (also stops the receiver thread)
		if (foobar != null) {
			foobar.getPlayer().getConnection().close();
		}

		// disable the main loop (timer thread)
		MainLoop.disable();

	}

}
