/*   
 *   Remuco - A remote control system for media players.
 *   Copyright (C) 2006-2009 Oben Sonne <obensonne@googlemail.com>
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
package remuco.player;

import remuco.comm.BinaryDataExecption;
import remuco.comm.Connection;
import remuco.comm.IMessageListener;
import remuco.comm.ISerializable;
import remuco.comm.Message;
import remuco.comm.Serial;
import remuco.util.Log;

/**
 * A player mirrors the state of a remote player and provides methods to control
 * the remote player, to request data from the remote player and to register
 * listener for changes in player state.
 * 
 * @author Oben Sonne
 * 
 */
public final class Player {

	// TODO better attach a unique reqest ID to each request (requires some
	// change to pa-api)
	private static final String REQ_PARAM_ID_PLAYLIST = "==PLAYLIST==";

	private static final String REQ_PARAM_ID_QUEUE = "==QUEUE==";

	private static String pathToString(String sa[]) {

		if (sa == null || sa.length == 0) {
			return "";
		}

		final StringBuffer sb = new StringBuffer();
		for (int i = 0; i < sa.length; i++) {
			sb.append('/').append(sa[i]);
		}

		return sb.toString();
	}

	/** Do not alter outside {@link Player}! */
	public final PlayerInfo info;

	/** Do not alter outside {@link Player}! */
	public final Item item;

	/** Do not alter outside {@link Player}! */
	public final Progress progress;

	/** Do not alter outside {@link Player}! */
	public final State state;

	private final Connection conn;

	private IItemListener itemListener;

	private IProgressListener progressListener = null;

	private IRequester reqCaller;

	/** Used to detect if incoming request replies are still up to date. */
	private String reqParamID = null;

	private IStateListener stateListener;

	/**
	 * Create a new player.
	 * 
	 * @param conn
	 *            the connection to the remote player
	 * @param info
	 *            information about the remote player
	 */
	public Player(Connection conn, PlayerInfo info) {

		this.conn = conn;
		this.info = info;

		state = new State();
		progress = new Progress();
		item = new Item();

		reqCaller = null;

	}

	public void actionFiles(ActionParam a) {

		action(Message.ACT_FILES, a);

	}

	public void actionMediaLib(ActionParam a) {

		action(Message.ACT_MEDIALIB, a);

	}

	public void actionPlaylist(ActionParam a) {

		action(Message.ACT_PLAYLIST, a);

	}

	public void actionQueue(ActionParam a) {

		action(Message.ACT_QUEUE, a);

	}

	public void actionSearch(ActionParam a) {

		action(Message.ACT_SEARCH, a);

	}

	public void ctrlNext() {

		ctrl(Message.CTRL_NEXT);
	}

	public void ctrlPlayPause() {

		ctrl(Message.CTRL_PLAYPAUSE);
	}

	public void ctrlPrev() {

		ctrl(Message.CTRL_PREV);
	}

	public void ctrlRate(int rating) {

		ctrl(Message.CTRL_RATE, rating);
	}

	public void ctrlSeek(int direction) {

		// not issued by PlayerScreen -> still need a feature check

		if (info.supports(Feature.CTRL_SEEK)) {
			ctrl(Message.CTRL_SEEK, direction);
		}
	}

	public void ctrlSetTags(String id, String tags) {

		final Tagging t = new Tagging(id, tags);

		ctrl(Message.CTRL_TAG, t);
	}

	public void ctrlShutdownHost() {

		ctrl(Message.CTRL_SHUTDOWN);
	}

	public void ctrlToggleFullscreen() {

		ctrl(Message.CTRL_FULLSCREEN);
	}

	public void ctrlToggleRepeat() {

		ctrl(Message.CTRL_REPEAT);
	}

	public void ctrlToggleShuffle() {

		ctrl(Message.CTRL_SHUFFLE);
	}

	public void ctrlVolume(int direction) {

		ctrl(Message.CTRL_VOLUME, direction);
	}

