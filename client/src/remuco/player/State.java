package remuco.player;

import remuco.comm.ISerializable;
import remuco.comm.SerialAtom;
import remuco.util.Log;

/**
 * Class to represent all relevant state information of a music player.
 */
public final class State implements ISerializable {

	public static final byte PLAYBACK_PAUSE = 1;

	public static final byte PLAYBACK_PLAY = 2;

	public static final byte PLAYBACK_STOP = 0;

	private final SerialAtom[] atoms;

	private int playback, volume, position;

	private boolean repeat, shuffle, queue;

	public State() {

		atoms = new SerialAtom[6];

		atoms[0] = new SerialAtom(SerialAtom.TYPE_I);
		atoms[1] = new SerialAtom(SerialAtom.TYPE_I);
		atoms[2] = new SerialAtom(SerialAtom.TYPE_B);
		atoms[3] = new SerialAtom(SerialAtom.TYPE_B);
		atoms[4] = new SerialAtom(SerialAtom.TYPE_I);
		atoms[5] = new SerialAtom(SerialAtom.TYPE_B);

		reset();
	}

	public void atomsHasBeenUpdated() {

		playback = atoms[0].i;
		volume = atoms[1].i;
		repeat = atoms[2].b;
		shuffle = atoms[3].b;
		position = atoms[4].i;
		queue = atoms[5].b;
	}

	public SerialAtom[] getAtoms() {
		return atoms;
	}

	public int getPlayback() {
		return playback;
	}

	/**
	 * Get playlist or queue position. Whatever is currently active. Use
	 * {@link #isPlayingFromQueue()} to check if the current plob is played form
	 * the playlist or the queue.
	 * 
	 * @return position
	 */
	public int getPosition() {
		return position;
	}

	public int getVolume() {
		return volume;
	}

	public boolean isPlayingFromQueue() {
		return queue;
	}

	public boolean isRepeat() {
		return repeat;
	}

	public boolean isShuffle() {
		return shuffle;
	}

	public void reset() {

		playback = PLAYBACK_STOP;
		volume = 50;
		repeat = false;
		shuffle = false;
		position = 0;
	}

	public String toString() {

		StringBuffer sb = new StringBuffer("|");
		sb.append(playback).append("|");
		sb.append(volume).append("|");
		sb.append(repeat).append("|");
		sb.append(shuffle).append("|");
		sb.append(position).append("|");

		return sb.toString();
	}

	public void updateAtoms() {

		// not needed
		Log.asssertNotReached(this);

	}

	protected void setRepeat(boolean repeat) {
		this.repeat = repeat;
	}

	protected void setShuffle(boolean shuffle) {
		this.shuffle = shuffle;
	}

	protected void setVolume(int volume) {
		this.volume = volume;
	}

}
