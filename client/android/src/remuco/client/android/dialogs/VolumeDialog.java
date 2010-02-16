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
import android.widget.ProgressBar;

public class VolumeDialog extends Dialog {

	private ProgressBar volumeBar;
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
		
		volumeBar = (ProgressBar) findViewById(R.id.volume_dialog_volumebar);
		volumeBar.setProgress(player.getPlayer().state.getVolume());
		
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
