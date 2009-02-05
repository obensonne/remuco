package remuco;

import remuco.comm.ISerializable;
import remuco.comm.Serial;
import remuco.comm.SerialAtom;
import remuco.util.Log;

public final class ClientInfo implements ISerializable {

	private final SerialAtom[] atoms;

	public static final ClientInfo ci = new ClientInfo();
	
	private ClientInfo() {

		atoms = new SerialAtom[3];
		atoms[0] = new SerialAtom(SerialAtom.TYPE_I);
		atoms[1] = new SerialAtom(SerialAtom.TYPE_I);
		atoms[2] = new SerialAtom(SerialAtom.TYPE_S);

		atoms[0].i = Config.SCREEN_WIDTH;
		atoms[1].i = Config.SCREEN_HEIGHT;
		
		atoms[2].s = Serial.ENCODING;
		
	}

	public void atomsHasBeenUpdated() {
		// not needed
		Log.asssertNotReached(this);
	}

	public SerialAtom[] getAtoms() {
		return atoms;
	}

	public void updateAtoms() {
		// always up2date
	}

}
