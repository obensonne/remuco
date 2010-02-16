package remuco.client.android.util;

import java.util.TimerTask;

import remuco.client.android.MessageFlag;
import remuco.client.android.R;
import remuco.client.android.Remuco;
import remuco.client.android.io.Socket;
import remuco.client.common.UserException;
import remuco.client.common.data.ClientInfo;
import remuco.client.common.io.Connection;
import remuco.client.common.io.Connection.IConnectionListener;
import remuco.client.common.util.Log;
import android.widget.Toast;

public class ConnectTask extends TimerTask {

	private String hostname;
	private Remuco remuco;
	
	
	
	public ConnectTask(String hostname, Remuco remuco) {
		super();
		this.hostname = hostname;
		this.remuco = remuco;
	}



	@Override
	public void run() {
		Log.ln("[CT] trying to connect to " + hostname);
		
		/*
		 * Create a socket (note that Socket is a wrapper around java.net.Socket
		 * which implements ISocket - this is because we need a uniform socket
		 * interface for JavaME and Android clients). The socket parameters are
		 * for connecting to localhost from an emulated Android device. The
		 * socket creation should be done in an extra thread (e.g. using the
		 * MainLoop) to not block the UI.
		 */
		
		Socket s = null;
		try {
			s = new Socket(hostname, Socket.PORT_DEFAULT);
		} catch (UserException e) {
			Log.ln("[CT] socket creation failed: ", e);
			
			// tell the view that we have no connection
			remuco.notifyDisconnected(s, e);
			
			return;
		}

		
		/* TODO: useful clientinfo
		 * 
		 * Create a client info, describing this device. Users should be able to
		 * set the first 3 parameters.
		 */
		ClientInfo ci = new ClientInfo(300, "PNG", 50, null);

		/* TODO: this one is not that pretty ... this should be done via a callback
		 * 
		 * 
		 * Given the socket and the client info, we can set up a connection. A
		 * connection cares about exchanging initial messages between client and
		 * server. If a connections has been established it provides a Player
		 * class which can be used to interact with the remote player. A
		 * connection automatically creates it's own thread, so this call
		 * returns immediately.
		 */
		remuco.setConnection(new Connection(s, remuco, 15, ci));
	}
	

}
