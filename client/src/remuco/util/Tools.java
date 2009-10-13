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
package remuco.util;

import java.util.Hashtable;
import java.util.Random;
import java.util.Vector;

/**
 * Miscellaneous J2ME specific utility constants and methods.
 * 
 * @author Oben Sonne
 * 
 */
public final class Tools {

	/** A global random number generator instance. */
	public static final Random RANDOM = new Random();

	/**
	 * Build a one element service list containing the connection URL for a
	 * service where all required URL parameters are known.
	 * 
	 * @param proto
	 *            URL parameter
	 * @param addr
	 *            URL parameter
	 * @param port
	 *            URL parameter
	 * @param options
	 *            URL parameter (may be <code>null</code> or empty string)
	 * 
	 * @return a hash table mapping the generic service name <em>Player</em> to
	 *         an URL build from the given parameters
	 */
	public static Hashtable buildManualServiceList(String proto, String addr,
			String port, String options) {

		final StringBuffer url = new StringBuffer(proto);

		url.append("://");
		url.append(addr);
		url.append(':');
		url.append(port);

		if (options != null && options.length() > 0) {
			if (options.charAt(0) != ';') {
				url.append(';');
			}
			url.append(options);
		}

		final Hashtable services = new Hashtable(1);

		services.put("Player", url.toString());

		return services;
	}

	/**
	 * Compare two byte arrays.
	 * 
	 * @param ba1
	 *            (may be null)
	 * @param ba2
	 *            (may be null)
	 * @return <code>true</code> if the arrays equal, <code>false</code>
	 *         otherwise
	 */
	public static boolean compare(byte[] ba1, byte[] ba2) {

		if (ba1 == ba2)
			return true;
		if (ba1 == null || ba2 == null)
			return false;
		if (ba1.length != ba2.length)
			return false;

		for (int i = 0; i < ba2.length; i++) {
			if (ba1[i] != ba2[i]) {
				return false;
			}

		}
		return true;
	}

	/**
	 * Compare two integer arrays.
	 * 
	 * @param ia1
	 *            (may be null)
	 * @param ia2
	 *            (may be null)
	 * @return <code>true</code> if the arrays equal, <code>false</code>
	 *         otherwise
	 */
	public static boolean compare(int[] ia1, int[] ia2) {

		if (ia1 == ia2)
			return true;
		if (ia1 == null || ia2 == null)
			return false;
		if (ia1.length != ia2.length)
			return false;

		for (int i = 0; i < ia1.length; i++) {
			if (ia1[i] != ia2[i])
				return false;
		}

		return true;

	}

	/**
	 * Compare two string arrays.
	 * 
	 * @param sa1
	 *            (may be null)
	 * @param sa2
	 *            (may be null)
	 * @return <code>true</code> if the arrays equal, <code>false</code>
	 *         otherwise
	 */
	public static boolean compare(String[] sa1, String[] sa2) {

		if (sa1 == sa2)
			return true;
		if (sa1 == null || sa2 == null)
			return false;
		if (sa1.length != sa2.length)
			return false;

		for (int i = 0; i < sa1.length; i++) {
			if (!sa1[i].equals(sa2[i]))
				return false;
		}

		return true;

	}

	/**
	 * Compare two vectors.
	 * 
	 * @param v1
	 *            (may be null)
	 * @param v2
	 *            (may be null)
	 * @return <code>true</code> if the vectors equal, <code>false</code>
	 *         otherwise
	 */
	public static boolean compare(Vector v1, Vector v2) {

		if (v1 == v2)
			return true;
		if (v1 == null || v2 == null)
			return false;
		if (v1.size() != v2.size())
			return false;

		for (int i = 0; i < v1.size(); i++) {
			if (!v1.elementAt(i).equals(v2.elementAt(i)))
				return false;
		}

		return true;

	}

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
