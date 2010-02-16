package remuco.client.android;

import java.util.Timer;
import java.util.TimerTask;

import remuco.client.android.dialogs.VolumeDialog;
import remuco.client.common.data.Item;
import remuco.client.common.data.State;
import remuco.client.common.player.IItemListener;
import remuco.client.common.player.IProgressListener;
import remuco.client.common.player.IStateListener;
import remuco.client.common.player.Player;
import remuco.client.common.util.Log;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class ViewHandler extends Handler implements IItemListener, IProgressListener, IStateListener {

	Remuco remuco;
	
	Player player;

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
	
	RatingBar infoRatingBar;
	
	int tick;
	Timer tickTimer;
	
	byte[] imageCache;
	
	public ViewHandler(Remuco remuco) {
		this.remuco = remuco;

		// get view handles
		infoTitle 	= (TextView) remuco.findViewById(R.id.infoTitle);
		infoArtist 	= (TextView) remuco.findViewById(R.id.infoArtist);
		infoAlbum 	= (TextView) remuco.findViewById(R.id.infoAlbum);

		infoCover 	= (ImageView) remuco.findViewById(R.id.infoCover);
		
		infoRatingBar = (RatingBar) remuco.findViewById(R.id.infoRatingBar);
		
		ctrlProgressBar = (SeekBar) remuco.findViewById(R.id.CtrlProgressBar);
		ctrlLength = (TextView) remuco.findViewById(R.id.CtrlLength);
		ctrlProgress = (TextView) remuco.findViewById(R.id.CtrlProgress);
		
		ctrlPrev = (ImageButton) remuco.findViewById(R.id.CtrlPrev);
		ctrlPlay = (ImageButton) remuco.findViewById(R.id.CtrlPlay);
		ctrlNext = (ImageButton) remuco.findViewById(R.id.CtrlNext);
		
		
		
		TimerTask tickTask = new TimerTask() {
			@Override
			public void run() {
				sendEmptyMessage(MessageFlag.TICK);
			}
		};
		tickTimer = new Timer();
		tickTimer.scheduleAtFixedRate(tickTask, 0, 1000);
		
		
		
		infoTitle.setText("not connected");
		infoArtist.setText("use the menu to connect");
		infoAlbum.setText("to your remuco server");
	}

	@Override
	public void handleMessage(Message msg) {

		switch(msg.what){

		
		case MessageFlag.CONNECTED:
			
			Log.ln("[VH] CONNECTED!");
			
			// obj should be of type Player
			if(!(msg.obj instanceof Player)) break;
			player = (Player)msg.obj;
			
			// inform the user
			String toast = String.format(
					remuco.getResources().getText(R.string.connection_successful).toString(), 
					player.info.getName()
					);
			Toast.makeText(remuco, toast, Toast.LENGTH_SHORT).show();
			
			// set player listeners
			player.setItemListener(this);
			player.setProgressListener(this);
			player.setStateListener(this);
			
			// enable buttons
			ctrlPrev.setClickable(true);
			ctrlPlay.setClickable(true);
			ctrlNext.setClickable(true);
			
			// update rating bar
			infoRatingBar.setNumStars(player.info.getMaxRating());
			
			// update the display
			sendEmptyMessage(MessageFlag.ITEM_CHANGED);

			break;
			
		case MessageFlag.DISCONNECTED:
			
			Log.ln("[VH] DISCONNECTED!");
			
			// remove player reference
			player = null;

			// disable buttons
			ctrlPrev.setClickable(false);
			ctrlPlay.setClickable(false);
			ctrlNext.setClickable(false);

			// show ape picture
			infoCover.setImageResource(R.drawable.remuco_128);
			
			// change text
			infoTitle.setText("not connected");
			infoArtist.setText("use the menu to connect");
			infoAlbum.setText("to your remuco server");
			
			break;
			
		case MessageFlag.ITEM_CHANGED:
			
			Log.ln("[VH] item changed");

			// get song metadata
			String title = player.item.getMeta(Item.META_TITLE);
			String artist = player.item.getMeta(Item.META_ARTIST);
			String album = player.item.getMeta(Item.META_ALBUM);
			
			Log.ln(String.format("now playing: \"%s\" by \"%s\" on \"%s\"",
					title, artist, album));
			
			// set song metadata
			infoTitle.setText(title);
			infoArtist.setText(artist);
			infoAlbum.setText(album);

			// set image
			byte[] image = player.item.getImg();
			setImage(image);
			
			// update rating bar
			infoRatingBar.setProgress(player.item.getRating());
			
			// done
			break;


		case MessageFlag.PROGRESS_CHANGED:
			
			Log.ln("[VH] progress changed");
			
			// get progress
			int length = player.progress.getLength();
			int progress =  player.progress.getProgress();
			
			// set progress bar
			ctrlProgressBar.setMax(length);
			
			// set info fields
			ctrlLength.setText(
					String.format("%02d:%02d", 
							length/60, 
							length%60)
					);
			
			// set tick
			tick = progress;
			
			
			break;
			
		case MessageFlag.TICK:
			
			/*
			 * the tick message ... tick is send every second
			 * this is used to update the progress bar every second
			 * although the player sends its updates only every fifth second
			 */
			
			// don't update if the player is not running
			if(player == null || player.state.getPlayback() != State.PLAYBACK_PLAY) break;
			
			tick++;
			
			ctrlProgressBar.setProgress(tick);
			
			ctrlProgress.setText(
					String.format("%02d:%02d", 
							tick/60,
							tick%60)
					);
			
			break;
			
			
		case MessageFlag.STATE_CHANGED:
			
			Log.ln("[VH] state changed");
			
			
			// toggle playbutton icon
			if(player.state.getPlayback() == State.PLAYBACK_PLAY){
				ctrlPlay.setImageResource(android.R.drawable.ic_media_pause);
			} else {
				ctrlPlay.setImageResource(android.R.drawable.ic_media_play);
			}
			
			// set volume
			VolumeDialog vDialog = remuco.getVolumeDialog();
			
			if(vDialog!=null)
				vDialog.setVolume(player.state.getVolume());
			
			
			break;
			
		}


	}

	// --- convert byte[] to image
	
	private void setImage(byte[] image) {
		if(image == null || image.length == 0){
			infoCover.setImageResource(R.drawable.remuco_128);
		} else {
			infoCover.setImageBitmap(
					BitmapFactory.decodeByteArray(image, 0, image.length)
			);
			
			imageCache = image;
		}
	}

	// --- player notification listeners
	
	@Override
	public void notifyItemChanged() {
		sendEmptyMessage(MessageFlag.ITEM_CHANGED);
	}

	@Override
	public void notifyProgressChanged() {
		sendEmptyMessage(MessageFlag.PROGRESS_CHANGED);
	}
	
	@Override
	public void notifyStateChanged() {
		sendEmptyMessage(MessageFlag.STATE_CHANGED);
	}
	
}
