package remuco.player;

import remuco.comm.BinaryDataExecption;
import remuco.comm.ISerializable;
import remuco.comm.SerialAtom;
import remuco.util.Log;

/**
 * Parameters of a request to send to the server.
 * 
 * @author Oben Sonne
 * 
 */
public class RequestParam implements ISerializable {

	private static final int[] ATOMS_FMT = new int[] { SerialAtom.TYPE_S,
			SerialAtom.TYPE_AS };

	private final SerialAtom[] atoms;

	/** Request for an item. */
	public RequestParam(String id) {
		this();
		atoms[0].s = id;
	}

	/** Request for a file system or media lib level. */
	public RequestParam(String path[]) {
		this();
		atoms[1].as = path;
	}

	private RequestParam() {
		atoms = SerialAtom.build(ATOMS_FMT);
	}

	public SerialAtom[] getAtoms() {
		return atoms;
	}

	public void notifyAtomsUpdated() throws BinaryDataExecption {
		Log.bug("Mar 9, 2009.6:34:50 PM");
	}

}
