package remuco.player;

import remuco.comm.ISerializable;
import remuco.comm.SerialAtom;
import remuco.util.Log;

/**
 * A ploblist is a list of plobs. A ploblist does not contain real {@link Plob}s.
 * In fact for each contained plob, it stores the plob's PID and title (which
 * may be anything - however, using the plobs's title and artist as title is a
 * meaningful choice). The actually contained {@link Plob}s can be requested
 * via their P(L)IDs with {@link Player#reqPlob(String, IPlobRequestor)}.
 * 
 * @author Christian Buennig
 * 
 */
public final class PlobList implements ISerializable {

	private static final String NAME_UNKNWON = "??NAME??";

	private static final String PLID_UNKNWON = "??PLID??";

	private final SerialAtom[] atoms;

	private String name, id;

	private String[] nestedIDs, nestedNames, plobIDs, plobNames;

	public PlobList() {

		atoms = new SerialAtom[6];
		atoms[0] = new SerialAtom(SerialAtom.TYPE_S);
		atoms[1] = new SerialAtom(SerialAtom.TYPE_S);
		atoms[2] = new SerialAtom(SerialAtom.TYPE_AS);
		atoms[3] = new SerialAtom(SerialAtom.TYPE_AS);
		atoms[4] = new SerialAtom(SerialAtom.TYPE_AS);
		atoms[5] = new SerialAtom(SerialAtom.TYPE_AS);

		reset();
	}

	public void atomsHasBeenUpdated() {

		id = atoms[0].s;
		name = atoms[1].s;

		nestedIDs = new String[atoms[2].as.length];
		System.arraycopy(atoms[2].as, 0, nestedIDs, 0, atoms[2].as.length);

		nestedNames = new String[atoms[3].as.length];
		System.arraycopy(atoms[3].as, 0, nestedNames, 0, atoms[3].as.length);

		plobIDs = new String[atoms[4].as.length];
		System.arraycopy(atoms[4].as, 0, plobIDs, 0, atoms[4].as.length);

		plobNames = new String[atoms[5].as.length];
		System.arraycopy(atoms[5].as, 0, plobNames, 0, atoms[5].as.length);

	}

	public SerialAtom[] getAtoms() {
		return atoms;
	}

	public String getID() {
		return id;
	}

	public String getName() {
		return name;
	}

	/** Get the ID of the nested ploblist #<code>i</code> (starting from 0). */
	public String getNestedID(int i) {

		try {
			return nestedIDs[i];
		} catch (ArrayIndexOutOfBoundsException e) {
			return "#~@X+.YO?/";
		}
	}

	/** Get the name of the nested ploblist #<code>i</code> (starting from 0). */
	public String getNestedName(int i) {

		try {
			return nestedNames[i];
		} catch (ArrayIndexOutOfBoundsException e) {
			return "#~@X+.YO?/";
		}
	}

	public int getNumNested() {
		
		return nestedIDs.length;
	}

	public int getNumPlobs() {
		
		return plobIDs.length;
	}

	/** Get the PID of the plob #<code>i</code> (starting from 0). */
	public String getPlobID(int i) {

		try {
			return plobIDs[i];
		} catch (ArrayIndexOutOfBoundsException e) {
			return "#~@X+.YO?/";
		}
	}

	/** Get the name of the plob #<code>i</code> (starting from 0). */
	public String getPlobName(int i) {

		try {
			return plobNames[i];
		} catch (ArrayIndexOutOfBoundsException e) {
			return "#~@X+.YO?/";
		}
	}

	public String toString() {

		StringBuffer sb = new StringBuffer("Ploblist: ").append(id);
		sb.append(" (").append(name).append(")\n");

		return sb.toString();
	}

	public void updateAtoms() {

		// not needed
		Log.asssertNotReached(this);
	}

	protected void reset() {

		name = NAME_UNKNWON;
		id = PLID_UNKNWON;

		nestedIDs = new String[0];
		nestedNames = new String[0];
		plobIDs = new String[0];
		plobNames = new String[0];
	}

}
