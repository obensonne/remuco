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

package remuco.client.android.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.SeekBar;
import android.widget.TextView;

import remuco.client.android.R;
import remuco.client.common.util.Log;

public class PlayerFragment extends BaseFragment implements OnClickListener {

    private ViewHandler viewHandler;

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

    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        
        Log.debug("--- " + this.getClass().getName() + ".onCreateView()");
        
        View rootView = inflater.inflate(R.layout.player_fragment, container, false);
        this.setViewhandlerCallbacks(rootView);
        viewHandler = new ViewHandler(this);
        return rootView;
    }
    
    
    @Override
    public void onResume() {
        super.onResume();
        Log.debug("--- " + this.getClass().getName() + ".onResume()");
        
        player.addHandler(viewHandler);
        if (player.getPlayer() != null ) {
            viewHandler.setRunning(player);
        } else {
            setConnectText();
        }
    }
    
    
    @Override
    public void onPause() {
        super.onPause();
        Log.debug("--- " + this.getClass().getName() + ".onPause()");
        
        viewHandler.setStopped();
        player.removeHandler(viewHandler);
    }
    
    
    /**
     * Loads all GUI elements from view, and sets action listeners.
     * @param view R.layout.main
     */
    private void setViewhandlerCallbacks(View view) {
        // --- retrieve and set view handles
        infoTitle     = (TextView) view.findViewById(R.id.infoTitle);
        infoArtist     = (TextView) view.findViewById(R.id.infoArtist);
        infoAlbum     = (TextView) view.findViewById(R.id.infoAlbum);

        infoCover     = (ImageView) view.findViewById(R.id.infoCover);
        
        infoRatingBar = (RatingBar) view.findViewById(R.id.infoRatingBar);
        
        ctrlProgressBar     = (SeekBar) view.findViewById(R.id.CtrlProgressBar);
        ctrlLength             = (TextView) view.findViewById(R.id.CtrlLength);
        ctrlProgress         = (TextView) view.findViewById(R.id.CtrlProgress);
        
        ctrlPrev = (ImageButton) view.findViewById(R.id.CtrlPrev);
        ctrlPlay = (ImageButton) view.findViewById(R.id.CtrlPlay);
        ctrlNext = (ImageButton) view.findViewById(R.id.CtrlNext);
        ctrlShuffle = (ImageButton) view.findViewById(R.id.CtrlShuffle);
        ctrlRepeat = (ImageButton) view.findViewById(R.id.CtrlRepeat);
        
        
        // --- set listeners
        ctrlPlay.setOnClickListener(this);
        ctrlPrev.setOnClickListener(this);
        ctrlNext.setOnClickListener(this);
        ctrlShuffle.setOnClickListener(this);
        ctrlRepeat.setOnClickListener(this);

        ctrlProgressBar.setOnSeekBarChangeListener(
            new SeekBar.OnSeekBarChangeListener() {
            int start = 0;
            
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                    boolean fromUser) {}
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                start = ctrlProgressBar.getProgress();
            }
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (player.getPlayer() == null)
                    return;
                int end = ctrlProgressBar.getProgress();
                player.getPlayer().ctrlSeek((end - start));
            }
        });
    }
    
    
    /**
     * Sets a connect to server text
     */
    protected void setConnectText() {
        infoTitle.setText(R.string.player_info_notconnected);
        infoArtist.setText(R.string.player_info_usemenu);
        infoAlbum.setText(R.string.player_info_connectoserver);        
    }


    @Override
    public void onClick(View v) {
        if (player.getPlayer() == null)
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
