package remuco.player;

import remuco.comm.BinaryDataExecption;
import remuco.comm.Connection;
import remuco.comm.IMessageListener;
import remuco.comm.Message;
import remuco.comm.Serial;
import remuco.comm.SerializableString;
import remuco.util.Log;

/**
 * A player mirrors the state of a remote player and provides methods to control
 * the remote player (methods starting with <code>ctrl</code>, e.g.
 * {@link #ctrlStop()}). To get notifications about player state changes use the
 * <code>register..</code> methods, e.g.
 * {@link #registerCurrentPlobListener(IPlobListener)}.
 * 
 * @author Oben Sonne
 * 
 */
public final class Player {

	private static final int VOLUME_STEP = 5;

	/** Do not alter outside {@link Player}! */
	public final PlayerInfo info;

	/** Do not alter outside {@link Player}! */
	public final Plob plob;

	/** Do not alter outside {@link Player}! */
	public final State state;

	private final Connection conn;

	private IPlobListener plobListener;

	private IPloblistRequestor ploblistRequestor;

	private IPlobRequestor plobRequestor;

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
		plob = new Plob();

		plobRequestor = null;
		ploblistRequestor = null;

	}

	/**
	 * Jump to a specific plob in the playlist or queue
	 * 
	 * @param id
	 *            id of the ploblist to jump into
	 * @param pos
	 *            position of the plob (within the specified ploblist) to play
	 *            (starting from 0)
	 * 
	 */
	public void ctrlJump(String id, int pos) {

		ctrl(Control.CMD_JUMP, pos, id);
	}

	public void ctrlNext() {

		ctrl(Control.CMD_NEXT, 0, null);
	}

	public void ctrlPlayNext(String pid) {

		ctrl(Control.CMD_PLAYNEXT, 0, null);
	}

	public void ctrlPlayPause() {

		ctrl(Control.CMD_PLAYPAUSE, 0, null);
	}

	public void ctrlPrev() {

		ctrl(Control.CMD_PREV, 0, null);
	}

	public void ctrlRate(int rating) {

		ctrl(Control.CMD_RATE, rating, null);
	}

	public void ctrlSeekBwd() {

		ctrl(Control.CMD_SEEK_BWD, 0, null);
	}

	public void ctrlSeekFwd() {

		ctrl(Control.CMD_SEEK_FWD, 0, null);
	}

	public void ctrlSetTags(String pid, String tags) {

		ctrl(Control.CMD_SETTAGS, 0, pid + ":" + tags);
	}

	public void ctrlShutdownHost() {

		ctrl(Control.CMD_SHUTDOWN, 0, null);
	}

	public void ctrlToggleRepeat() {

		ctrl(Control.CMD_REPEAT, 0, null);
	}

	public void ctrlToggleShuffle() {

		ctrl(Control.CMD_SHUFFLE, 0, null);
	}

	public void ctrlVolumeDown() {

		ctrlVolume(-1);
	}

	public void ctrlVolumeMute() {

		ctrlVolume(0);
	}

	public void ctrlVolumeUp() {

		ctrlVolume(1);
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

		switch (m.id) {

		case Message.ID_PLOB:

			Serial.in(plob, m.bytes);

			if (plobListener != null) {
				plobListener.currentPlobChanged();
			}

			break;
			
		case Message.ID_STATE:

			Serial.in(state, m.bytes);

			if (stateListener != null) {
				stateListener.stateChanged();
			}

			break;

		case Message.ID_REQ_PLOB:

			if (plobRequestor != null) {

				final Plob plob = new Plob();

				Serial.in(plob, m.bytes);
				plobRequestor.handlePlob(plob);
				plobRequestor = null;
			}
			// else {
			// May happen if plobs get requested back-to-back
			// Log.ln("[PL] warn - rx'ed plob, but no requestor");
			// }

			break;

		case Message.ID_REQ_PLOBLIST:

			if (ploblistRequestor != null) {

				final PlobList ploblist = new PlobList();

				Serial.in(ploblist, m.bytes);
				ploblistRequestor.handlePloblist(ploblist);
				ploblistRequestor = null;
			}
			// else {
			// May happen if ploblists get requested back-to-back
			// Log.ln("[PL] warn - rx'ed ploblist, but no requestor");
			// }

			break;

		default:

			Log.ln("[PL] warn - rx'ed unsupported message type " + m.id);

			break;
		}
	}

	/**
	 * Registers <code>cpl</code> to get notified when the current plob has
	 * changed.
	 */
	public void registerCurrentPlobListener(IPlobListener cpl) {

		plobListener = cpl;
	}

	/**
	 * Registers <code>sl</code> to get notified when the state (see
	 * {@link State}) has changed.
	 */
	public void registerStateListener(IStateListener sl) {

		stateListener = sl;
	}

	/**
	 * Request a plob.
	 * 
	 * @param pid
	 *            the plob's ID
	 * @param pr
	 *            the requester of the plob
	 * @return <code>true</code> if the request has been submitted to the remote
	 *         player, <code>false</code> if a previous plob request is still in
	 *         process.
	 */
	public boolean reqPlob(String pid, IPlobRequestor pr) {

		if (plobRequestor != null)
			return false;

		plobRequestor = pr;

		final SerializableString request = new SerializableString();
		request.set(pid);

		conn.send(new Message(Message.ID_REQ_PLOB, Serial.out(request)));

		return true;
	}

	/**
	 * Request a ploblist.
	 * 
	 * @param plid
	 *            the ploblist's ID
	 * @param response
	 *            this ploblist will be updated to reflect the requested
	 *            ploblist (to avoid many new object creations)
	 * @param plr
	 *            the requester of the ploblist
	 * @return <code>true</code> if the request has been submitted to the remote
	 *         player, <code>false</code> if a previous ploblist request is
	 *         still in process.
	 * 
	 * @see IPloblistRequestor#handlePloblist(PlobList)
	 */
	public boolean reqPloblist(String id, IPloblistRequestor plr) {

		if (ploblistRequestor != null)
			return false;

		ploblistRequestor = plr;
		final SerializableString request = new SerializableString();
		request.set(id);

		conn.send(new Message(Message.ID_REQ_PLOBLIST, Serial.out(request)));

		return true;
	}

	private void ctrl(int cmd, int paramI, String paramS) {

		final Control ctl = new Control();

		ctl.set(cmd, paramI, paramS);

		conn.send(new Message(Message.ID_CTRL, Serial.out(ctl)));
	}

	private void ctrlVolume(int direction) {

		int volumeNew;

		Log.ln("Vol control: " + direction);

		volumeNew = direction == 0 ? 0 : state.getVolume() + direction
				* VOLUME_STEP;

		volumeNew = volumeNew > 100 ? 100 : volumeNew < 0 ? 0 : volumeNew;

		ctrl(Control.CMD_VOLUME, volumeNew, null);

		// // small hack: trigger an immediate volume bar update
		state.setVolume(volumeNew);
		if (stateListener != null)
			stateListener.stateChanged();

	}

}
