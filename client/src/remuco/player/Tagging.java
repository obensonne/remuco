package remuco.player;

import java.util.Vector;

import remuco.comm.BinaryDataExecption;
import remuco.comm.ISerializable;
import remuco.comm.SerialAtom;
import remuco.util.Log;
import remuco.util.Tools;

public class Tagging implements ISerializable {

	private static final int[] ATOMS_FMT = new int[] { SerialAtom.TYPE_S,
			SerialAtom.TYPE_AS };

	public static String[] trimAndFlatten(String tagString) {

		final String tags[] = Tools.splitString(tagString, ',', true);

		final Vector clean = new Vector(tags.length);

		for (int j = 0; j < tags.length; j++) {

			final String tag = tags[j];

			if (tag.length() == 0 || clean.contains(tag)) {
				continue;
			}

			clean.addElement(tag);
		}

		final String ret[] = new String[clean.size()];

		clean.copyInto(ret);

		return ret;
	}

	private final SerialAtom[] atoms;

	public Tagging(String id, String tags) {
		atoms = SerialAtom.build(ATOMS_FMT);
		atoms[0].s = id;
		atoms[1].as = trimAndFlatten(tags);
	}

	public SerialAtom[] getAtoms() {
		return atoms;
	}

	public void notifyAtomsUpdated() throws BinaryDataExecption {
		Log.bug("Mar 9, 2009.6:32:37 PM");
	}

}
