package remuco.player;

import java.util.Enumeration;
import java.util.Vector;

import remuco.comm.IStructuredData;
import remuco.util.Log;
import remuco.util.Tools;

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
public final class PlobList implements IStructuredData {

	/** PLID of the playlist */
	public static final String PLID_PLAYLIST = "__PLAYLIST__";

	/** PLID of the queue */
	public static final String PLID_QUEUE = "__QUEUE__";

	public static final int[] sdFormatVector = new int[] { DT_STR, 2, DT_SV, 1 };

	private final boolean editable = false;

	private String name, plid;

	private Vector plobs;

	public PlobList() {
		this("", "");
	}

	public PlobList(String plid, String name) {

		Log.asssert(plid);
		// Assert.assert(name);

		this.plid = plid;
		this.name = name;

		plobs = new Vector();

	}

	public void addPlob(String pid, String title) {

		plobs.addElement(pid);
		plobs.addElement(title);

	}

	public PlobList clone() {

		PlobList pl;
		Enumeration enu;

		pl = new PlobList(plid, name);

		enu = plobs.elements();
		while (enu.hasMoreElements()) {
			pl.plobs.addElement(enu.nextElement());
		}

		return pl;

	}

	public boolean equals(Object o) {
		if (o == null)
			return false;
		if (this == o)
			return true;
		if (!(o instanceof PlobList))
			return false;

		PlobList pl = (PlobList) o;

		if (!plid.equals(pl.plid))
			return false;
		if (!name.equals(pl.name))
			return false;

		if (!Tools.compare(plobs, pl.plobs))
			return false;

		return true;

	}

	public int getLength() {
		return plobs.size() / 2;
	}

	public String getName() {
		return name;
	}

	public String getPlid() {
		return plid;
	}

	public String getPlobPid(int i) {

		return (String) plobs.elementAt(2 * i);

	}

	public String getPlobTitle(int i) {

		return (String) plobs.elementAt(2 * i + 1);

	}

	public void insertPlob(String pid, String title, int pos) {

		plobs.insertElementAt(pid, 2 * pos);
		plobs.insertElementAt(title, 2 * pos + 1);

	}

	public final boolean isEditable() {
		return editable;
	}

	public void removePlob(int pos) {

		plobs.removeElementAt(2 * pos);
		plobs.removeElementAt(2 * pos);
	}

	public void removePlob(String pid) {

		int pos;

		do {
			pos = plobs.indexOf(pid);
		} while (pos != -1 && pos % 2 != 0);

		Log.asssert(pos >= 0);

		plobs.removeElementAt(pos);
		plobs.removeElementAt(pos);

	}

	public Object[] sdGet() {

		Object[] bdv = new Object[2];
		String[] sv = new String[2];
		String[][] svv = new String[1][];

		sv[0] = plid;
		sv[1] = name;

		svv[0] = new String[plobs.size()];
		plobs.copyInto(svv[0]);

		bdv[0] = sv;
		bdv[1] = svv;

		return bdv;

	}

	public void sdSet(Object[] bdv) {

		String[] sv;
		String[][] svv;

		sv = (String[]) bdv[0];

		plid = sv[0];
		name = sv[1];

		svv = (String[][]) bdv[1];

		plobs.removeAllElements();
		for (int i = 0; i < svv[0].length; i++) {
			plobs.addElement(svv[0][i]);
		}

	}

	public String toString() {

		StringBuffer sb = new StringBuffer("Ploblist: ").append(plid);
		sb.append(" (").append(name).append(")\n");

		Enumeration enu;

		enu = plobs.elements();
		sb.append("Plobs in list:\n");
		while (enu.hasMoreElements()) {
			sb.append((String) enu.nextElement()).append(" - ");
			sb.append((String) enu.nextElement()).append("\n");
		}

		return sb.toString();
	}

	protected void reset() {
		name = "";
		plid = Plob.PID_ANY;
		plobs.removeAllElements();
	}

}
