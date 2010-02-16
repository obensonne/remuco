package remuco.client.android.dialogs;

import remuco.client.android.PlayerHandler;
import remuco.client.android.R;
import remuco.client.android.Remuco;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Path.FillType;
import android.widget.RatingBar;

public class RatingDialog extends Dialog {

	private RatingBar ratingBar;
	
	public RatingDialog(Remuco remuco) {
		super(remuco);

		setContentView(R.layout.rating_dialog);
		setTitle("Rate Song");
		
		ratingBar = (RatingBar) findViewById(R.id.rating_dialog_rating_bar);
		
		// TODO: hackish ... should be dynamic
		ratingBar.setNumStars(5);
	}

	public void setPlayHandler(PlayerHandler playerHandler) {
		ratingBar.setOnRatingBarChangeListener(playerHandler);
	}
	
	

}
