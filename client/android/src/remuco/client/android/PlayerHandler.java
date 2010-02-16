package remuco.client.android;

import remuco.client.android.dialogs.VolumeDialog;
import remuco.client.common.player.Player;
import remuco.client.common.util.Log;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.RatingBar;
import android.widget.RatingBar.OnRatingBarChangeListener;

public class PlayerHandler extends Handler implements OnClickListener, OnKeyListener, OnRatingBarChangeListener{

	Remuco remuco;
	
	Player player;
	
	ImageButton ctrlPrev;
	ImageButton ctrlPlay;
	ImageButton ctrlNext;
	
	RatingBar infoRatingBar;
	
	public PlayerHandler(Remuco remuco) {
		this.remuco = remuco;
		
		// get view handles
		ctrlPrev = (ImageButton) remuco.findViewById(R.id.CtrlPrev);
		ctrlPlay = (ImageButton) remuco.findViewById(R.id.CtrlPlay);
		ctrlNext = (ImageButton) remuco.findViewById(R.id.CtrlNext);
		
		infoRatingBar = (RatingBar) remuco.findViewById(R.id.infoRatingBar);
		
		// register onclick listener
		ctrlPrev.setOnClickListener(this);
		ctrlPlay.setOnClickListener(this);
		ctrlNext.setOnClickListener(this);
		
		infoRatingBar.setOnRatingBarChangeListener(this);
	}
	
	@Override
	public void handleMessage(Message msg) {
		
		switch(msg.what){
		
		case MessageFlag.CONNECTED:
			
			Log.ln("[PH] CONNECTED!");
			
			// obj should be of type Player
			if(!(msg.obj instanceof Player)) break;
			player = (Player)msg.obj;

			break;
			
		case MessageFlag.DISCONNECTED:
			
			// remove player reference
			player = null;

			break;
		
		}
		
	}

	@Override
	public void onClick(View v) {
		
		if(v.equals(ctrlPrev)){
			player.ctrlPrev();
		}
		
		if(v.equals(ctrlPlay)){
			player.ctrlPlayPause();
		}
		
		if(v.equals(ctrlNext)){
			player.ctrlNext();
		}
		
	}

	@Override
	public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {

		
		// handle volume dialog events
		if( dialog instanceof VolumeDialog ){
			
			switch(keyCode){
			case KeyEvent.KEYCODE_VOLUME_DOWN:
				player.ctrlVolume(-1);
				break;
				
			case KeyEvent.KEYCODE_VOLUME_UP:
				player.ctrlVolume(1);
				break;
			}

			return true;
		}
		
		return false;
	}

	@Override
	public void onRatingChanged(RatingBar bar, float rating, boolean fromUser) {
		
		if(fromUser && (bar.getId() == R.id.infoRatingBar || bar.getId() == R.id.rating_dialog_rating_bar)){
			
			Log.ln("[PH] on rating changed: " + rating + ", fromuser: " + fromUser );
			player.ctrlRate((int)Math.ceil(rating));
			
			// hackish stuff
//			bar.setRating((float) Math.ceil(rating));
		}
		
	}
	
}
