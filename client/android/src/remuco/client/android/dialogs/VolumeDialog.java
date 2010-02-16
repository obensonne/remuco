package remuco.client.android.dialogs;

import java.util.Timer;
import java.util.TimerTask;

import remuco.client.android.R;
import remuco.client.common.util.Log;
import android.app.Dialog;
import android.content.Context;
import android.widget.SeekBar;

public class VolumeDialog extends Dialog {

	SeekBar volumeBar;
	
	
	Timer t;
	
	public VolumeDialog(Context context) {
		super(context);
		
		Log.ln("[VD] VolumeDialog-Constructor");

		setContentView(R.layout.volume_dialog);
		setTitle(R.string.volume);
		
		volumeBar = (SeekBar) findViewById(R.id.volume_dialog_volumebar);
	}

	public void resetTimer(){
		
		
		Log.ln("[VD] resetTimer()");
		
		// cancel old task
		if(t!=null) t.cancel();
		
		// create new timer
		t = new Timer();
		
		// create task
		TimerTask dismissTask = new TimerTask() {
			@Override
			public void run() {
				Log.ln("[VD] dismiss volume dialog");
				dismiss();
			}
		};

		// start task
		t.schedule(dismissTask, 2000);
		
	}
	
	public void setVolume(int volume) {
		Log.ln("[VD] set volume: " + volume);
		volumeBar.setProgress(volume);
		resetTimer();
	}

}
