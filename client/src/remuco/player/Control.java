package remuco.player;

import remuco.comm.ISerializable;
import remuco.comm.SerialAtom;
import remuco.util.Log;

public final class Control implements ISerializable {

	public static final int CMD_IGNORE = 0;

	public static final int CMD_PLAYPAUSE = 1;

	public static final int CMD_NEXT = 3;

	public static final int CMD_PREV = 4;

	public static final int CMD_JUMP = 5;

	public static final int CMD_SEEK_FWD = 6;

	public static final int CMD_SEEK_BWD = 7;
	
	public static final int CMD_VOLUME = 8;

	public static final int CMD_RATE = 9;

	public static final int CMD_PLAYNEXT = 10;

	public static final int CMD_SETTAGS = 12;

	public static final int CMD_REPEAT = 13;

	public static final int CMD_SHUFFLE = 14;
	
	public static final int CMD_SHUTDOWN = 0x0100;

	private final SerialAtom[] atoms;

	/**
	 * Creates a simple control with the given command and param.
	 * 
	 * @param cmd
	 *            use constants <code>CMD_..</code>, e.g. {@link #CMD_NEXT}.
	 * @param param
	 */
	public Control() {

		atoms = new SerialAtom[3];
		atoms[0] = new SerialAtom(SerialAtom.TYPE_I);
		atoms[1] = new SerialAtom(SerialAtom.TYPE_I);
		atoms[2] = new SerialAtom(SerialAtom.TYPE_S);

	}

	public void atomsHasBeenUpdated() {
		Log.bug("Feb 22, 2009.6:26:18 PM");
	}

	public SerialAtom[] getAtoms() {
		return atoms;
	}

	/**
	 * Set command and integer param.
	 * 
	 * @param cmd
	 * @param param
	 */
	public void set(int cmd, int paramI) {

		atoms[0].i = cmd;
		atoms[1].i = paramI;
		atoms[2].s = null;
	}

	/**
	 * Set command and both param.
	 * 
	 * @param cmd
	 * @param paramI
	 * @param paramS
	 */
	public void set(int cmd, int paramI, String paramS) {

		// it is safe to set the atoms directly since this is a write only class
		// used synchronized in Player (whenever the atoms get read, they
		// immediately get serialized)

		atoms[0].i = cmd;
		atoms[1].i = paramI;
		atoms[2].s = paramS;
	}

	/**
	 * Set command and string param.
	 * 
	 * @param cmd
	 * @param paramS
	 */
	public void set(int cmd, String paramS) {

		atoms[0].i = cmd;
		atoms[1].i = 0;
		atoms[2].s = paramS;
	}

	public String toString() {

		StringBuffer sb = new StringBuffer();
		sb.append("CODE: ").append(atoms[0].i);
		sb.append(" - ParamI: ").append(atoms[1].i);
		sb.append(" - ParamS: ").append(atoms[2].s);

		return sb.toString();
	}

	public void updateAtoms() {

		// always up2date
	}
}
