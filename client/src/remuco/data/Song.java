/*
 * Copyright (C) 2006 Christian Buennig - See COPYING
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 * 
 */
package remuco.data;

import remuco.proto.Remuco;
import remuco.util.Tools;

/**
 * Class to represent a song. Song information is simply stored properties-like
 * as tags. You can add any tag you like to a song. However, when adding
 * standard tags (artist, genre, ..) use constants <code>TAG_...</code> as tag
 * names.
 * 
 * @author Christian Buennig
 * 
 */
public class Song {

    public static final int RATING_NONE = Integer
            .parseInt(Remuco.REM_TAG_VAL_RATING_UNRATED);

    public static final String TAG_ALBUM = Remuco.REM_TAG_NAME_ALBUM;

    public static final String TAG_ARTIST = Remuco.REM_TAG_NAME_ARTIST;

    public static final String TAG_BITRATE = Remuco.REM_TAG_NAME_BITRATE;

    public static final String TAG_COMMENT = Remuco.REM_TAG_NAME_COMMENT;

    public static final String TAG_GENRE = Remuco.REM_TAG_NAME_GENRE;

    public static final String TAG_LENGTH = Remuco.REM_TAG_NAME_LENGTH;

    public static final String TAG_RATING = Remuco.REM_TAG_NAME_RATING;

    public static final String TAG_TITLE = Remuco.REM_TAG_NAME_TITLE;

    public static final String TAG_TRACK = Remuco.REM_TAG_NAME_TRACK;

    public static final String TAG_UID = Remuco.REM_TAG_NAME_UID;

    public static final String TAG_YEAR = Remuco.REM_TAG_NAME_YEAR;

    private Tags tags;

    public Song() {
        tags = new Tags();
    }

    /**
     * Constructs a song using a set of tags.
     * 
     * @param tags
     *            the tags
     */
    public Song(Tags tags) {
        this.tags = tags;
    }

    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null)
            return false;
        if (!(o instanceof Song))
            return false;
        Song s = (Song) o;
        return tags.equals(s.tags);
    }

    /**
     * Get the song's playling length in seconds.
     * @return the duration or -1 if length information is missing.
     */
    public synchronized int getLen() {
        try {
            return Integer.parseInt(getTag(Song.TAG_LENGTH));
        } catch (NumberFormatException e) {
            return -1;
        } catch (NullPointerException e) {
            return -1;
        }
    }

    /**
     * Get the songs's playing length formatted: 'mm:ss' or '??' if no length
     * information is present.
     */
    public synchronized String getLenFormatted() {
        StringBuffer sb = new StringBuffer();
        int l, s;
        try {
            l = Integer.parseInt(getTag(Song.TAG_LENGTH));
            s = l % 60;
            sb.append((int) (l / 60)).append(":");
            sb.append(s < 10 ? "0" : "").append(s);
        } catch (NumberFormatException e) {
            sb.append("??");
        } catch (NullPointerException e) {
            sb.append("??");
        }
        return sb.toString();
    }

    /**
     * Get the song's rating. This is just a specific interpretation of the tag
     * {@link #TAG_RATING}.
     * 
     * @see #TAG_RATING
     */
    public int getRating() {
        String[] sa = Tools.splitString(tags.get(Song.TAG_RATING), "/");
        if (sa.length == 2) {
            try {
                return Integer.parseInt(sa[0]);
            } catch (NumberFormatException e) {
                return RATING_NONE;
            }
        } else {
            return RATING_NONE;
        }
    }

    /**
     * Get the song's maximum possible rating. This is just a specific
     * interpretation of the tag {@link #TAG_RATING}.
     * 
     * @see #TAG_RATING
     */
    public int getRatingMax() {
        String[] sa = Tools.splitString(tags.get(Song.TAG_RATING), "/");
        if (sa.length == 2) {
            try {
                return Integer.parseInt(sa[1]);
            } catch (NumberFormatException e) {
                return RATING_NONE;
            }
        } else {
            return RATING_NONE;
        }
    }

    /**
     * Get value of a specific tag. Use constants <code>TAG_...</code> !
     * 
     * @param tag
     *            a specific tag (e.g. {@link Song#TAG_ARTIST})
     * @return value of the tag
     */
    public String getTag(String tag) {
        return tags.get(tag);
    }

    public String[] getTagList() {
        return tags.list();
    }

    /**
     * Sets rating partameters. The values will be stored in tag
     * {@link #TAG_RATING}.
     * 
     * @param rating
     *            the song's rating (&lt; <code>ratingMax</code>)
     * @param ratingMax
     *            the song's maximum possible rating
     * @see #TAG_RATING
     */
    public synchronized void setRating(int rating, int ratingMax) {
        tags.add(Song.TAG_RATING, rating + "/" + ratingMax);
    }

    public synchronized void setTag(String name, String value) {
        tags.add(name, value);
    }
    
}
