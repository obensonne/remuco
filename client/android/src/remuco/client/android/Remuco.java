package remuco.client.android;

import java.util.ArrayList;
import java.util.TimerTask;

import remuco.client.android.dialogs.ConnectDialog;
import remuco.client.android.dialogs.VolumeDialog;
import remuco.client.android.io.Socket;
import remuco.client.android.util.AndroidLogPrinter;
import remuco.client.android.util.ConnectTask;
import remuco.client.common.MainLoop;
import remuco.client.common.UserException;
import remuco.client.common.data.ClientInfo;
import remuco.client.common.io.Connection;
import remuco.client.common.io.ISocket;
import remuco.client.common.io.Connection.IConnectionListener;
import remuco.client.common.player.Player;
import remuco.client.common.util.Log;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

public class Remuco extends Activity implements IConnectionListener{

	// --- preferences
	public static final String PREF_NAME = "remucoPreference";
	private SharedPreferences preference;
	
	private static final String LAST_HOSTNAME = "connect_dialog_last_hostnames";
	
	// --- handlers
	private ViewHandler viewHandler;
	private PlayerHandler playerHandler;
	private ArrayList<Handler> handlers;
	
	
	// --- dialog ids
	private static final int CONNECT_DIALOG = 1;
	private static final int VOLUME_DIALOG = 2;
	
	// --- dialog reference
	private VolumeDialog volumeDialog;
	
	// --- Remuco Connection
	private Connection connection;
	private Socket s;
	
	
	// --- lifecycle methods
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		
		Log.debug("--- onCreate()");
		
		// ------
		// android related initialization
		// ------
		
		// --- load layout
		setContentView(R.layout.main);

		// --- load preferences
		preference = getPreferences(Context.MODE_PRIVATE);

		// ------
		// remuco related initialization
		// ------
		
		// --- set log output (classes in common use Log for logging)
		Log.setOut(new AndroidLogPrinter());
		
		// --- enable the remuco main loop (timer thread)
		MainLoop.enable();
		
		
		// ------
		// communication initialization
		// ------
		
		// --- create handler array
		handlers = new ArrayList<Handler>();
		
		// --- create view handler
		viewHandler = new ViewHandler(this); 
		handlers.add(viewHandler);
		
		// create and add player handler
		playerHandler = new PlayerHandler(this);
		handlers.add(playerHandler);
		

	}
	
	/**
	 * this method gets called after on create
	 */
	@Override
	protected void onStart() {
		super.onStart();
		
		Log.ln("--- onStart()");

		
		// --- initialize the connection here if we can

		// 1. try to get back our last connection if it exists in our bundle
//		connection = (Connection)getLastNonConfigurationInstance();
//		if(connection != null)
//			return;
		
		// 2. try to connect to the last hostname
		String lastHostname = preference.getString(LAST_HOSTNAME, "");
		if( !lastHostname.equals("") ){
			connect(lastHostname);
		}
		
		
		
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		Log.ln("--- onStop()");
		
		// stop the connection
		connection.close();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();

		Log.ln("--- onDestroy()");

		// disable the main loop (timer thread)
		MainLoop.disable();

	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		return connection;
	}
	
	// --- connection methods
	
	// TODO: push this to another thread
	
	private void connect(String hostname){
		
		// opening the connection is done in another thread
		
		ConnectTask ct = new ConnectTask(hostname, this);
		MainLoop.schedule(ct);
		
	}
	
	private void disconnect(){
		if(connection!=null){
			connection.close();
			
			// notify handlers about the change
			for(Handler h : handlers){
				Message m = h.obtainMessage(MessageFlag.DISCONNECTED);
				m.sendToTarget();
			}
		}
	}
	

	// --- connection notifications
	
	@Override
	public void notifyConnected(Player player) {

		Log.ln("connected to player \"" + player.info.getName() + "\"");
		
		// notify handlers about the change
		for(Handler h : handlers){
			Log.debug("sending connected message");
			Message m = h.obtainMessage(MessageFlag.CONNECTED, player);
			m.sendToTarget();
		}
		
	}

	/**
	 * notifyDisconnected gets called only in case the connection fails
	 * not if the connection is shut down
	 * 
	 * @Override 
	 */
	public void notifyDisconnected(ISocket sock, UserException reason) {
		Log.ln("disconnected, oh no (" + reason + ")");
		
		// inform the user:
		String toast = this.getResources().getString(R.string.connection_failed, sock);
		Toast.makeText(this, toast, Toast.LENGTH_LONG);
		
		// notify handlers about the change
		for(Handler h : handlers){
			Message m = h.obtainMessage(MessageFlag.DISCONNECTED);
			m.sendToTarget();
		}
	}
	
	
	// --- Options Menu
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater mi = getMenuInflater();
		mi.inflate(R.menu.options_menu, menu);
		
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		switch(item.getItemId()){
		
		case R.id.options_menu_connect:
			showDialog(CONNECT_DIALOG);
			break;
			
		case R.id.options_menu_disconnect:
			Log.ln("disconnect button pressed");
			
			disconnect();
			
			break;
			
		}
		
		return true;
	}
	

	// --- dialogs
	
	@Override
	protected Dialog onCreateDialog(int id) {
		
		switch(id){
		
		// --- connection dialog
		case CONNECT_DIALOG:
			
			// create connect dialog
			ConnectDialog cDialog = new ConnectDialog(this);

			// fill auto complete with last 5 hostnames
			
			String hostname = preference.getString(LAST_HOSTNAME, "");
			cDialog.setHostname(hostname);
			
			// register callback listener
			
			cDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface dialog) {
					
					// connect to host
					String hostname = ((ConnectDialog)dialog).getSelectedHostname();
					connect(hostname);
					
					// save new address in preferences
					SharedPreferences.Editor editor = preference.edit();
					editor.putString(LAST_HOSTNAME, hostname);
					editor.commit();
				}
			});
			
			
			return cDialog;
			
		// --- volume dialog
		case VOLUME_DIALOG:
			volumeDialog = new VolumeDialog(this);
			volumeDialog.setOnKeyListener(playerHandler);
			
			volumeDialog.setVolume(playerHandler.player.state.getVolume());
			
			return volumeDialog;
		}
		
		Log.bug("onCreateDialog(" + id + ") ... we shouldn't be here");
		return null;
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		switch(keyCode){
		
		case KeyEvent.KEYCODE_VOLUME_UP:
			showDialog(VOLUME_DIALOG);
			return true;
			
		case KeyEvent.KEYCODE_VOLUME_DOWN:
			showDialog(VOLUME_DIALOG);
			return true;
		}
		
		return false;
	}

	public VolumeDialog getVolumeDialog() {
		return volumeDialog;
	}

	public void setConnection(Connection connection) {
		this.connection = connection;
	}
	
}
