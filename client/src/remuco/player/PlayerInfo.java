package remuco.player;

import remuco.comm.BinaryDataExecption;
import remuco.comm.ISerializable;
import remuco.comm.SerialAtom;
import remuco.util.Log;

/** A descriptive interface for the player. */
public class PlayerInfo implements ISerializable {

	private static final int FEATURE_PLAYLIST = 1 << 0;
	private static final int FEATURE_QUEUE = 1 << 1;
	private static final int FEATURE_LIBRARY = 1 << 2;
	private static final int FEATURE_TAGS = 1 << 3;
	private static final int FEATURE_PLOBINFO = 1 << 4;
	private static final int FEATURE_JUMP_PLAYLIST = 1 << 5;
	private static final int FEATURE_JUMP_QUEUE = 1 << 6;
	private static final int FEATURE_LOAD_PLAYLIST = 1 << 7;
	private static final int FEATURE_SHUTDOWN_HOST = 1 << 8;
	private static final int FEATURE_VOLUME_UNKNOWN = 1 << 17;
	private static final int FEATURE_PLAYBACK_UNKNOWN = 1 << 18;

	private final SerialAtom[] atoms;

	private int flags = 0;

	private int maxRating = 0;

	private String name = "Remuco";

	public PlayerInfo() {

		atoms = new SerialAtom[3];
		atoms[0] = new SerialAtom(SerialAtom.TYPE_S);
		atoms[1] = new SerialAtom(SerialAtom.TYPE_I);
		atoms[2] = new SerialAtom(SerialAtom.TYPE_I);

	}

	public void atomsHasBeenUpdated() throws BinaryDataExecption {

		name = atoms[0].s;
		flags = atoms[1].i;
		maxRating = atoms[2].i;
	}

	public SerialAtom[] getAtoms() {

		return atoms;

	}

	/** Get maximum rating or 0 if rating is not supported. */
	public int getMaxRating() {
		return maxRating;
	}

	/**
	 * Get the name of the player.
	 * 
	 * @return the name (never <code>null</code> - returns 'Remuco' when there
	 *         is no name)
	 */
	public String getName() {
		return name;
	}

	public boolean supportsJumpPlaylist() {
		return (flags & FEATURE_JUMP_PLAYLIST) != 0;
	}

	public boolean supportsJumpQueue() {
		return (flags & FEATURE_JUMP_QUEUE) != 0;
	}

	public boolean supportsLibrary() {
		return (flags & FEATURE_LIBRARY) != 0;
	}

	public boolean supportsLoadPlaylist() {
		return (flags & FEATURE_LOAD_PLAYLIST) != 0;
	}

	public boolean supportsPlaybackStatus() {
		return (flags & FEATURE_PLAYBACK_UNKNOWN) == 0;
	}

	public boolean supportsPlaylist() {
		return (flags & FEATURE_PLAYLIST) != 0;
	}

	public boolean supportsPlobInfo() {
		return (flags & FEATURE_PLOBINFO) != 0;
	}

	public boolean supportsQueue() {
		return (flags & FEATURE_QUEUE) != 0;
	}

	public boolean supportsShutdownHost() {
		return (flags & FEATURE_SHUTDOWN_HOST) != 0;
	}

	public boolean supportsTags() {
		return (flags & FEATURE_TAGS) != 0;
	}

	public boolean supportsVolumeStatus() {
		return (flags & FEATURE_VOLUME_UNKNOWN) == 0;
	}

	public void updateAtoms() {
		// not needed
		Log.bug("Jan 26, 2009.9:40:55 PM");
	}

}