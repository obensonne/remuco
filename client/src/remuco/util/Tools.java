package remuco.util;

import java.io.UnsupportedEncodingException;
import java.util.Vector;

/**
 * Miscellaneous J2ME specific utility constants and methods.
 * 
 * @author Christian Buennig
 * 
 */
public final class Tools {

	/**
	 * The system's default character encoding.
	 */
	public static final String defaultEncoding = System
			.getProperty("microedition.encoding");;

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
	 * Character encodings supported by the used J2ME implementation.
	 */
	private static String[] encsSupported;

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

	/**
	 * Detect which encodings are supported by the used J2ME implementation.
	 * 
	 * @return a String array of the supported encodings (a subset of
	 *         {@link #encs})
	 */
	public static String[] getSupportedEncodings() {

		if (encsSupported != null)
			return encsSupported;

		Vector v = new Vector();
		int i;

		v.addElement(defaultEncoding);

		for (i = 0; i < encs.length; i++) {
			try {
				"test".getBytes(encs[i]);
				v.addElement(encs[i]);
			} catch (UnsupportedEncodingException e) {
			} catch (Exception e) {
				// System.out.println(e.getMessage());
			}
		}

		encsSupported = new String[v.size()];
		v.copyInto(encsSupported);

		return encsSupported;
	}

	/**
	 * Get a random number.
	 * <p>
	 * This is a very bad random number generator, but it is enough if just a
	 * <em>taste</em> of random is needed. Not good to get a sequence of
	 * random numbers!
	 * 
	 * @param upper
	 * @return a number <code>y</code> with <code>0 &le; y &lt; upper</code>
	 */
	public static long random(long upper) {
		return (long) ((double) (System.currentTimeMillis() % 2141)
				/ (double) 2141 * upper);
	}

	/**
	 * Sleep a while. {@link InterruptedException} gets catched but sleeping
	 * won't be continued.
	 * 
	 * @param ms
	 */
	public static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			//e.printStackTrace();
		}
	}

	/**
	 * Sleep some random time. {@link InterruptedException} gets catched but
	 * sleeping won't be continued.
	 * 
	 * @param ms
	 *            maximum time to sleep
	 * 
	 * @see #random(long)
	 * @see #sleep(long)
	 */
	public static void sleepRandom(long ms) {
		sleep(random(ms));
	}

	/**
	 * Splits a string into a string array.
	 * @param s
	 * @param splitter
	 * @return
	 */
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
