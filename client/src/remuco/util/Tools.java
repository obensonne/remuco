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
package remuco.util;

import java.io.UnsupportedEncodingException;
import java.util.Vector;

/**
 * Misc Tools (expacially for J2ME classes).
 * 
 * @author Christian Buennig
 * 
 */
public abstract class Tools {

	/**
	 * Canonical names of the encodings supported by J2SE 1.3.
	 */
	private static final String encs[] = new String[] { "ASCII", "Cp1252",
			"ISO8859_1", "UnicodeBig", "UnicodeBigUnmarked", "UnicodeLittle",
			"UnicodeLittleUnmarked", "UTF8", "UTF-16", "Big5", "Big5_HKSCS",
			"Cp037", "Cp273", "Cp277", "Cp278", "Cp280", "Cp284", "Cp285",
			"Cp297", "Cp420", "Cp424", "Cp437", "Cp500", "Cp737", "Cp775",
			"Cp838", "Cp850", "Cp852", "Cp855", "Cp856", "Cp857", "Cp858",
			"Cp860", "Cp861", "Cp862", "Cp863", "Cp864", "Cp865", "Cp866",
			"Cp868", "Cp869", "Cp870", "Cp871", "Cp874", "Cp875", "Cp918",
			"Cp921", "Cp922", "Cp930", "Cp933", "Cp935", "Cp937", "Cp939",
			"Cp942", "Cp942C", "Cp943", "Cp943C", "Cp948", "Cp949", "Cp949C",
			"Cp950", "Cp964", "Cp970", "Cp1006", "Cp1025", "Cp1026", "Cp1046",
			"Cp1097", "Cp1098", "Cp1112", "Cp1122", "Cp1123", "Cp1124",
			"Cp1140", "Cp1141", "Cp1142", "Cp1143", "Cp1144", "Cp1145",
			"Cp1146", "Cp1147", "Cp1148", "Cp1149", "Cp1250", "Cp1251",
			"Cp1253", "Cp1254", "Cp1255", "Cp1256", "Cp1257", "Cp1258",
			"Cp1381", "Cp1383", "Cp33722", "EUC_CN", "EUC_JP", "EUC_JP_LINUX",
			"EUC_KR", "EUC_TW", "GBK", "ISO2022CN", "ISO2022CN_CNS",
			"ISO2022CN_GB", "ISO2022JP", "ISO2022KR", "ISO8859_2", "ISO8859_3",
			"ISO8859_4", "ISO8859_5", "ISO8859_6", "ISO8859_7", "ISO8859_8",
			"ISO8859_9", "ISO8859_13", "ISO8859_15_FDIS", "JIS0201", "JIS0208",
			"JIS0212", "JISAutoDetect", "Johab", "KOI8_R", "MS874", "MS932",
			"MS936", "MS949" };

	/**
	 * Detect which encodings are supported by the current J2ME implementation.
	 * 
	 * @return a String array of the supported encodings (a subset of
	 *         {@link #encs})
	 */
	public static String[] getSupportedEncodings() {
		String ea[];
		Vector v = new Vector();
		int i, n;
		for (i = 0; i < encs.length; i++) {
			try {
				"test".getBytes(encs[i]);
				v.addElement(encs[i]);
			} catch (UnsupportedEncodingException e) {
			}
		}

		n = v.size();
		ea = new String[n];
		for (i = 0; i < n; i++) {
			ea[i] = (String) v.elementAt(i);
		}

		return ea;
	}

	/**
	 * This is a very bad random number generator, but it is enough if just a
	 * <em>taste</em> of random is needed.
	 * 
	 * @param upper
	 * @return a number <code>y</code> with <code>0 &lt;= y &lt; upper</code>
	 */
	public static long random(long upper) {
		return (long) ((double) (System.currentTimeMillis() % 2141)
				/ (double) 2141 * upper);
	}

	/**
	 * Sleep a while. {@link InterruptedException} gets catched.
	 * 
	 * @param ms
	 */
	public static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Sleep some random time. {@link InterruptedException} gets catched.
	 * 
	 * @param ms
	 *            maximum time to sleep
	 * @see #random(long)
	 * @see #sleep(long)
	 */
	public static void sleepRandom(long ms) {
		sleep(random(ms));
	}

	public static String[] splitString(String s, String splitter) {
		String ret[];
		int first, last, spl, sal;

		spl = splitter.length();

		first = s.indexOf(splitter);
		sal = 1;
		while (first >= 0) {
			sal++;
			first = s.indexOf(splitter, first + spl);
		}
		ret = new String[sal];

		first = 0;
		last = s.indexOf(splitter);
		for (int i = 0; i < sal; i++) {
			if ((last = s.indexOf(splitter, first)) < 0) {
				last = s.length();
			}
			ret[i] = s.substring(first, last);
			first = last + spl;
		}
		return ret;
	}

}
