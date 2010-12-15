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

import android.os.Handler;
import android.os.Message;
import android.widget.ArrayAdapter;

import remuco.client.common.data.Item;
import remuco.client.common.data.ItemList;
import remuco.client.common.player.IRequester;
import remuco.client.common.util.Log;

public class RequesterAdapter extends Handler implements IRequester{

    RemucoLibrary remucolibrary;

    public RequesterAdapter(RemucoLibrary r){
        remucolibrary = r;
    }

	@Override
	public void handleMessage(Message msg) {

		switch(msg.what){
		case MessageFlag.CONNECTED:
			Log.ln("[VH] CONNECTED!");
            remucolibrary.getPlaylist();
			break;
        case MessageFlag.PLAYLIST:
			Log.ln("[VH] PLAYLIST");
            remucolibrary.setPlaylist((ItemList) msg.obj);
            break;
        case MessageFlag.QUEUE:
			Log.ln("[VH] QUEUE");
            remucolibrary.setQueue((ItemList) msg.obj);
            break;
        }
    }

    @Override
	public void handleFiles(ItemList files){
    }

    @Override
	public void handleItem(Item item){
    }

    @Override
	public void handleLibrary(ItemList library){
    }

    @Override
	public void handlePlaylist(ItemList playlist){
        Message msg = this.obtainMessage(MessageFlag.PLAYLIST, playlist);
        msg.sendToTarget();
    }

    @Override
	public void handleQueue(ItemList queue){
        Message msg = this.obtainMessage(MessageFlag.QUEUE, queue);
        msg.sendToTarget();
    }
	
    @Override
	public void handleSearch(ItemList search){
    }

}
