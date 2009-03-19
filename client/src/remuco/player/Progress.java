package remuco.player;

import remuco.comm.ISerializable;
import remuco.comm.SerialAtom;
import remuco.util.Tools;

public class Progress implements ISerializable {

	private static final int[] ATOMS_FMT = new int[] { SerialAtom.TYPE_I,
			SerialAtom.TYPE_I };

	private final SerialAtom[] atoms;

	public Progress() {
		atoms = SerialAtom.build(ATOMS_FMT);
	}

	public int getProgress() {
		return atoms[0].i;
	}

	public SerialAtom[] getAtoms() {
		return atoms;
	}

	public int getLength() {
		return atoms[1].i;
	}

	public void notifyAtomsUpdated() {
	}

	public String getLengthFormatted() {
		return Tools.formatTime(atoms[1].i);
	}

	public String getProgressFormatted() {
		return Tools.formatTime(atoms[0].i);
	}

}
