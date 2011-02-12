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

package remuco.client.android;

import remuco.client.common.data.Item;
import remuco.client.common.data.PlayerInfo;
import remuco.client.common.data.Progress;
import remuco.client.common.data.State;
import remuco.client.common.player.Player;
import remuco.client.common.util.Log;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.widget.RemoteViews;
import android.widget.Toast;

public class WidgetHandler extends Handler {

	byte[] imageCache;
	
    Context context;
    PlayerAdapter player;
	
	boolean running = false;
	
	
	public WidgetHandler(Context context, PlayerAdapter player) {
        this.context = context;
        this.player = player;
	}

    public void setRunning(boolean r) {

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.remucowidget);

        updateItemGui(views, player.getPlayer().item);
        updateStateGui(views, player.getPlayer().state);

        running = r;

        RemucoWidgetProvider.updateAllWidgets(context, views);
    }

	@Override
	public void handleMessage(Message msg) {

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.remucowidget);

		switch(msg.what){
		
		case MessageFlag.CONNECTED:
			
			Log.ln("[VH] CONNECTED!");
			
			// obj should be of type PlayerInfo
			PlayerInfo info = (PlayerInfo)msg.obj;
			
			// inform the user
			String toast = context.getResources().getString(
					R.string.connection_successful, 
					info.getName()
					);
			Toast.makeText(context, toast, Toast.LENGTH_SHORT).show();
			
			// update the display
			sendEmptyMessage(MessageFlag.ITEM_CHANGED);

			break;
			
		case MessageFlag.DISCONNECTED:
			
			Log.ln("[VH] DISCONNECTED!");
			
			// inform the user
			toast = context.getResources().getString(R.string.connection_failed);
			Toast.makeText(context, toast, Toast.LENGTH_SHORT).show();
			
			// show ape picture
            views.setImageViewResource(R.id.WidgetBackground, R.drawable.remuco_100);
			
			// change text
            views.setCharSequence(R.id.InfoTitle, "setText", "Not connected");
            views.setCharSequence(R.id.InfoArtist, "setText", "");
			
			// not running anymore
			running = false;

			break;
			
		case MessageFlag.ITEM_CHANGED:
			
			Log.ln("[VH] item changed");

			// msg.obj should be of type Item
			if(!(msg.obj instanceof Item)){
				break;
			}
			Item item = (Item)msg.obj;
            updateItemGui(views, item);

			// done
			break;


		case MessageFlag.STATE_CHANGED:
			
			Log.ln("[VH] state changed");
			
			// msg.obj should be of type State
			State state = (State)msg.obj;

            updateStateGui(views, state);
			
			break;
			
		}

        RemucoWidgetProvider.updateAllWidgets(context, views);

	}

	// --- convert byte[] to image
	private void convertByteArrayToImage(RemoteViews views, byte[] image) {
		if(image == null || image.length == 0){
            views.setImageViewResource(R.id.WidgetBackground, R.drawable.remuco_100);

            imageCache = null;
		} else {
            views.setImageViewBitmap(R.id.WidgetBackground, BitmapFactory.decodeByteArray(image, 0, image.length));
			
			imageCache = image;
		}
	}

    private void updateItemGui(RemoteViews views, Item item) {
        // get song metadata
        String title = item.getMeta(Item.META_TITLE);
        String artist = item.getMeta(Item.META_ARTIST);
			
        // set song metadata
        views.setCharSequence(R.id.InfoTitle, "setText", title);
        views.setCharSequence(R.id.InfoArtist, "setText", artist);

        // set image
        byte[] image = item.getImg();
        convertByteArrayToImage(views, image);
    }

    private void updateStateGui(RemoteViews views, State state) {
			// toggle playbutton icon
			if(state.getPlayback() == State.PLAYBACK_PLAY){
				Log.debug("[VH] playback = true");
                views.setInt(R.id.WidgetPlay, "setImageResource", R.drawable.ic_appwidget_music_pause);
				running = true;
			} else {
				Log.debug("[VH] playback = false");
                views.setInt(R.id.WidgetPlay, "setImageResource", R.drawable.ic_appwidget_music_play);
				running = false;
			}
    }


}
