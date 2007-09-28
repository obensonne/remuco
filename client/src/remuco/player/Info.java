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

	/**
	 * Means the player proxy can give us a playlist of the player.
	 */
	public static final int FEATURE_PLAYLIST = 0x0001;

	/**
	 * The PP is able to replace the content of the playlist by a new list of
	 * plobs.
	 * <p>
	 * <b>FUTURE FEATURE !</b> (not yet implemented)
	 */
	public static final int FEATURE_PLAYLIST_EDIT = 0x0002;

	/**
	 * The PP can jump to a specific position within the playlist.
	 */
	public static final int FEATURE_PLAYLIST_JUMP = 0x0004;

	/**
	 * The player has a repeat mode which repeats the current plob and the PP
	 * supports this.
	 */
	public static final int FEATURE_PLAYLIST_MODE_REPEAT_ONE_PLOB = 0x0008;

	/**
	 * The player has a repeat mode which repeats the current album and the PP
	 * supports this.
	 */
	public static final int FEATURE_PLAYLIST_MODE_REPEAT_ALBUM = 0x0010;

	/**
	 * The player has a repeat mode which repeats the whole playlist and the PP
	 * supports this.
	 */
	public static final int FEATURE_PLAYLIST_MODE_REPEAT_PLAYLIST = 0x0020;

	/**
	 * The player has a shuffle mode and the PP supports this.
	 */
	public static final int FEATURE_PLAYLIST_MODE_SHUFFLE = 0x0040;

	/**
	 * The player has a queue and the PP can read the queue contents.
	 */
	public static final int FEATURE_QUEUE = 0x0080;

	/**
	 * The PP is able to replace the content of the queue by a new list of
	 * plobs.
	 * <p>
	 * <b>FUTURE FEATURE !</b> (not yet implemented)
	 */
	public static final int FEATURE_QUEUE_EDIT = 0x0100;

	/**
	 * The PP can jump to a specific position within the queue.
	 */
	public static final int FEATURE_QUEUE_JUMP = 0x0200;

	/**
	 * The PP can alter the meta information of a plob.
	 */
	public static final int FEATURE_PLOB_EDIT = 0x0400;

	/**
	 * The player supports tags (e.g. as in XMMS2) and the PP supports this.
	 */
	public static final int FEATURE_PLOB_TAGS = 0x0800;

	/**
	 * The PP can seek to a specific postion within the current song.
	 * <p>
	 * <b>FUTURE FEATURE !</b> (not yet implemented)
	 */
	public static final int FEATURE_SEEK = 0x1000;

	/**
	 * The player supports rating and the PP supports this.
	 */
	public static final int FEATURE_RATE = 0x2000;

	/**
	 * The PP is able to use a specifc plob as the 'play next candidate'. This
	 * is used for votings to play a specific plob after the current without
	 * rumbeling the playlist too much. The plob may change often before it
	 * acutally should get played (i.e. when the current song finishes).
	 * <p>
	 * <b>FUTURE FEATURE !</b> (not yet implemented)
	 */
	public static final int FEATURE_PLAY_NEXT_CANDIDATE = 0x4000;

	/**
	 * The player supports searching for certain plobs and the PP supports this.
	 * <p>
	 * <b>FUTURE FEATURE !</b> (not yet implemented)
	 */
	public static final int FEATURE_SEARCH = 0x8000;

	/**
	 * The player and player proxy can provide a library, i.e. a list of preset
	 * ploblists and these ploblists can be loaded to be the new playlist.
	 */
	public static final int FEATURE_LIBRARY = 0x10000;
	public static final int FEATURE_LIBRARY_PLOBLIST_CONTENT = 0x20000;

	public static final Info UNKNOWN = new Info();

	public static final int[] sdFormatVector = new int[] { DT_STR, 1, DT_INT,
			3, DT_BA, 1 };

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

	public int getRatingNone() {
		return iv[2];
	}

	public boolean hasFeature(int feature) {

		return (iv[0] & feature) != 0;

	}

	public void removeChangeLister(IInfoListener il) {
		changeListener.removeElement(il);
	}

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