	/**
	 * Handle a player related message.
	 * <p>
	 * <b>Note:</b> This method is not from the Interface
	 * {@link IMessageListener} (though it does the same, except the possibly
	 * thrown exception).
	 * <p>
	 * Note: Synchronizing this method is not needed, since it only gets called
	 * by the conncetion thread.
	 * 
	 * @param m
	 *            the message
	 * @throws BinaryDataExecption
	 *             if the message's binary data is malformed
	 */
	public void handleMessage(Message m) throws BinaryDataExecption {

		if (Message.isRequest(m.id) && reqCaller == null) {
			// ignore outdated request replies
			return;
		}

		switch (m.id) {

		case Message.SYNC_ITEM:

			Serial.in(item, m.data);

			if (itemListener != null) {
				itemListener.notifyItemChanged();
			}

			break;

		case Message.SYNC_STATE:

			Serial.in(state, m.data);

			if (stateListener != null) {
				stateListener.notifyStateChanged();
			}

			break;

		case Message.SYNC_PROGRESS:

			Serial.in(progress, m.data);

			if (progressListener != null) {
				progressListener.notifyProgressChanged();
			}

			break;

		case Message.REQ_ITEM:

			final Item item = new Item();

			Serial.in(item, m.data);

			if (item.getId().equals(reqParamID)) {
				reqCaller.handleItem(item);
				reqCaller = null;
				reqParamID = null;
			}

			break;

		case Message.REQ_PLAYLIST:

			if (reqParamID == REQ_PARAM_ID_PLAYLIST) {
				final ItemList playlist = new ItemList(ItemList.TYPE_PLAYLIST);
				Serial.in(playlist, m.data);
				reqCaller.handlePlaylist(playlist);
				reqCaller = null;
				reqParamID = null;
			}

			break;

		case Message.REQ_QUEUE:

			if (reqParamID == REQ_PARAM_ID_QUEUE) {
				final ItemList queue = new ItemList(ItemList.TYPE_QUEUE);
				Serial.in(queue, m.data);
				reqCaller.handleQueue(queue);
				reqCaller = null;
				reqParamID = null;
			}

			break;

		case Message.REQ_MLIB:

			final ItemList mlib = new ItemList(ItemList.TYPE_MLIB);

			Serial.in(mlib, m.data);
			if (pathToString(mlib.getPath()).equals(reqParamID)) {
				reqCaller.handleLibrary(mlib);
				reqCaller = null;
				reqParamID = null;
			}

			break;

		case Message.REQ_FILES:

			final ItemList files = new ItemList(info.getFileActions());

			Serial.in(files, m.data);
			if (pathToString(files.getPath()).equals(reqParamID)) {
				reqCaller.handleFiles(files);
				reqCaller = null;
				reqParamID = null;
			}

			break;

		case Message.REQ_SEARCH:

			final ItemList search = new ItemList(ItemList.TYPE_SEARCH);

			Serial.in(search, m.data);
			if (pathToString(search.getPath()).equals(reqParamID)) {
				reqCaller.handleSearch(search);
				reqCaller = null;
				reqParamID = null;
			}

			break;

		default:
			Log.bug("rx'ed unsupported msg: " + m.id);
			break;
		}
	}

	public void reqCancel() {
		reqParamID = null;
		reqCaller = null;
	}

	public void reqFiles(IRequester lr, String path[]) {

		req(lr, Message.REQ_FILES, new RequestParam(path), pathToString(path));
	}

	public void reqItem(IRequester ir, String id) {

		req(ir, Message.REQ_ITEM, new RequestParam(id), id);
	}

	public void reqMLib(IRequester lr, String path[]) {

		req(lr, Message.REQ_MLIB, new RequestParam(path), pathToString(path));
	}

	public void reqPlaylist(IRequester lr) {

		req(lr, Message.REQ_PLAYLIST, null, REQ_PARAM_ID_PLAYLIST);
	}

	public void reqQueue(IRequester lr) {

		req(lr, Message.REQ_QUEUE, null, REQ_PARAM_ID_QUEUE);
	}

	public void reqSearch(IRequester lr, String query[]) {

		req(lr, Message.REQ_SEARCH, new RequestParam(query),
			pathToString(query));
	}

	/**
	 * Registers <em>il</em> to get notified when the current item has changed.
	 * 
	 * @see #item
	 */
	public void setItemListener(IItemListener il) {

		itemListener = il;
	}

	/**
	 * Registers <em>pl</em> to get notified when the progress has changed.
	 * 
	 * @see #progress
	 */
	public void setProgressListener(IProgressListener pl) {
		progressListener = pl;
	}

	/**
	 * Registers <em>sl</em> to get notified when the state has changed.
	 * 
	 * @see #state
	 */
	public void setStateListener(IStateListener sl) {

		stateListener = sl;
	}

	private void action(int msgID, ActionParam action) {

		final Message m = new Message();

		m.id = msgID;
		m.data = Serial.out(action);

		conn.send(m);
	}

	private void ctrl(int id) {

		ctrl(id, null);
	}

	private void ctrl(int id, int param) {

		ctrl(id, new ControlParam(param));
	}

	private void ctrl(int id, ISerializable ser) {

		final Message m = new Message();

		m.id = id;

		if (ser != null) {
			m.data = Serial.out(ser);
		}

		conn.send(m);
	}

	private void req(IRequester rc, int msgID, RequestParam req, String paramID) {

		reqCaller = rc;
		reqParamID = paramID;

		final Message m = new Message();

		m.id = msgID;
		m.data = Serial.out(req);

		conn.send(m);
	}

}
