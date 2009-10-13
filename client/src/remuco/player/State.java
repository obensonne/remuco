/*   
 *   Remuco - A remote control system for media players.
 *   Copyright (C) 2006-2009 by the Remuco team, see AUTHORS.
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

import remuco.comm.ISerializable;
import remuco.comm.SerialAtom;

/**
 * Class to represent all relevant state information of a music player.
 */
public final class State implements ISerializable {

	private static final int[] ATOMS_FMT = new int[] { SerialAtom.TYPE_Y,
			SerialAtom.TYPE_Y, SerialAtom.TYPE_I, SerialAtom.TYPE_B,
			SerialAtom.TYPE_B, SerialAtom.TYPE_B };

	public static final byte PLAYBACK_PAUSE = 1;

	public static final byte PLAYBACK_PLAY = 2;

	public static final byte PLAYBACK_STOP = 0;

	private final SerialAtom[] atoms;

	private int playback, volume, position;

	private boolean repeat, shuffle, queue;

	public State() {

		atoms = SerialAtom.build(ATOMS_FMT);

		reset();
	}

	public void notifyAtomsUpdated() {

		playback = atoms[0].y;
		volume = atoms[1].y;
		position = atoms[2].i;
		repeat = atoms[3].b;
		shuffle = atoms[4].b;
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
	 * {@link #isPlayingFromQueue()} to check if the current item is played form
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
		queue = false;
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
