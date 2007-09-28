package remuco.player;

import remuco.comm.IMessageReceiver;
import remuco.comm.IMessageSender;
import remuco.comm.Message;
import remuco.util.Log;

/**
 * Objects of this class mirror the state of a remote player and provide methods
 * to control the remote player (methods starting with <code>ctrl..</code>,
 * e.g. {@link #ctrlStop()}). To get notifications about player state changes
 * use the <code>register..</code> methods, e.g.
 * {@link #registerCurrentPlobListener(ICurrentPlobListener)}.
 * <p>
 * Note: All methods internally related to some I/O functions are synchronized.
 * 
 * @author Christian Buennig
 * 
 */
public final class Player implements IMessageReceiver {

	public Plob currentPlob = null;

	public final Info info = new Info();

	public final PlobList playlist = new PlobList(PlobList.PLID_PLAYLIST, null);

	public final PlobList queue = new PlobList(PlobList.PLID_QUEUE, null);

	public final State state = new State();

	private ICurrentPlobListener currentPlobListener;

	private ILibraryRequestor libraryRequestor;

	private final IMessageSender ms;

	private final Message msg = new Message();

	private IPlaylistListener playlistListener;

	private IPloblistRequestor ploblistRequestor;

	private IPlobRequestor plobRequestor;

	private IQueueListener queueListener;

	// TODO check if this final ref causes errors
	private final Plob respPlob = new Plob();

	// TODO check if this final ref causes errors
	private final PlobList respPloblist = new PlobList();

	// TODO check if this final ref causes errors
	private final Library respLibrary = new Library();

	// TODO check if this final ref causes errors
	private final PlobList respSearch = new PlobList();

	private final SimpleControl sctrl = new SimpleControl();

	private ISearchRequestor searchRequestor;

	private final StringParam sp = new StringParam();

	private IStateListener stateListener;

	private final Plob thePlob = new Plob();

	/**
	 * Creates a player with the given.
	 * 
	 * @param device
	 *            the remote device address
	 */
	public Player(IMessageSender ms) {

		this.ms = ms;

		reset();

	}

	/**
	 * Jump to a specific plob in the playlist or queue
	 * 
	 * @param pos
	 *            if &gt; 0, then jump to the plob in the playlist at position
	 *            <code>pos</code> (starting from 1); if &lt; 0, jump to the
	 *            plob at position <code>-pos</code> in the queue (starting
	 *            from 1); as you see, <code>pos == 0</code> has no effect ;)
	 * 
	 */
	public synchronized void ctrlJump(int pos) {

		simpleControl(SimpleControl.CMD_JUMP, pos);

	}

	public synchronized void ctrlNext() {

		simpleControl(SimpleControl.CMD_NEXT, 0);

	}

	public synchronized void ctrlPlayPloblist(String plid) {

		sp.setParam(plid);

		msg.id = Message.ID_CTL_PLAY_PLOBLIST;
		msg.sd = sp.sdGet();

		ms.sendMessage(msg);
	}

	public synchronized void ctrlPrev() {

		simpleControl(SimpleControl.CMD_PREV, 0);

	}

	public synchronized void ctrlRate(int rating) {

		simpleControl(SimpleControl.CMD_RATE, rating);

	}

	public synchronized void ctrlRestart() {

		simpleControl(SimpleControl.CMD_RESTART, 0);

	}

	public synchronized void ctrlStop() {

		simpleControl(SimpleControl.CMD_STOP, 0);

	}

	public synchronized void ctrlTooglePlayPause() {

		simpleControl(SimpleControl.CMD_PLAYPAUSE, 0);

	}

	public synchronized void ctrlUpdatePlob(Plob plob) {

		msg.id = Message.ID_CTL_UPD_PLOB;
		msg.sd = plob.sdGet();

		ms.sendMessage(msg);
	}

	public synchronized void ctrlUpdatePloblist(PlobList pl) {

		msg.id = Message.ID_CTL_UPD_PLOBLIST;
		msg.sd = pl.sdGet();

		ms.sendMessage(msg);
	}

	public synchronized void ctrlVolume(int volume) {

		simpleControl(SimpleControl.CMD_VOLUME, volume);

	}

