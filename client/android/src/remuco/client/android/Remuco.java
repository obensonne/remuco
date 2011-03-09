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

import remuco.client.android.dialogs.RatingDialog;
import remuco.client.common.data.ClientInfo;
import remuco.client.common.util.Log;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
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

public class Remuco extends RemucoActivity implements OnClickListener{
	
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

	
	/**
	 * Central utility method to create a client info.
	 *
	 * @param imgSize
	 */
	public static ClientInfo buildClientInfo(int imgSize) {
		// TODO: this should be configurable by a user
        // create extra info
        Hashtable<String,String> info = new Hashtable<String,String>();
        info.put("name", "Android Client on \"" + android.os.Build.MODEL + "\"");
        Log.ln("running on : " + android.os.Build.MODEL);
        // afaik every android (so far) has a touchscreen and is using unicode
        info.put("touch", "yes");
        info.put("utf8", "yes");
        return new ClientInfo(imgSize, "PNG", 50, info);
	}

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

        ctrlProgressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                int start = 0;

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress,
                                              boolean fromUser) {
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    start = ctrlProgressBar.getProgress();
                }
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    if (player == null ||
                        player.getPlayer() == null)
                        return;
                    int end = ctrlProgressBar.getProgress();
                    player.getPlayer().ctrlSeek((end - start));
                }
            });

		// TODO: externalize these
		infoTitle.setText("not connected");
		infoArtist.setText("use the menu to connect");
		infoAlbum.setText("to your remuco server");

		// --- create view handler
		viewHandler = new ViewHandler(this);

		// ------
		// remuco related initialization
		// ------
	}
	
	@Override
	protected void onResume() {
		super.onResume();

		// --- register view handler at player
        player.addHandler(viewHandler);

        if (player.getPlayer() != null &&
            player.getPlayer().getConnection() != null &&
            !player.getPlayer().getConnection().isClosed()) {
            viewHandler.setRunning(true);
        }
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
        if (super.onOptionsItemSelected(item)) return true;
		
		switch(item.getItemId()){
		
		case R.id.options_menu_library:
            final Intent intent = new Intent(this, RemucoLibraryTab.class);
            startActivity(intent);
			return true;
			
		case R.id.options_menu_rate:
			showDialog(RATING_DIALOG);
			return true;
		}
		
		return false;
	}
	
	// ------------------------
	// --- dialogs
	// ------------------------
	
	@Override
	protected Dialog onCreateDialog(int id) {
        Dialog d = super.onCreateDialog(id);
        if (d != null) return d;

		switch(id){
		// --- rating dialog
		case RATING_DIALOG:
			return new RatingDialog(this, player);
		}
		return null;
	}
	
	// --- handle clicks (this is the actual playback control)
	
	@Override
	public void onClick(View v) {
        if (player == null ||
            player.getPlayer() == null)
            return;

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
	
}
