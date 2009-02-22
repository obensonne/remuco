package remuco.util;

import remuco.Remuco;
import remuco.UserException;

public final class Log {

	private static ILogPrinter out = new ConsoleLogger();

	public static void bug(String id) {

		ln("[BUG] " + id);

	}

	public static void bug(String id, Exception ex) {

		ln("[BUG] " + id, ex);

	}

	public static void debug(String msg) {
		out.println(msg);
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
	 * Does a line break on the log sink.
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
	 * 
	 * @param msg
	 *            the log message
	 */
	public static void ln(String msg) {
		out.println(msg);
	}

	/**
	 * Logs an Exception. Output will be in the format 's ( e.getMessage)'
	 * 
	 * @param s
	 * @param e
	 */
	public static void ln(String s, Throwable e) {
		out.println(s + " (" + e.getMessage() + ")");
		if (Remuco.EMULATION) {
			out.println("----------------------- EX ----------------------");
			e.printStackTrace();
			out.println("-------------------------------------------------");
		}

	}

	/**
	 * Logs an {@link UserException}.
	 * 
	 * @param s
	 *            a prefix for the log
	 * @param e
	 *            the exception
	 * 
	 */
	public static void ln(String s, UserException e) {

		out.println(s + e.getError() + " (" + e.getDetails() + ")");
		if (Remuco.EMULATION) {
			out.println("----------------------- UE ----------------------");
			e.printStackTrace();
			out.println("-------------------------------------------------");
		}
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

}
