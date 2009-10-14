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
package remuco;

/**
 * An exception containing a error describing message suitable to display it to
 * the user.
 * <p>
 * <em>Note:</em> The precompiled exceptions do not provide valid stack traces.
 * They only provide some textual information.
 */
public final class UserException extends Exception {

	private String error, details;

	public UserException(String error, String details) {

		this(error, details, null);
	}

	/**
	 * Create a new user exception.
	 * 
	 * @param error
	 *            the error title
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

	public String getDetails() {
		return details;
	}

	public String getError() {
		return error;
	}

}
