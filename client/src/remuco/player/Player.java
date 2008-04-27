package remuco.player;

import remuco.comm.BinaryDataExecption;
import remuco.comm.IMessageListener;
import remuco.comm.IMessageSender;
import remuco.comm.ISerializable;
import remuco.comm.Message;
import remuco.comm.Serial;
import remuco.comm.SerialAtom;
import remuco.comm.SerializableString;
import remuco.util.Log;

/**
 * Objects of this class mirror the state of a remote player and provide methods
 * to control the remote player (methods starting with <code>ctrl</code>,
 * e.g. {@link #ctrlStop()}). To get notifications about player state changes
 * use the <code>register..</code> methods, e.g.
 * {@link #registerCurrentPlobListener(ICurrentPlobListener)}.
 * 
 * @author Christian Buennig
 * 
 */
public final class Player implements ISerializable, IPlayerInfo {

	// private static final int FLAG_BPP = 1 << 16; // not used

	private static final int FLAG_VOLUME_UNKNOWN = 1 << 17;

	private static final int FLAG_PLAYBACK_UNKNOWN = 1 << 18;

	private static final int VOLUME_STEP = 5;

	public Plob currentPlob = null;

	public final PlobList playlist;

	public final PlobList queue;

	public final State state;

	private final SerialAtom[] atoms;

	private boolean connected = false;

	private final Control ctl;

	private ICurrentPlobListener currentPlobListener;

	private int flags;

	private int maxRating;

	/**
	 * Note: Access to this message must be synchronized, since it may be used
	 * because of calls from the MIDlet- and an Adjuster-Thread. This also
	 * ensures, that interaction with the communicator thread (via
	 * {@link #server}) is synchronized.
	 */
	private final Message msg;

	private String name;

	private IPlaylistListener playlistListener;

	private final Plob plob;

	private IPloblistRequestor ploblistRequestor;

	private IPlobRequestor plobRequestor;

	private IQueueListener queueListener;

	private final SerializableString request;

	private Plob responsePlob;

	private PlobList responsePloblist;

	private final IMessageSender server;

	private IStateListener stateListener;

	/**
	 * Creates a player with the given.
	 * 
	 * @param server
	 *            destination to send control and request messages to
	 */
	public Player(IMessageSender server) {

		this.server = server;

		atoms = new SerialAtom[3];
		atoms[0] = new SerialAtom(SerialAtom.TYPE_S);
		atoms[1] = new SerialAtom(SerialAtom.TYPE_I);
		atoms[2] = new SerialAtom(SerialAtom.TYPE_I);

		ctl = new Control();
		request = new SerializableString();

		state = new State();
		plob = new Plob();
		playlist = new PlobList();
		queue = new PlobList();
		msg = new Message();
		responsePloblist = new PlobList();

		reset();

	}

