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
	private static final String defaultEncoding = System
			.getProperty("microedition.encoding");

	/**
	 * Some character encodings (it is really a mess to get a universal valid
	 * list of character encoding names understood by all: Java, <i>libiconv</i>
	 * and <i>glibc-iconv</i>). libiconv supports the encodings listed <a
	 * href="http://www.gnu.org/software/libiconv/">here</a>, but this is not
	 * the same supported by glibc-iconv. However, the following list seems to
	 * be a sufficient common list.
	 */
	private static final String[] encs = new String[] { "UTF-8", "UTF-16",
			"UTF-16BE", "UTF-16LE", "UTF-32BE", "UTF-32LE", "ISO-8859-1",
			"ISO-8859-2", "ISO-8859-3", "ISO-8859-4", "ISO-8859-5",
			"ISO-8859-6", "ISO-8859-7", "ISO-8859-8", "ISO-8859-9",
			"ISO-8859-10", "ISO-8859-11", "ISO-8859-12", "ISO-8859-13",
			"ISO-8859-14", "ISO-8859-15", "ISO-8859-16", "BIG5", "BIG5-HKSCS",
			"CP037", "CP273", "CP277", "CP278", "CP280", "CP284", "CP285",
			"CP297", "CP420", "CP424", "CP437", "CP500", "CP737", "CP775",
			"CP838", "CP850", "CP852", "CP855", "CP856", "CP857", "CP858",
			"CP860", "CP861", "CP862", "CP863", "CP864", "CP865", "CP866",
			"CP868", "CP869", "CP870", "CP871", "CP874", "CP875", "CP918",
			"CP921", "CP922", "CP930", "CP933", "CP935", "CP937", "CP939",
			"CP942", "CP942C", "CP943", "CP943C", "CP948", "CP949", "CP949C",
			"CP950", "CP964", "CP970", "CP1006", "CP1025", "CP1026", "CP1046",
			"CP1097", "CP1098", "CP1112", "CP1122", "CP1123", "CP1124",
			"CP1140", "CP1141", "CP1142", "CP1143", "CP1144", "CP1145",
			"CP1146", "CP1147", "CP1148", "CP1149", "CP1250", "CP1251",
			"CP1252", "CP1253", "CP1254", "CP1255", "CP1256", "CP1257",
			"CP1258", "CP1381", "CP1383", "CP33722", "EUC-CN", "EUC-JP",
			"EUC-JP-LINUX", "EUC-KR", "EUC-TW", "GBK", "ISO-2022-CN",
			"ISO-2022-CN-CNS", "ISO-2022-CN-EXT", "ISO-2022-CN-GB",
			"ISO-2022-JP", "ISO-2022-KR", "SJIS", "JIS0208", "JIS0212",
			"JOHAB", "KOI8-R", "KOI8-U", "KOI8-RU", "KOI8-T", "MS874", "MS932",
			"MS936", "MS949", "ASCII" };

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
	 * Get the default encoding to use. If supported, this is UTF-8, otherwise
	 * it is the value of the system property <code>microedition.encoding</code>.
	 * 
	 * @return the default encoding
	 */
	public static String getDefaultEncoding() {

		getSupportedEncodings();

		return encsSupported[0];
	}

	/**
	 * Get a random number.
	 * <p>
	 * This is a very bad random number generator, but it is enough if just a
	 * <em>taste</em> of random is needed. Not good to get a <em>sequence</em>
	 * of random numbers!
	 * 
	 * @param upper
	 * @return a number <code>y</code> with <code>0 &le; y &lt; upper</code>
	 */
	public static long random(long upper) {
		return (long) ((double) (System.currentTimeMillis() % 2141)
				/ (double) 2141 * upper);
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
	 * Sleep some random time. {@link InterruptedException} gets caught but
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
	 * 
	 * @param s
	 * @param splitter
	 * @return
	 */
	public static String[] splitString(String s, String splitter) {
		String[] ret;
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

	/**
	 * Detect which encodings are supported by the used J2ME implementation.
	 * 
	 * @return a String array of the supported encodings (a subset of
	 *         {@link #encs})
	 */
	private static String[] getSupportedEncodings() {

		final Vector v;
		int i;

		if (encsSupported != null)
			return encsSupported;

		Log.ln("[TL] def enc: " + defaultEncoding);

		v = new Vector();
		v.addElement(defaultEncoding);

		for (i = 0; i < encs.length; i++) {
			if (defaultEncoding.equalsIgnoreCase(encs[i]))
				continue;
			try {
				"test".getBytes(encs[i]);
				if (encs[i].equals("UTF-8"))
					v.insertElementAt(encs[i], 0);
				else
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

}
