/*
 * Copyright (C) 2006 Christian Buennig - See COPYING
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 * 
 */
package remuco.data;

import java.util.Vector;

import remuco.proto.Remuco;

/**
 * Class to represent all relevant state information of a music player.
 * 
 * @author Christian Buennig
 * 
 */
public class PlayerState {

	public static final byte ST_ERROR = Remuco.REM_PS_STATE_ERROR;

	public static final byte ST_OFF = Remuco.REM_PS_STATE_OFF;

	public static final byte ST_PAUSED = Remuco.REM_PS_STATE_PAUSE;

	public static final byte ST_PLAYING = Remuco.REM_PS_STATE_PLAY;

	public static final byte ST_PROBLEM = Remuco.REM_PS_STATE_PROBLEM;

	public static final byte ST_STOPPED = Remuco.REM_PS_STATE_STOP;

	public static final byte ST_UNKNOWN = 50;

	private Vector pl;

	private long plID;

	private short plPos;

	private boolean plRepeat;

	private boolean plShuffle;

	private byte state;

	private byte volume;

	public PlayerState() {
		pl = new Vector();
		plID = System.currentTimeMillis();
		plRepeat = false;
		plShuffle = false;
		plPos = 0;
		state = PlayerState.ST_UNKNOWN;
		volume = 0;
	}

	public synchronized boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || !(o instanceof PlayerState))
			return false;
		PlayerState ps = (PlayerState) o;
		synchronized (ps) {
			if (volume != ps.volume || state != ps.state || plPos != ps.plPos
					|| plRepeat != ps.plRepeat || plShuffle != ps.plShuffle)
				return false;
			if (pl != null && ps.pl == null || pl == null && ps.pl != null)
				return false;
			if (pl.size() != ps.pl.size())
				return false;
			int len = pl.size();
			for (int i = 0; i < len; i++) {
				if (!(pl.elementAt(i).equals(ps.pl.elementAt(i))))
					return false;
			}
		}
		return true;
	}

	/**
	 * @return the current song or <code>null</code> if there is no current
	 *         song
	 */
	public synchronized Song getCurrentSong() {
		return playlistGetSong(playlistGetPosition());
	}

	public synchronized byte getState() {
		return state;
	}

	public synchronized byte getVolume() {
		return volume;
	}

	public synchronized void playlistAddSong(Song s) {
		pl.addElement(s);
		plID++;
	}

	public synchronized void playlistClear() {
		pl.removeAllElements();
		plPos = 0;
		plID++;
	}

	/**
	 * Get the ID of the current playlist. The contract is, that whenever a song
	 * is added to, removed from or moved within the playlist, the ID changes.
	 * Further, the ID is never 0.
	 * 
	 * @return the playlist ID
	 */
	public synchronized long playlistGetID() {
		return plID;
	}

	public synchronized short playlistGetLength() {
		return (short) pl.size();
	}

	public synchronized short playlistGetPosition() {
		return plPos;
	}

	/**
	 * Get the song at playlist position pos
	 * 
	 * @param pos
	 *            playlist position
	 * @return the song or null if pos is out of bounds.
	 */
	public synchronized Song playlistGetSong(short pos) {
		if (pos < 0 || pos >= pl.size()) {
			return null;
		}
		return (Song) pl.elementAt(pos);
	}

	/**
	 * Insert a song into playlist at a specific position.
	 * 
	 * @param s
	 *            song to insert
	 * @param pos
	 *            Insert Position (0 &lt;= pos &lt; length of list). If pos is
	 *            out of these bounds, the song gets appended to the list
	 */
	public synchronized void playlistInsertSong(Song s, short pos) {
		if (pos < 0 || pos >= pl.size()) {
			pl.addElement(s);
		} else {
			pl.insertElementAt(s, pos);
		}
		plID++;
	}

	/**
	 * Get the repeat mode of the playlist.
	 * 
	 * @return repeat mode on or off
	 */
	public synchronized boolean playlistIsRepeat() {
		return plRepeat;
	}

	/**
	 * Get the shuffle mode of the playlist.
	 * 
	 * @return shuffle mode on or off
	 */
	public synchronized boolean playlistIsShuffle() {
		return plShuffle;
	}

	public synchronized void playlistSetPosition(short plPosistion) {
		this.plPos = plPosistion;
	}

	public synchronized void playlistSetRepeat(boolean playlistRepeat) {
		this.plRepeat = playlistRepeat;
	}

	public synchronized void playlistSetShuffle(boolean playlistShuffle) {
		this.plShuffle = playlistShuffle;
	}

	public synchronized void setState(byte state) {
		this.state = state;
	}

	public synchronized void setVolume(byte volume) {
		this.volume = volume;
	}

	public synchronized String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("ST:").append(state).append("|VOL:").append(volume);
		sb.append("|PLPOS:").append(plPos).append("|PLLEN:");
		sb.append(pl.size()).append("|PLREP:").append(plRepeat);
		sb.append("|PLSHU:").append(plShuffle);
		return sb.toString();
	}

}
