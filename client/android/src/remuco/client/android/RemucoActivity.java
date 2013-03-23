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
import remuco.client.android.dialogs.ConnectDialog.ConnectRequestHandler;
import remuco.client.android.dialogs.SearchDialog;
import remuco.client.android.dialogs.VolumeDialog;
import remuco.client.android.io.WifiSocket;
import remuco.client.android.util.AndroidLogPrinter;
import remuco.client.common.data.ClientInfo;
import remuco.client.common.util.Log;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MenuItem;

public class RemucoActivity extends FragmentActivity implements PlayerProvider, ConnectRequestHandler {

    // --- dialog ids
    protected static final int CONNECT_DIALOG = 1;
    protected static final int VOLUME_DIALOG = 2;
    protected static final int RATING_DIALOG = 3;
    protected static final int SEARCH_DIALOG = 4;

    // --- preferences
    protected SharedPreferences preference;

    private static final String PREF_NAME = "remucoPreference";
    protected static final String LAST_TYPE = "connect_dialog_last_type";
    protected static final String LAST_HOSTNAME = "connect_dialog_last_hostnames";
    protected static final String LAST_PORT = "connect_dialog_last_ports";
    protected static final String LAST_BLUEDEVICE = "connect_dialog_last_bluedevices";

    // --- the player adapter
    public PlayerAdapter player;

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

        // Build the client info
        Display d = getWindowManager().getDefaultDisplay();
        clientInfo = Client.buildClientInfo(d);

        // ------
        // communication initialization
        // ------

        // --- create player adapter
        player = connect(this.getApplicationContext(), clientInfo);
    }

    //FIXME
    // Bug in this code.
    // The service is started async AFTER the connect-call. The call fails the
    // first time the activity is called because the mainloop is not running.
    // This leads in a first-time start with no auto-connection.
    //
    public static PlayerAdapter connect(Context context, ClientInfo clientInfo) {
        // In onCreateDialog, we just reconnect to the server. In this method we
        // initialize the full connection giving the image size and client info.
        // It is called from `onCreate()`.

        // --- create player adapter
        PlayerAdapter player = new PlayerAdapter();

        // --- enable the Remuco Service
        context.startService(new Intent(context, RemucoService.class));

        // --- try to connect to the last hostname
        SharedPreferences preference = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

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

    @Override
    public PlayerAdapter getPlayer() {
        return this.player;
    }


    // ------------------------
    // --- Options Menu
    // ------------------------

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.debug("MENUITEMSELECTED "+item);
        switch(item.getItemId()){

        case R.id.options_menu_connect:
            showConnectDialog();
            return true;

        case R.id.options_menu_disconnect:
            Log.ln("disconnect button pressed");
            disconnect();
            return true;

        case R.id.options_menu_search:  //TODO: Remove
            Log.debug("SEARCHDIALOGTHING...");
            showDialog(SEARCH_DIALOG);
            return true;
        }

        return false;
    }


    // ------------------------
    // --- keys
    // ------------------------
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        switch(keyCode){

        case KeyEvent.KEYCODE_VOLUME_UP:
            if(player != null && player.getPlayer() != null) {
                player.getPlayer().ctrlVolume(+1);
            }
            showVolumeDialog();
            return true;

        case KeyEvent.KEYCODE_VOLUME_DOWN:
            if(player != null && player.getPlayer() != null) {
                player.getPlayer().ctrlVolume(-1);
            }
            showVolumeDialog();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }


    // ------------------------
    // --- dialogs
    // ------------------------

    private void showConnectDialog() {
        // create connect dialog
        FragmentManager fm = getSupportFragmentManager();
        ConnectDialog connectdialog = ConnectDialog.newInstance(
                preference.getInt(LAST_TYPE, R.id.connect_dialog_wifi),
                preference.getString(LAST_HOSTNAME, ""),
                preference.getInt(LAST_PORT, WifiSocket.PORT_DEFAULT),
                preference.getString(LAST_BLUEDEVICE, "")
                );

        connectdialog.show(fm, "dialog");
    }

    private void showVolumeDialog() {
        FragmentManager fm = getSupportFragmentManager();
        VolumeDialog volumedialog = VolumeDialog.newInstance(player);
        volumedialog.show(fm, "volumedialog");
    }

    
    @Override
    @Deprecated
    //TODO: Delete this dialog code when dialog is removed.
    protected Dialog onCreateDialog(int id) {
        if (id == SEARCH_DIALOG) {
            return new SearchDialog(this, player);
        }
        
        Log.bug("onCreateDialog(" + id + ") ... we shouldn't be here");
        return null;
    }

    
    // -----------------------
    // --- Connect methods
    // ---  (callbacks from connectdialog)
    // -----------------------

    @Override
    public void connectWifi(String hostname, int port) {
        // update preferences
        SharedPreferences.Editor editor = preference.edit();
        editor.putInt(LAST_TYPE, R.id.connect_dialog_wifi);
        editor.putString(LAST_HOSTNAME, hostname);
        editor.putInt(LAST_PORT, port);
        editor.commit();

        player.connectWifi(hostname, port, clientInfo);
    }

    @Override
    public void connectBluetooth(String bluedevice) {
        // update preferences
        SharedPreferences.Editor editor = preference.edit();
        editor.putInt(LAST_TYPE, R.id.connect_dialog_bluetooth);
        editor.putString(LAST_BLUEDEVICE, bluedevice);
        editor.commit();

        player.connectBluetooth(bluedevice, clientInfo);
    }

    
    public void disconnect() {
        try {
            player.disconnect();
        } catch(Exception e) {
            //Nothing to do
        }
    }
}
