/*   
 *   Remuco - A remote control system for media players.
 *   Copyright (C) 2006-2013 by the Remuco team, see AUTHORS.
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
import remuco.client.android.R;
import remuco.client.common.data.Item;
import remuco.client.common.data.PlayerInfo;
import remuco.client.common.util.Log;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.RatingBar.OnRatingBarChangeListener;

public class RatingDialog extends BaseDialog implements OnRatingBarChangeListener, OnClickListener{

    private RatingBar ratingBar;
    private Button okButton;

    private RatingHandler ratingHandler = new RatingHandler(this);
    
    
    public static RatingDialog newInstance(PlayerAdapter player) {
        RatingDialog dialog = new RatingDialog();
        return dialog;
    }
    
    
    //Approach from: http://stackoverflow.com/a/11336822
    static class RatingHandler extends Handler {
        WeakReference<RatingDialog> dialog;
        
        public RatingHandler(RatingDialog callback) {
            dialog = new WeakReference<RatingDialog>(callback);
        }
        
        @Override
        public void handleMessage(Message msg) {
            RatingDialog callback = dialog.get();
            if(callback == null) {
                return;
            }
            
            switch(msg.what){
            
            case MessageFlag.ITEM_CHANGED:
                Item item = (Item)msg.obj;
                callback.ratingBar.setProgress(item.getRating());
                break;
                
            case MessageFlag.CONNECTED:
                PlayerInfo playerInfo = (PlayerInfo)msg.obj;
                callback.ratingBar.setNumStars(playerInfo.getMaxRating());
            }
        }
    }
    
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.rating_dialog, container);
        getDialog().setTitle(R.string.rating_dialog_title);
        
        ratingBar = (RatingBar) view.findViewById(R.id.rating_dialog_rating_bar);
        okButton = (Button) view.findViewById(R.id.rating_dialog_ok_button);
        ratingBar.setStepSize(1);
        ratingBar.setOnRatingBarChangeListener(this);

        // configure ok button
        okButton.setOnClickListener(this);
        return view;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        Log.debug("[RD] onResume called");
        player.addHandler(ratingHandler);
        
        // configure rating bar
        // TODO: this will break pretty sure as soon as there is no player or item ...
        if (player.getPlayer() != null) {
            if (player.getPlayer().info != null) {
                ratingBar.setNumStars(player.getPlayer().info.getMaxRating());
            }
            if (player.getPlayer().item != null) {
                ratingBar.setProgress(player.getPlayer().item.getRating());
            }
        }

    }
    
    @Override
    public void onPause() {
        Log.debug("[RD] onPause called");
        player.removeHandler(ratingHandler);
        super.onPause();
    }

    @Override
    public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
        if(!fromUser) return;
        if(player.getPlayer() == null) return;
        player.getPlayer().ctrlRate((int)Math.ceil(rating));
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.rating_dialog_ok_button){
            dismiss();
        }
    }
    
}
