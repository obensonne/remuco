package remuco.client.android.dialogs;

import remuco.client.android.MessageFlag;
import remuco.client.android.PlayerAdapter;
import remuco.client.android.R;
import remuco.client.android.Remuco;
import remuco.client.common.data.Item;
import remuco.client.common.data.PlayerInfo;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.RatingBar.OnRatingBarChangeListener;

public class RatingDialog extends Dialog implements OnRatingBarChangeListener, OnClickListener{

	private RatingBar ratingBar;
	private Button okButton;
	
	private PlayerAdapter player;
	
	
	public RatingDialog(Remuco remuco, PlayerAdapter player) {
		super(remuco);

		this.player = player;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.rating_dialog);
		setTitle(R.string.rating_dialog_title);
		
		
		// configure rating bar
		ratingBar = (RatingBar) findViewById(R.id.rating_dialog_rating_bar);
		
		// TODO: this will break pretty sure as soon as there is no player or item ...
		ratingBar.setNumStars(player.getPlayer().info.getMaxRating());
		ratingBar.setProgress(player.getPlayer().item.getRating());
		ratingBar.setStepSize(1);
		
		ratingBar.setOnRatingBarChangeListener(this);

		// configure ok button
		okButton = (Button) findViewById(R.id.rating_dialog_ok_button);
		
		okButton.setOnClickListener(this);
		
		player.addHandler(new Handler(){
			@Override
			public void handleMessage(Message msg) {
				switch(msg.what){
				
				case MessageFlag.ITEM_CHANGED:
					Item item = (Item)msg.obj;
					ratingBar.setProgress(item.getRating());
					break;
					
				case MessageFlag.CONNECTED:
					PlayerInfo playerInfo = (PlayerInfo)msg.obj;
					ratingBar.setNumStars(playerInfo.getMaxRating());
				}
			}
		});
	}

	@Override
	public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
		if(!fromUser) return;
		player.getPlayer().ctrlRate((int)Math.ceil(rating));
	}

	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.rating_dialog_ok_button){
			dismiss();
		}
	}
	

}
