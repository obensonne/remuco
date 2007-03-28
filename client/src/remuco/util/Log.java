package remuco.util;

public class Log {

	private static ILogPrinter out = new ConsoleLogger();

	private static final int PACKSEP = ".".charAt(0);

	/**
	 * Same as {@link #ln(Object, String)}, but with <i>no</i> line break.
	 * 
	 * @param o
	 * @param msg
	 */
	public static void l(Object o, String msg) {
		out.print(toClassName(o) + ": " + msg);
	}

	/**
	 * Logs a message with <i>no</i> line break.
	 * 
	 * @param msg
	 *            the log message
	 */
	public static void l(String msg) {
		out.print(msg);
	}

	/**
	 * Does a linebreak on the log sink.
	 * 
	 */
	public static void ln() {
		out.println();
	}

	/**
	 * Logs a byte array as hex values.
	 * 
	 * @param ba
	 *            the byte array to log
	 */
	public static void ln(byte[] ba) {
		for (int i = 0; i < ba.length; i++) {
			out.print(Integer.toHexString(ba[i]) + " ");
		}
		out.println();
	}

	/**
	 * Logs a message with a line break.
	 * <p>
	 * <i>Note:</i> This method should not used to log frequent events (for
	 * performance reasons).
	 * 
	 * @param o
	 *            the short class name of this object will prefix the log
	 *            message
	 * @param msg
	 *            the log message
	 */
	public static void ln(Object o, String msg) {
		out.println(toClassName(o) + ": " + msg);
	}

	/**
	 * Logs a message with a line break.
	 * 
	 * @param msg
	 *            the log message
	 */
	public static void ln(String msg) {
		out.println(msg);
	}

	/**
	 * Sets the log sink.
	 * 
	 * @param out
	 *            the out sink to use for log messages
	 */
	public static void setOut(ILogPrinter out) {
		if (out == null)
			throw new NullPointerException("null logger not supported");
		Log.out = out;
	}

	private static String toClassName(Object o) {
		String s;
		int i;
		StringBuffer sb = new StringBuffer("[");
		if (o == null) {
			s = "X";
		} else if (o instanceof Class) {
			s = ((Class) o).getName();
		} else {
			s = o.getClass().getName();
		}
		i = s.lastIndexOf(PACKSEP) + 1;
		if (i > 0) {
			sb.append(s.substring(i));
		}
		sb.append("]");

		return sb.toString();
	}

}
