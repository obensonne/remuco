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
package remuco.client.common.util;

import java.util.Random;

/**
 * Miscellaneous utility constants and methods.
 * <p>
 * Some of the methods already exist in standard Java, but not JavaME! Classes
 * in package <em>remuco.client.common</em> must use the helper methods provided
 * here to work in JavaME as well.
 */
public final class Tools {

	/** A global random number generator instance. */
	public static final Random RANDOM = new Random();

	/** Format a time in seconds to something like 'mm:ss'. */
	public static String formatTime(int seconds) {
		final StringBuffer sb = new StringBuffer();
		if (seconds < 0) {
			return "";
		}
		final int s = seconds % 60;
		sb.append((int) (seconds / 60)).append(":");
		sb.append(s < 10 ? "0" : "").append(s);
		return sb.toString();
	}

	/**
	 * Get the first index of an object within an object array. Object equality
	 * is checked by {@link Object#equals(Object)}.
	 * 
	 * @param array
	 * @param element
	 * @return the index number or -1 if <i>element</i> is not contained within
	 *         <i>array</i>
	 */
	public static int getIndex(Object array[], Object element) {
		for (int i = 0; i < array.length; i++) {
			if (element.equals(array[i])) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Sleep a while. {@link InterruptedException} gets caught but sleeping
	 * won't be continued.
	 * 
	 * @param ms
	 */
	public static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			// e.printStackTrace();
		}
	}

	/**
	 * Splits a string into a string array.
	 * 
	 * @param s
	 *            the string to split (must not be <code>null</code>)
	 * @param splitter
	 *            the char to split the string at
	 * @param trim
	 *            if <code>true</code>, each split string will be trimmed
	 * @return a string array, if <em>s</em> does not contain <em>splitter</em>
	 *         then the array contains only one element which is <em>s</em>
	 *         itself (also if <em>s</em> is an empty string)
	 */
	public static String[] splitString(String s, char splitter, boolean trim) {

		int first, last, sal;

		first = s.indexOf(splitter);
		sal = 1;
		while (first >= 0) {
			sal++;
			first = s.indexOf(splitter, first + 1);
		}
		final String ret[] = new String[sal];

		first = 0;
		last = s.indexOf(splitter);
		for (int i = 0; i < sal; i++) {
			if ((last = s.indexOf(splitter, first)) < 0) {
				last = s.length();
			}
			ret[i] = s.substring(first, last);
			if (trim) {
				ret[i] = ret[i].trim();
			}
			first = last + 1;
		}
		return ret;
	}

}
