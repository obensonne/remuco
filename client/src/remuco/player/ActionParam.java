package remuco.player;

import remuco.comm.BinaryDataExecption;
import remuco.comm.ISerializable;
import remuco.comm.SerialAtom;
import remuco.util.Log;

/**
 * Parameters of an action (list or item) to send to the server.
 * 
 * @author Oben Sonne
 * 
 */
public class ActionParam implements ISerializable {

	private static final int[] ATOMS_FMT = new int[] { SerialAtom.TYPE_I,
			SerialAtom.TYPE_AS, SerialAtom.TYPE_AI, SerialAtom.TYPE_AS };

	private final SerialAtom[] atoms;

	/** Action on playlist/queue or its items. */
	public ActionParam(int id, int positions[], String itemIDs[]) {
		this();
		atoms[0].i = id;
		atoms[2].ai = positions;
		atoms[3].as = itemIDs;
	}

	/** Action on file list. */
	public ActionParam(int id, String files[]) {
		this();
		atoms[0].i = id;
		atoms[3].as = files;
	}

	/** Action on a library level or its items. */
	public ActionParam(int id, String libPath[], int positions[],
			String itemIDs[]) {
		this();
		atoms[0].i = id;
		atoms[1].as = libPath;
		atoms[2].ai = positions;
		atoms[3].as = itemIDs;
	}

	private ActionParam() {
		atoms = SerialAtom.build(ATOMS_FMT);
	}

	public SerialAtom[] getAtoms() {
		return atoms;
	}

	public void notifyAtomsUpdated() throws BinaryDataExecption {
		Log.bug("Mar 9, 2009.6:29:32 PM");
	}

}
