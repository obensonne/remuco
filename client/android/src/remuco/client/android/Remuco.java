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

import remuco.client.android.io.WifiSocket;
import remuco.client.android.dialogs.ConnectDialog;
import remuco.client.android.dialogs.RatingDialog;
import remuco.client.android.dialogs.VolumeDialog;
import remuco.client.common.util.Log;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class Remuco extends RemucoActivity implements OnClickListener{

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

		// --- create view handler
		viewHandler = new ViewHandler(this);
		
		// ------
		// remuco related initialization
		// ------
		
		// --- register view handler at player
		player.addHandler(viewHandler);
	}
	
	@Override
	protected void onResume() {
		super.onResume();

        if (player.getPlayer() != null &&
            player.getPlayer().getConnection() != null &&
            !player.getPlayer().getConnection().isClosed())
            viewHandler.setRunning(true);
	}

    public PlayerAdapter getPlayer(){
        return player;
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
			
		case R.id.options_menu_library:
            final Intent intent = new Intent(this, RemucoLibraryPlaylist.class);
            startActivity(intent);
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
                    player.disconnect();

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
