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

package remuco.client.android;

import java.util.Hashtable;

import remuco.client.android.io.WifiSocket;
import remuco.client.android.dialogs.ConnectDialog;
import remuco.client.android.dialogs.RatingDialog;
import remuco.client.android.dialogs.VolumeDialog;
import remuco.client.android.util.AndroidLogPrinter;
import remuco.client.common.MainLoop;
import remuco.client.common.data.ClientInfo;
import remuco.client.common.data.PlayerInfo;
import remuco.client.common.util.Log;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class Remuco extends Activity implements OnClickListener{

	// --- preferences
	private SharedPreferences preference;
	
	public static final String PREF_NAME = "remucoPreference";
	private static final String LAST_TYPE = "connect_dialog_last_type";
	private static final String LAST_HOSTNAME = "connect_dialog_last_hostnames";
	private static final String LAST_PORT = "connect_dialog_last_ports";
	private static final String LAST_BLUEDEVICE = "connect_dialog_last_bluedevices";
	
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
	ImageButton ctrlShuffle;
	ImageButton ctrlRepeat;
	
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
		ctrlShuffle.setOnClickListener(this);
		ctrlRepeat.setOnClickListener(this);
		
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
		
		// get screen size
        WindowManager w = getWindowManager();
        Display d = w.getDefaultDisplay();
        int width = d.getWidth();
        int height = d.getHeight(); 
        
        Log.debug("screensize: " + width + "x" + height);
		
        // use the smaller dimension
        int imageSize = Math.min(width, height);
        Log.debug("preferred image size: " + imageSize);

        // create extra info

		Hashtable<String,String> info = new Hashtable<String,String>();
		
		info.put("name", "Android Client on \"" + android.os.Build.MODEL + "\"");
		Log.ln("running on : " + android.os.Build.MODEL);
		
		// afaik every android (so far) has a touchscreen and is using unicode
		info.put("touch", "yes");
		info.put("utf8", "yes");
        
        
        // create client info
		clientInfo = new ClientInfo(imageSize, "PNG", 50, info);
		
		// ------
		// communication initialization
		// ------
		
		// --- create player adapter
		player = new PlayerAdapter();
		
		// --- register view handler at player
		player.addHandler(viewHandler);
		
		
		// --- try to connect to the last hostname
        int lastType = preference.getInt(LAST_TYPE, R.id.connect_dialog_wifi);
		String lastHostname = preference.getString(LAST_HOSTNAME, "");
		int lastPort = preference.getInt(LAST_PORT, WifiSocket.PORT_DEFAULT);
        String lastBluedevice = preference.getString(LAST_BLUEDEVICE, "");
		if ((lastType == R.id.connect_dialog_wifi) && (!lastHostname.equals(""))) {
			player.connectWifi(lastHostname, lastPort, clientInfo);
		} else if ((lastType == R.id.connect_dialog_bluetooth) && (!lastBluedevice.equals(""))) {
			player.connectBluetooth(lastBluedevice, clientInfo);
		}
		
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
		ctrlShuffle = (ImageButton) findViewById(R.id.CtrlShuffle);
		ctrlRepeat = (ImageButton) findViewById(R.id.CtrlRepeat);
	}

	
	
	/**
	 * this method gets called after on create
	 */
	@Override
	protected void onResume() {
		super.onStart();
		
		Log.debug("--- onResume()");

		// --- wake up the connection
		player.resumeConnection();

		

		
		
		
	}
	
	@Override
	protected void onPause() {
		super.onStop();
		Log.debug("--- onPause()");
		
		// --- pause the connection if possible
		player.pauseConnection();
		
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();

		Log.debug("--- onDestroy()");

		// stop the connection
		player.disconnect();
		
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
                    int type = ((ConnectDialog)dialog).getSelectedType();
					String hostname = ((ConnectDialog)dialog).getSelectedHostname();
                    int port = ((ConnectDialog)dialog).getSelectedPort();
                    String bluedevice = ((ConnectDialog)dialog).getSelectedBluedevice();
                    if (type == R.id.connect_dialog_wifi) {
                        player.connectWifi(hostname, port, clientInfo);
                    } else if (type == R.id.connect_dialog_bluetooth) {
                        player.connectBluetooth(bluedevice, clientInfo);
                    }
					
					// save new address in preferences
					SharedPreferences.Editor editor = preference.edit();
					editor.putInt(LAST_TYPE, type);
					editor.putString(LAST_HOSTNAME, hostname);
                    editor.putInt(LAST_PORT, port);
                    editor.putString(LAST_BLUEDEVICE, bluedevice);
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
			
			// set last hostname port
			int type = preference.getInt(LAST_TYPE, R.id.connect_dialog_wifi);
			cDialog.setType(type);
			String hostname = preference.getString(LAST_HOSTNAME, "");
			cDialog.setHostname(hostname);
			int port = preference.getInt(LAST_PORT, WifiSocket.PORT_DEFAULT);
			cDialog.setPort(port);
            String bluedevice = preference.getString(LAST_BLUEDEVICE, "");
			cDialog.setBluedevice(bluedevice);
			
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
			
		case R.id.CtrlShuffle:
			player.getPlayer().ctrlToggleShuffle();
			break;
			
		case R.id.CtrlRepeat:
			player.getPlayer().ctrlToggleRepeat();
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
