package remuco;

import remuco.comm.ISerializable;
import remuco.comm.Serial;
import remuco.comm.SerialAtom;
import remuco.util.Log;

public final class ClientInfo implements ISerializable {

	private static final int[] ATOMS_FMT = new int[] { SerialAtom.TYPE_I,
			SerialAtom.TYPE_I, SerialAtom.TYPE_S };

	private static ClientInfo instance = null;

	public static ClientInfo getInstance() {
		if (instance == null) {
			instance = new ClientInfo();
		}
		return instance;
	}

	private final SerialAtom[] atoms;

	private ClientInfo() {

		atoms = SerialAtom.build(ATOMS_FMT);

		atoms[0].i = Config.SCREEN_WIDTH;
		atoms[1].i = Config.SCREEN_HEIGHT;
		atoms[2].s = Serial.ENCODING;

	}

	public SerialAtom[] getAtoms() {
		return atoms;
	}

	public void notifyAtomsUpdated() {
		Log.bug("Feb 22, 2009.6:25:29 PM");
	}

}
