package remuco.player;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.microedition.lcdui.Image;

import remuco.Remuco;
import remuco.comm.IStructuredData;
import remuco.util.Log;
import remuco.util.Tools;

/**
 * A Plob is a <i>playable object</i>, which could be a song, a video or ..
 * well, any object you can 'play' somehow in a corresponding player application
 * (what about 'playing' photos in a slide show app?).
 * <p>
 * Plob meta information is stored as properties. You can set any meta
 * infromation you like. However, when adding standard information (artist,
 * genre, ..) use constants <code>META_...</code> as meta data keys, e.g.
 * {@link #META_ARTIST}.
 * 
 * @author Christian Buennig
 */

public final class Plob implements IStructuredData {

	public static final String META_ALBUM = "album";

	public static final String META_ARTIST = "artist";

	public static final String META_BITRATE = "bitrate";

	public static final String META_COMMENT = "comment";

	public static final String META_GENRE = "genre";

	public static final String META_LENGTH = "length";

	public static final String META_TAGS = "tags";

	public static final String META_TITLE = "title";

	public static final String META_TRACK = "track";

	public static final String META_YEAR = "year";

	private static final String META_RATING = "rating";

	/** Type of the plob, e.g. 'song', 'video', 'photo', ... */
	private static final String META_TYPE = "__type__";

	/** Meta information value for REM_PLOB_META_TYPE */
	private static final String META_TYPE_AUDIO = "audio";

	/** Meta information value for REM_PLOB_META_TYPE */
	private static final String META_TYPE_VIDEO = "video";

	/** Meta information value for REM_PLOB_META_TYPE */
	private static final String META_TYPE_OTHER = "other";

	public static final int TYPE_AUDIO = 1;

	public static final int TYPE_VIDEO = 2;

	public static final int TYPE_OTHER = 3;

	// META_ART = "__art__" is not needed by the client;

	public static final String META_ANY = "__any__";

	public static final String PID_ANY = "__XXX__";

	public static final int[] sdFormatVector = new int[] { DT_STR, 1, DT_SV, 1,
			DT_BA, 1 };

	private Image img;

	private Hashtable meta = new Hashtable(10);

	private String pid;

	/**
	 * New plob with PID {@link #PID_ANY}.
	 * 
	 * @param tags
	 *            the tags
	 */
	public Plob() {
		pid = PID_ANY;
	}

	/**
	 * New plob with a specified PID.
	 * 
	 * @param pid
	 *            the plob's PID
	 */
	public Plob(String pid) {

		this.pid = pid;

	}

	public boolean equals(Object o) {

		if (this == o)
			return true;
		if (o == null)
			return false;
		if (!(o instanceof Plob))
			return false;

		Plob p = (Plob) o;

		if (meta.size() != p.meta.size())
			return false;

		String s, v;
		Enumeration enu = meta.keys();
		while (enu.hasMoreElements()) {
			s = (String) enu.nextElement();
			v = (String) meta.get(s);
			if (!v.equals(p.meta.get(s)))
				return false;
		}

		if (pid == p.pid)
			return true;

		if (pid == null || p.pid == null)
			return false;

		if (!pid.equals(p.pid))
			return false;

		if (img != p.img)
			return false; // XXX

		return true;
	}

	public Image getImg() {
		return img;
	}

	/**
	 * Get the plob's playling length (duration) in seconds.
	 * 
	 * @return the duration or -1 if length information is missing.
	 */
	public int getLen() {
		try {
			return Integer.parseInt(getMeta(Plob.META_LENGTH));
		} catch (NumberFormatException e) {
			return -1;
		} catch (NullPointerException e) {
			return -1;
		}
	}

	/**
	 * Get the plobs's playing length formatted.
	 * 
	 * @return the length in format 'mm:ss' or '' if no length information is
	 *         present.
	 */
	public String getLenFormatted() {
		StringBuffer sb = new StringBuffer();
		int l, s;
		try {
			l = Integer.parseInt(getMeta(Plob.META_LENGTH));
			s = l % 60;
			sb.append((int) (l / 60)).append(":");
			sb.append(s < 10 ? "0" : "").append(s);
		} catch (NumberFormatException e) {
			sb.append("");
		} catch (NullPointerException e) {
			sb.append("");
		}
		return sb.toString();
	}

	/**
	 * Read a meta information tag.
	 * 
	 * @param name
	 *            the meta info name to get the value from (see constants
	 *            <code>META_...</code> ..)
	 * @return the value of the tag named <code>name</code> or the empty
	 *         string if the tag is not set
	 */
	public String getMeta(String name) {

		String s = (String) meta.get(name);

		return s != null ? s : "";
	}

	public String getPid() {
		return pid;
	}

	/**
	 * Get the plob's rating.
	 * 
	 * @return the rating or 0 if rating information is missing.
	 */
	public int getRating() {
		try {
			return Integer.parseInt(getMeta(Plob.META_RATING));
		} catch (NumberFormatException e) {
			return 0;
		} catch (NullPointerException e) {
			return 0;
		}
	}

