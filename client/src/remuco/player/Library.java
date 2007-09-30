package remuco.player;

import remuco.comm.IStructuredData;
import remuco.util.Log;

/**
 * The player's library. Actually just a list of ploblists.
 * 
 * @author Christian Buennig
 * 
 */
public final class Library implements IStructuredData {

	public static final int FLAG_EDITABLE = 0x0001;

	public static final int[] sdFormatVector = new int[] { DT_SV, 2, DT_IV, 1 };

	private int[][] flags;

	private String[][] plidsAndNames;

	public Library() {
		plidsAndNames = new String[2][];
		flags = new int[1][];
	}

	public int getLength() {

		return flags[0].length;

	}

	public int getPloblistFlags(int i) {

		return flags[0][i];

	}

	public String getPloblistName(int i) {

		return plidsAndNames[1][i];

	}

	public String getPloblistPlid(int i) {

		return plidsAndNames[0][i];

	}

	public Object[] sdGet() {

		Log.asssertNotReached();

		return null;

	}

	public void sdSet(Object[] bdv) {

		plidsAndNames = (String[][]) bdv[0];

		flags = (int[][]) bdv[1];

//		if (plidsAndNames[0] == null || plidsAndNames[1] == null
//				|| flags[0] == null) {
//			throw new BinaryDataExecption("some library attributes are null");
//		}
//		if (plidsAndNames[0].length != plidsAndNames[1].length
//				|| plidsAndNames[0].length != flags[0].length) {
//			throw new BinaryDataExecption(
//					"some library attributes format error");
//		}

	}

	 public String toString() {

		StringBuffer sb = new StringBuffer("Library: ");

		for (int i = 0; i < plidsAndNames[0].length; i++) {
			sb.append(plidsAndNames[0][i]).append(" - ");
			sb.append(plidsAndNames[1][i]).append(" - ");
			sb.append(flags[0][i]).append(", ");
		}

		return sb.toString();
	}

	protected void reset() {

		plidsAndNames = new String[2][];
		flags = new int[1][];

	}

}
