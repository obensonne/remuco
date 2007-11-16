package remuco.player;

import java.util.Enumeration;
import java.util.Vector;

import javax.microedition.lcdui.Image;

import remuco.comm.IStructuredData;
import remuco.util.Tools;

public final class Info implements IStructuredData {

	/**
	 * @emulator Only used for testing!
	 */
	public static final Info EMULATOR = newEmulatorInfo();

	/** Inspect the current playlist */
	public static final int FEATURE_PLAYLIST = 1 << 0;

	/** Edit the current playlist. <b>FUTURE FEATURE !</b> */
	public static final int FEATURE_PLAYLIST_EDIT = 1 << 1;

	/** Jump to a specific song in the current playlist */
	public static final int FEATURE_PLAYLIST_JUMP = 1 << 2;

	/** Show repeat and shuffle status */
	public static final int FEATURE_REPEAT_MODE_PLOB = 1 << 3;

	public static final int FEATURE_REPEAT_MODE_ALBUM = 1 << 4;

	public static final int FEATURE_REPEAT_MODE_PL = 1 << 5;

	public static final int FEATURE_SHUFFLE_MODE = 1 << 6;

	/** Inspect the play queue */
	public static final int FEATURE_QUEUE = 1 << 7;

	/** Edit the play queue. <i>FUTURE FEATURE</i> */
	public static final int FEATURE_QUEUE_EDIT = 1 << 8;

	/** Jump to a specific song in the play queue */
	public static final int FEATURE_QUEUE_JUMP = 1 << 9;

	/** Edit the meta information of plobs */
	public static final int FEATURE_PLOB_EDIT = 1 << 10;

	public static final int FEATURE_PLOB_TAGS = 1 << 11;

	/** Seek to position within the current plob. <b>FUTURE FEATURE !</b> */
	public static final int FEATURE_SEEK = 1 << 12;

	/** Rate plobs */
	public static final int FEATURE_RATE = 1 << 13;

	/** <b>FUTURE FEATURE !</b> */
	public static final int FEATURE_PLAY_NEXT = 1 << 14;

	/** Search plobs. <b>FUTURE FEATURE !</b> */
	public static final int FEATURE_SEARCH = 1 << 15;

	/** Show predefined ploblists and make them the new playlist */
	public static final int FEATURE_LIBRARY = 1 << 16;

	/** Show content of a predefined ploblist */
	public static final int FEATURE_LIBRARY_PL_CONTENT = 1 << 17;

	/**
	 * Play a certain ploblists.
	 * 
	 * @deprecated Redundant since this is required if {@link #FEATURE_LIBRARY}
	 *             is set.
	 */
	public static final int FEATURE_LIBRARY_PL_PLAY = 1 << 18;

	/** Edit any ploblist (not only playlist or queue). */
	public static final int FEATURE_PLOBLIST_EDIT = 1 << 19;

	public static final Info UNKNOWN = new Info();

	public static final int[] sdFormatVector = new int[] { DT_STR, 1, DT_INT,
			2, DT_BA, 1 };

	/**
	 * @emulator Only used for testing!
	 */
	private static Info newEmulatorInfo() {

		Info i = new Info();

		i.sv[0] = "Emulator";
		i.iv[1] = 10;
		i.iv[0] |= FEATURE_PLOB_EDIT;
		i.iv[0] |= FEATURE_RATE;
		i.iv[0] |= FEATURE_PLAYLIST;
		i.iv[0] |= FEATURE_QUEUE;
		i.iv[0] |= FEATURE_PLAYLIST_JUMP;

		return i;

	}

	private Image icon;

	private int[] iv = new int[3];

	private String[] sv = new String[1];

	private final Vector changeListener = new Vector();

	protected Info() {
		reset();
	}

	public void addChangeListener(IInfoListener il) {
		changeListener.addElement(il);
	}

	public boolean equals(Object obj) {

		if (obj == null)
			return false;

		if (!(obj instanceof Info))
			return false;

		Info i = (Info) obj;

		if (!Tools.compare(sv, i.sv))
			return false;

		if (!Tools.compare(iv, i.iv))
			return false;

		if (icon == i.icon)
			return true;

		if (icon == null || i.icon == null)
			return false;

		if (!icon.equals(i.icon))
			return false;

		return true;

	}

	public int getFeatures() {
		return iv[0];
	}

	public Image getIcon() {
		return icon;
	}

	public String getPlayerName() {
		return sv[0];
	}

	public int getRatingMax() {
		return iv[1];
	}

	public boolean hasFeature(int feature) {

		return (iv[0] & feature) != 0;

	}

	public void removeChangeLister(IInfoListener il) {
		changeListener.removeElement(il);
	}

	/**
	 * @deprecated Only used for testing!
	 */
	public Object[] sdGet() {

		Object[] bdv = new Object[3];

		byte[][] bav;

		bav = new byte[][] { null };

		bdv[0] = sv;
		bdv[1] = iv;
		bdv[2] = bav;

		return bdv;
	}

	public void sdSet(Object[] bdv) {

		byte[][] bav;

		sv = (String[]) bdv[0];
		iv = (int[]) bdv[1];
		bav = (byte[][]) bdv[2];

		if (bav[0] != null)
			icon = Image.createImage(bav[0], 0, bav[0].length);
		else
			icon = null;

		Enumeration enu = changeListener.elements();
		while (enu.hasMoreElements())
			((IInfoListener) enu.nextElement()).infoChanged();

	}

	protected void reset() {

		for (int i = 0; i < iv.length; i++) {
			iv[i] = 0;
		}

		sv[0] = "No Player";

		icon = null;

		Enumeration enu = changeListener.elements();
		while (enu.hasMoreElements())
			((IInfoListener) enu.nextElement()).infoChanged();

	}
}
