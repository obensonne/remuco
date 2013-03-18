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
            remucolibrary.getList();
			break;
		case MessageFlag.DISCONNECTED:
			Log.ln("[VH] DISCONNECTED!");
            remucolibrary.clearList();
            break;
        case MessageFlag.PLAYLIST:
        case MessageFlag.QUEUE:
        case MessageFlag.MLIB:
        case MessageFlag.FILES:
        case MessageFlag.SEARCH:
            remucolibrary.setList((ItemList) msg.obj);
            break;
        }
    }

    @Override
	public void handleFiles(ItemList files){
        Message msg = this.obtainMessage(MessageFlag.FILES, files);
        msg.sendToTarget();
    }

    @Override
	public void handleItem(Item item){
    }

    @Override
	public void handleLibrary(ItemList library){
        Message msg = this.obtainMessage(MessageFlag.MLIB, library);
        msg.sendToTarget();
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
        Message msg = this.obtainMessage(MessageFlag.SEARCH, search);
        msg.sendToTarget();
    }

}
