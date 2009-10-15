/*   
 *   Remuco - A remote control system for media players.
 *   Copyright (C) 2006-2009 by the Remuco team, see AUTHORS.
 *
 *   This file is part of Remuco.
 *
 *   Remuco is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Remuco is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Remuco.  If not, see <http://www.gnu.org/licenses/>.
 *   
 */
package remuco.player;

import java.util.Enumeration;
import java.util.Hashtable;

import javax.microedition.lcdui.Image;

import remuco.comm.ISerializable;
import remuco.comm.SerialAtom;
import remuco.util.Log;

/**
 * A Item is a <i>playable object</i>, which could be a song, a video or ..
 * well, any object you can 'play' somehow in a corresponding player application
 * (what about 'playing' photos in a slide show app?).
 * <p>
 * Item meta information is stored as properties. You can set any meta
 * Information you like. However, when adding standard information (artist,
 * genre, ..) use constants <code>META_...</code> as meta data keys, e.g.
 * {@link #META_ARTIST}.
 */

public final class Item implements ISerializable {

	public static final String ID_ANY = "__XXX__";

	public static final String ID_NONE = "__NO_SONG__";

	public static final String META_ABSTRACT = "__abstract__";

	public static final String META_ALBUM = "album";

	public static final String META_ARTIST = "artist";

	public static final String META_BITRATE = "bitrate";

	public static final String META_COMMENT = "comment";

	public static final String META_GENRE = "genre";

	public static final String META_LENGTH = "length";

	public static final String META_TAGS = "tags";

	public static final String META_TITLE = "title";

	public static final String META_TITLE_VALUE_NONE = "None";

	public static final String META_TRACK = "track";

	public static final String META_YEAR = "year";

	public static final int TYPE_AUDIO = 1;

	public static final int TYPE_OTHER = 3;

	public static final int TYPE_VIDEO = 2;

	private static final int[] ATOMS_FMT = new int[] { SerialAtom.TYPE_S,
			SerialAtom.TYPE_AS, SerialAtom.TYPE_AY };

	private static final String META_RATING = "rating";

	private static final String META_TYPE = "type";

	private static final String META_TYPE_AUDIO = "audio";

	private static final String META_TYPE_OTHER = "other";

	private static final String META_TYPE_VIDEO = "video";

	private final SerialAtom[] atoms;

	private String id = ID_NONE;

	private Image img;

	private final Hashtable meta;

	/**
	 * Create a new item with ID {@link #ID_NONE} and title
	 * {@link #META_TITLE_VALUE_NONE}.
	 * <p>
	 * Whenever this item gets updated as a result of deserialization (see
	 * {@link #notifyAtomsUpdated()}), ID and title get set to the values above
	 * fi they are missing in the deserialized data.
	 */
	public Item() {

		atoms = SerialAtom.build(ATOMS_FMT);

		meta = new Hashtable(10);

		setMeta(META_TITLE, META_TITLE_VALUE_NONE);
	}

	public SerialAtom[] getAtoms() {
		return atoms;
	}

	/**
	 * Get the ID of the item.
	 * 
	 * @return the ID or {@link #ID_ANY} if ID is missing
	 */
	public String getId() {

		return id != null ? id : ID_ANY;
	}

	/**
	 * Get the item's image.
	 * 
	 * @return the image or <code>null</code> is image is missing
	 */
	public Image getImg() {
		return img;
	}

	/**
	 * Get the item's playing length (duration) in seconds.
	 * 
	 * @return the duration or -1 if length information is missing.
	 */
	public int getLen() {
		try {
			return Integer.parseInt(getMeta(Item.META_LENGTH));
		} catch (NumberFormatException e) {
			return -1;
		} catch (NullPointerException e) {
			return -1;
		}
	}

	/**
	 * Get the item's playing length formatted.
	 * 
	 * @return the length in format 'mm:ss' or the empty string length is
	 *         missing
	 */
	public String getLenFormatted() {
		final StringBuffer sb = new StringBuffer();
		final int l, s;
		try {
			l = Integer.parseInt(getMeta(Item.META_LENGTH));
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
	 * @return the value of the tag named <code>name</code> or the empty string
	 *         if the tag is not set
	 */
	public String getMeta(String name) {

		final String s = (String) meta.get(name);

		return s != null ? s : "";
	}

	/**
	 * Get the item's rating.
	 * 
	 * @return the rating or 0 if rating information is missing.
	 */
	public int getRating() {
		try {
			return Integer.parseInt(getMeta(Item.META_RATING));
		} catch (NumberFormatException e) {
			return 0;
		} catch (NullPointerException e) {
			return 0;
		}
	}

	/**
	 * Get the item's type. The type is given by the meta information '{@value
	 * #META_TYPE}'. If this meta information is not present, it is assumed that
	 * the item is an audio track.
	 * 
	 * @return the type (one of {@value #META_TYPE_AUDIO}, {@value
	 *         #META_TYPE_VIDEO} or {@value #TYPE_OTHER}).
	 */
	public int getType() {

		final String s = getMeta(META_TYPE);

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

	/** Check if this item actually represents <i>no</i> item. */
	public boolean isNone() {

		return getId().equals(ID_NONE);

	}

	public void notifyAtomsUpdated() {

		id = atoms[0].s;

		final int meta_num = atoms[1].as.length / 2;

		meta.clear();
		for (int i = 0; i < meta_num; i++) {

			meta.put(atoms[1].as[2 * i], atoms[1].as[2 * i + 1]);

		}
		if (meta_num == 0) {
			id = ID_NONE;
			setMeta(META_TITLE, META_TITLE_VALUE_NONE);
		}

		if (atoms[2].ay.length > 0) {
			try {
				img = Image.createImage(atoms[2].ay, 0, atoms[2].ay.length);
			} catch (Exception e) {
				Log.ln("[IT] creating image failed", e);
				img = null;
			}
		} else {
			img = null;
		}
	}

	public void setMeta(String name, String value) {
		meta.put(name, value != null ? value : "");
	}

	/**
	 * Sets rating. The value will be stored in tag {@link #META_RATING}.
	 * 
	 * @param rating
	 *            the item's rating (&lt; <code>ratingMax</code>)
	 * @param ratingMax
	 *            the item's maximum possible rating
	 * @see #TAG_RATING
	 */
	public void setRating(int rating) {
		meta.put(Item.META_RATING, rating + "");
	}

	/**
	 * Set all tags of this item. Discards all previous existing tags.
	 * Duplicates will not be added. Trims all tags.
	 * 
	 * @param tags
	 *            the new tags as a string, tags are comma separated
	 */
	public void setTags(String tags) {

		final String[] sa = Tagging.splitAndTrim(tags);

		final StringBuffer sb = new StringBuffer();
		for (int i = 0; i < sa.length; i++) {
			sb.append(sa[i]).append(',');
		}
		if (sa.length > 0) {
			sb.deleteCharAt(sb.length() - 1);
		}
		setMeta(META_TAGS, sb.toString());

	}

	public String toString() {

		String s;
		final StringBuffer sb = new StringBuffer("Item: ");
		sb.append(getId()).append("\n");
		final Enumeration enu = meta.keys();
		while (enu.hasMoreElements()) {
			s = (String) enu.nextElement();
			sb.append(s).append(": ");
			sb.append(meta.get(s)).append("\n");
		}

		return sb.toString();
	}

}
