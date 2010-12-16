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

import java.util.ArrayList;

import remuco.client.android.util.ConnectTask;
import remuco.client.common.MainLoop;
import remuco.client.common.UserException;
import remuco.client.common.data.ClientInfo;
import remuco.client.common.data.Item;
import remuco.client.common.io.Connection;
import remuco.client.common.io.ISocket;
import remuco.client.common.io.Connection.IConnectionListener;
import remuco.client.common.player.IItemListener;
import remuco.client.common.player.IProgressListener;
import remuco.client.common.player.IStateListener;
import remuco.client.common.player.Player;
import remuco.client.common.util.Log;
import android.os.Handler;
import android.os.Message;

public class PlayerAdapter implements IConnectionListener, IItemListener, IProgressListener, IStateListener{

	private static final int PING_INTERVAL = 5;
	private static Player player;
	
	ArrayList<Handler> handlers;
	
	public PlayerAdapter() {
		handlers = new ArrayList<Handler>();
        if (this.player != null) {
            this.player.setItemListener(this);
            this.player.setProgressListener(this);
            this.player.setStateListener(this);
        }
	}

	// --- connection related methods
	
	/**
	 * connects to a wifi remote remuco server
	 * @param hostname the host to connect to
	 * @param port the port to connect to
	 * @param clientInfo client info describing this client
	 */
	public void connectWifi(String hostname, int port, ClientInfo clientInfo){
        if (player != null && !player.getConnection().isClosed()) return;
		MainLoop.schedule(new ConnectTask(ConnectTask.WIFI, hostname, port, clientInfo, this));
	}

	/**
	 * connects to a bluetooth remote remuco server
	 * @param hostname the host to connect to
	 * @param clientInfo client info describing this client
	 */
	public void connectBluetooth(String hostname, ClientInfo clientInfo){
        if (player != null && !player.getConnection().isClosed()) return;
		MainLoop.schedule(new ConnectTask(ConnectTask.BLUETOOTH, hostname, clientInfo, this));
	}
	
	/**
	 * disconnects from the server
	 * does nothing if not connected
	 */
	public void disconnect(){
		if(player!=null){
			player.getConnection().close();
			
			// we get no disconnect signal if we close the connection ourself
			notifyHandlers(MessageFlag.DISCONNECTED);
		}
	}

	public Player getPlayer(){
		return player;
	}
	
	
	// --- connection powersaving
	
	public void pauseConnection(){
		Log.debug("[PA] pausing connection");
		
		if(player == null){
			Log.debug("[PA] cannot pause connection: not connected");
			return;
		}
		
		Connection conn = player.getConnection();
		conn.setPing(0);
		remuco.client.common.io.Message pauseMessage = new remuco.client.common.io.Message();
		pauseMessage.id = remuco.client.common.io.Message.CONN_SLEEP;
		conn.send(pauseMessage);
	}
	
	public void resumeConnection(){
		Log.debug("[PA] waking up connection");
		
		if(player == null){
			Log.debug("[PA] cannot resume connection: not connected");
			return;
		}
		
	    Connection conn = player.getConnection(); 
	    conn.setPing(PING_INTERVAL);
	    remuco.client.common.io.Message m = new remuco.client.common.io.Message();
	    m.id = remuco.client.common.io.Message.CONN_WAKEUP;
	    conn.send(m);
	}
	
	
	// --- remuco event handlers
	
	@Override
	public void notifyConnected(Player player) {
		Log.ln("[PH] CONNECTED");
		
		this.player = player;
		this.player.setItemListener(this);
		this.player.setProgressListener(this);
		this.player.setStateListener(this);
		
		// set ping interval
		this.player.getConnection().setPing(PING_INTERVAL);
		
		notifyHandlers(MessageFlag.CONNECTED, player.info);
	}

	@Override
	public void notifyDisconnected(ISocket sock, UserException reason) {
		Log.ln("[PA] DISCONNECTED: " + reason.getMessage());
		
		notifyHandlers(MessageFlag.DISCONNECTED);
	}
	
	@Override
	public void notifyItemChanged() {
		Log.debug("[PA] now playing: " + player.item.getMeta(Item.META_TITLE) + " by " + player.item.getMeta(Item.META_ARTIST));
		notifyHandlers(MessageFlag.ITEM_CHANGED, player.item);
	}

	@Override
	public void notifyProgressChanged() {
		Log.debug("[PA] new progress: " + player.progress.getProgressFormatted() + "/" + player.progress.getLengthFormatted());
		notifyHandlers(MessageFlag.PROGRESS_CHANGED, player.progress);
	}

	@Override
	public void notifyStateChanged() {
		Log.debug("[PA] state changed");
		notifyHandlers(MessageFlag.STATE_CHANGED, player.state);
	}

	private void notifyHandlers(int what, Object obj){
		for(Handler h : handlers){
			Message msg = h.obtainMessage(what, obj);
			msg.sendToTarget();
		}
	}
	
	private void notifyHandlers(int what){
		for(Handler h : handlers){
			Message msg = h.obtainMessage(what);
			msg.sendToTarget();
		}
	}
	
	public void addHandler(Handler h){
		Log.debug("[PA] adding handler: " + h);
		handlers.add(h);
	}
	
	public void removeHandler(Handler h){
		Log.debug("[PA] removing handler: " + h);
		handlers.remove(h);
	}

	public void clearHandlers(){
		Log.debug("[PA] clear handler");
		handlers.clear();
	}
	
	
}
