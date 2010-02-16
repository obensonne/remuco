
package remuco.client.android;

import remuco.client.android.dialogs.ConnectDialog;
import remuco.client.android.dialogs.RatingDialog;
import remuco.client.android.dialogs.VolumeDialog;
import remuco.client.android.util.AndroidLogPrinter;
import remuco.client.common.MainLoop;
import remuco.client.common.data.ClientInfo;
import remuco.client.common.data.State;
import remuco.client.common.util.Log;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.SeekBar;
import android.widget.TextView;

public class Remuco extends Activity implements OnClickListener{

	// --- preferences
	private SharedPreferences preference;
	
	public static final String PREF_NAME = "remucoPreference";
	private static final String LAST_HOSTNAME = "connect_dialog_last_hostnames";
	
	// --- the player adapter
	private PlayerAdapter player;
	
	
	
	// --- client info
	private ClientInfo clientInfo;
	
	// --- dialog ids
	private static final int CONNECT_DIALOG = 1;
	private static final int VOLUME_DIALOG = 2;
	private static final int RATING_DIALOG = 3;
	
	// --- dialog reference
	private VolumeDialog volumeDialog;
	
	// --- view handler
	private ViewHandler viewHandler;
	
	// --- view handles
	
	// get view handles
	TextView infoTitle;
	TextView infoArtist;
	TextView infoAlbum;

	ImageView infoCover;

	SeekBar ctrlProgressBar;
	TextView ctrlProgress;
	TextView ctrlLength;
	
	ImageButton ctrlPrev;
	ImageButton ctrlPlay;
	ImageButton ctrlNext;
	
	RatingBar infoRatingBar;

	
	// -----------------------------------------------------------------------------
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
		
		// --- get view handles
		getViewHandles();
		
		// --- set listeners
		ctrlPlay.setOnClickListener(this);
		ctrlPrev.setOnClickListener(this);
		ctrlNext.setOnClickListener(this);
		
		// TODO: externalize these
		infoTitle.setText("not connected");
		infoArtist.setText("use the menu to connect");
		infoAlbum.setText("to your remuco server");

		// --- load preferences
		preference = getPreferences(Context.MODE_PRIVATE);

		// --- create view handler
		viewHandler = new ViewHandler(this);
		
		// ------
		// remuco related initialization
		// ------
		
		// --- set log output (classes in common use Log for logging)
		Log.setOut(new AndroidLogPrinter());
		
		// --- enable the remuco main loop (timer thread)
		MainLoop.enable();
		
		// --- construct client info
		clientInfo = new ClientInfo(250, "PNG", 50, null);
		
		// ------
		// communication initialization
		// ------
		
		// --- create player adapter
		player = new PlayerAdapter();
		
		// --- register view handler at player
		player.addHandler(viewHandler);
		
	}
	
	private void getViewHandles() {
		// get view handles
		infoTitle 	= (TextView) findViewById(R.id.infoTitle);
		infoArtist 	= (TextView) findViewById(R.id.infoArtist);
		infoAlbum 	= (TextView) findViewById(R.id.infoAlbum);

		infoCover 	= (ImageView) findViewById(R.id.infoCover);
		
		infoRatingBar = (RatingBar) findViewById(R.id.infoRatingBar);
		
		ctrlProgressBar 	= (SeekBar) findViewById(R.id.CtrlProgressBar);
		ctrlLength 			= (TextView) findViewById(R.id.CtrlLength);
		ctrlProgress 		= (TextView) findViewById(R.id.CtrlProgress);
		
		ctrlPrev = (ImageButton) findViewById(R.id.CtrlPrev);
		ctrlPlay = (ImageButton) findViewById(R.id.CtrlPlay);
		ctrlNext = (ImageButton) findViewById(R.id.CtrlNext);
	}

	/**
	 * this method gets called after on create
	 */
	@Override
	protected void onResume() {
		super.onStart();
		
		Log.debug("--- onResume()");

		// --- initialize the connection here if we can

		
		// try to connect to the last hostname
		String lastHostname = preference.getString(LAST_HOSTNAME, "");
		if( !lastHostname.equals("") ){
			player.connect(lastHostname, clientInfo);
		}
		
		
		
	}
	
	@Override
	protected void onPause() {
		super.onStop();
		Log.debug("--- onPause()");
		
		// stop the connection
		player.disconnect();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();

		Log.debug("--- onDestroy()");

		// disable the main loop (timer thread)
		MainLoop.disable();

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
			player.disconnect();
			break;
			
		case R.id.options_menu_rate:
			showDialog(RATING_DIALOG);
			break;
		}
		
		return true;
	}
	
	// ------------------------
	// --- dialogs
	// ------------------------
	
	@Override
	protected Dialog onCreateDialog(int id) {

		switch(id){
		
		// --- connection dialog
		case CONNECT_DIALOG:
			
			// create connect dialog
			ConnectDialog cDialog = new ConnectDialog(this, player);

			// register callback listener
			
			cDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface dialog) {
					
					// connect to host
					String hostname = ((ConnectDialog)dialog).getSelectedHostname();
					player.connect(hostname, clientInfo);
					
					// save new address in preferences
					SharedPreferences.Editor editor = preference.edit();
					editor.putString(LAST_HOSTNAME, hostname);
					editor.commit();
				}
			});
			
			
			return cDialog;
			
		// --- volume dialog
		case VOLUME_DIALOG:
			return new VolumeDialog(this, player);
			
		// --- rating dialog
		case RATING_DIALOG:
			return new RatingDialog(this, player);
		}
		
		Log.bug("onCreateDialog(" + id + ") ... we shouldn't be here");
		return null;
	}
	
	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		
		switch(id){
		
		case CONNECT_DIALOG:
			
			ConnectDialog cDialog = (ConnectDialog)dialog;
			
			// set last hostname
			String hostname = preference.getString(LAST_HOSTNAME, "");
			cDialog.setHostname(hostname);
			
			break;
		
		}
		
		
	}
	
	
	// --- handle clicks (this is the actual playback control)
	
	@Override
	public void onClick(View v) {
		switch(v.getId()){
		case R.id.CtrlPlay:
			player.getPlayer().ctrlPlayPause();
			break;
			
		case R.id.CtrlPrev:
			player.getPlayer().ctrlPrev();
			break;
			
		case R.id.CtrlNext:
			player.getPlayer().ctrlNext();
			break;
		}
	};	
	
	
	
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
	
}
