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
package remuco.client.android.dialogs;

import remuco.client.android.MessageFlag;
import remuco.client.android.PlayerAdapter;
import remuco.client.android.R;
import remuco.client.common.data.State;
import remuco.client.common.util.Log;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.widget.SeekBar;

public class VolumeDialog extends Dialog {

	private SeekBar volumeBar;
	private PlayerAdapter player;
	
	private Handler dismissHandler;
	
	private Runnable dismissRunnable = new Runnable() {
		@Override
		public void run() {
			Log.debug("[VD] dismissing dialog");
			VolumeDialog.this.dismiss();
		}
	};
	
	public VolumeDialog(Context context, PlayerAdapter player) {
		super(context);
		
		this.player = player;
		this.dismissHandler = new Handler();
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.debug("[VD] onCreate()");
		setTitle(R.string.volume_dialog_title);
		setContentView(R.layout.volume_dialog);
		
		volumeBar = (SeekBar) findViewById(R.id.volume_dialog_volumebar);

        if (player.getPlayer() != null && player.getPlayer().state != null) {
            volumeBar.setProgress(player.getPlayer().state.getVolume());
		}

		player.addHandler(new Handler(){
			@Override
			public void handleMessage(Message msg) {
				if(msg.what == MessageFlag.STATE_CHANGED){
					State state = (State)msg.obj;
					volumeBar.setProgress(state.getVolume());
				}
			}
		});
		
		resetDismissTimeout();
	}
	
	private void resetDismissTimeout(){
		Log.debug("[VD] set timeout to 2000ms");
		dismissHandler.removeCallbacks(dismissRunnable);
		dismissHandler.postDelayed(dismissRunnable, 2000);
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (player.getPlayer() == null) return false;

		switch(keyCode){
		case KeyEvent.KEYCODE_VOLUME_UP:
			player.getPlayer().ctrlVolume(1);
			resetDismissTimeout();
			return true;
		case KeyEvent.KEYCODE_VOLUME_DOWN:
			player.getPlayer().ctrlVolume(-1);
			resetDismissTimeout();
			return true;
		}
		return false;
	}


}
