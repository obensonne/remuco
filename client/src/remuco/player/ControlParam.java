package remuco.player;

import remuco.comm.BinaryDataExecption;
import remuco.comm.ISerializable;
import remuco.comm.SerialAtom;
import remuco.util.Log;

/**
 * Parameters of a control to send to the server.
 * 
 * @author Oben Sonne
 * 
 */
public class ControlParam implements ISerializable {

	private static final int[] ATOMS_FMT = new int[] { SerialAtom.TYPE_I };

	private final SerialAtom[] atoms;

	public ControlParam(int param) {
		atoms = SerialAtom.build(ATOMS_FMT);
		atoms[0].i = param;
	}

	public SerialAtom[] getAtoms() {
		return atoms;
	}

	public void notifyAtomsUpdated() throws BinaryDataExecption {
		Log.bug("Mar 9, 2009.5:36:51 PM");
	}

}