	public void atomsHasBeenUpdated() {

		synchronized (atoms) {

			name = atoms[0].s;
			flags = atoms[1].i;
			maxRating = atoms[2].i;

			connected = true;
		}
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

	public void ctrlToggleRepeat() {

		ctrl(Control.CMD_REPEAT, 0, null);
	}

	public void ctrlToggleShuffle() {

		ctrl(Control.CMD_SHUFFLE, 0, null);
	}

	public void ctrlSetTags(String pid, String tags) {

		ctrl(Control.CMD_SETTAGS, 0, pid + ":" + tags);
	}

	public void ctrlShutdownHost() {

		ctrl(Control.CMD_SHUTDOWN, 0, null);
	}

	public void ctrlStop() {

		ctrl(Control.CMD_STOP, 0, null);
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

	public SerialAtom[] getAtoms() {
		return atoms;
	}

	public int getMaxRating() {
		return maxRating;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see remuco.player.IPlayerInfo#getName()
	 */
	public String getName() {
		return name != null ? name : "Remuco";
	}

	/**
	 * Handle a player related message.
	 * <p>
	 * <b>Note:</b> This method is not from the Interface
	 * {@link IMessageListener} (though it does the same, except the possibly
	 * thrown exception).
	 * <p>
	 * Note: Synchronizing this method is not needed, since it only gets called
	 * by the communicator thread.
	 * 
	 * @param m
	 *            the message
	 * @throws BinaryDataExecption
	 *             if the message's binary data is malformed
	 */
	public void handleMessage(Message m) throws BinaryDataExecption {

		if (!connected)
			return;

		switch (m.id) {

		case Message.ID_SYN_PLOB:

			Serial.in(plob, m.bytes);

			if (plob.isEmpty()) {
				currentPlob = null;
			} else {
				currentPlob = plob;
			}

			if (currentPlobListener != null) {
				currentPlobListener.currentPlobChanged();
			}

			break;
		case Message.ID_SYN_PLAYLIST:

			Serial.in(playlist, m.bytes);

			if (playlistListener != null) {
				playlistListener.playlistChanged();
			}

			break;

		case Message.ID_SYN_QUEUE:

			Serial.in(queue, m.bytes);

			if (queueListener != null) {
				queueListener.queueChanged();
			}

			break;

		case Message.ID_SYN_STATE:

			Serial.in(state, m.bytes);

			if (stateListener != null) {
				stateListener.stateChanged();
			}

			break;

		case Message.ID_REQ_PLOB:

			if (plobRequestor != null) {
				Serial.in(responsePlob, m.bytes);
				plobRequestor.handlePlob(responsePlob);
				plobRequestor = null;
				responsePlob = null;
			}
			// else {
			// May happen if plobs get requested back-to-back
			// Log.ln("[PL] warn - rx'ed plob, but no requestor");
			// }

			break;

		case Message.ID_REQ_PLOBLIST:

			if (ploblistRequestor != null) {
				Serial.in(responsePloblist, m.bytes);
				ploblistRequestor.handlePloblist(responsePloblist);
				ploblistRequestor = null;
				responsePloblist = null;
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

	public boolean isPlaybackKnown() {
		return (flags & FLAG_PLAYBACK_UNKNOWN) == 0;
	}

	public boolean isVolumeKnown() {
		return (flags & FLAG_VOLUME_UNKNOWN) == 0;
	}

	/**
	 * Registers <code>cpl</code> to get notified when the current plob has
	 * changed.
	 */
	public void registerCurrentPlobListener(ICurrentPlobListener cpl) {

		currentPlobListener = cpl;
	}

	/**
	 * Registers <code>pll</code> to get notified when the playlist has
	 * changed.
	 */
	public void registerPlaylistListener(IPlaylistListener pll) {

		playlistListener = pll;
	}

	/**
	 * Registers <code>ql</code> to get notified when the queue has changed.
	 */
	public void registerQueueListener(IQueueListener ql) {

		queueListener = ql;
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
	 *            the requestor of the plob
	 * @return <code>true</code> if the request has been submitted to the
	 *         remote player, <code>false</code> if a previous plob request is
	 *         still in process.
	 */
	public boolean reqPlob(String pid, Plob response, IPlobRequestor pr) {

		if (plobRequestor != null)
			return false;

		synchronized (msg) {

			plobRequestor = pr;
			responsePlob = response;
			request.set(pid);
			msg.id = Message.ID_REQ_PLOB;
			msg.bytes = Serial.out(request);

			server.sendMessage(msg);
		}

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
	 * @return <code>true</code> if the request has been submitted to the
	 *         remote player, <code>false</code> if a previous ploblist
	 *         request is still in process.
	 * 
	 * @see IPloblistRequestor#handlePloblist(PlobList)
	 */
	public boolean reqPloblist(String id, PlobList response,
			IPloblistRequestor plr) {

		if (ploblistRequestor != null)
			return false;

		synchronized (msg) {

			ploblistRequestor = plr;
			responsePloblist = response;
			request.set(id);
			msg.id = Message.ID_REQ_PLOBLIST;
			msg.bytes = Serial.out(request);

			server.sendMessage(msg);
		}

		return true;
	}

	/**
	 * Resets all attributes of the player as if there is no conection to a
	 * remote player. Any references to the public attribtues ({@link #state},
	 * {@link #info}, {@link #playlist}, {@link #queue} and
	 * {@link #currentPlob}) are still valid !
	 * <p>
	 * Sets connection flag to <code>false</code>.
	 * 
	 */
	public void reset() {

		if (!connected)
			return;

		connected = false;

		plob.reset();
		plob.setMeta(Plob.META_TITLE, "No Song");
		currentPlob = null;
		playlist.reset();
		queue.reset();
		state.reset();
		plobRequestor = null;
		ploblistRequestor = null;

	}

	public void updateAtoms() {
		// not needed
		Log.asssertNotReached(this);
	}

	private void ctrl(int cmd, int paramI, String paramS) {

		synchronized (msg) {

			ctl.set(cmd, paramI, paramS);

			msg.id = Message.ID_CTL;
			msg.bytes = Serial.out(ctl);

			server.sendMessage(msg);
		}
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
