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

import remuco.client.android.dialogs.ConnectDialog;
import remuco.client.android.dialogs.SearchDialog;
import remuco.client.android.dialogs.VolumeDialog;
import remuco.client.android.io.WifiSocket;
import remuco.client.android.util.AndroidLogPrinter;
import remuco.client.common.data.ClientInfo;
import remuco.client.common.util.Log;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MenuItem;

public class RemucoActivity extends Activity{

	// --- dialog ids
	protected static final int CONNECT_DIALOG = 1;
	protected static final int VOLUME_DIALOG = 2;
	protected static final int RATING_DIALOG = 3;
	protected static final int SEARCH_DIALOG = 4;

	// --- dialog reference
	private VolumeDialog volumeDialog;

	// --- preferences
	protected SharedPreferences preference;
	
	private static final String PREF_NAME = "remucoPreference";
	protected static final String LAST_TYPE = "connect_dialog_last_type";
	protected static final String LAST_HOSTNAME = "connect_dialog_last_hostnames";
	protected static final String LAST_PORT = "connect_dialog_last_ports";
	protected static final String LAST_BLUEDEVICE = "connect_dialog_last_bluedevices";
	
	// --- the player adapter
	protected PlayerAdapter player;
	
	// --- client info
	protected ClientInfo clientInfo;
	
	// -----------------------------------------------------------------------------
	// --- lifecycle methods
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Log.debug("--- " + this.getClass().getName() + ".onCreate()");
		
		// ------
		// android related initialization
		// ------
		
		// --- load preferences
		preference = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

		// ------
		// remuco related initialization
		// ------
		
		// --- set log output (classes in common use Log for logging)
		Log.setOut(new AndroidLogPrinter());
		
		// --- construct client info
		
		// get screen size
        Display d = getWindowManager().getDefaultDisplay();
        int imgSize = Math.min(d.getWidth(), d.getHeight());

        clientInfo = Remuco.buildClientInfo(imgSize);
        
		// ------
		// communication initialization
		// ------
		
		// --- create player adapter
        player = connect(this.getApplicationContext(), imgSize);
	}

    public static PlayerAdapter connect(Context context, int imgSize) {
    	// FIXME: Why is this implemented in a static context?
    	// The problem is that this requires to manage a ClientInfo object
    	// twice, in a static and in an instance context
    	// (`Remuco.buildClientContext()` is a quick fix for this issue).
    	// I guess this connection method is used to automatically connect to
    	// the last used server. However, this results in redundant connection
    	// code as a similar task is done in `onCreateDialog()`.

        // In onCreateDialog, we just reconnect to the server. In this method we
        // initialize the full connection giving the image size and client info.
        // It is called from `onCreate()`.

        // --- create player adapter
        PlayerAdapter player = new PlayerAdapter();

        // --- enable the Remuco Service
        context.startService(new Intent(context, RemucoService.class));

        // --- try to connect to the last hostname
        SharedPreferences preference = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        ClientInfo clientInfo = Remuco.buildClientInfo(imgSize);
        int lastType = preference.getInt(LAST_TYPE, R.id.connect_dialog_wifi);
        String lastHostname = preference.getString(LAST_HOSTNAME, "");
        int lastPort = preference.getInt(LAST_PORT, WifiSocket.PORT_DEFAULT);
        String lastBluedevice = preference.getString(LAST_BLUEDEVICE, "");
        if ((lastType == R.id.connect_dialog_wifi) && (!lastHostname.equals(""))) {
            player.connectWifi(lastHostname, lastPort, clientInfo);
        } else if ((lastType == R.id.connect_dialog_bluetooth) && (!lastBluedevice.equals(""))) {
            player.connectBluetooth(lastBluedevice, clientInfo);
        }

        return player;
    }
	
	/**
	 * this method gets called after on create
	 */
	@Override
	protected void onResume() {
		super.onResume();
		
		Log.debug("--- " + this.getClass().getName() + ".onResume()");

		// --- wake up the connection
		player.resumeConnection();
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.debug("--- " + this.getClass().getName() + ".onPause()");
		
		// --- pause the connection if possible
        player.clearHandlers();
        player.pauseConnection();
	}

	// --- Options Menu
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){
		
		case R.id.options_menu_connect:
			showDialog(CONNECT_DIALOG);
            return true;
			
		case R.id.options_menu_disconnect:
			Log.ln("disconnect button pressed");
			player.disconnect();
            return true;

        case R.id.options_menu_search:
			showDialog(SEARCH_DIALOG);
            return true;
		}
		
		return false;
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

            // --- Search dialog
        case SEARCH_DIALOG:
            return new SearchDialog(this, player);

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
		
		return super.onKeyDown(keyCode, event);
	}
	
}
