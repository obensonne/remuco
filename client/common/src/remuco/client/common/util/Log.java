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

import remuco.client.common.UserException;

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
	}

	/**
	 * Logs a {@link UserException}.
	 * 
	 * @param s
	 *            a prefix for the log
	 * @param e
	 *            the exception
	 * 
	 */
	public static void ln(String s, UserException e) {
		out.println(s + e.getError() + " (" + e.getDetails() + ")");
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
