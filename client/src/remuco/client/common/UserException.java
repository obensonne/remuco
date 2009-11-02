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
package remuco.client.common;

/**
 * User exceptions are used for raising errors up to the user interface (e.g. if
 * a connection is broken). They should describe errors in a user friendly way.
 * <p>
 * The normal use case is to catch low level exceptions (e.g. IO errors),
 * translate them to a user exception and then throw this exception. Of course
 * this only works if some upper code (in exception handling terms) has access
 * to the UI to finally show the error to the user.
 */
public final class UserException extends Exception {

	private String error, details;

	/**
	 * Create a new user exception.
	 * 
	 * @param error
	 *            the error title (short)
	 * @param details
	 *            optional details of the error
	 */
	public UserException(String error, String details) {

		this(error, details, null);
	}

	/**
	 * Create a new user exception.
	 * 
	 * @param error
	 *            the error title (short)
	 * @param details
	 *            optional details of the error
	 * @param ex
	 *            an optional exception, whose message will be included in the
	 *            details
	 */
	public UserException(String error, String details, Exception ex) {

		this.error = error;

		if (details == null) {
			if (ex != null && ex.getMessage() != null) {
				this.details = ex.getMessage();
			} else {
				this.details = "";
			}
		} else {
			if (ex != null && ex.getMessage() != null) {
				if (details.endsWith(".")) {
					details = details.substring(0, details.length() - 1);
				}
				this.details = details + " (" + ex.getMessage() + ").";
			} else {
				this.details = details;
			}
		}

	}

	/** Get the error details. */
	public String getDetails() {
		return details;
	}

	/** Get the error title. */
	public String getError() {
		return error;
	}

}
