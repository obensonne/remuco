package remuco.player;

import remuco.comm.BinaryDataExecption;
import remuco.comm.ISerializable;
import remuco.comm.SerialAtom;
import remuco.util.Log;

/** A descriptive interface for the player. */
public class PlayerInfo implements ISerializable {

	// private static final int FLAG_BPP = 1 << 16; // not used

	private static final int FLAG_PLAYBACK_UNKNOWN = 1 << 18;

	private static final int FLAG_VOLUME_UNKNOWN = 1 << 17;

	private final SerialAtom[] atoms;

	public PlayerInfo() {

		atoms = new SerialAtom[3];
		atoms[0] = new SerialAtom(SerialAtom.TYPE_S);
		atoms[1] = new SerialAtom(SerialAtom.TYPE_I);
		atoms[2] = new SerialAtom(SerialAtom.TYPE_I);

	}

	public void atomsHasBeenUpdated() throws BinaryDataExecption {

		// not needed
	}

	public SerialAtom[] getAtoms() {

		return atoms;

	}

	/** Get maximum rating or 0 if rating is not supported. */
	public int getMaxRating() {
		return atoms[2].i;
	}

	/**
	 * Get the name of the player.
	 * 
	 * @return the name (never <code>null</code> - returns 'Remuco' when there
	 *         is no name)
	 */
	public String getName() {
		return atoms[0].s;
	}

	public boolean isPlaybackKnown() {
		return (atoms[1].i & FLAG_PLAYBACK_UNKNOWN) == 0;
	}

	public boolean isVolumeKnown() {
		return (atoms[1].i & FLAG_VOLUME_UNKNOWN) == 0;
	}

	public void updateAtoms() {
		// not needed
		Log.bug("Jan 26, 2009.9:40:55 PM");
	}

}