	public synchronized void receiveMessage(Message m) {

		switch (m.id) {

		case Message.ID_IFS_CURPLOB:

			if (m.sd != null) {
				thePlob.sdSet(m.sd);
				currentPlob = thePlob;
			} else {
				currentPlob = null;
			}

			if (currentPlobListener != null) {
				currentPlobListener.currentPlobChanged();
			}

			break;
		case Message.ID_IFS_PLAYLIST:

			playlist.sdSet(m.sd);

			if (playlistListener != null) {
				playlistListener.playlistChanged();
			}

			break;

		case Message.ID_IFS_QUEUE:

			queue.sdSet(m.sd);

			if (queueListener != null) {
				queueListener.queueChanged();
			}

			break;

		case Message.ID_IFS_STATE:

			state.sdSet(m.sd);

			if (stateListener != null) {
				stateListener.stateChanged();
			}

			break;

		case Message.ID_REQ_PLOB:

			if (plobRequestor != null) {
				respPlob.sdSet(m.sd);
				plobRequestor.handlePlob(respPlob);
				plobRequestor = null;
			}
			// else {
			// May happen if plobs get requested back-to-back
			// Log.ln("[PL] warn - rx'ed plob, but no requestor");
			// }

			break;

		case Message.ID_REQ_PLOBLIST:

			if (ploblistRequestor != null) {
				respPloblist.sdSet(m.sd);
				ploblistRequestor.handlePloblist(respPloblist);
				ploblistRequestor = null;
			}
			// else {
			// May happen if ploblists get requested back-to-back
			// Log.ln("[PL] warn - rx'ed ploblist, but no requestor");
			// }

			break;

		case Message.ID_REQ_LIBRARY:

			Log.debug("forward lib to lr");
			if (libraryRequestor != null) {
				Log.debug("forward lib to lr . now");
				respLibrary.sdSet(m.sd);
				libraryRequestor.handleLibrary(respLibrary);
				libraryRequestor = null;
			}
			// else {
			// May happen if the library gets requested back-to-back
			// Log.ln("[PL] warn - rx'ed library, but no requestor");
			// }

			break;

		case Message.ID_REQ_SEARCH:

			if (searchRequestor != null) {
				respSearch.sdSet(m.sd);
				searchRequestor.handleSearchResult(respSearch);
				searchRequestor = null;
			}
			// else {
			// May happen if a search gets requested back-to-back
			// Log.ln("[PL] warn - rx'ed search result, but no requestor");
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
	 * Request the library.
	 * 
	 * @param lr
	 *            the requestor of the library
	 * @return <code>true</code> if the request has been submitted to the
	 *         remote player, <code>false</code> if a previous library request
	 *         is still in process.
	 */
	public synchronized boolean reqLibrary(ILibraryRequestor lr) {

		Log.debug("library request");
		
		if (libraryRequestor != null)
			return false;

		Log.debug("library request . do now");

		libraryRequestor = lr;
		msg.id = Message.ID_REQ_LIBRARY;
		msg.sd = null;
		ms.sendMessage(msg);

		return true;
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
	public synchronized boolean reqPlob(String pid, IPlobRequestor pr) {

		if (plobRequestor != null)
			return false;

		plobRequestor = pr;
		msg.id = Message.ID_REQ_PLOB;
		sp.setParam(pid);
		msg.sd = sp.sdGet();
		ms.sendMessage(msg);

		return true;
	}

	/**
	 * Request a ploblist.
	 * 
	 * @param plid
	 *            the ploblist's ID
	 * @param plr
	 *            the requestor of the ploblist
	 * @return <code>true</code> if the request has been submitted to the
	 *         remote player, <code>false</code> if a previous ploblist
	 *         request is still in process.
	 */
	public synchronized boolean reqPloblist(String plid, IPloblistRequestor plr) {

		if (ploblistRequestor != null)
			return false;

		ploblistRequestor = plr;
		msg.id = Message.ID_REQ_PLOBLIST;
		sp.setParam(plid);
		msg.sd = sp.sdGet();
		ms.sendMessage(msg);

		return true;
	}

	/**
	 * 
	 * @param plob
	 * @param sr
	 * @return <code>true</code> if the request has been submitted to the
	 *         re,ote player, <code>false</code> if a previous search request
	 *         is still in process.
	 */
	public synchronized boolean reqSearchPlobs(Plob plob, ISearchRequestor sr) {

		if (searchRequestor != null)
			return false;

		searchRequestor = sr;
		msg.id = Message.ID_REQ_SEARCH;
		msg.sd = plob.sdGet();
		ms.sendMessage(msg);

		return true;
	}

	/**
	 * Resets all attributes of the player as if there is no conection to a
	 * remote player. Any references to the public attribtues ({@link #state},
	 * {@link #info}, {@link #playlist}, {@link #queue} and
	 * {@link #currentPlob}) are still valid !
	 * 
	 */
	public void reset() {

		thePlob.reset();
		thePlob.setMeta(Plob.META_TITLE, "No Song");
		currentPlob = null;
		playlist.reset();
		queue.reset();
		state.reset();
		plobRequestor = null;
		ploblistRequestor = null;
		libraryRequestor = null;
		searchRequestor = null;
		info.reset();
	}

	private void simpleControl(int cmd, int param) {

		sctrl.set(cmd, param);

		msg.id = Message.ID_CTL_SCTRL;
		msg.sd = sctrl.sdGet();

		ms.sendMessage(msg);

	}

}
