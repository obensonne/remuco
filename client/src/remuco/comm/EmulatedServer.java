package remuco.comm;

import java.util.Vector;

import remuco.player.Info;
import remuco.player.Plob;
import remuco.player.PlobList;
import remuco.player.SimpleControl;
import remuco.player.State;
import remuco.player.StringParam;
import remuco.util.Log;
import remuco.util.Tools;

/**
 * @emulator Only used for testing!
 */
public final class EmulatedServer implements IMessageSender {

	private final class EmulatedPlayer extends Thread {

		private Plob currentPlob;

		private State ps = new State();

		private int state, volume, plpos, repeat, shuffle;

		public void run() {

			// DEBUG TRY CATCH //
			try {
				while (!interrupted) {

					if (Tools.random(2) == 0) {

						state = (int) Tools.random(State.ST_COUNT);
						volume = (int) Tools.random(100);
						repeat = (int) Tools.random(4);
						shuffle = (int) Tools.random(2);

					}

					if (Tools.random(3) == 0) {

						currentPlob = (Plob) playlist.elementAt((int) Tools
								.random(playlist.size()));

					}

					Tools.sleep(2000 + Tools.random(28000));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		protected Plob getCurrentPlob() {

			return currentPlob;

		}

		protected State getState() {

			ps.set(state, volume, plpos, repeat, shuffle);

			return ps;

		}

		protected void sctrl(int cmd, int param) {

			switch (cmd) {
			case SimpleControl.CMD_JUMP:

				plpos = param;
				currentPlob = (Plob) playlist.elementAt(plpos - 1);
				break;

			case SimpleControl.CMD_NEXT:

				if (plpos >= playlist.size())
					return;
				plpos++;
				currentPlob = (Plob) playlist.elementAt(plpos - 1);
				break;

			case SimpleControl.CMD_PREV:

				if (plpos <= 1)
					return;
				plpos--;
				currentPlob = (Plob) playlist.elementAt(plpos - 1);
				break;

			case SimpleControl.CMD_VOLUME:

				volume = param;
				break;

			case SimpleControl.CMD_PLAYPAUSE:

				switch (state) {
				case State.ST_PLAY:
					state = State.ST_PAUSE;
					break;
				case State.ST_PAUSE:
				case State.ST_STOP:
					state = State.ST_PLAY;
					break;
				default:
					break;
				}

				break;

			case SimpleControl.CMD_RATE:

				if (currentPlob != null)
					currentPlob.setRating(param);
				break;

			case SimpleControl.CMD_RESTART:

				currentPlob = (Plob) playlist.firstElement();
				plpos = 1;
				state = State.ST_PLAY;
				break;

			case SimpleControl.CMD_SEEK:

				Log.ln("[EMUP] seek not yet implemented");
				break;

			case SimpleControl.CMD_STOP:

				state = State.ST_STOP;
				break;

			default:

				Log.ln("[EMUP] unknown command: " + cmd);
				break;
			}

		}

	}

	private final IMessageReceiver client;

	private boolean clientFullyConnected = false;

	private Plob currentPlob;

	private final EmulatedPlayer ep;

	private final Vector incomingMessageQueue = new Vector();

	private boolean interrupted = false;

	private final Info pi = Info.EMULATOR;

	private final Vector playlist = new Vector();

	private final Object select = new Object();

	private final State state = new State();

	public EmulatedServer(IMessageReceiver mr) {

		state.reset();

		ep = new EmulatedPlayer();

		this.client = mr;

		Plob plob;

		plob = new Plob("2001");
		plob.setMeta(Plob.META_ALBUM, "The Legend");
		plob.setMeta(Plob.META_ARTIST, "Bob Marley");
		plob.setMeta(Plob.META_TITLE, "Stir it up");
		plob.setRating(3);
		playlist.addElement(plob);

		plob = new Plob("2453");
		plob.setMeta(Plob.META_ALBUM, "The Legend");
		plob.setMeta(Plob.META_ARTIST, "Bob Marley");
		plob.setMeta(Plob.META_TITLE, "Redemption Song");
		plob.setRating(5);
		playlist.addElement(plob);

		plob = new Plob("5123");
		plob.setMeta(Plob.META_ALBUM, "Things To Make And Do");
		plob.setMeta(Plob.META_ARTIST, "Moloko");
		plob.setMeta(Plob.META_TITLE, "Bring it down");
		plob.setRating(4);
		playlist.addElement(plob);

		plob = new Plob("2134");
		plob.setMeta(Plob.META_ALBUM, "The Legend");
		plob.setMeta(Plob.META_ARTIST, "Bob Marley");
		plob.setMeta(Plob.META_TITLE, "Buffalo Soldier");
		plob.setMeta(Plob.META_GENRE, "Boombass");
		plob.setMeta(Plob.META_YEAR, "1989");
		plob.setMeta(Plob.META_LENGTH, "156");
		playlist.addElement(plob);

		plob = new Plob("4352");
		plob.setMeta(Plob.META_ALBUM, "Do You Like My Tighty Sweater?");
		plob.setMeta(Plob.META_ARTIST, "Moloko");
		plob.setMeta(Plob.META_TITLE, "Moscow");
		plob.setRating(2);
		playlist.addElement(plob);

	}

	/**
	 * Blocking method! Sends at some intervals messages (as they could come
	 * from the server) to the {@link IMessageReceiver} secified at
	 * {@link #EmulatedServer(IMessageReceiver)}.
	 * 
	 */
	public void loop() {

		try {
			ep.start();
		} catch (Exception e) {
			e.printStackTrace();
		}

		Message msgFromClient;
		Message msgToClient = new Message();

		while (!interrupted) {

			try {

				synchronized (select) {

					try {
						select.wait(2000);
					} catch (InterruptedException e) {
						Log.debug("[EMUP] interupted !?");
						if (interrupted)
							break;
						else
							continue;
					}

				}

				// handle incoming messages

				while (incomingMessageQueue.size() > 0) {

					msgFromClient = (Message) incomingMessageQueue
							.firstElement();

					incomingMessageQueue.removeElementAt(0);

					switch (msgFromClient.id) {
					case Message.ID_IFC_CINFO:

						msgToClient.id = Message.ID_IFS_PINFO;
						msgToClient.sd = pi.sdGet();

						client.receiveMessage(msgToClient);

						clientFullyConnected = true;

						PlobList pl = new PlobList(PlobList.PLID_PLAYLIST,
								"Playlist");

						pl.addPlob("123", "Dings mit Ihm - Dieses Lied");
						pl.addPlob("124", "Eins Zwo - Ich so Er so");

						msgToClient.id = Message.ID_IFS_PLAYLIST;
						msgToClient.sd = pl.sdGet();

						client.receiveMessage(msgToClient);

						break;

					case Message.ID_CTL_SCTRL:

						if (state.getState() != State.ST_PLAY
								&& state.getState() != State.ST_PAUSE
								&& state.getState() != State.ST_STOP)
							break;

						SimpleControl sctrl = new SimpleControl();
						sctrl.sdSet(msgFromClient.sd);
						ep.sctrl(sctrl.getCmd(), sctrl.getParam());

						break;

					case Message.ID_REQ_PLOB:

						StringParam sp = new StringParam();

						sp.sdSet(msgFromClient.sd);

						Log.ln("received request for plob" + sp.getParam());

						msgToClient.id = Message.ID_REQ_PLOB;

						if (currentPlob != null)
							msgToClient.sd = currentPlob.sdGet();
						else
							msgToClient.sd = new Plob(sp.getParam()).sdGet();

						client.receiveMessage(msgToClient);

					default:
						break;
					}

				}

				// handle player changes

				if (!ep.getState().copyInto(state)) {

					msgToClient.id = Message.ID_IFS_STATE;
					msgToClient.sd = state.sdGet();

					client.receiveMessage(msgToClient);

				}

				if (currentPlob != ep.getCurrentPlob()) {

					currentPlob = ep.getCurrentPlob();

					msgToClient.id = Message.ID_IFS_CURPLOB;
					msgToClient.sd = currentPlob == null ? null : currentPlob
							.sdGet();

					client.receiveMessage(msgToClient);

				}

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		Log.debug("[EMUP] loop done");

	}

	/**
	 * Sends a message as it could get send to the server.
	 */
	public void sendMessage(Message m) {

		serverReceivesMessage(m);

	}

	/**
	 * Makes the method {@link #loop()} return (with some delay).
	 * 
	 */
	public void stop() {

		Log.debug("[EMUP] going down");

		interrupted = true;

		ep.interrupt();

	}

	private void serverReceivesMessage(Message m) {

		Log.debug("[EMUP] rx'ed message " + m.id);

		Message mCopy = new Message();

		// XXX this may cause dubios behaviour since in most cases the caller
		// reuses the message m and its content .. so mCopy.sd may change until
		// it get processed by loop() .. well, may
		mCopy.id = m.id;
		mCopy.sd = m.sd;

		incomingMessageQueue.addElement(mCopy);

		synchronized (select) {
			select.notify();
		}

	}

}
