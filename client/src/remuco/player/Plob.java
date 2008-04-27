package remuco.player;

import java.util.Enumeration;
import java.util.Hashtable;

import javax.microedition.lcdui.Image;

import remuco.comm.ISerializable;
import remuco.comm.SerialAtom;
import remuco.util.Log;
import remuco.util.Tools;

/**
 * A Plob is a <i>playable object</i>, which could be a song, a video or ..
 * well, any object you can 'play' somehow in a corresponding player application
 * (what about 'playing' photos in a slide show app?).
 * <p>
 * Plob meta information is stored as properties. You can set any meta
 * Information you like. However, when adding standard information (artist,
 * genre, ..) use constants <code>META_...</code> as meta data keys, e.g.
 * {@link #META_ARTIST}.
 * 
 * @author Christian Buennig
 */

public final class Plob implements ISerializable {

	public static final String ID_ANY = "__XXX__";

	public static final String META_ABSTRACT = "__abstract__";

	public static final String META_ALBUM = "album";

	public static final String META_ARTIST = "artist";

	public static final String META_BITRATE = "bitrate";

	public static final String META_COMMENT = "comment";

	public static final String META_GENRE = "genre";

	public static final String META_ID = "__id__";

	public static final String META_LENGTH = "length";

	public static final String META_TAGS = "tags";

	public static final String META_TITLE = "title";

	public static final String META_TRACK = "track";

	public static final String META_YEAR = "year";

	public static final int TYPE_AUDIO = 1;

	public static final int TYPE_OTHER = 3;

	public static final int TYPE_VIDEO = 2;

	// META_ART = "__art__" is not needed by the client;

	private static final String META_RATING = "rating";

	private static final String META_TYPE = "type";

	private static final String META_TYPE_AUDIO = "audio";

	private static final String META_TYPE_OTHER = "other";

	private static final String META_TYPE_VIDEO = "video";

	private final SerialAtom[] atoms;

	private Image img;

	private final Hashtable meta;

	/**
	 * New plob with ID {@link #ID_ANY}.
	 * 
	 * @param tags
	 *            the tags
	 */
	public Plob() {

		atoms = new SerialAtom[2];
		atoms[0] = new SerialAtom(SerialAtom.TYPE_AS);
		atoms[1] = new SerialAtom(SerialAtom.TYPE_AY);

		meta = new Hashtable(10);
	}

	public void atomsHasBeenUpdated() {

		int meta_num;

		meta_num = atoms[0].as.length / 2;

		meta.clear();
		for (int i = 0; i < meta_num; i++) {

			meta.put(atoms[0].as[2 * i], atoms[0].as[2 * i + 1]);

		}

		if (atoms[1].ay.length > 0)
			try {
				img = Image.createImage(atoms[1].ay, 0, atoms[1].ay.length);
			} catch (Exception e) {
				Log.ln("[PLOB] creating image failed", e);
				img = null;
			}
		else
			img = null;
	}

	public SerialAtom[] getAtoms() {
		return atoms;
	}

	/**
	 * Get the ID of the plob.
	 * 
	 * @return the ID or {@link #ID_ANY} if ID is missing
	 */
	public String getId() {

		String id;

		id = getMeta(META_ID);

		return id != null ? id : ID_ANY;
	}

	/**
	 * Get the plob's image.
	 * 
	 * @return the image or <code>null</code> is image is missing
	 */
	public Image getImg() {
		return img;
	}

	/**
	 * Get the plob's playing length (duration) in seconds.
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
	 * @return the length in format 'mm:ss' or the empty string length is
	 *         missing
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

	public boolean hasAbstract() {

		return meta.get(META_ABSTRACT) != null;
	}

	public boolean hasTags() {

		return meta.get(META_TAGS) != null;
	}

	public boolean isEmpty() {

		return meta.size() == 0;

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
		StringBuffer sb = new StringBuffer("Plob: ");
		sb.append(getId()).append("\n");
		Enumeration enu = meta.keys();
		while (enu.hasMoreElements()) {
			s = (String) enu.nextElement();
			sb.append(s).append(": ");
			sb.append(meta.get(s)).append("\n");
		}

		return sb.toString();
	}

	public void updateAtoms() {

		// not needed
		Log.asssertNotReached(this);

	}

	/** Clear meta data (incl. ID) and set image to <code>null</code>. */
	protected void reset() {
		meta.clear();
		img = null;
	}

}
