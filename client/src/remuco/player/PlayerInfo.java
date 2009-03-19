package remuco.player;

import java.util.Vector;

import remuco.comm.BinaryDataExecption;
import remuco.comm.ISerializable;
import remuco.comm.SerialAtom;

/** A descriptive interface for the player. */
public class PlayerInfo implements ISerializable {

	private static final int[] ATOMS_FMT = new int[] { SerialAtom.TYPE_S,
			SerialAtom.TYPE_I, SerialAtom.TYPE_Y, SerialAtom.TYPE_AI,
			SerialAtom.TYPE_AS, SerialAtom.TYPE_AB, SerialAtom.TYPE_AS };

	private final SerialAtom[] atoms;

	private final Vector fileActions;

	private int flags = 0;

	private int maxRating = 0;

	private String name = "Remuco";

	public PlayerInfo() {

		atoms = SerialAtom.build(ATOMS_FMT);

		fileActions = new Vector();

	}

	public SerialAtom[] getAtoms() {
		return atoms;
	}

	public Vector getFileActions() {
		return fileActions;
	}

	public int getMaxRating() {
		return maxRating;
	}

	public String getName() {
		return name;
	}

	public void notifyAtomsUpdated() throws BinaryDataExecption {

		name = atoms[0].s;
		flags = atoms[1].i;
		maxRating = atoms[2].y;

		fileActions.removeAllElements();
		int off = 3;
		for (int i = 0; i < atoms[off].ai.length; i++) {
			fileActions.addElement(new ItemAction(atoms[off].ai[i],
					atoms[off + 1].as[i], atoms[off + 2].ab[i],
					atoms[off + 3].as[i]));
		}

	}

	public boolean supports(int feature) {
		return (flags & feature) != 0;
	}

	public boolean supportsMediaBrowser() {

		boolean b = false;
		b |= (flags & Feature.REQ_PL) != 0;
		b |= (flags & Feature.REQ_QU) != 0;
		b |= (flags & Feature.REQ_MLIB) != 0;
		b |= fileActions.size() > 0;
		return b;
	}
}