package remuco.player;

import remuco.comm.IStructuredData;
import remuco.util.Tools;

/**
 * Class to represent all relevant state information of a music player.
 * 
 * @author Christian Buennig
 * 
 */
public final class Status implements IStructuredData {

	/** Player state */
	public static final byte ST_STOP = 0;

	/** Player state */
	public static final byte ST_PLAY = 1;

	/** Player state */
	public static final byte ST_PAUSE = 2;

	/** Player state */
	public static final byte ST_OFF = 3;

	public static final byte ST_COUNT = 4;

	/** Remuco server state (no player state) */
	public static final byte ST_ERROR = ST_COUNT + 0;

	/** Remuco server state (no player state) */
	public static final byte ST_SRVOFF = ST_COUNT + 1;

	/** Remuco client state (no server or player state) */
	public static final byte ST_UNKNOWN = 50;

	public static final byte SHUFFLE_MODE_OFF = 0;

	public static final byte SHUFFLE_MODE_ON = 1;

	public static final byte REPEAT_MODE_NONE = 0;

	public static final byte REPEAT_MODE_PLOB = 1 << 0;

	public static final byte REPEAT_MODE_ALBUM = 1 << 1;

	public static final byte REPEAT_MODE_PL = 1 << 2;

	// ////////////////////////////////////////////////////////////////////////

	public static final int[] sdFormatVector = new int[] { DT_INT, 5 };

	private static final int IVPOS_STATE = 0;

	private static final int IVPOS_VOLUME = 1;

	private static final int IVPOS_PLPOS = 2;

	private static final int IVPOS_REPMODE = 3;

	private static final int IVPOS_SHFMODE = 4;

	// ////////////////////////////////////////////////////////////////////////

	private final int[] serialIv = new int[5];

	private final Object[] serialBdv = new Object[] { serialIv };

	// ////////////////////////////////////////////////////////////////////////

	public Status() {
		reset();
	}

	/**
	 * Copy this state's values into <code>dest</code>.
	 * 
	 * @param dest
	 *            destination state
	 * 
	 * @return <code>true</code> if the this state and <code>dest</code>
	 *         have been equal, <code>false</code> otherwise.
	 * 
	 */
	public boolean copyInto(Status dest) {

		boolean equal = true;

		for (int i = 0; i < serialIv.length; i++) {

			if (dest.serialIv[i] != serialIv[i]) {
				dest.serialIv[i] = serialIv[i];
				equal = false;
			}

		}

		return equal;

	}

	/**
	 * @emulator Only used for testing!
	 */
	public boolean equals(Object o) {
		if (o == null)
			return false;
		if (this == o)
			return true;
		if (!(o instanceof Status))
			return false;

		Status ps = (Status) o;

		return Tools.compare(serialIv, ps.serialIv);
	}

	/**
	 * Get playlist or queue position <b>(starting from 1)</b>. Whatever is
	 * currently active. Use {@link #isPlayingFromQueue()} to check if the
	 * current plob played form the playlist or the queue.
	 * 
	 * @return position, starting from 1, or 0 if no song form the playlist or
	 *         queue is currently played
	 */
	public int getPlaylistPosition() {
		return Math.abs(serialIv[IVPOS_PLPOS]);
	}

	public int getRepeatMode() {
		return serialIv[IVPOS_REPMODE];
	}

	public int getShuffleMode() {
		return serialIv[IVPOS_SHFMODE];
	}

	public int getState() {
		return serialIv[IVPOS_STATE];
	}

	public int getVolume() {
		return serialIv[IVPOS_VOLUME];
	}

	public boolean isPlayingFromQueue() {
		return serialIv[IVPOS_PLPOS] < 0;
	}

	public void reset() {
		serialIv[IVPOS_PLPOS] = 0;
		serialIv[IVPOS_REPMODE] = REPEAT_MODE_NONE;
		serialIv[IVPOS_SHFMODE] = SHUFFLE_MODE_OFF;
		serialIv[IVPOS_STATE] = ST_UNKNOWN;
		serialIv[IVPOS_VOLUME] = 50;
	}

	/**
	 * @forTestsOnly
	 */
	public Object[] sdGet() {
		return serialBdv;
	}

	public void sdSet(Object[] bdv) {

		int[] iv = (int[]) bdv[0];

		for (int i = 0; i < iv.length; i++) {
			serialIv[i] = iv[i];
		}
	}

	/**
	 * @emulator Only used for testing!
	 */
	public void set(int state, int volume, int plpos, int repeat, int shuffle) {

		serialIv[0] = state;
		serialIv[1] = volume;
		serialIv[2] = plpos;
		serialIv[3] = repeat;
		serialIv[4] = shuffle;

	}

	public String toString() {

		StringBuffer sb = new StringBuffer("|");

		for (int i = 0; i < serialIv.length; i++) {
			sb.append(serialIv[i]).append("|");
		}

		return sb.toString();
	}

}