	public String[] getTags() {

		String tags = getMeta(META_TAGS);

		if (tags == null)
			return new String[0];

		return Tools.splitString(tags, ",");

	}

	/**
	 * Get the plob's type. The type is given by the meta information '{@value #META_TYPE}'.
	 * If this meta information is not present, it is assumed that the plob is
	 * an audio track.
	 * 
	 * @return the type (one of {@value #META_TYPE_AUDIO},
	 *         {@value #META_TYPE_VIDEO} or {@value #TYPE_OTHER}).
	 */
	public int getType() {

		String s = getMeta(META_TYPE);

		if (s.equals(META_TYPE_AUDIO))
			return TYPE_AUDIO;
		if (s.equals(META_TYPE_VIDEO))
			return TYPE_VIDEO;
		if (s.equals(META_TYPE_OTHER))
			return TYPE_OTHER;
		
		return TYPE_AUDIO;

	}

	public Object[] sdGet() {

		String s;
		String[] sv;
		String[][] svv;
		int i;
		Object[] bdv;
		Enumeration enu;

		bdv = new Object[3];

		sv = new String[] { pid };

		svv = new String[1][meta.size() * 2];
		i = 0;
		enu = meta.keys();
		while (enu.hasMoreElements()) {
			s = (String) enu.nextElement();
			svv[0][i] = s;
			svv[0][i + 1] = getMeta(s);
			i += 2;
		}

		bdv[0] = sv;
		bdv[1] = svv;
		bdv[2] = new byte[][] { null }; // don't send images back to server

		return bdv;
	}

	public void sdSet(Object[] bdv) {

		String[] sv;
		String[][] svv;
		byte[][] bav;

		sv = (String[]) bdv[0];
		pid = sv[0];

		meta.clear();
		svv = (String[][]) bdv[1];
		for (int i = 0; i < svv[0].length; i += 2) {

			meta.put(svv[0][i], svv[0][i + 1]);

		}

		bav = (byte[][]) bdv[2];
		if (bav[0] != null)
			try {
				img = Image.createImage(bav[0], 0, bav[0].length);
			} catch (Exception e) {
				Log.ln("[PLOB] creating image failed (" + e.getMessage() + ")");
				img = null;
			}
		else
			img = null;

		if (Remuco.EMULATION) {
			try {
				img = Image.createImage("/cover.test.png");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	public void setMeta(String name, String value) {
		meta.put(name, value != null ? value : "");
	}

	/**
	 * Sets rating. The value will be stored in tag {@link #META_RATING}.
	 * 
	 * @param rating
	 *            the plob's rating (&lt; <code>ratingMax</code>)
	 * @param ratingMax
	 *            the plob's maximum possible rating
	 * @see #TAG_RATING
	 */
	public void setRating(int rating) {
		meta.put(Plob.META_RATING, rating + "");
	}

	/**
	 * Set all tags of this plob. Discards all previous exisdting tags.
	 * Duplicates will not be added. Trims all tags.
	 * 
	 * @param tags
	 *            the new tags as a string, tags are comma seperated
	 */
	public void setTags(String tags) {

		String[] sa;

		sa = Tools.splitString(tags, ",");

		setTags(sa);

	}

	/**
	 * Set all tags of this plob. Discards all previous exisdting tags.
	 * Duplicates will not be added. Trims all tags.
	 * 
	 * @param tags
	 *            the new tags as a string array
	 */
	public void setTags(String[] tags) {

		int l, i;

		meta.remove(META_TAGS);

		l = tags.length;
		for (i = 0; i < l; i++) {
			tag(tags[i]);
		}

	}

	/**
	 * Adds a single tag to the plob's tag list. Duplicates will not be added.
	 * New tag gets trimmed.
	 * 
	 * @param tag
	 *            the tag to add
	 */
	public void tag(String tag) {

		String tags = getMeta(META_TAGS);
		int l, i;

		tag = tag.trim();

		if (tag.length() == 0)
			return;

		if (tags == null) {
			setMeta(META_TAGS, tag);
			return;
		}

		String[] tagList = Tools.splitString(tags, ",");
		l = tagList.length;
		for (i = 0; i < l; i++) {
			if (tagList[i].equals(tag))
				return;
		}

		if (tags.length() == 0)
			setMeta(META_TAGS, tag);
		else
			setMeta(META_TAGS, tags + "," + tag);

	}

	public String toString() {

		String s;
		StringBuffer sb = new StringBuffer("Plob: ").append(pid).append("\n");
		Enumeration enu = meta.keys();
		while (enu.hasMoreElements()) {
			s = (String) enu.nextElement();
			sb.append(s).append(": ");
			sb.append(meta.get(s)).append("\n");
		}

		return sb.toString();
	}

	protected void reset() {
		pid = "xxx";
		meta.clear();
		img = null;
	}

}
