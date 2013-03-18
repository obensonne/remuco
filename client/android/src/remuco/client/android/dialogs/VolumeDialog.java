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

import java.lang.ref.WeakReference;

import remuco.client.android.MessageFlag;
import remuco.client.android.PlayerAdapter;
import remuco.client.android.PlayerProvider;
import remuco.client.android.R;
import remuco.client.common.data.State;
import remuco.client.common.util.Log;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.DialogFragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

/**
 * Shows the volume of the player.
 * TODO: Control volume by controlling the volume bar
 * TODO: Faster client volume change reflection
 * TODO: Cleanup, how to set the player in a safe way, and removing the need for player != null
 */
public class VolumeDialog extends DialogFragment implements OnKeyListener{
	
	private SeekBar volumeBar;
	private PlayerAdapter player;
	private Handler dismissHandler;
	private VolumeHandler volumeHandler = new VolumeHandler(this);
	
	
	public static VolumeDialog newInstance(PlayerAdapter player) {
		VolumeDialog dialog = new VolumeDialog();
		return dialog;
	}
	
	
	//Approach from: http://stackoverflow.com/a/11336822
	static class VolumeHandler extends Handler {
		WeakReference<VolumeDialog> dialog;
		
		public VolumeHandler(VolumeDialog callback) {
			dialog = new WeakReference<VolumeDialog>(callback);
		}
		
		@Override
		public void handleMessage(Message msg) {
			if(msg.what == MessageFlag.STATE_CHANGED){
				State state = (State)msg.obj;
				VolumeDialog callback = dialog.get();
				if(callback != null) {
					callback.setVolume(state.getVolume());
				}
			}
		}
	}
	
	
	private Runnable dismissRunnable = new Runnable() {
		@Override
		public void run() {
			Log.debug("[VD] dismissing dialog (window timeout)");
			VolumeDialog.this.dismiss();
		}
	};
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.debug("[VD] onCreateView()");
		
		View view = inflater.inflate(R.layout.volume_dialog, container);
		getDialog().setTitle(R.string.volume_dialog_title);
        getDialog().setOnKeyListener(this);
		
		volumeBar = (SeekBar) view.findViewById(R.id.volume_dialog_volumebar);
		dismissHandler = new Handler();
		
		return view;
	}
	
	
	@Override
	public void onResume() {
		super.onResume();
		Log.debug("[VD] onResume called");
		
		try {
			PlayerProvider a = (PlayerProvider) getActivity();
			player = ((PlayerProvider) a).getPlayer();
			player.addHandler(volumeHandler);
	        updateVolume();
		} catch(ClassCastException e) {
			Log.bug("-- BaseFragment gots an unsupported activity type, expected a PlayerProvider.");
		}
		resetDismissTimeout();
	}
	
	
	@Override
	public void onPause() {
		super.onPause();
		Log.debug("[VD] onPause called");
		
		dismissHandler.removeCallbacks(dismissRunnable);
		if(player != null) {
			player.removeHandler(volumeHandler);
		}
		player = null;
	}
	
	
	@Override
	public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
		switch(keyCode){
		case KeyEvent.KEYCODE_VOLUME_UP:
			if(player != null && player.getPlayer() != null) {
				player.getPlayer().ctrlVolume(1);
			}
			resetDismissTimeout();
			return true;
		case KeyEvent.KEYCODE_VOLUME_DOWN:
			if(player != null && player.getPlayer() != null) {
				player.getPlayer().ctrlVolume(-1);
			}
			resetDismissTimeout();
			return true;
		}
		
		return false;
	}
	
	
	/**
	 * Sets the progressbar volume, by retrieving the value from the player.
	 */
	private void updateVolume() {
		Log.debug("[VD] update volume bar");
		if (volumeBar == null) {
			return;
		}
		
        if (player != null && player.getPlayer() != null && player.getPlayer().state != null) {
            setVolume(player.getPlayer().state.getVolume());
		}
	}
	
	/**
	 * Updates the volume bar by setting its value.
	 * @param volume
	 */
	private void setVolume(int volume) {
		volumeBar.setProgress(volume);
	}
	
	
	private void resetDismissTimeout(){
		Log.debug("[VD] set timeout to 2000ms");
		dismissHandler.removeCallbacks(dismissRunnable);
		dismissHandler.postDelayed(dismissRunnable, 2000);
	}
	
}
