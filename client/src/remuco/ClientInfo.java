package remuco;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;

import remuco.comm.ISerializable;
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

		// just to get the screen size
		Canvas c = new Canvas() {
			protected void paint(Graphics g) {
			}
		};

		atoms[0].i = c.getWidth();
		atoms[1].i = c.getHeight();
		atoms[2].s = Config.encoding;
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
