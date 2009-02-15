package remuco.player;

import remuco.comm.ISerializable;
import remuco.comm.SerialAtom;
import remuco.util.Log;

/**
 * TODO doc
 * 
 * @author Oben Sonne
 * 
 */
public final class PlobList implements ISerializable {

	protected static final String PATH_PLAYLIST[] = new String[] { "__PLAYLIST__" };

	protected static final String PATH_QUEUE[] = new String[] { "__QUEUE__" };

	private static final String UNKNWON = "#~@X+.YO?/";

	private final SerialAtom[] atoms;

	private String pathStr, path[], nested[], plobIds[], plobNames[];

	public PlobList() {

		atoms = new SerialAtom[4];
		atoms[0] = new SerialAtom(SerialAtom.TYPE_AS);
		atoms[1] = new SerialAtom(SerialAtom.TYPE_AS);
		atoms[2] = new SerialAtom(SerialAtom.TYPE_AS);
		atoms[3] = new SerialAtom(SerialAtom.TYPE_AS);

		pathStr = null;
		path = null;
		nested = null;
		plobIds = null;
		plobNames = null;
	}

	public void atomsHasBeenUpdated() {
		pathStr = null;
		path = atoms[0].as;
		nested = atoms[1].as;
		plobIds = atoms[2].as;
		plobNames = atoms[3].as;
	}

	public SerialAtom[] getAtoms() {
		return atoms;
	}

	public String getName() {
		if (path == null || path.length == 0) {
			return "Library";
		}
		return path[path.length - 1];
	}

	/** Get the name of the nested ploblist #<code>i</code> (starting from 0). */
	public String getNested(int i) {

		try {
			return nested[i];
		} catch (NullPointerException e) {
			return UNKNWON;
		} catch (ArrayIndexOutOfBoundsException e) {
			return UNKNWON;
		}
	}

	public int getNumNested() {

		return nested != null ? nested.length : 0;
	}

	public int getNumPlobs() {

		return plobIds != null ? plobIds.length : 0;
	}

	/** Get the ploblist's path as slash separated list. */
	public String getPath() {

		if (pathStr == null) {

			if (path == null || path.length == 0) {
				pathStr = "";
			}
			final StringBuffer sb = new StringBuffer();
			for (int i = 0; i < path.length; i++) {
				sb.append(path[i]).append('/');
			}
			sb.deleteCharAt(sb.length() - 1);
			pathStr = sb.toString();
			
		}
		return pathStr;
	}

	public String getPathForNested(int i) {

		if (i >= nested.length) {
			return UNKNWON;
		}

		final String base = getPath();
		if (base.length() == 0) {
			return nested[i];
		} else {
			return base + "/" + nested[i];
		}

	}

	/** Get the PID of the plob #<code>i</code> (starting from 0). */
	public String getPlobID(int i) {

		try {
			return plobIds[i];
		} catch (NullPointerException e) {
			return UNKNWON;
		} catch (ArrayIndexOutOfBoundsException e) {
			return UNKNWON;
		}
	}

	/** Get the name of the plob #<code>i</code> (starting from 0). */
	public String getPlobName(int i) {

		try {
			return plobNames[i];
		} catch (NullPointerException e) {
			return UNKNWON;
		} catch (ArrayIndexOutOfBoundsException e) {
			return UNKNWON;
		}
	}

	public String toString() {

		return "Ploblist: '" + getPath() + "'";
	}

	public void updateAtoms() {

		// not needed
		Log.bug("Feb 6, 2009.12:57:27 AM");

	}

}
