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

	/** Well known path for the currently active playlist. */
	public static final String PATH_PLAYLIST[] = new String[] { "__PLAYLIST__" };

	/** Well known path for the currently active playlist as string. */
	public static final String PATH_PLAYLIST_S = PATH_PLAYLIST[0];

	/** Well known path for the queue. */
	public static final String PATH_QUEUE[] = new String[] { "__QUEUE__" };

	/** Well known path for the queue as string. */
	public static final String PATH_QUEUE_S = PATH_QUEUE[0];

	private static final String UNKNWON = "#~@X+.YO?/";

	private static String pathArrayToString(String path[]) {

		if (path == null || path.length == 0) {
			return "";
		}

		final StringBuffer sb = new StringBuffer();

		for (int i = 0; i < path.length; i++) {
			sb.append(path[i]).append('/');
		}
		sb.deleteCharAt(sb.length() - 1);

		return sb.toString();
	}

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

	/**
	 * Get the name of the ploblist (which is the last element in the list's
	 * path).
	 * 
	 * @return the name
	 */
	public String getName() {
		if (isLibraryRoot()) {
			return "Library";
		}
		if (isPlaylist()) {
			return "Playlist";
		}
		if (isQueue()) {
			return "Queue";
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

	/**
	 * Get the ploblist's parent list path as a slash separated list.
	 * 
	 * @return the parent path or <code>null</code> if this ploblist is the
	 *         playlist, queue or library root
	 */
	public String getParentPath() {

		if (isPlaylist() || isQueue() || isLibraryRoot()) {
			return null;
		}

		final String parent[] = new String[path.length - 1];

		for (int i = 0; i < parent.length; i++) {
			parent[i] = path[i];
		}

		return pathArrayToString(parent);

	}

	/** Get the ploblist's path as slash separated list. */
	public String getPath() {

		if (pathStr == null) {
			pathStr = pathArrayToString(path);
		}
		return pathStr;
	}

	/**
	 * Get the ploblist's path as a string array.
	 * 
	 * @return the path elements (never <code>null</code>)
	 */
	public String[] getPathElements() {
		return path == null ? new String[0] : path;
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

	public boolean isLibraryRoot() {
		return path == null || path.length == 0;
	}

	public boolean isPlaylist() {
		return path != null && path.length == 1
				&& path[0].equals(PATH_PLAYLIST[0]);
	}

	public boolean isQueue() {
		return path != null && path.length == 1
				&& path[0].equals(PATH_QUEUE[0]);
	}

	public String toString() {

		return "Ploblist: '" + getPath() + "'";
	}

	public void updateAtoms() {

		// not needed
		Log.bug("Feb 6, 2009.12:57:27 AM");

	}
}
