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
import remuco.client.android.util.AndroidLogPrinter;
import remuco.client.common.MainLoop;
import remuco.client.common.data.ClientInfo;
import remuco.client.common.util.Log;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Display;
import android.view.WindowManager;

public class RemucoActivity extends Activity{

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
		
        // --- enable the remuco main loop (timer thread)
        MainLoop.enable();

		// --- create player adapter
        player = new PlayerAdapter();

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

}
